package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for LookupRepository.
 *
 * <p>The NamedParameterJdbcTemplate is mocked; each stub executes the repository's
 * RowMapper against a mocked ResultSet so column names and mapped fields are verified
 * without a real database connection. SQL strings are captured and inspected.</p>
 */
@ExtendWith(MockitoExtension.class)
class LookupRepositoryTest {

    @Mock NamedParameterJdbcTemplate jdbc;
    @Mock ResultSet rs;

    LookupRepository repo;

    @BeforeEach
    void setUp() {
        repo = new LookupRepository(jdbc);
    }

    // ---------------------------------------------------------------
    // Helpers — stub the two jdbc.query overloads to run the row mapper
    // ---------------------------------------------------------------

    /** Stubs the Map-based query overload (used by the no-parameter finders). */
    @SuppressWarnings("unchecked")
    private void stubMapQueryRunsRowMapper() {
        given(jdbc.query(anyString(), anyMap(), any(RowMapper.class)))
                .willAnswer(inv -> {
                    RowMapper<?> rm = inv.getArgument(2);
                    return Collections.singletonList(rm.mapRow(rs, 0));
                });
    }

    /** Stubs the SqlParameterSource-based query overload (used by the parameterised finders). */
    @SuppressWarnings("unchecked")
    private void stubParamQueryRunsRowMapper() {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willAnswer(inv -> {
                    RowMapper<?> rm = inv.getArgument(2);
                    return Collections.singletonList(rm.mapRow(rs, 0));
                });
    }

    private void stubLookupRow(String code, String description) throws Exception {
        given(rs.getString("code")).willReturn(code);
        given(rs.getString("description")).willReturn(description);
    }

    @SuppressWarnings("unchecked")
    private String captureMapQuerySql() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), anyMap(), any(RowMapper.class));
        return sqlCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private String captureParamQuerySql() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        return sqlCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private MapSqlParameterSource captureParamQueryParams() {
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), paramsCaptor.capture(), any(RowMapper.class));
        return paramsCaptor.getValue();
    }

    // ---------------------------------------------------------------
    // findSpeciesGroupCode
    // ---------------------------------------------------------------

    @Test
    void findSpeciesGroupCode_rowFound_returnsFirstCode_andBindsSpeciesParam() throws Exception {
        given(rs.getString("code")).willReturn("HEM");
        stubParamQueryRunsRowMapper();

        Optional<String> result = repo.findSpeciesGroupCode("HB");

        assertThat(result).contains("HEM");
        assertThat(captureParamQuerySql())
                .contains("THE.log_sale_species_group_list")
                .contains("log_sale_species_code = :species")
                .contains("ORDER BY log_sale_species_group_code");
        assertThat(captureParamQueryParams().getValue("species")).isEqualTo("HB");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findSpeciesGroupCode_noRows_returnsEmpty() {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());

        assertThat(repo.findSpeciesGroupCode("XX")).isEmpty();
    }

    @Test
    void findSpeciesGroupCode_nullCodeColumn_returnsEmpty() {
        // rs.getString("code") returns null by default — Optional.ofNullable collapses to empty
        stubParamQueryRunsRowMapper();

        assertThat(repo.findSpeciesGroupCode("HB")).isEmpty();
    }

    // ---------------------------------------------------------------
    // findMaturityCodes
    // ---------------------------------------------------------------

    @Test
    void findMaturityCodes_mapsCodeAndDescription() throws Exception {
        stubLookupRow("M", "Mature");
        stubMapQueryRunsRowMapper();

        List<LookupItem> result = repo.findMaturityCodes();

        assertThat(result).containsExactly(new LookupItem("M", "Mature"));
        assertThat(captureMapQuerySql())
                .contains("THE.log_sale_type_code")
                .contains("effective_date <= SYSDATE")
                .contains("ORDER BY log_sale_type_code");
    }

    @Test
    void findMaturityCodes_nullDescription_mapsNullField() throws Exception {
        stubLookupRow("M", null);
        stubMapQueryRunsRowMapper();

        assertThat(repo.findMaturityCodes()).containsExactly(new LookupItem("M", null));
    }

    // ---------------------------------------------------------------
    // findInvoiceTypes
    // ---------------------------------------------------------------

    @Test
    void findInvoiceTypes_mapsCodeAndDescription() throws Exception {
        stubLookupRow("SI", "Standard Invoice");
        stubMapQueryRunsRowMapper();

        List<LookupItem> result = repo.findInvoiceTypes();

        assertThat(result).containsExactly(new LookupItem("SI", "Standard Invoice"));
        assertThat(captureMapQuerySql())
                .contains("THE.csp_invoice_type_code")
                .contains("ORDER BY csp_invoice_type_code");
    }

    // ---------------------------------------------------------------
    // findInvoiceStatuses — legacy query has no ORDER BY
    // ---------------------------------------------------------------

    @Test
    void findInvoiceStatuses_mapsRow_andHasNoOrderBy() throws Exception {
        stubLookupRow("APP", "Approved");
        stubMapQueryRunsRowMapper();

        List<LookupItem> result = repo.findInvoiceStatuses();

        assertThat(result).containsExactly(new LookupItem("APP", "Approved"));
        String sql = captureMapQuerySql();
        assertThat(sql)
                .contains("THE.log_sale_entry_status_code")
                .doesNotContain("ORDER BY");
    }

    // ---------------------------------------------------------------
    // findSubmissionStatuses — expiry-only filter
    // ---------------------------------------------------------------

    @Test
    void findSubmissionStatuses_mapsRow_andFiltersOnExpiryOnly() throws Exception {
        stubLookupRow("INB", "In Progress");
        stubMapQueryRunsRowMapper();

        List<LookupItem> result = repo.findSubmissionStatuses();

        assertThat(result).containsExactly(new LookupItem("INB", "In Progress"));
        assertThat(captureMapQuerySql())
                .contains("THE.csp_submission_status_code")
                .contains("expiry_date > SYSDATE")
                .contains("ORDER BY csp_submission_status_code")
                .doesNotContain("effective_date");
    }

    // ---------------------------------------------------------------
    // findSortCodes
    // ---------------------------------------------------------------

    @Test
    void findSortCodes_mapsCodeAndDescription() throws Exception {
        stubLookupRow("01", "Sort one");
        stubMapQueryRunsRowMapper();

        List<LookupItem> result = repo.findSortCodes();

        assertThat(result).containsExactly(new LookupItem("01", "Sort one"));
        assertThat(captureMapQuerySql())
                .contains("THE.log_sale_sort_code")
                .contains("ORDER BY log_sale_sort_code");
    }

    // ---------------------------------------------------------------
    // findSpeciesCodes
    // ---------------------------------------------------------------

    @Test
    void findSpeciesCodes_mapsCodeAndDescription() throws Exception {
        stubLookupRow("FIR", "Fir");
        stubMapQueryRunsRowMapper();

        List<LookupItem> result = repo.findSpeciesCodes();

        assertThat(result).containsExactly(new LookupItem("FIR", "Fir"));
        assertThat(captureMapQuerySql())
                .contains("THE.log_sale_species_code")
                .contains("ORDER BY log_sale_species_code");
    }

    // ---------------------------------------------------------------
    // findModellingCodes — expiry-only filter, no ORDER BY
    // ---------------------------------------------------------------

    @Test
    void findModellingCodes_mapsRow_expiryFilterOnly_noOrderBy() throws Exception {
        stubLookupRow("MC1", "Modelling one");
        stubMapQueryRunsRowMapper();

        List<LookupItem> result = repo.findModellingCodes();

        assertThat(result).containsExactly(new LookupItem("MC1", "Modelling one"));
        assertThat(captureMapQuerySql())
                .contains("THE.csp_modelling_code")
                .contains("expiry_date > SYSDATE")
                .doesNotContain("ORDER BY")
                .doesNotContain("effective_date");
    }

    // ---------------------------------------------------------------
    // findFobCodes
    // ---------------------------------------------------------------

    @Test
    void findFobCodes_mapsCodeAndDescription() throws Exception {
        stubLookupRow("W", "Water");
        stubMapQueryRunsRowMapper();

        List<LookupItem> result = repo.findFobCodes();

        assertThat(result).containsExactly(new LookupItem("W", "Water"));
        assertThat(captureMapQuerySql())
                .contains("THE.log_sale_fob_location_code")
                .contains("ORDER BY log_sale_fob_location_code");
    }

    // ---------------------------------------------------------------
    // findGradeCodes
    // ---------------------------------------------------------------

    @Test
    void findGradeCodes_mapsCodeAndDescription() throws Exception {
        stubLookupRow("J", "J grade");
        stubMapQueryRunsRowMapper();

        List<LookupItem> result = repo.findGradeCodes();

        assertThat(result).containsExactly(new LookupItem("J", "J grade"));
        assertThat(captureMapQuerySql())
                .contains("THE.log_sale_grade_code")
                .contains("ORDER BY log_sale_grade_code");
    }

    // ---------------------------------------------------------------
    // findSpeciesGradeCombinations
    // ---------------------------------------------------------------

    @Test
    void findSpeciesGradeCombinations_mapsSpeciesAndGrade() throws Exception {
        given(rs.getString("species")).willReturn("FIR");
        given(rs.getString("grade")).willReturn("J");
        stubMapQueryRunsRowMapper();

        List<LookupRepository.SpeciesGradeCombo> result = repo.findSpeciesGradeCombinations();

        assertThat(result).containsExactly(new LookupRepository.SpeciesGradeCombo("FIR", "J"));
        assertThat(captureMapQuerySql())
                .contains("THE.csp_species_grade_xref")
                .contains("ORDER BY log_sale_species_code, log_sale_grade_code");
    }

    // ---------------------------------------------------------------
    // findGradesBySpecies
    // ---------------------------------------------------------------

    @Test
    void findGradesBySpecies_mapsRow_andBindsSpeciesParam() throws Exception {
        stubLookupRow("J", "J grade");
        stubParamQueryRunsRowMapper();

        List<LookupItem> result = repo.findGradesBySpecies("FIR");

        assertThat(result).containsExactly(new LookupItem("J", "J grade"));
        assertThat(captureParamQuerySql())
                .contains("THE.csp_species_grade_xref x")
                .contains("JOIN THE.log_sale_grade_code g")
                .contains("x.log_sale_species_code = :species")
                .contains("ORDER BY g.log_sale_grade_code");
        assertThat(captureParamQueryParams().getValue("species")).isEqualTo("FIR");
    }
}
