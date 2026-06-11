package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class LookupRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public LookupRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * The log-sale species GROUP code a species belongs to, used to group line
     * items for flat-price conversion. Returns empty when the species has no
     * group mapping (callers fall back to the species code itself).
     */
    public Optional<String> findSpeciesGroupCode(String species) {
        List<String> codes = jdbc.query(
                "SELECT log_sale_species_group_code AS code FROM THE.log_sale_species_group_list" +
                " WHERE log_sale_species_code = :species" +
                " ORDER BY log_sale_species_group_code",
                new MapSqlParameterSource("species", species),
                (rs, rowNum) -> rs.getString("code"));
        return codes.isEmpty() ? Optional.empty() : Optional.ofNullable(codes.get(0));
    }

    public List<LookupItem> findMaturityCodes() {
        return jdbc.query(
                "SELECT log_sale_type_code AS code, description FROM THE.log_sale_type_code" +
                " WHERE effective_date <= SYSDATE AND (expiry_date IS NULL OR expiry_date >= SYSDATE)" +
                " ORDER BY log_sale_type_code",
                Collections.emptyMap(),
                (rs, rowNum) -> new LookupItem(rs.getString("code"), rs.getString("description"))
        );
    }

    public List<LookupItem> findInvoiceTypes() {
        return jdbc.query(
                "SELECT csp_invoice_type_code AS code, description FROM THE.csp_invoice_type_code" +
                " WHERE effective_date <= SYSDATE AND (expiry_date IS NULL OR expiry_date >= SYSDATE)" +
                " ORDER BY csp_invoice_type_code",
                Collections.emptyMap(),
                (rs, rowNum) -> new LookupItem(rs.getString("code"), rs.getString("description"))
        );
    }

    public List<LookupItem> findInvoiceStatuses() {
        // No ORDER BY — the legacy app had no ordering clause on this query; Oracle returns rows in natural table order.
        return jdbc.query(
                "SELECT log_sale_entry_status_code AS code, description FROM THE.log_sale_entry_status_code" +
                " WHERE effective_date <= SYSDATE AND (expiry_date IS NULL OR expiry_date >= SYSDATE)",
                Collections.emptyMap(),
                (rs, rowNum) -> new LookupItem(rs.getString("code"), rs.getString("description"))
        );
    }

    public List<LookupItem> findSubmissionStatuses() {
        return jdbc.query(
                "SELECT csp_submission_status_code AS code, description FROM THE.csp_submission_status_code" +
                " WHERE expiry_date > SYSDATE" +
                " ORDER BY csp_submission_status_code",
                Collections.emptyMap(),
                (rs, rowNum) -> new LookupItem(rs.getString("code"), rs.getString("description"))
        );
    }

    public List<LookupItem> findSortCodes() {
        return jdbc.query(
                "SELECT log_sale_sort_code AS code, description FROM THE.log_sale_sort_code" +
                " WHERE effective_date <= SYSDATE AND (expiry_date IS NULL OR expiry_date >= SYSDATE)" +
                " ORDER BY log_sale_sort_code",
                Collections.emptyMap(),
                (rs, rowNum) -> new LookupItem(rs.getString("code"), rs.getString("description"))
        );
    }

    public List<LookupItem> findSpeciesCodes() {
        return jdbc.query(
                "SELECT log_sale_species_code AS code, description FROM THE.log_sale_species_code" +
                " WHERE effective_date <= SYSDATE AND (expiry_date IS NULL OR expiry_date >= SYSDATE)" +
                " ORDER BY log_sale_species_code",
                Collections.emptyMap(),
                (rs, rowNum) -> new LookupItem(rs.getString("code"), rs.getString("description"))
        );
    }


    public List<LookupItem> findModellingCodes() {
        return jdbc.query(
                "SELECT csp_modelling_code AS code, description FROM THE.csp_modelling_code" +
                        " WHERE expiry_date > SYSDATE",
                Collections.emptyMap(),
                (rs, rowNum) -> new LookupItem(rs.getString("code"), rs.getString("description"))
        );
    }

    public List<LookupItem> findFobCodes() {
        return jdbc.query(
                "SELECT log_sale_fob_location_code AS code, description FROM THE.log_sale_fob_location_code" +
                " WHERE effective_date <= SYSDATE AND (expiry_date IS NULL OR expiry_date >= SYSDATE)" +
                " ORDER BY log_sale_fob_location_code",
                Collections.emptyMap(),
                (rs, rowNum) -> new LookupItem(rs.getString("code"), rs.getString("description"))
        );
    }

    public List<LookupItem> findGradeCodes() {
        return jdbc.query(
                "SELECT log_sale_grade_code AS code, description FROM THE.log_sale_grade_code" +
                " WHERE effective_date <= SYSDATE AND (expiry_date IS NULL OR expiry_date >= SYSDATE)" +
                " ORDER BY log_sale_grade_code",
                Collections.emptyMap(),
                (rs, rowNum) -> new LookupItem(rs.getString("code"), rs.getString("description"))
        );
    }

    /** Simple (species, grade) pair record used only by the combo lookup. */
    public record SpeciesGradeCombo(String species, String grade) {}

    public List<SpeciesGradeCombo> findSpeciesGradeCombinations() {
        return jdbc.query(
                "SELECT log_sale_species_code AS species, log_sale_grade_code AS grade" +
                " FROM THE.csp_species_grade_xref" +
                " WHERE effective_date <= SYSDATE AND (expiry_date IS NULL OR expiry_date >= SYSDATE)" +
                " ORDER BY log_sale_species_code, log_sale_grade_code",
                Collections.emptyMap(),
                (rs, rowNum) -> new SpeciesGradeCombo(rs.getString("species"), rs.getString("grade"))
        );
    }

    public List<LookupItem> findGradesBySpecies(String species) {
        return jdbc.query(
                "SELECT DISTINCT g.log_sale_grade_code AS code, g.description" +
                " FROM THE.csp_species_grade_xref x" +
                " JOIN THE.log_sale_grade_code g ON g.log_sale_grade_code = x.log_sale_grade_code" +
                " WHERE x.log_sale_species_code = :species" +
                " AND x.effective_date <= SYSDATE AND (x.expiry_date IS NULL OR x.expiry_date >= SYSDATE)" +
                " AND g.effective_date <= SYSDATE AND (g.expiry_date IS NULL OR g.expiry_date >= SYSDATE)" +
                " ORDER BY g.log_sale_grade_code",
                new MapSqlParameterSource("species", species),
                (rs, rowNum) -> new LookupItem(rs.getString("code"), rs.getString("description"))
        );
    }
}
