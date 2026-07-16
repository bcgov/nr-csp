package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.service.model.InboxCriteria;
import ca.bc.gov.nrs.csp.backend.service.model.InboxRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Repository for the Inbox search screen.
 *
 * <p>Executes a native SQL query against THE.csp_submission (grouped per submission)
 * with invoice status counts derived from THE.coastal_log_sale. 
 */
@Repository
public class InboxRepository {

    // Base SELECT — no WHERE yet; criteria fragments are appended dynamically.
    // INNER JOIN to coastal_log_sale 
    // subStatus.description is in the SELECT and also in the GROUP BY — no MIN() needed.
    private static final String BASE_QUERY = """
            SELECT sub.csp_submission_id                                                            AS csp_submission_id,
                   MIN(inv.coastal_log_sale_id)                                                     AS coastal_log_sale_id,
                   sub.submission_id                                                                 AS submission_id,
                   sub.entry_timestamp                                                               AS entry_timestamp,
                   subStatus.description                                                             AS submission_status,
                   CASE WHEN sub.submission_id IS NOT NULL THEN 'Electronic' ELSE 'Manual' END      AS submission_type,
                   sub.number_invoices_submitted                                                     AS inv_total,
                   count(CASE WHEN inv.log_sale_entry_status_code = 'APP' THEN 1 END)               AS inv_approved,
                   count(CASE WHEN inv.log_sale_entry_status_code = 'REJ' THEN 1 END)               AS inv_rejected,
                   count(CASE WHEN inv.log_sale_entry_status_code = 'PRO' THEN 1 END)               AS inv_processing,
                   count(CASE WHEN inv.log_sale_entry_status_code = 'CAN' THEN 1 END)               AS inv_cancelled
            FROM   THE.csp_submission sub
            INNER JOIN THE.coastal_log_sale inv
                    ON sub.csp_submission_id = inv.csp_submission_id
            INNER JOIN THE.csp_submission_status_code subStatus
                    ON sub.csp_submission_status_code = subStatus.csp_submission_status_code
            WHERE 1=1
            """;

    // GROUP BY: all non-aggregate SELECT columns.
    private static final String GROUP_BY = """
            GROUP BY sub.csp_submission_id,
                     sub.submission_id,
                     sub.entry_timestamp,
                     sub.number_invoices_submitted,
                     subStatus.description,
                     CASE WHEN sub.submission_id IS NOT NULL THEN 'Electronic' ELSE 'Manual' END
            """;

    // Whitelist: API sort key → SQL column/expression.
    // Aggregate columns (invApproved etc.) use the column alias from the outer subquery;
    // Oracle allows ORDER BY alias when the grouped query is wrapped in a subquery.
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "submissionId",     "submission_id",
            "submissionDate",   "entry_timestamp",
            "submissionStatus", "submission_status",
            "submissionType",   "submission_type",
            "invTotal",         "inv_total",
            "invApproved",      "inv_approved",
            "invRejected",      "inv_rejected",
            "invProcessing",    "inv_processing",
            "invCancelled",     "inv_cancelled"
    );

    private static final String DEFAULT_ORDER_BY = "entry_timestamp DESC";

    private final NamedParameterJdbcTemplate jdbc;

    public InboxRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Page<InboxRow> search(InboxCriteria criteria, Pageable pageable) {
        StringBuilder whereFragments = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendCriteriaFilters(whereFragments, params, criteria);

        String orderBy = buildOrderBy(pageable.getSort());

        // Wrap the grouped query in a subquery so ORDER BY can reference column aliases
        // (required for aggregate sort columns like inv_approved).
        String innerQuery = BASE_QUERY + whereFragments + "\n" + GROUP_BY;
        String outerAlias = "inbox_results";

        String keywordClause = buildKeywordClause(criteria, params);

        String dataSql = "SELECT * FROM (" + innerQuery + ") " + outerAlias
                + keywordClause
                + " ORDER BY " + orderBy
                + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
        params.addValue("offset", pageable.getOffset());
        params.addValue("limit", pageable.getPageSize());

        // COUNT wraps only the grouped inner query (counts distinct submission rows, not raw invoice rows).
        String countSql = "SELECT count(*) FROM (" + innerQuery + ") cnt" + keywordClause;

        List<InboxRow> content = jdbc.query(dataSql, params, (rs, rowNum) -> new InboxRow(
                RepositoryUtils.getLongNullable(rs, "csp_submission_id"),
                RepositoryUtils.getLongNullable(rs, "coastal_log_sale_id"),
                rs.getString("submission_id"),           // null for Manual rows
                RepositoryUtils.getLocalDateNullable(rs, "entry_timestamp"),
                rs.getString("submission_status"),
                rs.getString("submission_type"),
                rs.getObject("inv_total", Integer.class),
                rs.getObject("inv_approved", Integer.class),
                rs.getObject("inv_rejected", Integer.class),
                rs.getObject("inv_processing", Integer.class),
                rs.getObject("inv_cancelled", Integer.class)
        ));

        Long total = jdbc.queryForObject(countSql, params, Long.class);
        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    /**
     * Builds a WHERE clause for keyword search applied to the outer subquery.
     * Returns an empty string when no keyword is present.
     * The pattern is a case-insensitive contains match (%keyword%) across
     * submission_id, submission_status, submission_type, and the formatted date.
     */
    private String buildKeywordClause(InboxCriteria criteria, MapSqlParameterSource params) {
        if (criteria.keyword() == null || criteria.keyword().isBlank()) return "";
        params.addValue("keyword", "%" + criteria.keyword() + "%");
        return " WHERE (UPPER(submission_id) LIKE UPPER(:keyword)"
                + " OR UPPER(submission_status) LIKE UPPER(:keyword)"
                + " OR UPPER(submission_type) LIKE UPPER(:keyword)"
                + " OR TO_CHAR(entry_timestamp, 'YYYY-MM-DD') LIKE :keyword)";
    }

    /**
     * Builds the dynamic WHERE fragments from the criteria, mirroring the legacy
     * InboxCriteriaDTOHelper.createNativeQueryCriteria() logic
     */
    private void appendCriteriaFilters(StringBuilder sql, MapSqlParameterSource params, InboxCriteria criteria) {

        // Date range — submissionDateTo is forced to end-of-day 23:59:59 (legacy :148-159)
        if (criteria.submissionDateFrom() != null) {
            sql.append(" AND sub.entry_timestamp >= :startDate");
            params.addValue("startDate", Timestamp.valueOf(criteria.submissionDateFrom().atStartOfDay()));
        }
        if (criteria.submissionDateTo() != null) {
            Calendar cal = Calendar.getInstance();
            cal.set(criteria.submissionDateTo().getYear(),
                    criteria.submissionDateTo().getMonthValue() - 1,
                    criteria.submissionDateTo().getDayOfMonth(),
                    23, 59, 59);
            cal.set(Calendar.MILLISECOND, 0);
            params.addValue("endDate", new Timestamp(cal.getTimeInMillis()));
            sql.append(" AND sub.entry_timestamp <= :endDate");
        }

        // Submitted By: Buyer or Seller join condition
        if (criteria.submittedBy() != null && !criteria.submittedBy().isBlank()) {
            if ("Buyer".equalsIgnoreCase(criteria.submittedBy())) {
                sql.append(" AND sub.client_number = inv.BUYER_CLIENT_NUMBER");
                sql.append(" AND sub.CLIENT_LOCN_CODE = inv.BUYER_CLIENT_LOCN_CODE");
            } else {
                // Seller (legacy treated any non-Buyer value as Seller)
                sql.append(" AND sub.client_number = inv.SELLER_CLIENT_NUMBER");
                sql.append(" AND sub.CLIENT_LOCN_CODE = inv.SELLER_CLIENT_LOCN_CODE");
            }
        }

        // Submitter client number + location — named params, NOT string concatenation.
        // Both must be non-blank; the autocomplete always selects them as a pair.
        // Binding a null :clientLoc would produce "= NULL" in Oracle (always UNKNOWN),
        // silently returning zero rows.
        if (criteria.submitterClientNum() != null && !criteria.submitterClientNum().isBlank()) {
            sql.append(" AND sub.client_number = :clientNum");
            params.addValue("clientNum", criteria.submitterClientNum());
            if (criteria.submitterLocNum() != null && !criteria.submitterLocNum().isBlank()) {
                sql.append(" AND sub.CLIENT_LOCN_CODE = :clientLoc");
                params.addValue("clientLoc", criteria.submitterLocNum());
            }
        }

        // Submission type
        if (criteria.submissionType() != null && !criteria.submissionType().isBlank()) {
            if ("Electronic".equalsIgnoreCase(criteria.submissionType())) {
                sql.append(" AND sub.submission_id IS NOT NULL");
            } else {
                // Manual
                sql.append(" AND sub.submission_id IS NULL");
            }
        }

        // Invoice number: wildcard LIKE match (mirrors SearchRepository.toInvoiceNumberPattern).
        // Value is already trimmed and uppercased by InboxService; wildcards *, %, ? are preserved.
        // UPPER the column too so the match is case-insensitive regardless of how the invoice
        // number is stored (the uppercased param alone only matches uppercase-stored values).
        if (criteria.invoiceNum() != null && !criteria.invoiceNum().isBlank()) {
            sql.append(" AND UPPER(inv.CLIENT_INVOICE_NO) LIKE UPPER(:invoiceNum) ESCAPE '\\'");
            params.addValue("invoiceNum", toInvoiceNumberPattern(criteria.invoiceNum()));
        }

        // Submission status code
        if (criteria.submissionStatus() != null && !criteria.submissionStatus().isBlank()) {
            sql.append(" AND sub.CSP_SUBMISSION_STATUS_CODE = :submissionStatus");
            params.addValue("submissionStatus", criteria.submissionStatus());
        }
    }

    /**
     * Builds the LIKE pattern for the invoice number filter (used with ESCAPE '\').
     * Mirrors SearchRepository.toInvoiceNumberPattern() exactly.
     * User-facing wildcards: '*' or '%' match any sequence, '?' matches a single character.
     * Without wildcards, falls back to a substring (contains) match.
     */
    static String toInvoiceNumberPattern(String invoiceNum) {
        boolean hasWildcard = invoiceNum.indexOf('*') >= 0
                || invoiceNum.indexOf('?') >= 0
                || invoiceNum.indexOf('%') >= 0;

        String escaped = invoiceNum
                .replace("\\", "\\\\")
                .replace("_", "\\_");

        if (hasWildcard) {
            return escaped.replace('*', '%').replace('?', '_');
        }
        return "%" + escaped + "%";
    }

    /**
     * Builds the ORDER BY clause from the Pageable sort, using the SORT_COLUMNS whitelist.
     * Any unknown sort key throws a BadRequestException (400) to prevent SQL injection.
     * Appends a stable tiebreaker (csp_submission_id DESC) so results don't shuffle between pages.
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
}
