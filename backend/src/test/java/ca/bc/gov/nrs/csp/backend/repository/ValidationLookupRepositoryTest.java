package ca.bc.gov.nrs.csp.backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for ValidationLookupRepository.
 *
 * <p>The NamedParameterJdbcTemplate is mocked; SQL strings and parameter sources
 * are captured and inspected, so no real database connection is needed. Null-argument
 * short circuits are verified to skip JDBC entirely.</p>
 */
@ExtendWith(MockitoExtension.class)
class ValidationLookupRepositoryTest {

    private static final LocalDate ON_DATE = LocalDate.of(2024, Month.MARCH, 1);

    @Mock NamedParameterJdbcTemplate jdbc;
    @Mock ResultSet rs;

    ValidationLookupRepository repo;

    @BeforeEach
    void setUp() {
        repo = new ValidationLookupRepository(jdbc);
    }

    // ---------------------------------------------------------------
    // Helpers to stub / capture the COUNT(*) queryForObject call
    // ---------------------------------------------------------------

    private void stubCount(Integer count) {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(count);
    }

    private String captureCountSql() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(sqlCaptor.capture(), any(MapSqlParameterSource.class), eq(Integer.class));
        return sqlCaptor.getValue();
    }

    private MapSqlParameterSource captureCountParams() {
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(anyString(), paramsCaptor.capture(), eq(Integer.class));
        return paramsCaptor.getValue();
    }

    // ---------------------------------------------------------------
    // existsActiveMaturityCode
    // ---------------------------------------------------------------

    @Test
    void existsActiveMaturityCode_nullCode_returnsFalseWithoutJdbc() {
        assertThat(repo.existsActiveMaturityCode(null, ON_DATE)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void existsActiveMaturityCode_nullDate_returnsFalseWithoutJdbc() {
        assertThat(repo.existsActiveMaturityCode("M", null)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void existsActiveMaturityCode_positiveCount_returnsTrue_andBindsParams() {
        stubCount(1);

        assertThat(repo.existsActiveMaturityCode("M", ON_DATE)).isTrue();

        String sql = captureCountSql();
        assertThat(sql)
                .contains("THE.log_sale_type_code")
                .contains("log_sale_type_code = :code")
                .contains("effective_date <= :onDate")
                .contains("expiry_date IS NULL OR expiry_date >= :onDate");

        MapSqlParameterSource params = captureCountParams();
        assertThat(params.getValue("code")).isEqualTo("M");
        assertThat(params.getValue("onDate")).isEqualTo(Date.valueOf(ON_DATE));
    }

    @Test
    void existsActiveMaturityCode_zeroCount_returnsFalse() {
        stubCount(0);
        assertThat(repo.existsActiveMaturityCode("M", ON_DATE)).isFalse();
    }

    @Test
    void existsActiveMaturityCode_nullCount_returnsFalse() {
        stubCount(null);
        assertThat(repo.existsActiveMaturityCode("M", ON_DATE)).isFalse();
    }

    // ---------------------------------------------------------------
    // existsActiveInvoiceTypeCode
    // ---------------------------------------------------------------

    @Test
    void existsActiveInvoiceTypeCode_nullArgs_returnsFalseWithoutJdbc() {
        assertThat(repo.existsActiveInvoiceTypeCode(null, ON_DATE)).isFalse();
        assertThat(repo.existsActiveInvoiceTypeCode("SI", null)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void existsActiveInvoiceTypeCode_positiveCount_returnsTrue_andBindsParams() {
        stubCount(2);

        assertThat(repo.existsActiveInvoiceTypeCode("SI", ON_DATE)).isTrue();

        String sql = captureCountSql();
        assertThat(sql)
                .contains("THE.csp_invoice_type_code")
                .contains("csp_invoice_type_code = :code");

        MapSqlParameterSource params = captureCountParams();
        assertThat(params.getValue("code")).isEqualTo("SI");
        assertThat(params.getValue("onDate")).isEqualTo(Date.valueOf(ON_DATE));
    }

    @Test
    void existsActiveInvoiceTypeCode_zeroCount_returnsFalse() {
        stubCount(0);
        assertThat(repo.existsActiveInvoiceTypeCode("SI", ON_DATE)).isFalse();
    }

    // ---------------------------------------------------------------
    // existsActiveSortCode
    // ---------------------------------------------------------------

    @Test
    void existsActiveSortCode_nullArgs_returnsFalseWithoutJdbc() {
        assertThat(repo.existsActiveSortCode(null, ON_DATE)).isFalse();
        assertThat(repo.existsActiveSortCode("01", null)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void existsActiveSortCode_positiveCount_returnsTrue_andBindsParams() {
        stubCount(1);

        assertThat(repo.existsActiveSortCode("01", ON_DATE)).isTrue();

        String sql = captureCountSql();
        assertThat(sql)
                .contains("THE.log_sale_sort_code")
                .contains("log_sale_sort_code = :code");

        MapSqlParameterSource params = captureCountParams();
        assertThat(params.getValue("code")).isEqualTo("01");
        assertThat(params.getValue("onDate")).isEqualTo(Date.valueOf(ON_DATE));
    }

    @Test
    void existsActiveSortCode_zeroCount_returnsFalse() {
        stubCount(0);
        assertThat(repo.existsActiveSortCode("01", ON_DATE)).isFalse();
    }

    // ---------------------------------------------------------------
    // existsSpeciesGradeCombination
    // ---------------------------------------------------------------

    @Test
    void existsSpeciesGradeCombination_nullArgs_returnsFalseWithoutJdbc() {
        assertThat(repo.existsSpeciesGradeCombination(null, "J")).isFalse();
        assertThat(repo.existsSpeciesGradeCombination("FIR", null)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void existsSpeciesGradeCombination_positiveCount_returnsTrue_andBindsParams() {
        stubCount(1);

        assertThat(repo.existsSpeciesGradeCombination("FIR", "J")).isTrue();

        String sql = captureCountSql();
        assertThat(sql)
                .contains("THE.csp_species_grade_xref")
                .contains("log_sale_species_code = :species AND log_sale_grade_code = :grade");

        MapSqlParameterSource params = captureCountParams();
        assertThat(params.getValue("species")).isEqualTo("FIR");
        assertThat(params.getValue("grade")).isEqualTo("J");
    }

    @Test
    void existsSpeciesGradeCombination_zeroCount_returnsFalse() {
        stubCount(0);
        assertThat(repo.existsSpeciesGradeCombination("FIR", "J")).isFalse();
    }

    // ---------------------------------------------------------------
    // existsClientLocation
    // ---------------------------------------------------------------

    @Test
    void existsClientLocation_nullArgs_returnsFalseWithoutJdbc() {
        assertThat(repo.existsClientLocation(null, "00")).isFalse();
        assertThat(repo.existsClientLocation("00012345", null)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void existsClientLocation_positiveCount_returnsTrue_andBindsParams() {
        stubCount(1);

        assertThat(repo.existsClientLocation("00012345", "00")).isTrue();

        String sql = captureCountSql();
        assertThat(sql)
                .contains("THE.client_location")
                .contains("client_number = :clientNumber AND client_locn_code = :clientLocnCode");

        MapSqlParameterSource params = captureCountParams();
        assertThat(params.getValue("clientNumber")).isEqualTo("00012345");
        assertThat(params.getValue("clientLocnCode")).isEqualTo("00");
    }

    @Test
    void existsClientLocation_zeroCount_returnsFalse() {
        stubCount(0);
        assertThat(repo.existsClientLocation("00012345", "00")).isFalse();
    }

    // ---------------------------------------------------------------
    // findSpeciesGradeXrefId — row mapper executed against a mocked ResultSet
    // ---------------------------------------------------------------

    @Test
    void findSpeciesGradeXrefId_nullArgs_returnsEmptyWithoutJdbc() {
        assertThat(repo.findSpeciesGradeXrefId(null, "J", ON_DATE)).isEmpty();
        assertThat(repo.findSpeciesGradeXrefId("FIR", null, ON_DATE)).isEmpty();
        assertThat(repo.findSpeciesGradeXrefId("FIR", "J", null)).isEmpty();
        verifyNoInteractions(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findSpeciesGradeXrefId_noRows_returnsEmpty() {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());

        assertThat(repo.findSpeciesGradeXrefId("FIR", "J", ON_DATE)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findSpeciesGradeXrefId_rowFound_mapsIdAndBindsParams() throws Exception {
        given(rs.getLong("csp_species_grade_xref_id")).willReturn(42L);
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willAnswer(inv -> {
                    RowMapper<Long> rm = inv.getArgument(2);
                    return List.of(rm.mapRow(rs, 0));
                });

        Optional<Long> result = repo.findSpeciesGradeXrefId("FIR", "J", ON_DATE);

        assertThat(result).contains(42L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("THE.csp_species_grade_xref")
                .contains("ORDER BY effective_date DESC")
                .contains("FETCH FIRST 1 ROWS ONLY");

        MapSqlParameterSource params = paramsCaptor.getValue();
        assertThat(params.getValue("species")).isEqualTo("FIR");
        assertThat(params.getValue("grade")).isEqualTo("J");
        assertThat(params.getValue("onDate")).isEqualTo(Date.valueOf(ON_DATE));
    }

    // ---------------------------------------------------------------
    // existsSubmissionStatusCode
    // ---------------------------------------------------------------

    @Test
    void existsSubmissionStatusCode_nullOrBlank_returnsFalseWithoutJdbc() {
        assertThat(repo.existsSubmissionStatusCode(null)).isFalse();
        assertThat(repo.existsSubmissionStatusCode("   ")).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void existsSubmissionStatusCode_positiveCount_returnsTrue_andBindsParam() {
        stubCount(1);

        assertThat(repo.existsSubmissionStatusCode("INB")).isTrue();

        String sql = captureCountSql();
        assertThat(sql)
                .contains("THE.csp_submission_status_code")
                .contains("csp_submission_status_code = :code");

        MapSqlParameterSource params = captureCountParams();
        assertThat(params.getValue("code")).isEqualTo("INB");
    }

    @Test
    void existsSubmissionStatusCode_zeroCount_returnsFalse() {
        stubCount(0);
        assertThat(repo.existsSubmissionStatusCode("INB")).isFalse();
    }

    // ---------------------------------------------------------------
    // existsDuplicateFlatPriceConversion — optional excludeId branch
    // ---------------------------------------------------------------

    @Test
    void existsDuplicateFlatPriceConversion_anyNullRequiredArg_returnsFalseWithoutJdbc() {
        assertThat(repo.existsDuplicateFlatPriceConversion(null, "01", 7L, "M", ON_DATE, null)).isFalse();
        assertThat(repo.existsDuplicateFlatPriceConversion("MC", null, 7L, "M", ON_DATE, null)).isFalse();
        assertThat(repo.existsDuplicateFlatPriceConversion("MC", "01", null, "M", ON_DATE, null)).isFalse();
        assertThat(repo.existsDuplicateFlatPriceConversion("MC", "01", 7L, null, ON_DATE, null)).isFalse();
        assertThat(repo.existsDuplicateFlatPriceConversion("MC", "01", 7L, "M", null, null)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void existsDuplicateFlatPriceConversion_withoutExcludeId_omitsExcludeFragment() {
        stubCount(1);

        assertThat(repo.existsDuplicateFlatPriceConversion("MC", "01", 7L, "M", ON_DATE, null)).isTrue();

        String sql = captureCountSql();
        assertThat(sql)
                .contains("THE.log_sale_flat_price_conversion")
                .contains("csp_modelling_code = :modellingCode")
                .contains("log_sale_sort_code = :sortCode")
                .contains("csp_species_grade_xref_id = :xrefId")
                .contains("log_sale_type_code = :maturity")
                .contains("effective_date = :effectiveDate")
                .doesNotContain(":excludeId");

        MapSqlParameterSource params = captureCountParams();
        assertThat(params.getValue("modellingCode")).isEqualTo("MC");
        assertThat(params.getValue("sortCode")).isEqualTo("01");
        assertThat(params.getValue("xrefId")).isEqualTo(7L);
        assertThat(params.getValue("maturity")).isEqualTo("M");
        assertThat(params.getValue("effectiveDate")).isEqualTo(Date.valueOf(ON_DATE));
        assertThat(params.getValues()).doesNotContainKey("excludeId");
    }

    @Test
    void existsDuplicateFlatPriceConversion_withExcludeId_appendsExcludeFragment() {
        stubCount(0);

        assertThat(repo.existsDuplicateFlatPriceConversion("MC", "01", 7L, "M", ON_DATE, 99L)).isFalse();

        String sql = captureCountSql();
        assertThat(sql).contains("log_sale_flat_price_cnvrsn_id <> :excludeId");

        MapSqlParameterSource params = captureCountParams();
        assertThat(params.getValue("excludeId")).isEqualTo(99L);
    }

    @Test
    void existsDuplicateFlatPriceConversion_nullCount_returnsFalse() {
        stubCount(null);
        assertThat(repo.existsDuplicateFlatPriceConversion("MC", "01", 7L, "M", ON_DATE, null)).isFalse();
    }
}
