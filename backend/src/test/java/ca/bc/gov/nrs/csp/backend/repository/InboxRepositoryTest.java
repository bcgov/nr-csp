package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.service.model.InboxCriteria;
import ca.bc.gov.nrs.csp.backend.service.model.InboxRow;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for InboxRepository SQL construction.
 *
 * <p>These tests verify that each criterion produces the correct SQL fragment and
 * parameter binding, without a real database connection. The NamedParameterJdbcTemplate
 * is mocked and SQL strings are captured and inspected.</p>
 */
@ExtendWith(MockitoExtension.class)
class InboxRepositoryTest {

    private static final Pageable DEFAULT_PAGE = PageRequest.of(0, 100);

    @Mock NamedParameterJdbcTemplate jdbc;

    InboxRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InboxRepository(jdbc);
    }

    // ---------------------------------------------------------------
    // Helpers to stub the two JDBC calls (data + count)
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void stubJdbc() {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .willReturn(0L);
    }

    @SuppressWarnings("unchecked")
    private String captureDataSql() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        return sqlCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private MapSqlParameterSource captureParams() {
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), paramsCaptor.capture(), any(RowMapper.class));
        return paramsCaptor.getValue();
    }

    private InboxCriteria emptyCriteria() {
        return new InboxCriteria(null, null, null, null, null, null, null, null, null);
    }

    // ---------------------------------------------------------------
    // All-blank criteria — no WHERE fragments added
    // ---------------------------------------------------------------

    @Test
    void search_allBlankCriteria_noUserConditionFragmentsInSql() {
        stubJdbc();
        repo.search(emptyCriteria(), DEFAULT_PAGE);

        String sql = captureDataSql();
        // Only the fixed WHERE 1=1 should appear — no dynamic AND clauses
        assertThat(sql)
                .doesNotContain(":startDate")
                .doesNotContain(":endDate")
                .doesNotContain(":invoiceNum")
                .doesNotContain(":submissionStatus")
                .doesNotContain(":clientNum");
    }

    // ---------------------------------------------------------------
    // submissionDateFrom
    // ---------------------------------------------------------------

    @Test
    void search_submissionDateFrom_appendsStartDateFragment() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                LocalDate.of(2024, Month.JANUARY, 1), null, null, null, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql).contains("sub.entry_timestamp >= :startDate");

        MapSqlParameterSource params = captureParams();
        assertThat(params.getValues()).containsKey("startDate");
        // Verify it's a Timestamp (not a date string)
        assertThat(params.getValue("startDate")).isInstanceOf(Timestamp.class);
    }

    // ---------------------------------------------------------------
    // submissionDateTo — must be end-of-day 23:59:59
    // ---------------------------------------------------------------

    @Test
    void search_submissionDateTo_appendsEndDateFragmentWithEndOfDayTime() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, LocalDate.of(2024, Month.MARCH, 15), null, null, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql).contains("sub.entry_timestamp <= :endDate");

        MapSqlParameterSource params = captureParams();
        Timestamp endDate = (Timestamp) params.getValue("endDate");
        assertThat(endDate).isNotNull();

        // Verify end-of-day: the timestamp must be on 2024-03-15 at 23:59:59
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(endDate.getTime());
        assertThat(cal.get(java.util.Calendar.HOUR_OF_DAY)).isEqualTo(23);
        assertThat(cal.get(java.util.Calendar.MINUTE)).isEqualTo(59);
        assertThat(cal.get(java.util.Calendar.SECOND)).isEqualTo(59);
    }

    // ---------------------------------------------------------------
    // submittedBy
    // ---------------------------------------------------------------

    @Test
    void search_submittedByBuyer_appendsBuyerJoinCondition() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, "Buyer", null, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql)
                .contains("inv.BUYER_CLIENT_NUMBER")
                .contains("inv.BUYER_CLIENT_LOCN_CODE")
                .doesNotContain("inv.SELLER_CLIENT_NUMBER");
    }

    @Test
    void search_submittedBySeller_appendsSellerJoinCondition() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, "Seller", null, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql)
                .contains("inv.SELLER_CLIENT_NUMBER")
                .contains("inv.SELLER_CLIENT_LOCN_CODE")
                .doesNotContain("inv.BUYER_CLIENT_NUMBER");
    }

    // ---------------------------------------------------------------
    // submitterClientNum — must use named params, NOT string concatenation
    // ---------------------------------------------------------------

    @Test
    void search_submitterClientNum_usesNamedParams_notStringConcatenation() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, null, null, null, null, "00012345", "00", null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        // Named params must appear in SQL; the raw value must NOT be
        // string-concatenated into the SQL
        assertThat(sql)
                .contains(":clientNum")
                .contains(":clientLoc")
                .doesNotContain("00012345")
                .doesNotContain("'00012345'");

        MapSqlParameterSource params = captureParams();
        assertThat(params.getValue("clientNum")).isEqualTo("00012345");
        assertThat(params.getValue("clientLoc")).isEqualTo("00");
    }

    // ---------------------------------------------------------------
    // submissionType
    // ---------------------------------------------------------------

    @Test
    void search_submissionTypeElectronic_appendsIsNotNullCondition() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, null, "Electronic", null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql()).contains("sub.submission_id IS NOT NULL");
    }

    @Test
    void search_submissionTypeManual_appendsIsNullCondition() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, null, "Manual", null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql()).contains("sub.submission_id IS NULL");
    }

    // ---------------------------------------------------------------
    // invoiceNum (null-safe, trimmed, uppercased, contains LIKE when no wildcards)
    // ---------------------------------------------------------------

    @Test
    void search_nullInvoiceNum_noLikeFragmentInSql() {
        stubJdbc();
        repo.search(emptyCriteria(), DEFAULT_PAGE);

        assertThat(captureDataSql()).doesNotContain(":invoiceNum");
    }

    @Test
    void search_nonBlankInvoiceNum_appendsLikeContainsPattern() {
        stubJdbc();
        // InboxCriteria always receives a value already normalised by InboxService
        // (trimmed + uppercased). Without user wildcards, falls back to a %contains% match
        // (mirrors SearchRepository.toInvoiceNumberPattern).
        InboxCriteria criteria = new InboxCriteria(
                null, null, null, null, null, "ABC", null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql).contains("inv.CLIENT_INVOICE_NO LIKE :invoiceNum");

        MapSqlParameterSource params = captureParams();
        assertThat(params.getValue("invoiceNum")).isEqualTo("%ABC%");
    }

    // ---------------------------------------------------------------
    // keyword — applied to outer subquery WHERE clause
    // ---------------------------------------------------------------

    @Test
    void search_nonBlankKeyword_appendsKeywordWhereClause() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, null, null, null, null, null, null, "hello");
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql)
                .contains("UPPER(submission_id) LIKE UPPER(:keyword)")
                .contains("UPPER(submission_status) LIKE UPPER(:keyword)")
                .contains("UPPER(submission_type) LIKE UPPER(:keyword)")
                .contains("TO_CHAR(entry_timestamp");

        MapSqlParameterSource params = captureParams();
        assertThat(params.getValue("keyword")).isEqualTo("%hello%");
    }

    @Test
    void search_nullKeyword_noKeywordWhereClauseInSql() {
        stubJdbc();
        repo.search(emptyCriteria(), DEFAULT_PAGE);

        assertThat(captureDataSql()).doesNotContain(":keyword");
    }

    @Test
    void search_blankKeyword_noKeywordWhereClauseInSql() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, null, null, null, null, null, null, "   ");
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql()).doesNotContain(":keyword");
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_keyword_appearsInBothDataAndCountSql() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, null, null, null, null, null, null, "sub001");
        repo.search(criteria, DEFAULT_PAGE);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        verify(jdbc).queryForObject(sqlCaptor.capture(), any(MapSqlParameterSource.class), eq(Long.class));

        assertThat(sqlCaptor.getAllValues().get(0)).contains("UPPER(submission_id) LIKE UPPER(:keyword)");
        assertThat(sqlCaptor.getAllValues().get(1)).contains("UPPER(submission_id) LIKE UPPER(:keyword)");
    }

    // ---------------------------------------------------------------
    // submissionStatus
    // ---------------------------------------------------------------

    @Test
    void search_submissionStatus_appendsCodeEqualityCondition() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, null, null, "INB", null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql).contains("sub.CSP_SUBMISSION_STATUS_CODE = :submissionStatus");

        MapSqlParameterSource params = captureParams();
        assertThat(params.getValue("submissionStatus")).isEqualTo("INB");
    }

    // ---------------------------------------------------------------
    // Paging parameters
    // ---------------------------------------------------------------

    @Test
    void search_pagingParams_offsetAndLimitBound() {
        stubJdbc();
        Pageable pageable = PageRequest.of(2, 50); // offset = 100, limit = 50
        repo.search(emptyCriteria(), pageable);

        MapSqlParameterSource params = captureParams();
        assertThat(params.getValue("offset")).isEqualTo(100L);
        assertThat(params.getValue("limit")).isEqualTo(50);
    }

    // ---------------------------------------------------------------
    // Sort whitelist (Q8)
    // ---------------------------------------------------------------

    @Test
    void search_defaultSort_usesEntryTimestampDesc() {
        stubJdbc();
        repo.search(emptyCriteria(), PageRequest.of(0, 100)); // no sort specified

        String sql = captureDataSql();
        assertThat(sql).contains("entry_timestamp DESC");
    }

    @Test
    void search_submissionDateSortAsc_usesEntryTimestampAsc() {
        stubJdbc();
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "submissionDate"));
        repo.search(emptyCriteria(), pageable);

        assertThat(captureDataSql()).contains("entry_timestamp ASC");
    }

    @Test
    void search_invApprovedSortDesc_usesAggregateExpression() {
        stubJdbc();
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "invApproved"));
        repo.search(emptyCriteria(), pageable);

        assertThat(captureDataSql()).contains("inv_approved DESC");
    }

    @Test
    void search_unknownSortField_throwsBadRequest() {
        Pageable pageable = PageRequest.of(0, 100, Sort.by("unknownField"));
        InboxCriteria criteria = emptyCriteria();

        assertThatThrownBy(() -> repo.search(criteria, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported sort field: unknownField");
    }

    // ---------------------------------------------------------------
    // COUNT query uses the same WHERE as the data query
    // ---------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void search_countQueryIncludesSameCriteriaAsDataQuery() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, null, null, "COM", null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        // Capture both the data SQL and count SQL
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        verify(jdbc).queryForObject(sqlCaptor.capture(), any(MapSqlParameterSource.class), eq(Long.class));

        String dataSql = sqlCaptor.getAllValues().get(0);
        String countSql = sqlCaptor.getAllValues().get(1);

        // Both must contain the status condition
        assertThat(dataSql).contains("sub.CSP_SUBMISSION_STATUS_CODE = :submissionStatus");
        assertThat(countSql).contains("sub.CSP_SUBMISSION_STATUS_CODE = :submissionStatus");
    }

    // ---------------------------------------------------------------
    // Return value — Page is assembled correctly
    // ---------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void search_returnsPageWithTotalFromCountQuery() {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .willReturn(42L);

        Page<InboxRow> page = repo.search(emptyCriteria(), DEFAULT_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(42L);
        assertThat(page.getContent()).isEmpty();
    }

    // ---------------------------------------------------------------
    // Row mapper — executes against a mocked ResultSet
    // ---------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void search_rowMapper_mapsAllColumns() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getLong("csp_submission_id")).willReturn(11L);
        given(rs.getLong("coastal_log_sale_id")).willReturn(22L);
        given(rs.wasNull()).willReturn(false);
        given(rs.getString("submission_id")).willReturn("SUB-001");
        given(rs.getDate("entry_timestamp")).willReturn(Date.valueOf(LocalDate.of(2024, Month.FEBRUARY, 10)));
        given(rs.getString("submission_status")).willReturn("In Progress");
        given(rs.getString("submission_type")).willReturn("Electronic");
        given(rs.getObject("inv_total", Integer.class)).willReturn(10);
        given(rs.getObject("inv_approved", Integer.class)).willReturn(4);
        given(rs.getObject("inv_rejected", Integer.class)).willReturn(3);
        given(rs.getObject("inv_processing", Integer.class)).willReturn(2);
        given(rs.getObject("inv_cancelled", Integer.class)).willReturn(1);

        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willAnswer(inv -> {
                    RowMapper<InboxRow> rm = (RowMapper<InboxRow>) inv.getArgument(2);
                    return List.of(rm.mapRow(rs, 0));
                });
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .willReturn(1L);

        Page<InboxRow> page = repo.search(emptyCriteria(), DEFAULT_PAGE);

        assertThat(page.getContent()).hasSize(1);
        InboxRow row = page.getContent().get(0);
        assertThat(row.cspSubmissionId()).isEqualTo(11L);
        assertThat(row.coastalLogSaleId()).isEqualTo(22L);
        assertThat(row.submissionId()).isEqualTo("SUB-001");
        assertThat(row.submissionDate()).isEqualTo(LocalDate.of(2024, Month.FEBRUARY, 10));
        assertThat(row.submissionStatus()).isEqualTo("In Progress");
        assertThat(row.submissionType()).isEqualTo("Electronic");
        assertThat(row.invTotal()).isEqualTo(10);
        assertThat(row.invApproved()).isEqualTo(4);
        assertThat(row.invRejected()).isEqualTo(3);
        assertThat(row.invProcessing()).isEqualTo(2);
        assertThat(row.invCancelled()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_rowMapper_nullColumns_mapToNullFields() throws Exception {
        // Only wasNull() is stubbed: getLong() defaults to 0 with wasNull() = true (→ null Longs),
        // getString()/getDate()/getObject() default to null on an unstubbed mock (Manual rows have
        // a null submission_id, and OUTER counts can come back null).
        ResultSet rs = mock(ResultSet.class);
        given(rs.wasNull()).willReturn(true);

        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willAnswer(inv -> {
                    RowMapper<InboxRow> rm = (RowMapper<InboxRow>) inv.getArgument(2);
                    return List.of(rm.mapRow(rs, 0));
                });
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .willReturn(1L);

        Page<InboxRow> page = repo.search(emptyCriteria(), DEFAULT_PAGE);

        InboxRow row = page.getContent().get(0);
        assertThat(row.cspSubmissionId()).isNull();
        assertThat(row.coastalLogSaleId()).isNull();
        assertThat(row.submissionId()).isNull();
        assertThat(row.submissionDate()).isNull();
        assertThat(row.submissionStatus()).isNull();
        assertThat(row.submissionType()).isNull();
        assertThat(row.invTotal()).isNull();
        assertThat(row.invApproved()).isNull();
        assertThat(row.invRejected()).isNull();
        assertThat(row.invProcessing()).isNull();
        assertThat(row.invCancelled()).isNull();
    }

    // ---------------------------------------------------------------
    // toInvoiceNumberPattern — mirrors SearchRepository exactly
    // ---------------------------------------------------------------

    @Test
    void toInvoiceNumberPattern_plainTerm_becomesContainsMatch() {
        assertThat(InboxRepository.toInvoiceNumberPattern("ABC")).isEqualTo("%ABC%");
    }

    @Test
    void toInvoiceNumberPattern_starBecomesPercent() {
        assertThat(InboxRepository.toInvoiceNumberPattern("WFP521046*")).isEqualTo("WFP521046%");
    }

    @Test
    void toInvoiceNumberPattern_questionMarkBecomesUnderscore() {
        assertThat(InboxRepository.toInvoiceNumberPattern("INV-?-23")).isEqualTo("INV-_-23");
    }

    @Test
    void toInvoiceNumberPattern_percentPassesThroughAsWildcard() {
        assertThat(InboxRepository.toInvoiceNumberPattern("INV-2024-%")).isEqualTo("INV-2024-%");
    }

    @Test
    void toInvoiceNumberPattern_mixedWildcards() {
        assertThat(InboxRepository.toInvoiceNumberPattern("*-2024-?")).isEqualTo("%-2024-_");
    }

    @Test
    void toInvoiceNumberPattern_escapesUnderscoreInPlainTerm() {
        assertThat(InboxRepository.toInvoiceNumberPattern("ab_cd")).isEqualTo("%ab\\_cd%");
    }

    @Test
    void toInvoiceNumberPattern_escapesUnderscoreInPatternMode() {
        assertThat(InboxRepository.toInvoiceNumberPattern("a_b*")).isEqualTo("a\\_b%");
    }

    @Test
    void toInvoiceNumberPattern_escapesBackslash() {
        assertThat(InboxRepository.toInvoiceNumberPattern("a\\b")).isEqualTo("%a\\\\b%");
    }
}
