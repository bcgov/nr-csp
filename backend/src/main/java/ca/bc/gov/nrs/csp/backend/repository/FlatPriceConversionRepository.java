package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.service.model.FlatPriceConversion;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class FlatPriceConversionRepository {

    // ---------------------------------------------------------------
    // SQL constants
    // ---------------------------------------------------------------

    private static final String SQL_SEARCH_BASE = """
            SELECT f.log_sale_flat_price_cnvrsn_id,
                   f.csp_modelling_code,
                   f.log_sale_sort_code,
                   x.log_sale_species_code,
                   x.log_sale_grade_code,
                   f.log_sale_type_code,
                   f.flat_price_conversion,
                   f.effective_date,
                   f.expiry_date,
                   f.revision_count,
                   f.entry_userid,
                   f.entry_timestamp,
                   f.update_userid,
                   f.update_timestamp
            FROM THE.log_sale_flat_price_conversion f
            JOIN THE.csp_species_grade_xref x ON x.csp_species_grade_xref_id = f.csp_species_grade_xref_id
            WHERE f.csp_modelling_code = :modellingCode
            """;

    private static final String SQL_SEARCH_ORDER_BY =
            " ORDER BY x.log_sale_species_code, x.log_sale_grade_code," +
            " f.log_sale_sort_code, f.log_sale_type_code";

    // Resolve the single applicable conversion factor for a line item at a
    // given invoice date: PRODUCT modelling, matching maturity/sort/species/
    // grade, effective on or before the date and not yet expired. Latest
    // effective date wins (mirrors the legacy getConversionFactor()).
    private static final String SQL_FIND_APPLICABLE_FACTOR = """
            SELECT f.flat_price_conversion
            FROM THE.log_sale_flat_price_conversion f
            JOIN THE.csp_species_grade_xref x ON x.csp_species_grade_xref_id = f.csp_species_grade_xref_id
            WHERE f.csp_modelling_code = 'P'
              AND f.log_sale_type_code = :maturity
              AND f.log_sale_sort_code = :sortCode
              AND x.log_sale_species_code = :species
              AND x.log_sale_grade_code = :grade
              AND f.effective_date <= :invoiceDate
              AND (f.expiry_date IS NULL OR f.expiry_date >= :invoiceDate)
            ORDER BY f.effective_date DESC
            """;

    // Whether ANY PRODUCT conversion factor is defined for a sort code — used
    // to decide whether a missing factor is worth warning about.
    private static final String SQL_EXISTS_FOR_SORT_CODE = """
            SELECT COUNT(*) FROM THE.log_sale_flat_price_conversion
            WHERE csp_modelling_code = 'P' AND log_sale_sort_code = :sortCode
            """;

    private static final String SQL_FIND_BY_ID = """
            SELECT f.log_sale_flat_price_cnvrsn_id,
                   f.csp_modelling_code,
                   f.log_sale_sort_code,
                   x.log_sale_species_code,
                   x.log_sale_grade_code,
                   f.log_sale_type_code,
                   f.flat_price_conversion,
                   f.effective_date,
                   f.expiry_date,
                   f.revision_count,
                   f.entry_userid,
                   f.entry_timestamp,
                   f.update_userid,
                   f.update_timestamp
            FROM THE.log_sale_flat_price_conversion f
            JOIN THE.csp_species_grade_xref x ON x.csp_species_grade_xref_id = f.csp_species_grade_xref_id
            WHERE f.log_sale_flat_price_cnvrsn_id = :id
            """;

    private static final String SQL_INSERT = """
            INSERT INTO THE.log_sale_flat_price_conversion (
                log_sale_flat_price_cnvrsn_id, csp_modelling_code, log_sale_sort_code,
                csp_species_grade_xref_id, log_sale_type_code, flat_price_conversion,
                effective_date, expiry_date, revision_count,
                entry_userid, entry_timestamp, update_userid, update_timestamp
            ) VALUES (
                LOG_SALE_FLAT_PRICE_CNVRSN_SEQ.nextval, :modellingCode, :sortCode,
                :xrefId, :maturity, :flatPriceConversion,
                :effectiveDate, :expiryDate, 0,
                :userId, CURRENT_TIMESTAMP, :userId, CURRENT_TIMESTAMP
            )
            """;

    private static final String SQL_UPDATE = """
            UPDATE THE.log_sale_flat_price_conversion SET
                log_sale_sort_code        = :sortCode,
                csp_species_grade_xref_id = :xrefId,
                log_sale_type_code        = :maturity,
                flat_price_conversion     = :flatPriceConversion,
                effective_date            = :effectiveDate,
                expiry_date               = :expiryDate,
                revision_count            = revision_count + 1,
                update_userid             = :userId,
                update_timestamp          = CURRENT_TIMESTAMP
            WHERE log_sale_flat_price_cnvrsn_id = :id
            """;

    private static final String SQL_AUDIT_DELETE_BY_ID = """
            INSERT INTO THE.log_sale_flat_price_cnvrsn_aud (
                csp_audit_action_type_code, log_sale_flat_price_cnvrsn_id, effective_date,
                expiry_date, csp_modelling_code, log_sale_sort_code, flat_price_conversion,
                revision_count, entry_userid, entry_timestamp, update_userid, update_timestamp,
                csp_species_grade_xref_id, log_sale_type_code
            )
            SELECT 'D', t.log_sale_flat_price_cnvrsn_id, t.effective_date, t.expiry_date,
                t.csp_modelling_code, t.log_sale_sort_code, t.flat_price_conversion,
                t.revision_count, t.entry_userid, t.entry_timestamp,
                :userId, CURRENT_TIMESTAMP, t.csp_species_grade_xref_id, t.log_sale_type_code
            FROM THE.log_sale_flat_price_conversion t
            WHERE t.log_sale_flat_price_cnvrsn_id = :id
            """;

    private static final String SQL_DELETE_BY_ID = """
            DELETE FROM THE.log_sale_flat_price_conversion WHERE log_sale_flat_price_cnvrsn_id = :id
            """;

    private static final String SQL_COPY_AUDIT_TARGET = """
            INSERT INTO THE.log_sale_flat_price_cnvrsn_aud (
                csp_audit_action_type_code, log_sale_flat_price_cnvrsn_id, effective_date,
                expiry_date, csp_modelling_code, log_sale_sort_code, flat_price_conversion,
                revision_count, entry_userid, entry_timestamp, update_userid, update_timestamp,
                csp_species_grade_xref_id, log_sale_type_code
            )
            SELECT 'D', t.log_sale_flat_price_cnvrsn_id, t.effective_date, t.expiry_date,
                t.csp_modelling_code, t.log_sale_sort_code, t.flat_price_conversion,
                t.revision_count, t.entry_userid, t.entry_timestamp,
                :userId, CURRENT_TIMESTAMP, t.csp_species_grade_xref_id, t.log_sale_type_code
            FROM THE.log_sale_flat_price_conversion t WHERE t.csp_modelling_code = :targetCode
            """;

    private static final String SQL_COPY_DELETE_TARGET = """
            DELETE FROM THE.log_sale_flat_price_conversion WHERE csp_modelling_code = :targetCode
            """;

    private static final String SQL_COPY_INSERT_FROM_SOURCE = """
            INSERT INTO THE.log_sale_flat_price_conversion (
                log_sale_flat_price_cnvrsn_id, csp_modelling_code, log_sale_sort_code,
                csp_species_grade_xref_id, log_sale_type_code, flat_price_conversion,
                effective_date, expiry_date, revision_count,
                entry_userid, entry_timestamp, update_userid, update_timestamp
            )
            SELECT LOG_SALE_FLAT_PRICE_CNVRSN_SEQ.nextval, :targetCode, t.log_sale_sort_code,
                t.csp_species_grade_xref_id, t.log_sale_type_code, t.flat_price_conversion,
                t.effective_date, t.expiry_date, 0,
                :userId, CURRENT_TIMESTAMP, :userId, CURRENT_TIMESTAMP
            FROM THE.log_sale_flat_price_conversion t
            WHERE t.csp_modelling_code = :sourceCode
              AND (t.expiry_date > SYSDATE OR t.expiry_date IS NULL)
            """;

    private static final String SQL_EXISTS_BY_MODELLING_CODE = """
            SELECT COUNT(*) FROM THE.log_sale_flat_price_conversion
            WHERE csp_modelling_code = :modellingCode
            """;

    private static final String SQL_CLEAR_ALL = """
            DELETE FROM THE.log_sale_flat_price_conversion WHERE csp_modelling_code = :modellingCode
            """;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    private final NamedParameterJdbcTemplate jdbc;

    public FlatPriceConversionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------
    // Public methods
    // ---------------------------------------------------------------

    public List<FlatPriceConversion> search(
            String modellingCode, String maturity, String sortCode, String species, String grade) {
        StringBuilder sql = new StringBuilder(SQL_SEARCH_BASE);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("modellingCode", modellingCode);
        if (maturity != null) {
            sql.append(" AND f.log_sale_type_code = :maturity");
            params.addValue("maturity", maturity);
        }
        if (sortCode != null) {
            sql.append(" AND f.log_sale_sort_code = :sortCode");
            params.addValue("sortCode", sortCode);
        }
        if (species != null) {
            sql.append(" AND x.log_sale_species_code = :species");
            params.addValue("species", species);
        }
        if (grade != null) {
            sql.append(" AND x.log_sale_grade_code = :grade");
            params.addValue("grade", grade);
        }
        sql.append(SQL_SEARCH_ORDER_BY);
        return jdbc.query(sql.toString(), params, this::mapRow);
    }

    /**
     * The applicable PRODUCT conversion factor (integer percent, 0-999) for a
     * line item on the given invoice date, or empty when none is configured.
     */
    public Optional<Integer> findApplicableFactor(
            String maturity, String sortCode, String species, String grade, java.time.LocalDate invoiceDate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maturity", maturity)
                .addValue("sortCode", sortCode)
                .addValue("species", species)
                .addValue("grade", grade)
                .addValue("invoiceDate", invoiceDate != null ? Date.valueOf(invoiceDate) : null);
        List<Integer> factors = jdbc.query(
                SQL_FIND_APPLICABLE_FACTOR, params, (rs, rowNum) -> rs.getObject("flat_price_conversion", Integer.class));
        return factors.isEmpty() ? Optional.empty() : Optional.ofNullable(factors.get(0));
    }

    /** Whether any PRODUCT conversion factor exists for the given sort code. */
    public boolean existsForSortCode(String sortCode) {
        Integer count = jdbc.queryForObject(
                SQL_EXISTS_FOR_SORT_CODE, new MapSqlParameterSource("sortCode", sortCode), Integer.class);
        return count != null && count > 0;
    }

    public Optional<FlatPriceConversion> findById(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id);
        List<FlatPriceConversion> results = jdbc.query(SQL_FIND_BY_ID, params, this::mapRow);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void insert(FlatPriceConversion record, Long speciesGradeXrefId, String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("modellingCode", record.modellingCode())
                .addValue("sortCode", record.sortCode())
                .addValue("xrefId", speciesGradeXrefId)
                .addValue("maturity", record.maturity())
                .addValue("flatPriceConversion", record.flatPriceConversion())
                .addValue("effectiveDate", record.effectiveDate() != null ? Date.valueOf(record.effectiveDate()) : null)
                .addValue("expiryDate", record.expiryDate() != null ? Date.valueOf(record.expiryDate()) : null)
                .addValue("userId", userId);
        jdbc.update(SQL_INSERT, params);
    }

    public void update(Long id, FlatPriceConversion record, Long speciesGradeXrefId, String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("sortCode", record.sortCode())
                .addValue("xrefId", speciesGradeXrefId)
                .addValue("maturity", record.maturity())
                .addValue("flatPriceConversion", record.flatPriceConversion())
                .addValue("effectiveDate", record.effectiveDate() != null ? Date.valueOf(record.effectiveDate()) : null)
                .addValue("expiryDate", record.expiryDate() != null ? Date.valueOf(record.expiryDate()) : null)
                .addValue("userId", userId);
        jdbc.update(SQL_UPDATE, params);
    }

    public void auditDelete(Long id, String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        jdbc.update(SQL_AUDIT_DELETE_BY_ID, params);
    }

    public void deleteById(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id);
        jdbc.update(SQL_DELETE_BY_ID, params);
    }

    public void copy(String sourceModellingCode, String targetModellingCode, String userId) {
        MapSqlParameterSource auditAndDeleteParams = new MapSqlParameterSource()
                .addValue("targetCode", targetModellingCode)
                .addValue("userId", userId);
        jdbc.update(SQL_COPY_AUDIT_TARGET, auditAndDeleteParams);
        jdbc.update(SQL_COPY_DELETE_TARGET, auditAndDeleteParams);

        MapSqlParameterSource insertParams = new MapSqlParameterSource()
                .addValue("sourceCode", sourceModellingCode)
                .addValue("targetCode", targetModellingCode)
                .addValue("userId", userId);
        jdbc.update(SQL_COPY_INSERT_FROM_SOURCE, insertParams);
    }

    public boolean existsByModellingCode(String modellingCode) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("modellingCode", modellingCode);
        Integer count = jdbc.queryForObject(SQL_EXISTS_BY_MODELLING_CODE, params, Integer.class);
        return count != null && count > 0;
    }

    public void clearAll(String modellingCode) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("modellingCode", modellingCode);
        jdbc.update(SQL_CLEAR_ALL, params);
    }

    // ---------------------------------------------------------------
    // Row mapper
    // ---------------------------------------------------------------

    private FlatPriceConversion mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new FlatPriceConversion(
                RepositoryUtils.getLongNullable(rs, "log_sale_flat_price_cnvrsn_id"),
                rs.getString("csp_modelling_code"),
                rs.getString("log_sale_type_code"),
                rs.getString("log_sale_species_code"),
                rs.getString("log_sale_grade_code"),
                rs.getString("log_sale_sort_code"),
                rs.getObject("flat_price_conversion", Integer.class),
                RepositoryUtils.getLocalDateNullable(rs, "effective_date"),
                RepositoryUtils.getLocalDateNullable(rs, "expiry_date"),
                rs.getObject("revision_count", Integer.class),
                rs.getString("entry_userid"),
                RepositoryUtils.getLocalDateNullable(rs, "entry_timestamp"),
                rs.getString("update_userid"),
                RepositoryUtils.getLocalDateNullable(rs, "update_timestamp")
        );
    }
}
