package ca.bc.gov.nrs.csp.backend.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public class ValidationLookupRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ValidationLookupRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsActiveMaturityCode(String code, LocalDate onDate) {
        if (code == null || onDate == null) return false;
        String sql = "SELECT COUNT(*) FROM THE.log_sale_type_code" +
                " WHERE log_sale_type_code = :code" +
                " AND effective_date <= :onDate" +
                " AND (expiry_date IS NULL OR expiry_date >= :onDate)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("code", code)
                .addValue("onDate", Date.valueOf(onDate));
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public boolean existsActiveInvoiceTypeCode(String code, LocalDate onDate) {
        if (code == null || onDate == null) return false;
        String sql = "SELECT COUNT(*) FROM THE.csp_invoice_type_code" +
                " WHERE csp_invoice_type_code = :code" +
                " AND effective_date <= :onDate" +
                " AND (expiry_date IS NULL OR expiry_date >= :onDate)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("code", code)
                .addValue("onDate", Date.valueOf(onDate));
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public boolean existsActiveSortCode(String code, LocalDate onDate) {
        if (code == null || onDate == null) return false;
        String sql = "SELECT COUNT(*) FROM THE.log_sale_sort_code" +
                " WHERE log_sale_sort_code = :code" +
                " AND effective_date <= :onDate" +
                " AND (expiry_date IS NULL OR expiry_date >= :onDate)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("code", code)
                .addValue("onDate", Date.valueOf(onDate));
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public boolean existsSpeciesGradeCombination(String species, String grade) {
        if (species == null || grade == null) return false;
        String sql = "SELECT COUNT(*) FROM THE.csp_species_grade_xref" +
                " WHERE log_sale_species_code = :species AND log_sale_grade_code = :grade";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("species", species)
                .addValue("grade", grade);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public boolean existsClientLocation(String clientNumber, String clientLocnCode) {
        if (clientNumber == null || clientLocnCode == null) return false;
        String sql = "SELECT COUNT(*) FROM THE.client_location" +
                " WHERE client_number = :clientNumber AND client_locn_code = :clientLocnCode";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("clientNumber", clientNumber)
                .addValue("clientLocnCode", clientLocnCode);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public Optional<Long> findSpeciesGradeXrefId(String species, String grade, LocalDate onDate) {
        if (species == null || grade == null || onDate == null) return Optional.empty();
        String sql = "SELECT csp_species_grade_xref_id FROM THE.csp_species_grade_xref" +
                " WHERE log_sale_species_code = :species AND log_sale_grade_code = :grade" +
                " AND effective_date <= :onDate AND (expiry_date IS NULL OR expiry_date >= :onDate)" +
                " ORDER BY effective_date DESC" +
                " FETCH FIRST 1 ROWS ONLY";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("species", species)
                .addValue("grade", grade)
                .addValue("onDate", Date.valueOf(onDate));
        List<Long> results = jdbc.query(sql, params, (rs, rowNum) -> rs.getLong("csp_species_grade_xref_id"));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public boolean existsSubmissionStatusCode(String code) {
        if (code == null || code.isBlank()) return false;
        String sql = "SELECT COUNT(*) FROM THE.csp_submission_status_code" +
                " WHERE csp_submission_status_code = :code";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("code", code);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public boolean existsDuplicateFlatPriceConversion(
            String modellingCode, String sortCode, Long speciesGradeXrefId,
            String maturity, LocalDate effectiveDate, Long excludeId) {
        if (modellingCode == null || sortCode == null || speciesGradeXrefId == null ||
                maturity == null || effectiveDate == null) {
            return false;
        }
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM THE.log_sale_flat_price_conversion" +
                " WHERE csp_modelling_code = :modellingCode" +
                " AND log_sale_sort_code = :sortCode" +
                " AND csp_species_grade_xref_id = :xrefId" +
                " AND log_sale_type_code = :maturity" +
                " AND effective_date = :effectiveDate");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("modellingCode", modellingCode)
                .addValue("sortCode", sortCode)
                .addValue("xrefId", speciesGradeXrefId)
                .addValue("maturity", maturity)
                .addValue("effectiveDate", Date.valueOf(effectiveDate));
        if (excludeId != null) {
            sql.append(" AND log_sale_flat_price_cnvrsn_id <> :excludeId");
            params.addValue("excludeId", excludeId);
        }
        Integer count = jdbc.queryForObject(sql.toString(), params, Integer.class);
        return count != null && count > 0;
    }
}
