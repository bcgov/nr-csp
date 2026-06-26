package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionDetailResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionHistoryRowResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionInvoiceResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionLineItemResponse;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for the Submission History screens.
 *
 * <p>Native Oracle SQL against the {@code THE} schema. Column/table mappings
 * mirror the legacy CSP model (csp_submission, electronic_submission,
 * client_location, coastal_log_sale, coastal_log_sale_detail) and the existing
 * {@code InvoiceRepository} / {@code LineItemRepository}.</p>
 *
 * <ul>
 *   <li>{@link #search} — the paged list (one row per submission).</li>
 *   <li>{@link #findDetail} — a submission header plus its invoices and the
 *       flattened list of all their line items.</li>
 * </ul>
 *
 * <p>Submitter name and contact details come from the related electronic
 * submission and client location: {@code submitted_by}/{@code email_id} from
 * {@code electronic_submission}, phone/email from {@code client_location}. The
 * admin comment is the first non-null {@code reviewer_notes} across the
 * submission's invoices.</p>
 */
@Repository
public class SubmissionHistoryRepository {

    // Shared joins: status description, client name, electronic submission
    // (submitter name + email), client location (email + phone).
    private static final String LIST_QUERY = """
            SELECT sub.csp_submission_id                                                       AS csp_submission_id,
                   sub.entry_timestamp                                                         AS entry_timestamp,
                   COALESCE(es.submitted_by, sub.entry_userid)                                 AS submitted_by,
                   sub.client_number                                                           AS client_number,
                   fc.client_name                                                              AS client_name,
                   subStatus.description                                                       AS submission_status,
                   (SELECT cmt.reviewer_notes
                      FROM THE.coastal_log_sale cmt
                     WHERE cmt.csp_submission_id = sub.csp_submission_id
                       AND cmt.reviewer_notes IS NOT NULL
                       AND ROWNUM = 1)                                                         AS comment_text
            FROM   THE.csp_submission sub
            INNER JOIN THE.csp_submission_status_code subStatus
                    ON sub.csp_submission_status_code = subStatus.csp_submission_status_code
            LEFT JOIN THE.forest_client fc
                    ON sub.client_number = fc.client_number
            LEFT JOIN THE.electronic_submission es
                    ON sub.submission_id = es.submission_id
            WHERE 1=1
            """;

    // API sort key → SQL alias from the SELECT (Oracle allows ORDER BY alias).
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "submissionDate",   "entry_timestamp",
            "submittedBy",      "submitted_by",
            "clientName",       "client_name",
            "submissionStatus", "submission_status"
    );

    private static final String DEFAULT_ORDER_BY = "entry_timestamp DESC";

    private static final String DETAIL_QUERY = """
            SELECT sub.csp_submission_id                                                       AS csp_submission_id,
                   sub.submission_id                                                           AS submission_id,
                   sub.entry_timestamp                                                         AS entry_timestamp,
                   COALESCE(es.submitted_by, sub.entry_userid)                                 AS submitted_by,
                   subStatus.description                                                       AS submission_status,
                   sub.client_number                                                           AS client_number,
                   fc.client_name                                                              AS client_name,
                   sub.client_locn_code                                                        AS client_locn_code,
                   COALESCE(es.email_id, cl.email_address)                                     AS email,
                   COALESCE(cl.business_phone, cl.cell_phone, cl.home_phone)                   AS telephone,
                   sub.month_complete_ind                                                      AS month_complete_ind,
                   CASE WHEN EXISTS (
                            SELECT 1 FROM THE.coastal_log_sale s
                             WHERE s.csp_submission_id = sub.csp_submission_id
                               AND s.seller_client_number = sub.client_number
                               AND s.seller_client_locn_code = sub.client_locn_code)
                        THEN 'Y' ELSE 'N' END                                                  AS seller_submission_ind,
                   (SELECT cmt.reviewer_notes
                      FROM THE.coastal_log_sale cmt
                     WHERE cmt.csp_submission_id = sub.csp_submission_id
                       AND cmt.reviewer_notes IS NOT NULL
                       AND ROWNUM = 1)                                                         AS admin_comment
            FROM   THE.csp_submission sub
            INNER JOIN THE.csp_submission_status_code subStatus
                    ON sub.csp_submission_status_code = subStatus.csp_submission_status_code
            LEFT JOIN THE.forest_client fc
                    ON sub.client_number = fc.client_number
            LEFT JOIN THE.electronic_submission es
                    ON sub.submission_id = es.submission_id
            LEFT JOIN THE.client_location cl
                    ON sub.client_number = cl.client_number
                   AND sub.client_locn_code = cl.client_locn_code
            WHERE  sub.csp_submission_id = :id
            """;

    private static final String DETAIL_INVOICES_QUERY = """
            SELECT inv.coastal_log_sale_id                                  AS coastal_log_sale_id,
                   inv.client_invoice_no                                    AS invoice_number,
                   inv.client_invoice_date                                  AS invoice_date,
                   inv.csp_invoice_type_code                                AS invoice_type,
                   inv.seller_client_number                                 AS seller_client_number,
                   inv.seller_client_locn_code                              AS seller_client_locn_code,
                   inv.buyer_client_number                                  AS buyer_client_number,
                   inv.buyer_client_locn_code                               AS buyer_client_locn_code,
                   inv.log_sale_type_code                                   AS maturity,
                   inv.log_sale_fob_location                                AS fob_location,
                   inv.client_total_invoice_amt                             AS total_amount,
                   inv.client_total_invoice_volume                          AS total_volume,
                   inv.client_total_invoice_pieces                          AS total_pieces
            FROM   THE.coastal_log_sale inv
            WHERE  inv.csp_submission_id = :id
            ORDER BY inv.coastal_log_sale_id
            """;

    private static final String DETAIL_LINE_ITEMS_QUERY = """
            SELECT inv.client_invoice_no                                    AS invoice_number,
                   sgx.log_sale_species_code                                AS species,
                   sgx.log_sale_grade_code                                  AS grade,
                   d.log_sale_sort_code                                     AS sort_code,
                   d.client_secondary_sort_code                            AS client_sort_code,
                   d.pieces                                                 AS pieces,
                   d.volume                                                 AS volume,
                   d.price                                                  AS price
            FROM   THE.coastal_log_sale_detail d
            INNER JOIN THE.coastal_log_sale inv
                    ON d.coastal_log_sale_id = inv.coastal_log_sale_id
            INNER JOIN THE.csp_species_grade_xref sgx
                    ON d.csp_species_grade_xref_id = sgx.csp_species_grade_xref_id
            WHERE  inv.csp_submission_id = :id
            ORDER BY inv.coastal_log_sale_id, d.coastal_log_sale_detail_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public SubmissionHistoryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Paged list of submissions. */
    public Page<SubmissionHistoryRowResponse> search(Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String orderBy = buildOrderBy(pageable.getSort());

        String dataSql = LIST_QUERY
                + " ORDER BY " + orderBy
                + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
        params.addValue("offset", pageable.getOffset());
        params.addValue("limit", pageable.getPageSize());

        String countSql = "SELECT count(*) FROM (" + LIST_QUERY + ") cnt";

        List<SubmissionHistoryRowResponse> content = jdbc.query(dataSql, params, (rs, rowNum) ->
                new SubmissionHistoryRowResponse(
                        RepositoryUtils.getLongNullable(rs, "csp_submission_id"),
                        RepositoryUtils.getLocalDateNullable(rs, "entry_timestamp"),
                        rs.getString("submitted_by"),
                        rs.getString("client_number"),
                        rs.getString("client_name"),
                        rs.getString("submission_status"),
                        rs.getString("comment_text")
                ));

        Long total = jdbc.queryForObject(countSql, params, Long.class);
        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    /** Loads a submission header plus its invoices and line items. */
    public Optional<SubmissionDetailResponse> findDetail(Long cspSubmissionId) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", cspSubmissionId);

        SubmissionDetailHeader header;
        try {
            header = jdbc.queryForObject(DETAIL_QUERY, params, (rs, rowNum) -> new SubmissionDetailHeader(
                    RepositoryUtils.getLongNullable(rs, "csp_submission_id"),
                    rs.getString("submission_id"),
                    RepositoryUtils.getLocalDateNullable(rs, "entry_timestamp"),
                    rs.getString("submitted_by"),
                    rs.getString("submission_status"),
                    rs.getString("client_number"),
                    rs.getString("client_name"),
                    rs.getString("client_locn_code"),
                    rs.getString("email"),
                    rs.getString("telephone"),
                    rs.getString("month_complete_ind"),
                    rs.getString("seller_submission_ind"),
                    rs.getString("admin_comment")
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
        if (header == null) {
            return Optional.empty();
        }

        List<SubmissionInvoiceResponse> invoices = jdbc.query(DETAIL_INVOICES_QUERY, params, (rs, rowNum) ->
                new SubmissionInvoiceResponse(
                        RepositoryUtils.getLongNullable(rs, "coastal_log_sale_id"),
                        rs.getString("invoice_number"),
                        RepositoryUtils.getLocalDateNullable(rs, "invoice_date"),
                        rs.getString("invoice_type"),
                        formatClient(rs.getString("seller_client_number"), rs.getString("seller_client_locn_code")),
                        formatClient(rs.getString("buyer_client_number"), rs.getString("buyer_client_locn_code")),
                        rs.getString("maturity"),
                        rs.getString("fob_location"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("total_volume"),
                        rs.getObject("total_pieces", Integer.class)
                ));

        List<SubmissionLineItemResponse> lineItems = jdbc.query(DETAIL_LINE_ITEMS_QUERY, params, (rs, rowNum) ->
                new SubmissionLineItemResponse(
                        rs.getString("invoice_number"),
                        rs.getString("species"),
                        rs.getString("grade"),
                        rs.getString("sort_code"),
                        rs.getString("client_sort_code"),
                        rs.getObject("pieces", Integer.class),
                        rs.getBigDecimal("volume"),
                        rs.getBigDecimal("price")
                ));

        return Optional.of(new SubmissionDetailResponse(
                header.cspSubmissionId(),
                header.submissionId(),
                header.submissionDate(),
                header.submittedBy(),
                header.submissionStatus(),
                header.clientNumber(),
                header.clientName(),
                header.clientLocnCode(),
                header.email(),
                header.telephone(),
                header.monthComplete(),
                header.sellerSubmission(),
                header.adminComment(),
                invoices,
                lineItems
        ));
    }

    /** Formats a client number + location code pair as "number/locn", or null when absent. */
    private String formatClient(String clientNumber, String locnCode) {
        if (clientNumber == null || clientNumber.isBlank()) {
            return null;
        }
        return locnCode == null || locnCode.isBlank() ? clientNumber : clientNumber + "/" + locnCode;
    }

    /**
     * Builds the ORDER BY clause from the Pageable sort using the whitelist.
     * Unknown sort keys throw a 400. A stable tiebreaker keeps paging deterministic.
     */
    private String buildOrderBy(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return DEFAULT_ORDER_BY + ", csp_submission_id DESC";
        }
        StringBuilder orderBy = new StringBuilder();
        for (Sort.Order order : sort) {
            String column = SORT_COLUMNS.get(order.getProperty());
            if (column == null) {
                throw new BadRequestException("Unsupported sort field: " + order.getProperty()
                        + ". Allowed values: " + String.join(", ", SORT_COLUMNS.keySet()));
            }
            if (!orderBy.isEmpty()) orderBy.append(", ");
            orderBy.append(column).append(' ').append(order.isAscending() ? "ASC" : "DESC");
        }
        orderBy.append(", csp_submission_id DESC");
        return orderBy.toString();
    }

    /** Internal carrier for the submission header row before invoices/line items are attached. */
    private record SubmissionDetailHeader(
            Long cspSubmissionId,
            String submissionId,
            java.time.LocalDate submissionDate,
            String submittedBy,
            String submissionStatus,
            String clientNumber,
            String clientName,
            String clientLocnCode,
            String email,
            String telephone,
            String monthComplete,
            String sellerSubmission,
            String adminComment
    ) {}
}
