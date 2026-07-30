package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.service.model.SearchCriteria;
import ca.bc.gov.nrs.csp.backend.service.model.SearchResult;
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

@ExtendWith(MockitoExtension.class)
class SearchRepositoryTest {

    private static final Pageable DEFAULT_PAGE = PageRequest.of(0, 100);

    @Mock NamedParameterJdbcTemplate jdbc;

    SearchRepository repo;

    @BeforeEach
    void setUp() {
        repo = new SearchRepository(jdbc);
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

    // SearchCriteria components in order:
    // (invDate, startDate, endDate, submitterClientNum, sellerBuyerClientNum,
    //  sellerBuyerLocNum, sellerSubmitter, invNumber, invStatus, invType, maturity, keyword)
    private SearchCriteria emptyCriteria() {
        return new SearchCriteria(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    // ---------------------------------------------------------------
    // toInvoiceNumberPattern
    // ---------------------------------------------------------------

    @Test
    void toInvoiceNumberPattern_plainTerm_becomesContainsMatch() {
        assertThat(SearchRepository.toInvoiceNumberPattern("500")).isEqualTo("%500%");
    }

    @Test
    void toInvoiceNumberPattern_starBecomesPercent() {
        assertThat(SearchRepository.toInvoiceNumberPattern("INV-2024-*")).isEqualTo("INV-2024-%");
    }

    @Test
    void toInvoiceNumberPattern_percentPassesThroughAsWildcard() {
        assertThat(SearchRepository.toInvoiceNumberPattern("INV-2024-%")).isEqualTo("INV-2024-%");
    }

    @Test
    void toInvoiceNumberPattern_questionMarkBecomesUnderscore() {
        assertThat(SearchRepository.toInvoiceNumberPattern("INV-?-23")).isEqualTo("INV-_-23");
    }

    @Test
    void toInvoiceNumberPattern_mixedWildcards() {
        assertThat(SearchRepository.toInvoiceNumberPattern("*-2024-?")).isEqualTo("%-2024-_");
    }

    @Test
    void toInvoiceNumberPattern_starAndPercentBothBecomeWildcards() {
        assertThat(SearchRepository.toInvoiceNumberPattern("50%*")).isEqualTo("50%%");
    }

    @Test
    void toInvoiceNumberPattern_escapesUnderscoreLiteralInPlainTerm() {
        // A literal '_' (not the '?' wildcard) must be escaped so it matches literally.
        assertThat(SearchRepository.toInvoiceNumberPattern("ab_cd")).isEqualTo("%ab\\_cd%");
    }

    @Test
    void toInvoiceNumberPattern_escapesUnderscoreEvenInPatternMode() {
        // '_' stays escaped even when the term is a pattern (a wildcard is present).
        assertThat(SearchRepository.toInvoiceNumberPattern("a_b*")).isEqualTo("a\\_b%");
    }

    @Test
    void toInvoiceNumberPattern_escapesBackslash() {
        assertThat(SearchRepository.toInvoiceNumberPattern("a\\b")).isEqualTo("%a\\\\b%");
    }

    // ---------------------------------------------------------------
    // All-null criteria — no dynamic WHERE fragments added
    // ---------------------------------------------------------------

    @Test
    void search_allNullCriteria_noDynamicFragmentsInSql() {
        stubJdbc();
        repo.search(emptyCriteria(), DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql)
                .doesNotContain(":invDate")
                .doesNotContain(":startDate")
                .doesNotContain(":endDate")
                .doesNotContain(":submitterClientNum")
                .doesNotContain(":sellerBuyerClientNum")
                .doesNotContain(":sellerBuyerLocNum")
                .doesNotContain(":invNumber")
                .doesNotContain(":invStatus")
                .doesNotContain(":invType")
                .doesNotContain(":maturity")
                .doesNotContain(":keyword");
    }

    // ---------------------------------------------------------------
    // Date criteria
    // ---------------------------------------------------------------

    @Test
    void search_invDate_appendsEqualityFragment_andBindsIsoString() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                LocalDate.of(2024, Month.JANUARY, 15), null, null, null, null, null, null, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql())
                .contains("inv.client_invoice_date = TO_DATE(:invDate, 'YYYY-MM-DD')");
        assertThat(captureParams().getValue("invDate")).isEqualTo("2024-01-15");
    }

    @Test
    void search_startAndEndDate_appendRangeFragments() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2024, Month.MARCH, 31),
                null, null, null, null, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql)
                .contains("inv.client_invoice_date >= TO_DATE(:startDate, 'YYYY-MM-DD')")
                .contains("inv.client_invoice_date <= TO_DATE(:endDate, 'YYYY-MM-DD')");

        MapSqlParameterSource params = captureParams();
        assertThat(params.getValue("startDate")).isEqualTo("2024-01-01");
        assertThat(params.getValue("endDate")).isEqualTo("2024-03-31");
    }

    // ---------------------------------------------------------------
    // Client criteria — named params, NOT string concatenation
    // ---------------------------------------------------------------

    @Test
    void search_submitterClientNum_usesNamedParams_notStringConcatenation() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, "00012345", null, null, null, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql)
                .contains("sub.client_number = :submitterClientNum")
                .doesNotContain("00012345");

        assertThat(captureParams().getValue("submitterClientNum")).isEqualTo("00012345");
    }

    @Test
    void search_sellerBuyerClientNum_appendsSellerOrBuyerCondition() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, "00099999", null, null, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql()).contains(
                "(inv.seller_client_number = :sellerBuyerClientNum OR inv.buyer_client_number = :sellerBuyerClientNum)");
        assertThat(captureParams().getValue("sellerBuyerClientNum")).isEqualTo("00099999");
    }

    @Test
    void search_sellerBuyerLocNum_appendsSellerOrBuyerLocnCondition() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, null, "01", null, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql()).contains(
                "(inv.seller_client_locn_code = :sellerBuyerLocNum OR inv.buyer_client_locn_code = :sellerBuyerLocNum)");
        assertThat(captureParams().getValue("sellerBuyerLocNum")).isEqualTo("01");
    }

    // ---------------------------------------------------------------
    // sellerSubmitter — TRUE matches seller, FALSE matches buyer
    // ---------------------------------------------------------------

    @Test
    void search_sellerSubmitterTrue_appendsSellerEqualsSubmitterCondition() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, null, null, Boolean.TRUE, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql)
                .contains("(inv.seller_client_number = sub.client_number AND inv.seller_client_locn_code = sub.client_locn_code)")
                .doesNotContain("inv.buyer_client_number = sub.client_number");
    }

    @Test
    void search_sellerSubmitterFalse_appendsBuyerEqualsSubmitterCondition() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, null, null, Boolean.FALSE, null, null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql)
                .contains("(inv.buyer_client_number = sub.client_number AND inv.buyer_client_locn_code = sub.client_locn_code)")
                .doesNotContain("inv.seller_client_number = sub.client_number");
    }

    // ---------------------------------------------------------------
    // invNumber — LIKE with ESCAPE and wildcard pattern conversion
    // ---------------------------------------------------------------

    @Test
    void search_invNumber_appendsLikeFragment_andBindsPattern() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, null, null, null, "500", null, null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql()).contains("UPPER(inv.client_invoice_no) LIKE UPPER(:invNumber) ESCAPE '\\'");
        assertThat(captureParams().getValue("invNumber")).isEqualTo("%500%");
    }

    // ---------------------------------------------------------------
    // invStatus / invType / maturity
    // ---------------------------------------------------------------

    @Test
    void search_invStatus_appendsCodeEqualityCondition() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, null, null, null, null, "APP", null, null, null);
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql()).contains("inv.log_sale_entry_status_code = :invStatus");
        assertThat(captureParams().getValue("invStatus")).isEqualTo("APP");
    }

    @Test
    void search_invType_appendsCodeEqualityCondition() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, null, null, null, null, null, "SI", null, null);
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql()).contains("inv.csp_invoice_type_code = :invType");
        assertThat(captureParams().getValue("invType")).isEqualTo("SI");
    }

    @Test
    void search_maturity_appendsCodeEqualityCondition() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, null, null, null, null, null, null, "M", null);
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql()).contains("inv.log_sale_type_code = :maturity");
        assertThat(captureParams().getValue("maturity")).isEqualTo("M");
    }

    // ---------------------------------------------------------------
    // keyword — applied to outer subquery WHERE clause, trimmed pattern
    // ---------------------------------------------------------------

    @Test
    void search_nonBlankKeyword_appendsKeywordWhereClause_andTrimsPattern() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, null, null, null, null, null, null, null, "  hello  ");
        repo.search(criteria, DEFAULT_PAGE);

        String sql = captureDataSql();
        assertThat(sql)
                .contains("UPPER(invoice_status)  LIKE UPPER(:keyword)")
                .contains("UPPER(invoice_number) LIKE UPPER(:keyword)")
                .contains("UPPER(client_name)    LIKE UPPER(:keyword)")
                .contains("TO_CHAR(invoice_date, 'YYYY-MM-DD') LIKE :keyword");

        assertThat(captureParams().getValue("keyword")).isEqualTo("%hello%");
    }

    @Test
    void search_blankKeyword_noKeywordWhereClauseInSql() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, null, null, null, null, null, null, null, "   ");
        repo.search(criteria, DEFAULT_PAGE);

        assertThat(captureDataSql()).doesNotContain(":keyword");
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_keyword_appearsInBothDataAndCountSql() {
        stubJdbc();
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, null, null, null, null, null, null, null, null, "inv001");
        repo.search(criteria, DEFAULT_PAGE);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        verify(jdbc).queryForObject(sqlCaptor.capture(), any(MapSqlParameterSource.class), eq(Long.class));

        assertThat(sqlCaptor.getAllValues().get(0)).contains("UPPER(invoice_number) LIKE UPPER(:keyword)");
        assertThat(sqlCaptor.getAllValues().get(1)).contains("UPPER(invoice_number) LIKE UPPER(:keyword)");
    }

    // ---------------------------------------------------------------
    // Paging parameters
    // ---------------------------------------------------------------

    @Test
    void search_pagingParams_offsetAndLimitBound() {
        stubJdbc();
        Pageable pageable = PageRequest.of(2, 25); // offset = 50, limit = 25
        repo.search(emptyCriteria(), pageable);

        MapSqlParameterSource params = captureParams();
        assertThat(params.getValue("offset")).isEqualTo(50L);
        assertThat(params.getValue("limit")).isEqualTo(25);
    }

    // ---------------------------------------------------------------
    // Sort whitelist
    // ---------------------------------------------------------------

    @Test
    void search_defaultSort_usesCspSubmissionIdDesc() {
        stubJdbc();
        repo.search(emptyCriteria(), DEFAULT_PAGE); // no sort specified

        assertThat(captureDataSql()).contains("ORDER BY csp_submission_id DESC");
    }

    @Test
    void search_knownSortFieldAsc_appendsStableTiebreaker() {
        stubJdbc();
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "invoiceDate"));
        repo.search(emptyCriteria(), pageable);

        assertThat(captureDataSql()).contains("ORDER BY invoice_date ASC, csp_submission_id DESC");
    }

    @Test
    void search_multipleSortFields_buildsCommaSeparatedOrderBy() {
        stubJdbc();
        Pageable pageable = PageRequest.of(0, 100,
                Sort.by(Sort.Order.asc("clientName"), Sort.Order.desc("type")));
        repo.search(emptyCriteria(), pageable);

        assertThat(captureDataSql()).contains("ORDER BY client_name ASC, type DESC, csp_submission_id DESC");
    }

    @Test
    void search_unknownSortField_throwsBadRequest() {
        Pageable pageable = PageRequest.of(0, 100, Sort.by("unknownField"));
        SearchCriteria criteria = emptyCriteria();

        assertThatThrownBy(() -> repo.search(criteria, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported sort field: unknownField");
    }

    // ---------------------------------------------------------------
    // Row mapper — executes against a mocked ResultSet
    // ---------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void search_rowMapper_mapsAllColumns() throws Exception {
        try (ResultSet rs = mock(ResultSet.class)) {
            given(rs.getLong("coastal_log_sale_id")).willReturn(101L);
            given(rs.getLong("csp_submission_id")).willReturn(202L);
            given(rs.getLong("submission_id")).willReturn(303L);
            given(rs.wasNull()).willReturn(false);
            given(rs.getString("invoice_status")).willReturn("Approved");
            given(rs.getString("invoice_number")).willReturn("INV-2024-001");
            given(rs.getDate("invoice_date")).willReturn(Date.valueOf(LocalDate.of(2024, Month.MAY, 1)));
            given(rs.getString("type")).willReturn("Standard Invoice");
            given(rs.getString("client_number")).willReturn("00012345");
            given(rs.getString("client_name")).willReturn("ACME TIMBER LTD");
            given(rs.getString("maturity")).willReturn("Mature");
            given(rs.getString("submission_type")).willReturn("Electronic");

            given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .willAnswer(inv -> {
                        RowMapper<SearchResult> rm = inv.getArgument(2);
                        return List.of(rm.mapRow(rs, 0));
                    });
            given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                    .willReturn(1L);

            Page<SearchResult> page = repo.search(emptyCriteria(), DEFAULT_PAGE);

            assertThat(page.getContent()).hasSize(1);
            SearchResult row = page.getContent().getFirst();
            assertThat(row.coastalLogSaleId()).isEqualTo(101L);
            assertThat(row.cspSubmissionId()).isEqualTo(202L);
            assertThat(row.submissionId()).isEqualTo(303L);
            assertThat(row.invoiceStatus()).isEqualTo("Approved");
            assertThat(row.invoiceNumber()).isEqualTo("INV-2024-001");
            assertThat(row.invoiceDate()).isEqualTo(LocalDate.of(2024, Month.MAY, 1));
            assertThat(row.type()).isEqualTo("Standard Invoice");
            assertThat(row.clientNumber()).isEqualTo("00012345");
            assertThat(row.clientName()).isEqualTo("ACME TIMBER LTD");
            assertThat(row.maturity()).isEqualTo("Mature");
            assertThat(row.submissionType()).isEqualTo("Electronic");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_rowMapper_nullColumns_mapToNullFields() throws Exception {
        // Only wasNull() is stubbed: getLong() defaults to 0 with wasNull() = true (→ null Longs),
        // getString()/getDate() default to null on an unstubbed mock.
        try (ResultSet rs = mock(ResultSet.class)) {
            given(rs.wasNull()).willReturn(true);

            given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .willAnswer(inv -> {
                        RowMapper<SearchResult> rm = inv.getArgument(2);
                        return List.of(rm.mapRow(rs, 0));
                    });
            given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                    .willReturn(1L);

            Page<SearchResult> page = repo.search(emptyCriteria(), DEFAULT_PAGE);

            SearchResult row = page.getContent().getFirst();
            assertThat(row.coastalLogSaleId()).isNull();
            assertThat(row.cspSubmissionId()).isNull();
            assertThat(row.submissionId()).isNull();
            assertThat(row.invoiceStatus()).isNull();
            assertThat(row.invoiceNumber()).isNull();
            assertThat(row.invoiceDate()).isNull();
            assertThat(row.type()).isNull();
            assertThat(row.clientNumber()).isNull();
            assertThat(row.clientName()).isNull();
            assertThat(row.maturity()).isNull();
            assertThat(row.submissionType()).isNull();
        }
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

        Page<SearchResult> page = repo.search(emptyCriteria(), DEFAULT_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(42L);
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_nullTotal_returnsZeroTotalElements() {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .willReturn(null);

        Page<SearchResult> page = repo.search(emptyCriteria(), DEFAULT_PAGE);

        assertThat(page.getTotalElements()).isZero();
    }
}
