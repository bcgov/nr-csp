package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.service.model.SearchCriteria;
import ca.bc.gov.nrs.csp.backend.service.model.SearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class SearchRepository {

    private static final String BASE_QUERY = """
            SELECT
                inv.coastal_log_sale_id,
                sub.csp_submission_id,
                sub.submission_id,
                COALESCE(invSt.description, inv.log_sale_entry_status_code) AS invoice_status,
                inv.client_invoice_no                                        AS invoice_number,
                inv.client_invoice_date                                      AS invoice_date,
                COALESCE(invType.description, inv.csp_invoice_type_code)    AS type,
                sub.client_number                                            AS client_number,
                fc.client_name                                               AS client_name,
                COALESCE(logType.description, inv.log_sale_type_code)       AS maturity,
                CASE WHEN sub.submission_id IS NULL THEN 'Manual' ELSE 'Electronic' END AS submission_type
            FROM THE.csp_submission sub
            JOIN THE.coastal_log_sale inv
                ON sub.csp_submission_id = inv.csp_submission_id
            LEFT JOIN THE.log_sale_entry_status_code invSt
                ON inv.log_sale_entry_status_code = invSt.log_sale_entry_status_code
            LEFT JOIN THE.csp_invoice_type_code invType
                ON inv.csp_invoice_type_code = invType.csp_invoice_type_code
            LEFT JOIN THE.log_sale_type_code logType
                ON inv.log_sale_type_code = logType.log_sale_type_code
            LEFT JOIN THE.forest_client fc
                ON sub.client_number = fc.client_number
            WHERE 1=1
            """;

    // Whitelist of sortable fields: maps API field name (camelCase) to the SQL alias/column
    // selected above. Required for safety — sort properties are spliced into the SQL string,
    // so any unknown field must be rejected up front to prevent SQL injection.
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "invoiceStatus",  "invoice_status",
            "invoiceNumber",  "invoice_number",
            "invoiceDate",    "invoice_date",
            "type",           "type",
            "clientNumber",   "client_number",
            "clientName",     "client_name",
            "maturity",       "maturity",
            "submissionType", "submission_type"
    );

    private static final String DEFAULT_ORDER_BY = "csp_submission_id DESC";

    // Columns the cross-field keyword filter searches. invoice_date is converted to text so
    // the user can type "2024-01" and still hit it.
    private static final String KEYWORD_WHERE = """
             WHERE (
                UPPER(invoice_status)  LIKE UPPER(:keyword)
                OR UPPER(invoice_number) LIKE UPPER(:keyword)
                OR UPPER(type)           LIKE UPPER(:keyword)
                OR UPPER(client_number)  LIKE UPPER(:keyword)
                OR UPPER(client_name)    LIKE UPPER(:keyword)
                OR UPPER(maturity)       LIKE UPPER(:keyword)
                OR UPPER(submission_type) LIKE UPPER(:keyword)
                OR TO_CHAR(invoice_date, 'YYYY-MM-DD') LIKE :keyword
             )
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public SearchRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Page<SearchResult> search(SearchCriteria criteria, Pageable pageable) {
        StringBuilder core = new StringBuilder(BASE_QUERY);
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendCriteriaFilters(core, params, criteria);

        boolean hasKeyword = criteria.keyword() != null && !criteria.keyword().isBlank();
        if (hasKeyword) {
            params.addValue("keyword", "%" + criteria.keyword().trim() + "%");
        }

        String orderBy = buildOrderBy(pageable.getSort());

        // Wrap in a subquery so the keyword filter and ORDER BY can reference the SELECT aliases.
        String baseSubquery = "SELECT * FROM (" + core + ") result";
        String filteredSubquery = hasKeyword ? baseSubquery + KEYWORD_WHERE : baseSubquery;

        String dataSql = filteredSubquery
                + " ORDER BY " + orderBy
                + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
        params.addValue("offset", pageable.getOffset());
        params.addValue("limit", pageable.getPageSize());

        String countSql = "SELECT COUNT(*) FROM (" + filteredSubquery + ") cnt";

        List<SearchResult> content = jdbc.query(dataSql, params, (rs, rowNum) -> new SearchResult(
                RepositoryUtils.getLongNullable(rs, "coastal_log_sale_id"),
                RepositoryUtils.getLongNullable(rs, "csp_submission_id"),
                RepositoryUtils.getLongNullable(rs, "submission_id"),
                rs.getString("invoice_status"),
                rs.getString("invoice_number"),
                RepositoryUtils.getLocalDateNullable(rs, "invoice_date"),
                rs.getString("type"),
                rs.getString("client_number"),
                rs.getString("client_name"),
                rs.getString("maturity"),
                rs.getString("submission_type")
        ));

        Long total = jdbc.queryForObject(countSql, params, Long.class);
        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private void appendCriteriaFilters(StringBuilder sql, MapSqlParameterSource params, SearchCriteria criteria) {
        if (criteria.invDate() != null) {
            sql.append(" AND inv.client_invoice_date = TO_DATE(:invDate, 'YYYY-MM-DD')");
            params.addValue("invDate", criteria.invDate().toString());
        }
        if (criteria.startDate() != null) {
            sql.append(" AND inv.client_invoice_date >= TO_DATE(:startDate, 'YYYY-MM-DD')");
            params.addValue("startDate", criteria.startDate().toString());
        }
        if (criteria.endDate() != null) {
            sql.append(" AND inv.client_invoice_date <= TO_DATE(:endDate, 'YYYY-MM-DD')");
            params.addValue("endDate", criteria.endDate().toString());
        }
        if (criteria.submitterClientNum() != null) {
            sql.append(" AND sub.client_number = :submitterClientNum");
            params.addValue("submitterClientNum", criteria.submitterClientNum());
        }
        if (criteria.sellerBuyerClientNum() != null) {
            sql.append(" AND (inv.seller_client_number = :sellerBuyerClientNum OR inv.buyer_client_number = :sellerBuyerClientNum)");
            params.addValue("sellerBuyerClientNum", criteria.sellerBuyerClientNum());
        }
        if (criteria.sellerBuyerLocNum() != null) {
            sql.append(" AND (inv.seller_client_locn_code = :sellerBuyerLocNum OR inv.buyer_client_locn_code = :sellerBuyerLocNum)");
            params.addValue("sellerBuyerLocNum", criteria.sellerBuyerLocNum());
        }
        if (criteria.sellerSubmitter() != null) {
            if (Boolean.TRUE.equals(criteria.sellerSubmitter())) {
                sql.append(" AND (inv.seller_client_number = sub.client_number AND inv.seller_client_locn_code = sub.client_locn_code)");
            } else {
                sql.append(" AND (inv.buyer_client_number = sub.client_number AND inv.buyer_client_locn_code = sub.client_locn_code)");
            }
        }
        if (criteria.invNumber() != null) {
            sql.append(" AND inv.client_invoice_no LIKE :invNumber");
            params.addValue("invNumber", "%" + criteria.invNumber() + "%");
        }
        if (criteria.invStatus() != null) {
            sql.append(" AND inv.log_sale_entry_status_code = :invStatus");
            params.addValue("invStatus", criteria.invStatus());
        }
        if (criteria.invType() != null) {
            sql.append(" AND inv.csp_invoice_type_code = :invType");
            params.addValue("invType", criteria.invType());
        }
        if (criteria.maturity() != null) {
            sql.append(" AND inv.log_sale_type_code = :maturity");
            params.addValue("maturity", criteria.maturity());
        }
    }

    private String buildOrderBy(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return DEFAULT_ORDER_BY;
        }
        StringBuilder orderBy = new StringBuilder();
        for (Sort.Order order : sort) {
            String column = SORT_COLUMNS.get(order.getProperty());
            if (column == null) {
                throw new BadRequestException("Unsupported sort field: " + order.getProperty());
            }
            if (orderBy.length() > 0) orderBy.append(", ");
            orderBy.append(column).append(' ').append(order.isAscending() ? "ASC" : "DESC");
        }
        // Append a stable tiebreaker so identical sort values don't reshuffle between pages.
        orderBy.append(", csp_submission_id DESC");
        return orderBy.toString();
    }
}
