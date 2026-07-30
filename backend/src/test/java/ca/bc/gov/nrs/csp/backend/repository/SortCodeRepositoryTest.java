package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.service.model.SortCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for SortCodeRepository.
 *
 * <p>The fluent JdbcClient is mocked; row mappers are executed against a mocked
 * ResultSet so column names and mapped fields are verified without a real database
 * connection. SQL strings and bound parameters are captured and inspected.</p>
 */
@ExtendWith(MockitoExtension.class)
class SortCodeRepositoryTest {

    private static final LocalDate EFFECTIVE = LocalDate.of(2024, Month.JANUARY, 1);
    private static final LocalDate EXPIRY = LocalDate.of(2025, Month.DECEMBER, 31);
    private static final LocalDate UPDATED = LocalDate.of(2024, Month.JUNE, 15);

    @Mock JdbcClient jdbc;
    @Mock JdbcClient.StatementSpec spec;
    @Mock JdbcClient.StatementSpec countSpec;
    @Mock ResultSet rs;

    SortCodeRepository repo;

    @BeforeEach
    void setUp() {
        repo = new SortCodeRepository(jdbc);
    }

    // ---------------------------------------------------------------
    // Helpers — stub the fluent JdbcClient chain
    // ---------------------------------------------------------------

    /** Routes data SQL to {@code spec} and COUNT SQL to {@code countSpec}. */
    private void stubSqlRouting() {
        given(jdbc.sql(anyString())).willAnswer(inv ->
                ((String) inv.getArgument(0)).startsWith("SELECT COUNT") ? countSpec : spec);
    }

    /** Stubs {@code spec.query(rowMapper)} to run the mapper against the mocked ResultSet, exposing list(). */
    @SuppressWarnings("unchecked")
    private void stubQueryReturnsList() {
        given(spec.query(any(RowMapper.class))).willAnswer(inv -> {
            RowMapper<SortCode> rm = (RowMapper<SortCode>) inv.getArgument(0);
            // Run the mapper BEFORE stubbing — invoking a mock inside given(...)
            // triggers Mockito's UnfinishedStubbing error.
            List<SortCode> rows = List.of(rm.mapRow(rs, 0));
            JdbcClient.MappedQuerySpec<SortCode> mapped = mock(JdbcClient.MappedQuerySpec.class);
            given(mapped.list()).willReturn(rows);
            return mapped;
        });
    }

    /** Stubs {@code spec.query(rowMapper)} to run the mapper against the mocked ResultSet, exposing optional(). */
    @SuppressWarnings("unchecked")
    private void stubQueryReturnsOptional() {
        given(spec.query(any(RowMapper.class))).willAnswer(inv -> {
            RowMapper<SortCode> rm = (RowMapper<SortCode>) inv.getArgument(0);
            Optional<SortCode> row = Optional.of(rm.mapRow(rs, 0));
            JdbcClient.MappedQuerySpec<SortCode> mapped = mock(JdbcClient.MappedQuerySpec.class);
            given(mapped.optional()).willReturn(row);
            return mapped;
        });
    }

    @SuppressWarnings("unchecked")
    private void stubCountQuery(Long total) {
        JdbcClient.MappedQuerySpec<Long> mapped = mock(JdbcClient.MappedQuerySpec.class);
        given(mapped.single()).willReturn(total);
        given(countSpec.query(Long.class)).willReturn(mapped);
    }

    private void stubFullResultSetRow() throws Exception {
        given(rs.getString("LOG_SALE_SORT_CODE")).willReturn("01");
        given(rs.getString("DESCRIPTION")).willReturn("Sort one");
        given(rs.getDate("EFFECTIVE_DATE")).willReturn(Date.valueOf(EFFECTIVE));
        given(rs.getDate("EXPIRY_DATE")).willReturn(Date.valueOf(EXPIRY));
        given(rs.getDate("UPDATE_TIMESTAMP")).willReturn(Date.valueOf(UPDATED));
    }

    private String captureSql() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).sql(sqlCaptor.capture());
        return sqlCaptor.getValue();
    }

    // ---------------------------------------------------------------
    // findAll (unpaged) — row mapper executed against a mocked ResultSet
    // ---------------------------------------------------------------

    @Test
    void findAll_mapsAllColumns() throws Exception {
        given(jdbc.sql(anyString())).willReturn(spec);
        stubFullResultSetRow();
        stubQueryReturnsList();

        List<SortCode> result = repo.findAll();

        assertThat(result).hasSize(1);
        SortCode row = result.get(0);
        assertThat(row.sortCode()).isEqualTo("01");
        assertThat(row.description()).isEqualTo("Sort one");
        assertThat(row.effectiveDate()).isEqualTo(EFFECTIVE);
        assertThat(row.expiryDate()).isEqualTo(EXPIRY);
        assertThat(row.updateTimestamp()).isEqualTo(UPDATED);

        assertThat(captureSql())
                .contains("FROM THE.LOG_SALE_SORT_CODE")
                .contains("ORDER BY LOG_SALE_SORT_CODE");
    }

    @Test
    void findAll_nullDateColumns_mapToNullLocalDates() throws Exception {
        given(jdbc.sql(anyString())).willReturn(spec);
        // Only the string columns are stubbed; getDate(...) returns null by default
        given(rs.getString("LOG_SALE_SORT_CODE")).willReturn("02");
        given(rs.getString("DESCRIPTION")).willReturn("Sort two");
        stubQueryReturnsList();

        List<SortCode> result = repo.findAll();

        SortCode row = result.get(0);
        assertThat(row.sortCode()).isEqualTo("02");
        assertThat(row.effectiveDate()).isNull();
        assertThat(row.expiryDate()).isNull();
        assertThat(row.updateTimestamp()).isNull();
    }

    // ---------------------------------------------------------------
    // findAll (paged) — SQL construction, paging params, count query
    // ---------------------------------------------------------------

    @Test
    void findAllPaged_defaultSort_buildsSqlWithDefaultOrderByAndBindsPaging() throws Exception {
        stubSqlRouting();
        given(spec.param(anyString(), any())).willReturn(spec);
        stubFullResultSetRow();
        stubQueryReturnsList();
        // Total must be consistent with the page geometry (offset 20 + 1 row),
        // or PageImpl rewrites it to offset + content.size().
        stubCountQuery(100L);

        Page<SortCode> page = repo.findAll(PageRequest.of(1, 20)); // offset = 20, limit = 20

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).sortCode()).isEqualTo("01");
        assertThat(page.getTotalElements()).isEqualTo(100L);

        verify(spec).param("offset", 20L);
        verify(spec).param("limit", 20);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(2)).sql(sqlCaptor.capture());
        String dataSql = sqlCaptor.getAllValues().get(0);
        String countSql = sqlCaptor.getAllValues().get(1);

        assertThat(dataSql)
                .contains("ORDER BY LOG_SALE_SORT_CODE ASC")
                .contains("OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY");
        assertThat(countSql).isEqualTo("SELECT COUNT(*) FROM THE.LOG_SALE_SORT_CODE");
    }

    @Test
    void findAllPaged_singleSortField_buildsOrderByFromWhitelist() throws Exception {
        stubSqlRouting();
        given(spec.param(anyString(), any())).willReturn(spec);
        stubFullResultSetRow();
        stubQueryReturnsList();
        stubCountQuery(1L);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "description"));
        repo.findAll(pageable);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(2)).sql(sqlCaptor.capture());
        assertThat(sqlCaptor.getAllValues().get(0)).contains("ORDER BY DESCRIPTION DESC");
    }

    @Test
    void findAllPaged_multipleSortFields_buildsCommaSeparatedOrderBy() throws Exception {
        stubSqlRouting();
        given(spec.param(anyString(), any())).willReturn(spec);
        stubFullResultSetRow();
        stubQueryReturnsList();
        stubCountQuery(1L);

        Pageable pageable = PageRequest.of(0, 10,
                Sort.by(Sort.Order.asc("effectiveDate"), Sort.Order.desc("expiryDate")));
        repo.findAll(pageable);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(2)).sql(sqlCaptor.capture());
        assertThat(sqlCaptor.getAllValues().get(0))
                .contains("ORDER BY EFFECTIVE_DATE ASC, EXPIRY_DATE DESC");
    }

    @Test
    void findAllPaged_unknownSortField_throwsBadRequestBeforeJdbc() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("unknownField"));

        assertThatThrownBy(() -> repo.findAll(pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported sort field: unknownField");
        verifyNoInteractions(jdbc);
    }

    @Test
    void findAllPaged_nullTotal_returnsZeroTotalElements() throws Exception {
        stubSqlRouting();
        given(spec.param(anyString(), any())).willReturn(spec);
        stubFullResultSetRow();
        stubQueryReturnsList();
        stubCountQuery(null);

        Page<SortCode> page = repo.findAll(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1L); // PageImpl clamps total to content size on first page
    }

    // ---------------------------------------------------------------
    // findByCode — row mapper executed against a mocked ResultSet
    // ---------------------------------------------------------------

    @Test
    void findByCode_rowFound_mapsAllColumns() throws Exception {
        given(jdbc.sql(anyString())).willReturn(spec);
        given(spec.param("code", "01")).willReturn(spec);
        stubFullResultSetRow();
        stubQueryReturnsOptional();

        Optional<SortCode> result = repo.findByCode("01");

        assertThat(result).isPresent();
        SortCode row = result.get();
        assertThat(row.sortCode()).isEqualTo("01");
        assertThat(row.description()).isEqualTo("Sort one");
        assertThat(row.effectiveDate()).isEqualTo(EFFECTIVE);
        assertThat(row.expiryDate()).isEqualTo(EXPIRY);
        assertThat(row.updateTimestamp()).isEqualTo(UPDATED);

        assertThat(captureSql()).contains("WHERE LOG_SALE_SORT_CODE = :code");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCode_noRow_returnsEmpty() {
        given(jdbc.sql(anyString())).willReturn(spec);
        given(spec.param("code", "ZZ")).willReturn(spec);
        JdbcClient.MappedQuerySpec<SortCode> mapped = mock(JdbcClient.MappedQuerySpec.class);
        given(mapped.optional()).willReturn(Optional.empty());
        given(spec.query(any(RowMapper.class))).willReturn(mapped);

        assertThat(repo.findByCode("ZZ")).isEmpty();
    }

    // ---------------------------------------------------------------
    // existsByCode
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void stubExistsCount(Integer count) {
        given(jdbc.sql(anyString())).willReturn(spec);
        given(spec.param(anyString(), any())).willReturn(spec);
        JdbcClient.MappedQuerySpec<Integer> mapped = mock(JdbcClient.MappedQuerySpec.class);
        given(mapped.single()).willReturn(count);
        given(spec.query(Integer.class)).willReturn(mapped);
    }

    @Test
    void existsByCode_positiveCount_returnsTrue_andBindsCodeParam() {
        stubExistsCount(1);

        assertThat(repo.existsByCode("01")).isTrue();

        assertThat(captureSql()).contains("SELECT COUNT(*) FROM THE.LOG_SALE_SORT_CODE WHERE LOG_SALE_SORT_CODE = :code");
        verify(spec).param("code", "01");
    }

    @Test
    void existsByCode_zeroCount_returnsFalse() {
        stubExistsCount(0);
        assertThat(repo.existsByCode("ZZ")).isFalse();
    }

    @Test
    void existsByCode_nullCount_returnsFalse() {
        stubExistsCount(null);
        assertThat(repo.existsByCode("ZZ")).isFalse();
    }

    // ---------------------------------------------------------------
    // insert
    // ---------------------------------------------------------------

    @Test
    void insert_bindsAllParams_asSqlDates() {
        given(jdbc.sql(anyString())).willReturn(spec);
        given(spec.param(anyString(), any())).willReturn(spec);
        given(spec.update()).willReturn(1);

        repo.insert(new SortCode("09", "New sort", EFFECTIVE, EXPIRY, UPDATED));

        assertThat(captureSql()).contains("INSERT INTO THE.LOG_SALE_SORT_CODE");
        verify(spec).param("sortCode", "09");
        verify(spec).param("description", "New sort");
        verify(spec).param("effectiveDate", Date.valueOf(EFFECTIVE));
        verify(spec).param("expiryDate", Date.valueOf(EXPIRY));
        verify(spec).param("updateTimestamp", Date.valueOf(UPDATED));
        verify(spec).update();
    }

    // ---------------------------------------------------------------
    // update
    // ---------------------------------------------------------------

    @Test
    void update_bindsAllParams_includingWhereCode() {
        given(jdbc.sql(anyString())).willReturn(spec);
        given(spec.param(anyString(), any())).willReturn(spec);
        given(spec.update()).willReturn(1);

        repo.update("01", new SortCode("01", "Changed", EFFECTIVE, EXPIRY, UPDATED));

        assertThat(captureSql())
                .contains("UPDATE THE.LOG_SALE_SORT_CODE")
                .contains("WHERE LOG_SALE_SORT_CODE = :code");
        verify(spec).param("description", "Changed");
        verify(spec).param("effectiveDate", Date.valueOf(EFFECTIVE));
        verify(spec).param("expiryDate", Date.valueOf(EXPIRY));
        verify(spec).param("updateTimestamp", Date.valueOf(UPDATED));
        verify(spec).param("code", "01");
        verify(spec).update();
    }

    // ---------------------------------------------------------------
    // delete
    // ---------------------------------------------------------------

    @Test
    void delete_bindsCodeParam() {
        given(jdbc.sql(anyString())).willReturn(spec);
        given(spec.param("code", "01")).willReturn(spec);
        given(spec.update()).willReturn(1);

        repo.delete("01");

        assertThat(captureSql()).isEqualTo("DELETE FROM THE.LOG_SALE_SORT_CODE WHERE LOG_SALE_SORT_CODE = :code");
        verify(spec).update();
    }
}
