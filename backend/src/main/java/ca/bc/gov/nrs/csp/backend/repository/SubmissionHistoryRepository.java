package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionDetailResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionHistoryRowResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionInvoiceCommentResponse;
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
 * <p>Submitter name comes from {@code forest_client} and {@code submitted_by}
 * from {@code electronic_submission} (falling back to the submission's entry
 * user). The submission's own email and telephone are not yet columns on
 * {@code csp_submission} (a planned addition); until they exist the detail
 * returns them as null rather than borrowing the client's contact info from
 * {@code client_location}. The admin comment is the first non-null
 * {@code reviewer_notes} across the submission's invoices.</p>
 */
@Repository
public class SubmissionHistoryRepository {

    // Result-set column aliases reused across the SELECTs, sort whitelist and row mappers.
    private static final String COL_ENTRY_TIMESTAMP = "entry_timestamp";
    private static final String COL_SUBMITTED_BY = "submitted_by";
    private static final String COL_CLIENT_NAME = "client_name";
    private static final String COL_SUBMISSION_STATUS = "submission_status";
    private static final String COL_INVOICE_NUMBER = "invoice_number";

    // Shared joins: status description, client name, electronic submission
    // (submitter name + email), client location (email + phone).
    private static final String LIST_QUERY = """
            SELECT sub.csp_submission_id                                                       AS csp_submission_id,
                   sub.entry_timestamp                                                         AS entry_timestamp,
                   COALESCE(es.submitted_by, sub.entry_userid)                                 AS submitted_by,
                   sub.client_number                                                           AS client_number,
                   fc.client_name                                                              AS client_name,
                   subStatus.description                                                       AS submission_status,
                   (SELECT COUNT(*)
                      FROM THE.coastal_log_sale ic
                     WHERE ic.csp_submission_id = sub.csp_submission_id)                       AS invoice_count,
                   (SELECT COUNT(*)
                      FROM THE.coastal_log_sale ic
                     WHERE ic.csp_submission_id = sub.csp_submission_id
                       AND ic.reviewer_notes IS NOT NULL)                                      AS commented_invoice_count
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
            "submissionDate",   COL_ENTRY_TIMESTAMP,
            "submittedBy",      COL_SUBMITTED_BY,
            "clientName",       COL_CLIENT_NAME,
            "submissionStatus", COL_SUBMISSION_STATUS
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
                   -- email/telephone are not yet columns on csp_submission (planned addition);
                   -- source them from sub.* once those columns exist. Null until then — do NOT
                   -- fall back to client_location, which holds the client's contact info, not
                   -- the submission's.
                   NULL                                                                        AS email,
                   NULL                                                                        AS telephone,
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
            WHERE  sub.csp_submission_id = :id
            """;

    // One row per invoice in the submission. The plain columns back the table
    // row; the joins + correlated subqueries supply the expandable per-invoice
    // detail panel. "Other party" is whichever side (buyer/seller) isn't the
    // submitting client — mirrors InvoiceRepository.mapLoadedInvoice. Its
    // name/city/province live in log_sale_participant ONLY for non-registered
    // parties; when the other party is a registered client (has a client
    // number) those participant columns are null, so we COALESCE to the
    // forest_client name and client_location city/province instead. The
    // LISTAGG subqueries flatten the log-source (boom/mark/weigh) and related-
    // invoice (replaces/adjusts) child rows the same way InvoiceRepository's
    // helper methods do.
    private static final String DETAIL_INVOICES_QUERY = """
            SELECT inv.coastal_log_sale_id                                  AS coastal_log_sale_id,
                   inv.client_invoice_no                                    AS invoice_number,
                   inv.client_invoice_date                                  AS invoice_date,
                   inv.csp_invoice_type_code                                AS invoice_type,
                   COALESCE(st.description, inv.log_sale_entry_status_code)  AS status,
                   inv.seller_client_number                                 AS seller_client_number,
                   inv.seller_client_locn_code                              AS seller_client_locn_code,
                   inv.buyer_client_number                                  AS buyer_client_number,
                   inv.buyer_client_locn_code                               AS buyer_client_locn_code,
                   inv.log_sale_type_code                                   AS maturity,
                   inv.log_sale_fob_location                                AS fob_location,
                   inv.client_total_invoice_amt                             AS total_amount,
                   inv.client_total_invoice_volume                          AS total_volume,
                   inv.client_total_invoice_pieces                          AS total_pieces,
                   inv.log_sale_sort_code                                   AS primary_sort_code,
                   inv.client_primary_sort_code                             AS client_primary_sort_code,
                   inv.submitter_notes                                      AS submitter_notes,
                   inv.reviewer_notes                                       AS staff_comment,
                   CASE WHEN inv.seller_client_number = sub.client_number
                             AND inv.seller_client_locn_code = sub.client_locn_code
                        THEN COALESCE(buyer_part.name, buyer_fc.client_name)
                        ELSE COALESCE(seller_part.name, seller_fc.client_name) END       AS other_party_name,
                   CASE WHEN inv.seller_client_number = sub.client_number
                             AND inv.seller_client_locn_code = sub.client_locn_code
                        THEN COALESCE(buyer_part.city, buyer_cl.city)
                        ELSE COALESCE(seller_part.city, seller_cl.city) END              AS other_party_city,
                   CASE WHEN inv.seller_client_number = sub.client_number
                             AND inv.seller_client_locn_code = sub.client_locn_code
                        THEN COALESCE(buyer_part.province, buyer_cl.province)
                        ELSE COALESCE(seller_part.province, seller_cl.province) END       AS other_party_prov_state,
                   (SELECT LISTAGG(rcls.client_invoice_no, ', ')
                              WITHIN GROUP (ORDER BY r.coastal_log_sale_rltd_invc_id)
                      FROM THE.coastal_log_sale_rltd_invc r
                      JOIN THE.coastal_log_sale rcls ON r.related_coastal_log_sale_id = rcls.coastal_log_sale_id
                     WHERE r.coastal_log_sale_id = inv.coastal_log_sale_id
                       AND r.csp_invoice_ref_type_code = 'REP')             AS replaces_invoice_numbers,
                   (SELECT LISTAGG(rcls.client_invoice_no, ', ')
                              WITHIN GROUP (ORDER BY r.coastal_log_sale_rltd_invc_id)
                      FROM THE.coastal_log_sale_rltd_invc r
                      JOIN THE.coastal_log_sale rcls ON r.related_coastal_log_sale_id = rcls.coastal_log_sale_id
                     WHERE r.coastal_log_sale_id = inv.coastal_log_sale_id
                       AND r.csp_invoice_ref_type_code = 'ADJ')             AS adjusts_invoice_numbers,
                   (SELECT LISTAGG(ls.source_document_reference, ', ')
                              WITHIN GROUP (ORDER BY ls.coastal_log_sale_log_source_id)
                      FROM THE.coastal_log_sale_log_source ls
                     WHERE ls.coastal_log_sale_id = inv.coastal_log_sale_id
                       AND ls.log_source_code = 'BOOM')                     AS boom_numbers,
                   (SELECT LISTAGG(ls.source_document_reference, ', ')
                              WITHIN GROUP (ORDER BY ls.coastal_log_sale_log_source_id)
                      FROM THE.coastal_log_sale_log_source ls
                     WHERE ls.coastal_log_sale_id = inv.coastal_log_sale_id
                       AND ls.log_source_code = 'MARK')                     AS timber_marks,
                   (SELECT LISTAGG(ls.source_document_reference, ', ')
                              WITHIN GROUP (ORDER BY ls.coastal_log_sale_log_source_id)
                      FROM THE.coastal_log_sale_log_source ls
                     WHERE ls.coastal_log_sale_id = inv.coastal_log_sale_id
                       AND ls.log_source_code = 'WEIGH')                    AS weigh_slips
            FROM   THE.coastal_log_sale inv
            INNER JOIN THE.csp_submission sub
                    ON inv.csp_submission_id = sub.csp_submission_id
            LEFT JOIN THE.log_sale_entry_status_code st
                    ON inv.log_sale_entry_status_code = st.log_sale_entry_status_code
            LEFT JOIN THE.log_sale_participant buyer_part
                    ON inv.buyer_log_sale_participant_id = buyer_part.log_sale_participant_id
            LEFT JOIN THE.log_sale_participant seller_part
                    ON inv.seller_log_sale_participant_id = seller_part.log_sale_participant_id
            LEFT JOIN THE.forest_client buyer_fc
                    ON inv.buyer_client_number = buyer_fc.client_number
            LEFT JOIN THE.forest_client seller_fc
                    ON inv.seller_client_number = seller_fc.client_number
            LEFT JOIN THE.client_location buyer_cl
                    ON inv.buyer_client_number = buyer_cl.client_number
                   AND inv.buyer_client_locn_code = buyer_cl.client_locn_code
            LEFT JOIN THE.client_location seller_cl
                    ON inv.seller_client_number = seller_cl.client_number
                   AND inv.seller_client_locn_code = seller_cl.client_locn_code
            WHERE  inv.csp_submission_id = :id
            ORDER BY inv.coastal_log_sale_id
            """;

    // Per-invoice status + reviewer comment for a submission, backing the
    // expanded "Invoice comments" sub-table. Status description join mirrors
    // SearchRepository; ordering matches DETAIL_INVOICES_QUERY.
    private static final String INVOICE_COMMENTS_QUERY = """
            SELECT inv.client_invoice_no                                    AS invoice_number,
                   COALESCE(st.description, inv.log_sale_entry_status_code)  AS status,
                   inv.reviewer_notes                                       AS comment_text
            FROM   THE.coastal_log_sale inv
            LEFT JOIN THE.log_sale_entry_status_code st
                    ON inv.log_sale_entry_status_code = st.log_sale_entry_status_code
            WHERE  inv.csp_submission_id = :id
            ORDER BY inv.coastal_log_sale_id
            """;

    private static final String DETAIL_LINE_ITEMS_QUERY = """
            SELECT inv.coastal_log_sale_id                                  AS coastal_log_sale_id,
                   inv.client_invoice_no                                    AS invoice_number,
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

        // orderBy is composed only from the SORT_COLUMNS whitelist and hardcoded
        // ASC/DESC keywords (see buildOrderBy); offset/limit are bound parameters.
        // No user-controlled value reaches the SQL string, so this is not injectable.
        List<SubmissionHistoryRowResponse> content = jdbc.query(dataSql, params, (rs, rowNum) -> // NOSONAR S2077 - ORDER BY is whitelist-only, not injectable
                new SubmissionHistoryRowResponse(
                        RepositoryUtils.getLongNullable(rs, "csp_submission_id"),
                        RepositoryUtils.getLocalDateNullable(rs, COL_ENTRY_TIMESTAMP),
                        rs.getString(COL_SUBMITTED_BY),
                        rs.getString("client_number"),
                        rs.getString(COL_CLIENT_NAME),
                        rs.getString(COL_SUBMISSION_STATUS),
                        rs.getObject("invoice_count", Integer.class),
                        rs.getObject("commented_invoice_count", Integer.class)
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
                    RepositoryUtils.getLocalDateNullable(rs, COL_ENTRY_TIMESTAMP),
                    rs.getString(COL_SUBMITTED_BY),
                    rs.getString(COL_SUBMISSION_STATUS),
                    rs.getString("client_number"),
                    rs.getString(COL_CLIENT_NAME),
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

        List<SubmissionInvoiceResponse> invoices = jdbc.query(DETAIL_INVOICES_QUERY, params, (rs, rowNum) ->
                new SubmissionInvoiceResponse(
                        RepositoryUtils.getLongNullable(rs, "coastal_log_sale_id"),
                        rs.getString(COL_INVOICE_NUMBER),
                        RepositoryUtils.getLocalDateNullable(rs, "invoice_date"),
                        rs.getString("invoice_type"),
                        rs.getString("status"),
                        formatClient(rs.getString("seller_client_number"), rs.getString("seller_client_locn_code")),
                        formatClient(rs.getString("buyer_client_number"), rs.getString("buyer_client_locn_code")),
                        rs.getString("maturity"),
                        rs.getString("fob_location"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("total_volume"),
                        rs.getObject("total_pieces", Integer.class),
                        rs.getString("replaces_invoice_numbers"),
                        rs.getString("adjusts_invoice_numbers"),
                        rs.getString("seller_client_locn_code"),
                        rs.getString("buyer_client_locn_code"),
                        rs.getString("other_party_name"),
                        rs.getString("other_party_city"),
                        rs.getString("other_party_prov_state"),
                        rs.getString("primary_sort_code"),
                        rs.getString("client_primary_sort_code"),
                        rs.getString("boom_numbers"),
                        rs.getString("timber_marks"),
                        rs.getString("weigh_slips"),
                        rs.getString("submitter_notes"),
                        rs.getString("staff_comment")
                ));

        List<SubmissionLineItemResponse> lineItems = jdbc.query(DETAIL_LINE_ITEMS_QUERY, params, (rs, rowNum) ->
                new SubmissionLineItemResponse(
                        RepositoryUtils.getLongNullable(rs, "coastal_log_sale_id"),
                        rs.getString(COL_INVOICE_NUMBER),
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

    /** Per-invoice status + reviewer comment for a submission's expanded "Invoice comments" sub-table. */
    public List<SubmissionInvoiceCommentResponse> findInvoiceComments(Long cspSubmissionId) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", cspSubmissionId);
        return jdbc.query(INVOICE_COMMENTS_QUERY, params, (rs, rowNum) ->
                new SubmissionInvoiceCommentResponse(
                        rs.getString(COL_INVOICE_NUMBER),
                        rs.getString("status"),
                        rs.getString("comment_text")
                ));
    }

    /** Formats a client number + location code pair as "number/locn", or null when absent. */
    static String formatClient(String clientNumber, String locnCode) {
        if (clientNumber == null || clientNumber.isBlank()) {
            return null;
        }
        return locnCode == null || locnCode.isBlank() ? clientNumber : clientNumber + "/" + locnCode;
    }

    /**
     * Builds the ORDER BY clause from the Pageable sort using the whitelist.
     * Unknown sort keys throw a 400. A stable tiebreaker keeps paging deterministic.
     */
    static String buildOrderBy(Sort sort) {
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
