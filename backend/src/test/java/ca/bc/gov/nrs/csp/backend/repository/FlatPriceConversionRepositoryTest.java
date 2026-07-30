package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.service.model.FlatPriceConversion;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for FlatPriceConversionRepository SQL construction, parameter
 * binding and row mapping.
 *
 * <p>The NamedParameterJdbcTemplate is mocked; SQL strings and parameter
 * sources are captured and inspected, and row mappers are executed against a
 * mocked ResultSet so mapping logic runs without a database.</p>
 */
@ExtendWith(MockitoExtension.class)
class FlatPriceConversionRepositoryTest {

    @Mock NamedParameterJdbcTemplate jdbc;

    FlatPriceConversionRepository repo;

    @BeforeEach
    void setUp() {
        repo = new FlatPriceConversionRepository(jdbc);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void stubQueryEmpty() {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private void stubQueryMapsRow(ResultSet rs) {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willAnswer(inv -> {
                    RowMapper<Object> rowMapper = inv.getArgument(2);
                    return Collections.singletonList(rowMapper.mapRow(rs, 0));
                });
    }

    @SuppressWarnings("unchecked")
    private String captureQuerySql() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        return sqlCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private MapSqlParameterSource captureQueryParams() {
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), paramsCaptor.capture(), any(RowMapper.class));
        return paramsCaptor.getValue();
    }

    private String captureUpdateSql() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sqlCaptor.capture(), any(MapSqlParameterSource.class));
        return sqlCaptor.getValue();
    }

    private MapSqlParameterSource captureUpdateParams() {
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), paramsCaptor.capture());
        return paramsCaptor.getValue();
    }

    /** ResultSet stubbed with a value for every column read by mapRow. */
    private ResultSet fullRow() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getLong("log_sale_flat_price_cnvrsn_id")).willReturn(77L);
        given(rs.wasNull()).willReturn(false);
        given(rs.getString("csp_modelling_code")).willReturn("P");
        given(rs.getString("log_sale_type_code")).willReturn("I");
        given(rs.getString("log_sale_species_code")).willReturn("FI");
        given(rs.getString("log_sale_grade_code")).willReturn("H");
        given(rs.getString("log_sale_sort_code")).willReturn("212");
        given(rs.getObject("flat_price_conversion", Integer.class)).willReturn(85);
        given(rs.getDate("effective_date")).willReturn(Date.valueOf(LocalDate.of(2024, Month.JANUARY, 1)));
        given(rs.getDate("expiry_date")).willReturn(Date.valueOf(LocalDate.of(2025, Month.DECEMBER, 31)));
        given(rs.getObject("revision_count", Integer.class)).willReturn(3);
        given(rs.getString("entry_userid")).willReturn("ENTRY_USER");
        given(rs.getDate("entry_timestamp")).willReturn(Date.valueOf(LocalDate.of(2024, Month.JANUARY, 2)));
        given(rs.getString("update_userid")).willReturn("UPDATE_USER");
        given(rs.getDate("update_timestamp")).willReturn(Date.valueOf(LocalDate.of(2024, Month.FEBRUARY, 3)));
        return rs;
    }

    private static FlatPriceConversion inputRecord(LocalDate effectiveDate, LocalDate expiryDate) {
        return new FlatPriceConversion(
                null, "1", "I", "FI", "H", "212", 85,
                effectiveDate, expiryDate, null, null, null, null, null);
    }

    // ---------------------------------------------------------------
    // search — dynamic WHERE construction
    // ---------------------------------------------------------------

    @Test
    void search_onlyModellingCode_noOptionalFragmentsInSql() {
        stubQueryEmpty();
        repo.search("P", null, null, null, null);

        String sql = captureQuerySql();
        assertThat(sql)
                .contains("f.csp_modelling_code = :modellingCode")
                .doesNotContain(":maturity")
                .doesNotContain(":sortCode")
                .doesNotContain(":species")
                .doesNotContain(":grade")
                .contains("ORDER BY x.log_sale_species_code, x.log_sale_grade_code");

        MapSqlParameterSource params = captureQueryParams();
        assertThat(params.getValue("modellingCode")).isEqualTo("P");
        assertThat(params.getValues()).doesNotContainKeys("maturity", "sortCode", "species", "grade");
    }

    @Test
    void search_allFilters_appendsAllFragmentsAndBindsAllParams() {
        stubQueryEmpty();
        repo.search("1", "I", "212", "FI", "H");

        String sql = captureQuerySql();
        assertThat(sql)
                .contains("AND f.log_sale_type_code = :maturity")
                .contains("AND f.log_sale_sort_code = :sortCode")
                .contains("AND x.log_sale_species_code = :species")
                .contains("AND x.log_sale_grade_code = :grade");

        MapSqlParameterSource params = captureQueryParams();
        assertThat(params.getValue("modellingCode")).isEqualTo("1");
        assertThat(params.getValue("maturity")).isEqualTo("I");
        assertThat(params.getValue("sortCode")).isEqualTo("212");
        assertThat(params.getValue("species")).isEqualTo("FI");
        assertThat(params.getValue("grade")).isEqualTo("H");
    }

    // ---------------------------------------------------------------
    // search — row mapping
    // ---------------------------------------------------------------

    @Test
    void search_mapsEveryColumnOfTheRow() throws SQLException {
        stubQueryMapsRow(fullRow());

        List<FlatPriceConversion> results = repo.search("P", null, null, null, null);

        assertThat(results).hasSize(1);
        FlatPriceConversion row = results.getFirst();
        assertThat(row.id()).isEqualTo(77L);
        assertThat(row.modellingCode()).isEqualTo("P");
        assertThat(row.maturity()).isEqualTo("I");
        assertThat(row.species()).isEqualTo("FI");
        assertThat(row.grade()).isEqualTo("H");
        assertThat(row.sortCode()).isEqualTo("212");
        assertThat(row.flatPriceConversion()).isEqualTo(85);
        assertThat(row.effectiveDate()).isEqualTo(LocalDate.of(2024, Month.JANUARY, 1));
        assertThat(row.expiryDate()).isEqualTo(LocalDate.of(2025, Month.DECEMBER, 31));
        assertThat(row.revisionCount()).isEqualTo(3);
        assertThat(row.entryUserid()).isEqualTo("ENTRY_USER");
        assertThat(row.entryTimestamp()).isEqualTo(LocalDate.of(2024, Month.JANUARY, 2));
        assertThat(row.updateUserid()).isEqualTo("UPDATE_USER");
        assertThat(row.updateTimestamp()).isEqualTo(LocalDate.of(2024, Month.FEBRUARY, 3));
    }

    @Test
    void search_mapsNullableColumnsToNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // getLong returns 0 by default; wasNull=true makes the nullable id null.
        given(rs.wasNull()).willReturn(true);
        // All other getters default to null on the mock.
        stubQueryMapsRow(rs);

        List<FlatPriceConversion> results = repo.search("P", null, null, null, null);

        assertThat(results).hasSize(1);
        FlatPriceConversion row = results.getFirst();
        assertThat(row.id()).isNull();
        assertThat(row.modellingCode()).isNull();
        assertThat(row.maturity()).isNull();
        assertThat(row.species()).isNull();
        assertThat(row.grade()).isNull();
        assertThat(row.sortCode()).isNull();
        assertThat(row.flatPriceConversion()).isNull();
        assertThat(row.effectiveDate()).isNull();
        assertThat(row.expiryDate()).isNull();
        assertThat(row.revisionCount()).isNull();
        assertThat(row.entryUserid()).isNull();
        assertThat(row.entryTimestamp()).isNull();
        assertThat(row.updateUserid()).isNull();
        assertThat(row.updateTimestamp()).isNull();
    }

    // ---------------------------------------------------------------
    // findApplicableFactor
    // ---------------------------------------------------------------

    @Test
    void findApplicableFactor_found_returnsFactorAndBindsParams() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getObject("flat_price_conversion", Integer.class)).willReturn(85);
        stubQueryMapsRow(rs);

        Optional<Integer> factor = repo.findApplicableFactor(
                "I", "212", "FI", "H", LocalDate.of(2024, Month.JUNE, 15));

        assertThat(factor).contains(85);

        String sql = captureQuerySql();
        assertThat(sql)
                .contains("f.csp_modelling_code = 'P'")
                .contains("f.effective_date <= :invoiceDate")
                .contains("(f.expiry_date IS NULL OR f.expiry_date >= :invoiceDate)")
                .contains("ORDER BY f.effective_date DESC");

        MapSqlParameterSource params = captureQueryParams();
        assertThat(params.getValue("maturity")).isEqualTo("I");
        assertThat(params.getValue("sortCode")).isEqualTo("212");
        assertThat(params.getValue("species")).isEqualTo("FI");
        assertThat(params.getValue("grade")).isEqualTo("H");
        assertThat(params.getValue("invoiceDate")).isEqualTo(Date.valueOf(LocalDate.of(2024, Month.JUNE, 15)));
    }

    @Test
    void findApplicableFactor_noRows_returnsEmpty() {
        stubQueryEmpty();

        assertThat(repo.findApplicableFactor("I", "212", "FI", "H", LocalDate.of(2024, Month.JUNE, 15)))
                .isEmpty();
    }

    @Test
    void findApplicableFactor_nullFactorColumn_returnsEmpty() {
        // Row exists but the column value is NULL — Optional.ofNullable(null).
        ResultSet rs = mock(ResultSet.class);
        stubQueryMapsRow(rs);

        assertThat(repo.findApplicableFactor("I", "212", "FI", "H", LocalDate.of(2024, Month.JUNE, 15)))
                .isEmpty();
    }

    @Test
    void findApplicableFactor_nullInvoiceDate_bindsNullParam() {
        stubQueryEmpty();
        repo.findApplicableFactor("I", "212", "FI", "H", null);

        MapSqlParameterSource params = captureQueryParams();
        assertThat(params.hasValue("invoiceDate")).isTrue();
        assertThat(params.getValue("invoiceDate")).isNull();
    }

    // ---------------------------------------------------------------
    // existsForSortCode
    // ---------------------------------------------------------------

    @Test
    void existsForSortCode_positiveCount_returnsTrue() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(2);

        assertThat(repo.existsForSortCode("212")).isTrue();

        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(sqlCaptor.capture(), paramsCaptor.capture(), eq(Integer.class));
        assertThat(sqlCaptor.getValue()).contains("csp_modelling_code = 'P' AND log_sale_sort_code = :sortCode");
        assertThat(paramsCaptor.getValue().getValue("sortCode")).isEqualTo("212");
    }

    @Test
    void existsForSortCode_zeroCount_returnsFalse() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(0);

        assertThat(repo.existsForSortCode("212")).isFalse();
    }

    @Test
    void existsForSortCode_nullCount_returnsFalse() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(null);

        assertThat(repo.existsForSortCode("212")).isFalse();
    }

    // ---------------------------------------------------------------
    // findById
    // ---------------------------------------------------------------

    @Test
    void findById_found_returnsMappedRecordAndBindsId() throws SQLException {
        stubQueryMapsRow(fullRow());

        Optional<FlatPriceConversion> result = repo.findById(77L);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(77L);
        assertThat(result.get().modellingCode()).isEqualTo("P");

        String sql = captureQuerySql();
        assertThat(sql).contains("WHERE f.log_sale_flat_price_cnvrsn_id = :id");

        MapSqlParameterSource params = captureQueryParams();
        assertThat(params.getValue("id")).isEqualTo(77L);
    }

    @Test
    void findById_noRows_returnsEmpty() {
        stubQueryEmpty();

        assertThat(repo.findById(99L)).isEmpty();
    }

    // ---------------------------------------------------------------
    // insert
    // ---------------------------------------------------------------

    @Test
    void insert_withDates_bindsAllParamsAsSqlDates() {
        repo.insert(inputRecord(LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2025, Month.DECEMBER, 31)), 55L, "USER1");

        String sql = captureUpdateSql();
        assertThat(sql)
                .contains("INSERT INTO THE.log_sale_flat_price_conversion")
                .contains("LOG_SALE_FLAT_PRICE_CNVRSN_SEQ.nextval");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("modellingCode")).isEqualTo("1");
        assertThat(params.getValue("sortCode")).isEqualTo("212");
        assertThat(params.getValue("xrefId")).isEqualTo(55L);
        assertThat(params.getValue("maturity")).isEqualTo("I");
        assertThat(params.getValue("flatPriceConversion")).isEqualTo(85);
        assertThat(params.getValue("effectiveDate")).isEqualTo(Date.valueOf(LocalDate.of(2024, Month.JANUARY, 1)));
        assertThat(params.getValue("expiryDate")).isEqualTo(Date.valueOf(LocalDate.of(2025, Month.DECEMBER, 31)));
        assertThat(params.getValue("userId")).isEqualTo("USER1");
    }

    @Test
    void insert_withNullDates_bindsNullDateParams() {
        repo.insert(inputRecord(null, null), 55L, "USER1");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("effectiveDate")).isNull();
        assertThat(params.getValue("expiryDate")).isNull();
    }

    // ---------------------------------------------------------------
    // update
    // ---------------------------------------------------------------

    @Test
    void update_withDates_bindsAllParamsAndTargetsId() {
        repo.update(77L, inputRecord(LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2025, Month.DECEMBER, 31)), 55L, "USER1");

        String sql = captureUpdateSql();
        assertThat(sql)
                .contains("UPDATE THE.log_sale_flat_price_conversion SET")
                .contains("revision_count            = revision_count + 1")
                .contains("WHERE log_sale_flat_price_cnvrsn_id = :id");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("id")).isEqualTo(77L);
        assertThat(params.getValue("sortCode")).isEqualTo("212");
        assertThat(params.getValue("xrefId")).isEqualTo(55L);
        assertThat(params.getValue("maturity")).isEqualTo("I");
        assertThat(params.getValue("flatPriceConversion")).isEqualTo(85);
        assertThat(params.getValue("effectiveDate")).isEqualTo(Date.valueOf(LocalDate.of(2024, Month.JANUARY, 1)));
        assertThat(params.getValue("expiryDate")).isEqualTo(Date.valueOf(LocalDate.of(2025, Month.DECEMBER, 31)));
        assertThat(params.getValue("userId")).isEqualTo("USER1");
    }

    @Test
    void update_withNullDates_bindsNullDateParams() {
        repo.update(77L, inputRecord(null, null), 55L, "USER1");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("effectiveDate")).isNull();
        assertThat(params.getValue("expiryDate")).isNull();
    }

    // ---------------------------------------------------------------
    // auditDelete / deleteById
    // ---------------------------------------------------------------

    @Test
    void auditDelete_insertsAuditRowWithDeleteActionAndBindsIdAndUser() {
        repo.auditDelete(77L, "USER1");

        String sql = captureUpdateSql();
        assertThat(sql)
                .contains("INSERT INTO THE.log_sale_flat_price_cnvrsn_aud")
                .contains("SELECT 'D'")
                .contains("WHERE t.log_sale_flat_price_cnvrsn_id = :id");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("id")).isEqualTo(77L);
        assertThat(params.getValue("userId")).isEqualTo("USER1");
    }

    @Test
    void deleteById_deletesByPrimaryKey() {
        repo.deleteById(77L);

        String sql = captureUpdateSql();
        assertThat(sql).contains("DELETE FROM THE.log_sale_flat_price_conversion WHERE log_sale_flat_price_cnvrsn_id = :id");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("id")).isEqualTo(77L);
    }

    // ---------------------------------------------------------------
    // copy — audit target, delete target, insert from source (in order)
    // ---------------------------------------------------------------

    @Test
    void copy_auditsThenDeletesTargetThenInsertsFromSource() {
        repo.copy("P", "1", "USER1");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc, times(3)).update(sqlCaptor.capture(), paramsCaptor.capture());

        List<String> sqls = sqlCaptor.getAllValues();
        List<MapSqlParameterSource> params = paramsCaptor.getAllValues();

        // 1. Audit the rows about to be replaced in the target.
        assertThat(sqls.getFirst())
                .contains("INSERT INTO THE.log_sale_flat_price_cnvrsn_aud")
                .contains("WHERE t.csp_modelling_code = :targetCode");
        assertThat(params.getFirst().getValue("targetCode")).isEqualTo("1");
        assertThat(params.getFirst().getValue("userId")).isEqualTo("USER1");

        // 2. Delete the target rows.
        assertThat(sqls.get(1))
                .contains("DELETE FROM THE.log_sale_flat_price_conversion WHERE csp_modelling_code = :targetCode");
        assertThat(params.get(1).getValue("targetCode")).isEqualTo("1");

        // 3. Insert unexpired rows from the source under the target code.
        assertThat(sqls.get(2))
                .contains("SELECT LOG_SALE_FLAT_PRICE_CNVRSN_SEQ.nextval, :targetCode")
                .contains("WHERE t.csp_modelling_code = :sourceCode")
                .contains("(t.expiry_date > SYSDATE OR t.expiry_date IS NULL)");
        assertThat(params.get(2).getValue("sourceCode")).isEqualTo("P");
        assertThat(params.get(2).getValue("targetCode")).isEqualTo("1");
        assertThat(params.get(2).getValue("userId")).isEqualTo("USER1");
    }

    // ---------------------------------------------------------------
    // existsByModellingCode
    // ---------------------------------------------------------------

    @Test
    void existsByModellingCode_positiveCount_returnsTrue() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(5);

        assertThat(repo.existsByModellingCode("P")).isTrue();

        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(anyString(), paramsCaptor.capture(), eq(Integer.class));
        assertThat(paramsCaptor.getValue().getValue("modellingCode")).isEqualTo("P");
    }

    @Test
    void existsByModellingCode_zeroCount_returnsFalse() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(0);

        assertThat(repo.existsByModellingCode("P")).isFalse();
    }

    @Test
    void existsByModellingCode_nullCount_returnsFalse() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(null);

        assertThat(repo.existsByModellingCode("P")).isFalse();
    }

    // ---------------------------------------------------------------
    // clearAll
    // ---------------------------------------------------------------

    @Test
    void clearAll_deletesAllRowsForModellingCode() {
        repo.clearAll("1");

        String sql = captureUpdateSql();
        assertThat(sql).contains("DELETE FROM THE.log_sale_flat_price_conversion WHERE csp_modelling_code = :modellingCode");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("modellingCode")).isEqualTo("1");
    }
}
