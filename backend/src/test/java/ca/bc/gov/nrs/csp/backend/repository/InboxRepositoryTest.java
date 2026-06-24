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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
        assertThat(sql).doesNotContain(":startDate");
        assertThat(sql).doesNotContain(":endDate");
        assertThat(sql).doesNotContain(":invoiceNum");
        assertThat(sql).doesNotContain(":submissionStatus");
        assertThat(sql).doesNotContain(":clientNum");
    }

    // ---------------------------------------------------------------
    // submissionDateFrom
    // ---------------------------------------------------------------

    @Test
    void search_submissionDateFrom_appendsStartDateFragment() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                LocalDate.of(2024, 1, 1), null, null, null, null, null, null, null, null);
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
                null, LocalDate.of(2024, 3, 15), null, null, null, null, null, null, null);
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
        assertThat(sql).contains("inv.BUYER_CLIENT_NUMBER");
        assertThat(sql).contains("inv.BUYER_CLIENT_LOCN_CODE");
        assertThat(sql).doesNotContain("inv.SELLER_CLIENT_NUMBER");
    }

    @Test
    void search_submittedBySeller_appendsSellerJoinCondition() {
        stubJdbc();
        InboxCriteria criteria = new InboxCriteria(
                null, null, "Seller", null, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql).contains("inv.SELLER_CLIENT_NUMBER");
        assertThat(sql).contains("inv.SELLER_CLIENT_LOCN_CODE");
        assertThat(sql).doesNotContain("inv.BUYER_CLIENT_NUMBER");
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
        // Named params must appear in SQL
        assertThat(sql).contains(":clientNum");
        assertThat(sql).contains(":clientLoc");
        // The raw value must NOT be string-concatenated into the SQL
        assertThat(sql).doesNotContain("00012345");
        assertThat(sql).doesNotContain("'00012345'");

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
        assertThat(sql).contains("UPPER(submission_id) LIKE UPPER(:keyword)");
        assertThat(sql).contains("UPPER(submission_status) LIKE UPPER(:keyword)");
        assertThat(sql).contains("UPPER(submission_type) LIKE UPPER(:keyword)");
        assertThat(sql).contains("TO_CHAR(entry_timestamp");

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

        assertThatThrownBy(() -> repo.search(emptyCriteria(), pageable))
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
