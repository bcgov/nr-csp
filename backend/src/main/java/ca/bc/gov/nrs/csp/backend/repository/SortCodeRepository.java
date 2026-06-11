package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.service.model.SortCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class SortCodeRepository {

    private static final String TABLE = "THE.LOG_SALE_SORT_CODE";

    private static final String BASE_SELECT =
            "SELECT LOG_SALE_SORT_CODE, DESCRIPTION, EFFECTIVE_DATE, EXPIRY_DATE, UPDATE_TIMESTAMP" +
            " FROM " + TABLE;

    private static final String SELECT_ALL = BASE_SELECT + " ORDER BY LOG_SALE_SORT_CODE";

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "sortCode",      "LOG_SALE_SORT_CODE",
            "description",   "DESCRIPTION",
            "effectiveDate", "EFFECTIVE_DATE",
            "expiryDate",    "EXPIRY_DATE"
    );

    private static final String DEFAULT_ORDER_BY = "LOG_SALE_SORT_CODE ASC";

    private static final String SELECT_BY_CODE =
            "SELECT LOG_SALE_SORT_CODE, DESCRIPTION, EFFECTIVE_DATE, EXPIRY_DATE, UPDATE_TIMESTAMP" +
            " FROM " + TABLE +
            " WHERE LOG_SALE_SORT_CODE = :code";

    private static final String EXISTS_BY_CODE =
            "SELECT COUNT(*) FROM " + TABLE + " WHERE LOG_SALE_SORT_CODE = :code";

    private static final String INSERT =
            "INSERT INTO " + TABLE +
            " (LOG_SALE_SORT_CODE, DESCRIPTION, EFFECTIVE_DATE, EXPIRY_DATE, UPDATE_TIMESTAMP)" +
            " VALUES (:sortCode, :description, :effectiveDate, :expiryDate, :updateTimestamp)";

    private static final String UPDATE =
            "UPDATE " + TABLE +
            " SET DESCRIPTION = :description," +
            "     EFFECTIVE_DATE = :effectiveDate," +
            "     EXPIRY_DATE = :expiryDate," +
            "     UPDATE_TIMESTAMP = :updateTimestamp" +
            " WHERE LOG_SALE_SORT_CODE = :code";

    private static final String DELETE =
            "DELETE FROM " + TABLE + " WHERE LOG_SALE_SORT_CODE = :code";

    private final JdbcClient jdbc;

    public SortCodeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<SortCode> findAll() {
        return jdbc.sql(SELECT_ALL)
                .query((rs, rowNum) -> new SortCode(
                        rs.getString("LOG_SALE_SORT_CODE"),
                        rs.getString("DESCRIPTION"),
                        RepositoryUtils.getLocalDateNullable(rs, "EFFECTIVE_DATE"),
                        RepositoryUtils.getLocalDateNullable(rs, "EXPIRY_DATE"),
                        RepositoryUtils.getLocalDateNullable(rs, "UPDATE_TIMESTAMP")
                ))
                .list();
    }

    public Page<SortCode> findAll(Pageable pageable) {
        String orderBy = buildOrderBy(pageable.getSort());
        String dataSql = BASE_SELECT
                + " ORDER BY " + orderBy
                + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
        String countSql = "SELECT COUNT(*) FROM " + TABLE;

        List<SortCode> content = jdbc.sql(dataSql)
                .param("offset", pageable.getOffset())
                .param("limit", pageable.getPageSize())
                .query((rs, rowNum) -> new SortCode(
                        rs.getString("LOG_SALE_SORT_CODE"),
                        rs.getString("DESCRIPTION"),
                        RepositoryUtils.getLocalDateNullable(rs, "EFFECTIVE_DATE"),
                        RepositoryUtils.getLocalDateNullable(rs, "EXPIRY_DATE"),
                        RepositoryUtils.getLocalDateNullable(rs, "UPDATE_TIMESTAMP")
                ))
                .list();

        Long total = jdbc.sql(countSql).query(Long.class).single();
        return new PageImpl<>(content, pageable, total == null ? 0L : total);
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
        return orderBy.toString();
    }

    public Optional<SortCode> findByCode(String code) {
        return jdbc.sql(SELECT_BY_CODE)
                .param("code", code)
                .query((rs, rowNum) -> new SortCode(
                        rs.getString("LOG_SALE_SORT_CODE"),
                        rs.getString("DESCRIPTION"),
                        RepositoryUtils.getLocalDateNullable(rs, "EFFECTIVE_DATE"),
                        RepositoryUtils.getLocalDateNullable(rs, "EXPIRY_DATE"),
                        RepositoryUtils.getLocalDateNullable(rs, "UPDATE_TIMESTAMP")
                ))
                .optional();
    }

    public boolean existsByCode(String code) {
        Integer count = jdbc.sql(EXISTS_BY_CODE)
                .param("code", code)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    public void insert(SortCode sc) {
        jdbc.sql(INSERT)
                .param("sortCode", sc.sortCode())
                .param("description", sc.description())
                .param("effectiveDate", Date.valueOf(sc.effectiveDate()))
                .param("expiryDate", Date.valueOf(sc.expiryDate()))
                .param("updateTimestamp", Date.valueOf(sc.updateTimestamp()))
                .update();
    }

    public void update(String code, SortCode sc) {
        jdbc.sql(UPDATE)
                .param("description", sc.description())
                .param("effectiveDate", Date.valueOf(sc.effectiveDate()))
                .param("expiryDate", Date.valueOf(sc.expiryDate()))
                .param("updateTimestamp", Date.valueOf(sc.updateTimestamp()))
                .param("code", code)
                .update();
    }

    public void delete(String code) {
        jdbc.sql(DELETE)
                .param("code", code)
                .update();
    }
}
