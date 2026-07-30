package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for InvoiceRepository SQL construction and row mapping.
 *
 * <p>The NamedParameterJdbcTemplate is mocked. Query stubs invoke the actual
 * RowMapper lambdas against a mocked ResultSet so the private row-mapping code
 * executes, and SQL strings / parameter sources are captured and inspected.</p>
 */
@ExtendWith(MockitoExtension.class)
class InvoiceRepositoryTest {

    @Mock NamedParameterJdbcTemplate jdbc;

    InvoiceRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InvoiceRepository(jdbc);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Stubs jdbc.query to run the repository's RowMapper against the given ResultSet. */
    @SuppressWarnings("unchecked")
    private void stubQueryWithResultSet(ResultSet rs) {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willAnswer(inv -> {
                    RowMapper<Object> rm = inv.getArgument(2);
                    return List.of(rm.mapRow(rs, 0));
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

    private MapSqlParameterSource captureUpdateParams() {
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), paramsCaptor.capture());
        return paramsCaptor.getValue();
    }

    private String captureUpdateSql() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sqlCaptor.capture(), any(MapSqlParameterSource.class));
        return sqlCaptor.getValue();
    }

    @SuppressWarnings("java:S5961")
    private static void assertSellerInsertSqlAndParams(
            ArgumentCaptor<String> sqlCaptor,
            ArgumentCaptor<String[]> keyColsCaptor,
            ArgumentCaptor<MapSqlParameterSource> paramsCaptor) {
        assertThat(sqlCaptor.getValue())
                .contains("INSERT INTO THE.coastal_log_sale")
                .contains("THE.COASTAL_LOG_SALE_SEQ.NEXTVAL");
        assertThat(keyColsCaptor.getValue()).containsExactly("COASTAL_LOG_SALE_ID");

        assertThat(paramsCaptor.getValue().getValues())
                .containsEntry("submissionId", 500L)
                .containsEntry("status", "DFT")
                .containsEntry("sellerClientNumber", "00001111")
                .containsEntry("buyerClientNumber", "00002222")
                .containsEntry("invNumber", "INV-2026-001")
                .containsEntry("totalPieces", 100)
                .containsEntry("userId", "user123");
    }

    /** Builds a fully-populated InvoiceDetails; submitter is 00001111/01, other party 00002222/02. */
    private static InvoiceDetails details(String submittedBy, LocalDate invoiceDate, Integer totalPieces) {
        return new InvoiceDetails(
                77L,
                "INV-2026-001",
                invoiceDate,
                "DFT",
                "SALE",
                "M",
                "FOB01",
                "SORT01",
                new BigDecimal("1250.75"),
                totalPieces,
                new BigDecimal("12.5"),
                "00001111",
                "01",
                submittedBy,
                "00001111",
                "01",
                "00002222",
                "02",
                "ABC Logging Ltd.",
                "Nanaimo",
                "BC",
                List.of("B123"),
                List.of("TM1"),
                List.of("WS1"),
                null,
                null,
                "review it",
                "submitted via UI",
                "user123");
    }

    // ---------------------------------------------------------------
    // findByClientInvoiceNo
    // ---------------------------------------------------------------

    @Test
    void findByClientInvoiceNo_mapsEveryColumnOfInvoiceMatch() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getLong("coastal_log_sale_id")).willReturn(101L);
        given(rs.getString("invoice_status_code")).willReturn("DFT");
        given(rs.getString("invoice_type_code")).willReturn("SALE");
        given(rs.getString("submitter_client_number")).willReturn("00001111");
        given(rs.getString("submitter_client_locn_code")).willReturn("01");
        given(rs.getString("buyer_client_number")).willReturn("00002222");
        given(rs.getString("buyer_client_locn_code")).willReturn("02");
        given(rs.getString("seller_client_number")).willReturn("00003333");
        given(rs.getString("seller_client_locn_code")).willReturn("03");
        given(rs.getString("buyer_participant_name")).willReturn("Buyer Co");
        given(rs.getString("buyer_participant_city")).willReturn("Victoria");
        given(rs.getString("buyer_participant_province")).willReturn("BC");
        given(rs.getString("seller_participant_name")).willReturn("Seller Co");
        given(rs.getString("seller_participant_city")).willReturn("Nanaimo");
        given(rs.getString("seller_participant_province")).willReturn("AB");
        stubQueryWithResultSet(rs);

        List<InvoiceRepository.InvoiceMatch> result = repo.findByClientInvoiceNo("INV-2026-001");

        assertThat(result).hasSize(1);
        InvoiceRepository.InvoiceMatch match = result.get(0);
        assertThat(match.coastalLogSaleId()).isEqualTo(101L);
        assertThat(match.invoiceStatusCode()).isEqualTo("DFT");
        assertThat(match.invoiceTypeCode()).isEqualTo("SALE");
        assertThat(match.submitterClientNumber()).isEqualTo("00001111");
        assertThat(match.submitterClientLocnCode()).isEqualTo("01");
        assertThat(match.buyerClientNumber()).isEqualTo("00002222");
        assertThat(match.buyerClientLocnCode()).isEqualTo("02");
        assertThat(match.sellerClientNumber()).isEqualTo("00003333");
        assertThat(match.sellerClientLocnCode()).isEqualTo("03");
        assertThat(match.buyerParticipantName()).isEqualTo("Buyer Co");
        assertThat(match.buyerParticipantCity()).isEqualTo("Victoria");
        assertThat(match.buyerParticipantProvince()).isEqualTo("BC");
        assertThat(match.sellerParticipantName()).isEqualTo("Seller Co");
        assertThat(match.sellerParticipantCity()).isEqualTo("Nanaimo");
        assertThat(match.sellerParticipantProvince()).isEqualTo("AB");

        assertThat(captureQuerySql()).contains("cls.client_invoice_no = :clientInvoiceNo");
        assertThat(captureQueryParams().getValue("clientInvoiceNo")).isEqualTo("INV-2026-001");
    }

    // ---------------------------------------------------------------
    // findByInvoiceNoAndClient
    // ---------------------------------------------------------------

    @Test
    void findByInvoiceNoAndClient_mapsRowAndBindsAllParams() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getLong("coastal_log_sale_id")).willReturn(55L);
        given(rs.getString("invoice_status_code")).willReturn("APP");
        stubQueryWithResultSet(rs);

        List<InvoiceRepository.RelatedInvoice> result =
                repo.findByInvoiceNoAndClient("INV-9", "00001111", "01");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).coastalLogSaleId()).isEqualTo(55L);
        assertThat(result.get(0).invoiceStatusCode()).isEqualTo("APP");

        String sql = captureQuerySql();
        assertThat(sql)
                .contains("cls.client_invoice_no = :clientInvoiceNo")
                .contains("cl.client_number = :clientNumber")
                .contains("cl.client_locn_code = :clientLocnCode");

        MapSqlParameterSource params = captureQueryParams();
        assertThat(params.getValue("clientInvoiceNo")).isEqualTo("INV-9");
        assertThat(params.getValue("clientNumber")).isEqualTo("00001111");
        assertThat(params.getValue("clientLocnCode")).isEqualTo("01");
    }

    // ---------------------------------------------------------------
    // isMonthCompleted
    // ---------------------------------------------------------------

    @Test
    void isMonthCompleted_nullInvoiceDate_returnsFalseWithoutQuerying() {
        assertThat(repo.isMonthCompleted(null, "00001111", "01", 5L)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void isMonthCompleted_nullClientNumber_returnsFalseWithoutQuerying() {
        assertThat(repo.isMonthCompleted(LocalDate.of(2026, Month.MAY, 1), null, "01", 5L)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void isMonthCompleted_nullClientLocnCode_returnsFalseWithoutQuerying() {
        assertThat(repo.isMonthCompleted(LocalDate.of(2026, Month.MAY, 1), "00001111", null, 5L)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void isMonthCompleted_positiveCount_returnsTrueAndBindsParams() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(2);

        boolean result = repo.isMonthCompleted(LocalDate.of(2026, Month.MAY, 19), "00001111", "01", 42L);

        assertThat(result).isTrue();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(sqlCaptor.capture(), paramsCaptor.capture(), eq(Integer.class));

        assertThat(sqlCaptor.getValue())
                .contains("sub.month_complete_ind = 'Y'")
                .contains("TO_CHAR(cls.client_invoice_date, 'YYYY-MM') = TO_CHAR(:invoiceDate, 'YYYY-MM')")
                .contains(":excludeInvoiceId IS NULL OR cls.coastal_log_sale_id <> :excludeInvoiceId");

        MapSqlParameterSource params = paramsCaptor.getValue();
        assertThat(params.getValue("clientNumber")).isEqualTo("00001111");
        assertThat(params.getValue("clientLocnCode")).isEqualTo("01");
        assertThat(params.getValue("invoiceDate")).isEqualTo(Date.valueOf(LocalDate.of(2026, Month.MAY, 19)));
        assertThat(params.getValue("excludeInvoiceId")).isEqualTo(42L);
    }

    @Test
    void isMonthCompleted_zeroCount_returnsFalse() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(0);

        assertThat(repo.isMonthCompleted(LocalDate.of(2026, Month.MAY, 19), "00001111", "01", null)).isFalse();
    }

    @Test
    void isMonthCompleted_nullCount_returnsFalse() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(null);

        assertThat(repo.isMonthCompleted(LocalDate.of(2026, Month.MAY, 19), "00001111", "01", null)).isFalse();
    }

    // ---------------------------------------------------------------
    // countBoomNumberDuplicates
    // ---------------------------------------------------------------

    @Test
    void countBoomNumberDuplicates_nullBoomNumber_returnsZeroWithoutQuerying() {
        assertThat(repo.countBoomNumberDuplicates(1L, "BOOM", null)).isZero();
        verifyNoInteractions(jdbc);
    }

    @Test
    void countBoomNumberDuplicates_blankBoomNumber_returnsZeroWithoutQuerying() {
        assertThat(repo.countBoomNumberDuplicates(1L, "BOOM", "   ")).isZero();
        verifyNoInteractions(jdbc);
    }

    @Test
    void countBoomNumberDuplicates_returnsCountAndBindsParams() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(3);

        int count = repo.countBoomNumberDuplicates(9L, "BOOM", "B123");

        assertThat(count).isEqualTo(3);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(sqlCaptor.capture(), paramsCaptor.capture(), eq(Integer.class));

        assertThat(sqlCaptor.getValue())
                .contains("clls.log_source_code = :logSourceCode")
                .contains("clls.source_document_reference = :boomNumber");
        assertThat(paramsCaptor.getValue().getValue("logSourceCode")).isEqualTo("BOOM");
        assertThat(paramsCaptor.getValue().getValue("boomNumber")).isEqualTo("B123");
        assertThat(paramsCaptor.getValue().getValue("excludeInvoiceId")).isEqualTo(9L);
    }

    @Test
    void countBoomNumberDuplicates_nullCount_returnsZero() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(null);

        assertThat(repo.countBoomNumberDuplicates(null, "BOOM", "B123")).isZero();
    }

    // ---------------------------------------------------------------
    // findReviewCommentsById
    // ---------------------------------------------------------------

    @Test
    void findReviewCommentsById_nullId_returnsNullWithoutQuerying() {
        assertThat(repo.findReviewCommentsById(null)).isNull();
        verifyNoInteractions(jdbc);
    }

    @Test
    void findReviewCommentsById_noRows_returnsNull() {
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .willReturn(List.of());

        assertThat(repo.findReviewCommentsById(7L)).isNull();
    }

    @Test
    void findReviewCommentsById_returnsFirstRowAndBindsId() {
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .willReturn(List.of("looks good", "ignored"));

        assertThat(repo.findReviewCommentsById(7L)).isEqualTo("looks good");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(sqlCaptor.capture(), paramsCaptor.capture(), eq(String.class));
        assertThat(sqlCaptor.getValue()).contains("SELECT reviewer_notes FROM THE.coastal_log_sale");
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(7L);
    }

    // ---------------------------------------------------------------
    // findById — full row mapping including log sources and related invoices
    // ---------------------------------------------------------------

    @Test
    void findById_nullId_returnsEmptyWithoutQuerying() {
        assertThat(repo.findById(null)).isEmpty();
        verifyNoInteractions(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_noRows_returnsEmpty() {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());

        assertThat(repo.findById(99L)).isEmpty();
    }

    /** Stubs the nested queryForList calls made by mapLoadedInvoice (log sources + related refs). */
    private void stubNestedLookups() {
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .willAnswer(inv -> {
                    MapSqlParameterSource p = inv.getArgument(1);
                    if (p.hasValue("code")) {
                        String code = (String) p.getValue("code");
                        if ("BOOM".equals(code)) return List.of("B123", "B124");
                        if ("MARK".equals(code)) return List.of("TM1");
                        return List.of(); // WEIGH
                    }
                    String refType = (String) p.getValue("refTypeCode");
                    if ("REP".equals(refType)) return List.of("OLD-1", "OLD-2");
                    return List.of(); // ADJ
                });
    }

    @Test
    void findById_sellerSubmitted_mapsAllFieldsWithBuyerAsOtherParty() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getObject("submission_id", Long.class)).willReturn(500L);
        given(rs.getObject("submission_number", Long.class)).willReturn(9001L);
        given(rs.getObject("buyer_participant_id", Long.class)).willReturn(61L);
        given(rs.getObject("seller_participant_id", Long.class)).willReturn(62L);
        given(rs.getString("submitter_client_num")).willReturn("00001111");
        given(rs.getString("submitter_location")).willReturn("01");
        given(rs.getString("seller_client_number")).willReturn("00001111");
        given(rs.getString("seller_client_locn_code")).willReturn("01");
        given(rs.getString("buyer_client_number")).willReturn("00002222");
        given(rs.getString("buyer_client_locn_code")).willReturn("02");
        given(rs.getString("buyer_participant_name")).willReturn("Buyer Co");
        given(rs.getString("buyer_participant_city")).willReturn("Victoria");
        given(rs.getString("buyer_participant_province")).willReturn("BC");
        given(rs.getObject("inv_id", Long.class)).willReturn(10L);
        given(rs.getString("inv_number")).willReturn("INV-2026-001");
        given(rs.getDate("invoice_date")).willReturn(Date.valueOf(LocalDate.of(2026, Month.MAY, 19)));
        given(rs.getString("inv_status")).willReturn("DFT");
        given(rs.getString("inv_type")).willReturn("SALE");
        given(rs.getString("maturity")).willReturn("M");
        given(rs.getString("fob_code")).willReturn("FOB01");
        given(rs.getString("primary_sort_code")).willReturn("SORT01");
        given(rs.getBigDecimal("total_amt")).willReturn(new BigDecimal("1250.75"));
        given(rs.getObject("total_pieces", Integer.class)).willReturn(100);
        given(rs.getBigDecimal("total_vol")).willReturn(new BigDecimal("12.5"));
        given(rs.getString("review_comments")).willReturn("review it");
        given(rs.getString("submit_comments")).willReturn("submitted via UI");
        given(rs.getString("entry_userid")).willReturn("user123");
        stubNestedLookups();
        stubQueryWithResultSet(rs);

        Optional<InvoiceRepository.LoadedInvoice> result = repo.findById(10L);

        assertThat(result).isPresent();
        InvoiceRepository.LoadedInvoice loaded = result.get();
        assertThat(loaded)
                .extracting(
                        InvoiceRepository.LoadedInvoice::submissionId,
                        InvoiceRepository.LoadedInvoice::submissionNumber,
                        InvoiceRepository.LoadedInvoice::buyerParticipantId,
                        InvoiceRepository.LoadedInvoice::sellerParticipantId)
                .containsExactly(500L, 9001L, 61L, 62L);

        InvoiceDetails d = loaded.details();
        assertThat(d)
                .extracting(
                        InvoiceDetails::invID,
                        InvoiceDetails::invNumber,
                        InvoiceDetails::invoiceDate,
                        InvoiceDetails::invStatus,
                        InvoiceDetails::invType,
                        InvoiceDetails::maturity,
                        InvoiceDetails::fobCode,
                        InvoiceDetails::primarySortCode,
                        InvoiceDetails::totalPieces,
                        InvoiceDetails::submitterClientNum,
                        InvoiceDetails::submitterLocation,
                        InvoiceDetails::submittedBy,
                        InvoiceDetails::clientNumber,
                        InvoiceDetails::clientLocation,
                        InvoiceDetails::otherClientNum,
                        InvoiceDetails::otherClientLocation,
                        InvoiceDetails::otherClientName,
                        InvoiceDetails::otherClientCity,
                        InvoiceDetails::otherClientProvState,
                        InvoiceDetails::replaceInvNum,
                        InvoiceDetails::adjustInvNum,
                        InvoiceDetails::reviewComments,
                        InvoiceDetails::submitComments,
                        InvoiceDetails::entryUserID)
                .containsExactly(
                        10L,
                        "INV-2026-001",
                        LocalDate.of(2026, Month.MAY, 19),
                        "DFT",
                        "SALE",
                        "M",
                        "FOB01",
                        "SORT01",
                        100,
                        "00001111",
                        "01",
                        "Seller",
                        "00001111",
                        "01",
                        "00002222",
                        "02",
                        "Buyer Co",
                        "Victoria",
                        "BC",
                        "OLD-1,OLD-2",
                        null,
                        "review it",
                        "submitted via UI",
                        "user123");
        assertThat(d.totalAmt()).isEqualByComparingTo("1250.75");
        assertThat(d.totalVol()).isEqualByComparingTo("12.5");
        assertThat(d.boomNumbers()).containsExactly("B123", "B124");
        assertThat(d.timberMarks()).containsExactly("TM1");
        assertThat(d.weightSlips()).isEmpty();

        assertThat(captureQuerySql()).contains("cls.coastal_log_sale_id = :id");
        assertThat(captureQueryParams().getValue("id")).isEqualTo(10L);
    }

    @Test
    void findById_buyerSubmitted_nullableColumnsNull_mapsSellerAsOtherParty() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getObject("submission_id", Long.class)).willReturn(500L);
        given(rs.getObject("submission_number", Long.class)).willReturn(null);
        given(rs.getObject("buyer_participant_id", Long.class)).willReturn(null);
        given(rs.getObject("seller_participant_id", Long.class)).willReturn(null);
        // Submitter matches the buyer side, not the seller side
        given(rs.getString("submitter_client_num")).willReturn("00002222");
        given(rs.getString("submitter_location")).willReturn("02");
        given(rs.getString("seller_client_number")).willReturn("00001111");
        given(rs.getString("seller_client_locn_code")).willReturn("01");
        given(rs.getString("seller_participant_name")).willReturn("Seller Co");
        given(rs.getString("seller_participant_city")).willReturn("Nanaimo");
        given(rs.getString("seller_participant_province")).willReturn("BC");
        given(rs.getObject("inv_id", Long.class)).willReturn(11L);
        given(rs.getString("inv_number")).willReturn("INV-2026-002");
        given(rs.getDate("invoice_date")).willReturn(null);
        given(rs.getString("inv_status")).willReturn("INB");
        given(rs.getString("inv_type")).willReturn("SALE");
        given(rs.getString("maturity")).willReturn(null);
        given(rs.getString("fob_code")).willReturn(null);
        given(rs.getString("primary_sort_code")).willReturn(null);
        given(rs.getBigDecimal("total_amt")).willReturn(null);
        given(rs.getObject("total_pieces", Integer.class)).willReturn(null);
        given(rs.getBigDecimal("total_vol")).willReturn(null);
        given(rs.getString("review_comments")).willReturn(null);
        given(rs.getString("submit_comments")).willReturn(null);
        given(rs.getString("entry_userid")).willReturn("user456");
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .willReturn(List.of());
        stubQueryWithResultSet(rs);

        Optional<InvoiceRepository.LoadedInvoice> result = repo.findById(11L);

        assertThat(result).isPresent();
        InvoiceRepository.LoadedInvoice loaded = result.get();
        assertThat(loaded)
                .extracting(
                        InvoiceRepository.LoadedInvoice::submissionId,
                        InvoiceRepository.LoadedInvoice::submissionNumber,
                        InvoiceRepository.LoadedInvoice::buyerParticipantId,
                        InvoiceRepository.LoadedInvoice::sellerParticipantId)
                .containsExactly(500L, null, null, null);

        InvoiceDetails d = loaded.details();
        assertThat(d)
                .extracting(
                        InvoiceDetails::invID,
                        InvoiceDetails::invoiceDate,
                        InvoiceDetails::maturity,
                        InvoiceDetails::fobCode,
                        InvoiceDetails::primarySortCode,
                        InvoiceDetails::totalAmt,
                        InvoiceDetails::totalPieces,
                        InvoiceDetails::totalVol,
                        InvoiceDetails::submittedBy,
                        InvoiceDetails::submitterClientNum,
                        InvoiceDetails::submitterLocation,
                        InvoiceDetails::clientNumber,
                        InvoiceDetails::clientLocation,
                        InvoiceDetails::otherClientNum,
                        InvoiceDetails::otherClientLocation,
                        InvoiceDetails::otherClientName,
                        InvoiceDetails::otherClientCity,
                        InvoiceDetails::otherClientProvState,
                        InvoiceDetails::replaceInvNum,
                        InvoiceDetails::adjustInvNum,
                        InvoiceDetails::reviewComments,
                        InvoiceDetails::submitComments,
                        InvoiceDetails::entryUserID)
                .containsExactly(
                        11L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Buyer",
                        "00002222",
                        "02",
                        "00001111",
                        "01",
                        "00001111",
                        "01",
                        "Seller Co",
                        "Nanaimo",
                        "BC",
                        null,
                        null,
                        null,
                        null,
                        "user456");
        assertThat(d.boomNumbers()).isEmpty();
        assertThat(d.timberMarks()).isEmpty();
        assertThat(d.weightSlips()).isEmpty();
    }

    // ---------------------------------------------------------------
    // findRelatedInvoiceNumbers
    // ---------------------------------------------------------------

    @Test
    void findRelatedInvoiceNumbers_nullParentId_returnsNullWithoutQuerying() {
        assertThat(repo.findRelatedInvoiceNumbers(null, "REP")).isNull();
        verifyNoInteractions(jdbc);
    }

    @Test
    void findRelatedInvoiceNumbers_nullRefType_returnsNullWithoutQuerying() {
        assertThat(repo.findRelatedInvoiceNumbers(1L, null)).isNull();
        verifyNoInteractions(jdbc);
    }

    @Test
    void findRelatedInvoiceNumbers_noRows_returnsNull() {
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .willReturn(List.of());

        assertThat(repo.findRelatedInvoiceNumbers(1L, "ADJ")).isNull();
    }

    @Test
    void findRelatedInvoiceNumbers_joinsNumbersWithCommaAndBindsParams() {
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .willReturn(List.of("INV-A", "INV-B"));

        assertThat(repo.findRelatedInvoiceNumbers(1L, "REP")).isEqualTo("INV-A,INV-B");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(sqlCaptor.capture(), paramsCaptor.capture(), eq(String.class));
        assertThat(sqlCaptor.getValue())
                .contains("r.coastal_log_sale_id = :parentId")
                .contains("r.csp_invoice_ref_type_code = :refTypeCode");
        assertThat(paramsCaptor.getValue().getValue("parentId")).isEqualTo(1L);
        assertThat(paramsCaptor.getValue().getValue("refTypeCode")).isEqualTo("REP");
    }

    // ---------------------------------------------------------------
    // findLogSources
    // ---------------------------------------------------------------

    @Test
    @SuppressWarnings("java:S5961")
    void findLogSources_returnsListAndBindsParams() {
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .willReturn(List.of("B1", "B2"));

        List<String> result = repo.findLogSources(4L, "BOOM");

        assertThat(result).containsExactly("B1", "B2");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(sqlCaptor.capture(), paramsCaptor.capture(), eq(String.class));
        assertThat(sqlCaptor.getValue())
                .contains("FROM THE.coastal_log_sale_log_source")
                .contains("coastal_log_sale_id = :id AND log_source_code = :code");
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(4L);
        assertThat(paramsCaptor.getValue().getValue("code")).isEqualTo("BOOM");
    }

    // ---------------------------------------------------------------
    // insertInvoice
    // ---------------------------------------------------------------

    private void stubInsertKeyHolder(long generatedId) {
        given(jdbc.update(anyString(), any(MapSqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .willAnswer(inv -> {
                    KeyHolder kh = inv.getArgument(2);
                    kh.getKeyList().add(Map.of("COASTAL_LOG_SALE_ID", new BigDecimal(generatedId)));
                    return 1;
                });
    }

    @Test
    @SuppressWarnings("java:S5961")
    void insertInvoice_sellerSubmitted_derivesColumnsAndReturnsGeneratedKey() {
        stubInsertKeyHolder(4242L);
        InvoiceDetails d = details("Seller", LocalDate.of(2026, Month.MAY, 19), 100);

        Long id = repo.insertInvoice(d, 500L, "DFT", 61L, 62L, "user123");

        assertThat(id).isEqualTo(4242L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        ArgumentCaptor<String[]> keyColsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(jdbc).update(sqlCaptor.capture(), paramsCaptor.capture(), any(KeyHolder.class), keyColsCaptor.capture());

        assertSellerInsertSqlAndParams(sqlCaptor, keyColsCaptor, paramsCaptor);
    }

    @Test
    void insertInvoice_buyerSubmitted_nullDateAndPieces_swapsColumnsAndDefaultsPieces() {
        stubInsertKeyHolder(4243L);
        InvoiceDetails d = details("Buyer", null, null);

        Long id = repo.insertInvoice(d, 500L, "INB", null, null, "user123");

        assertThat(id).isEqualTo(4243L);

        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), paramsCaptor.capture(), any(KeyHolder.class), any(String[].class));

        MapSqlParameterSource params = paramsCaptor.getValue();
        // Submitter is the buyer: buyer columns = submitter, seller columns = other party
        assertThat(params.getValue("buyerClientNumber")).isEqualTo("00001111");
        assertThat(params.getValue("buyerClientLocnCode")).isEqualTo("01");
        assertThat(params.getValue("sellerClientNumber")).isEqualTo("00002222");
        assertThat(params.getValue("sellerClientLocnCode")).isEqualTo("02");
        assertThat(params.getValue("invoiceDate")).isNull();
        assertThat(params.getValue("amvCalcDate")).isNull();
        assertThat(params.getValue("totalPieces")).isEqualTo(0);
        assertThat(params.getValue("buyerParticipantId")).isNull();
        assertThat(params.getValue("sellerParticipantId")).isNull();
    }

    // ---------------------------------------------------------------
    // updateInvoice
    // ---------------------------------------------------------------

    @Test
    void updateInvoice_sellerSubmitted_bindsAllParams() {
        InvoiceDetails d = details("Seller", LocalDate.of(2026, Month.JUNE, 1), 50);

        repo.updateInvoice(10L, d, "DFT", 61L, 62L, "user123");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(sqlCaptor.capture(), paramsCaptor.capture());

        assertThat(sqlCaptor.getValue())
                .contains("UPDATE THE.coastal_log_sale SET")
                .contains("revision_count = revision_count + 1")
                .contains("WHERE coastal_log_sale_id = :id");

        MapSqlParameterSource params = paramsCaptor.getValue();
        assertThat(params.getValue("id")).isEqualTo(10L);
        assertThat(params.getValue("status")).isEqualTo("DFT");
        assertThat(params.getValue("invType")).isEqualTo("SALE");
        assertThat(params.getValue("maturity")).isEqualTo("M");
        assertThat(params.getValue("fobCode")).isEqualTo("FOB01");
        assertThat(params.getValue("primarySortCode")).isEqualTo("SORT01");
        assertThat(params.getValue("clientPrimarySortCode")).isEqualTo("SORT01");
        assertThat(params.getValue("sellerClientNumber")).isEqualTo("00001111");
        assertThat(params.getValue("sellerClientLocnCode")).isEqualTo("01");
        assertThat(params.getValue("buyerClientNumber")).isEqualTo("00002222");
        assertThat(params.getValue("buyerClientLocnCode")).isEqualTo("02");
        assertThat(params.getValue("buyerParticipantId")).isEqualTo(61L);
        assertThat(params.getValue("sellerParticipantId")).isEqualTo(62L);
        assertThat(params.getValue("invNumber")).isEqualTo("INV-2026-001");
        assertThat(params.getValue("invoiceDate")).isEqualTo(Date.valueOf(LocalDate.of(2026, Month.JUNE, 1)));
        assertThat(params.getValue("totalPieces")).isEqualTo(50);
        assertThat(params.getValue("totalVol")).isEqualTo(new BigDecimal("12.5"));
        assertThat(params.getValue("totalAmt")).isEqualTo(new BigDecimal("1250.75"));
        assertThat(params.getValue("reviewerNotes")).isEqualTo("review it");
        assertThat(params.getValue("submitterNotes")).isEqualTo("submitted via UI");
        assertThat(params.getValue("userId")).isEqualTo("user123");
    }

    @Test
    void updateInvoice_buyerSubmitted_nullDateAndPieces_swapsColumnsAndDefaultsPieces() {
        InvoiceDetails d = details("Buyer", null, null);

        repo.updateInvoice(11L, d, "INB", null, null, "user456");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("buyerClientNumber")).isEqualTo("00001111");
        assertThat(params.getValue("buyerClientLocnCode")).isEqualTo("01");
        assertThat(params.getValue("sellerClientNumber")).isEqualTo("00002222");
        assertThat(params.getValue("sellerClientLocnCode")).isEqualTo("02");
        assertThat(params.getValue("invoiceDate")).isNull();
        assertThat(params.getValue("totalPieces")).isEqualTo(0);
        assertThat(params.getValue("userId")).isEqualTo("user456");
    }

    // ---------------------------------------------------------------
    // updateStatus / updateReviewerNotes
    // ---------------------------------------------------------------

    @Test
    void updateStatus_bindsStatusUserIdAndId() {
        repo.updateStatus(10L, "APP", "user123");

        assertThat(captureUpdateSql())
                .contains("log_sale_entry_status_code = :status")
                .contains("revision_count = revision_count + 1")
                .contains("WHERE coastal_log_sale_id = :id");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("id")).isEqualTo(10L);
        assertThat(params.getValue("status")).isEqualTo("APP");
        assertThat(params.getValue("userId")).isEqualTo("user123");
    }

    @Test
    void updateReviewerNotes_bindsNotesUserIdAndId() {
        repo.updateReviewerNotes(10L, "new notes", "user123");

        assertThat(captureUpdateSql())
                .contains("reviewer_notes = :notes")
                .contains("WHERE coastal_log_sale_id = :id");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("id")).isEqualTo(10L);
        assertThat(params.getValue("notes")).isEqualTo("new notes");
        assertThat(params.getValue("userId")).isEqualTo("user123");
    }

    // ---------------------------------------------------------------
    // delete methods
    // ---------------------------------------------------------------

    @Test
    void deleteInvoice_deletesByCoastalLogSaleId() {
        repo.deleteInvoice(10L);

        assertThat(captureUpdateSql())
                .contains("DELETE FROM THE.coastal_log_sale WHERE coastal_log_sale_id = :id");
        assertThat(captureUpdateParams().getValue("id")).isEqualTo(10L);
    }

    @Test
    void deleteAllLogSources_deletesByInvoiceId() {
        repo.deleteAllLogSources(10L);

        assertThat(captureUpdateSql())
                .contains("DELETE FROM THE.coastal_log_sale_log_source WHERE coastal_log_sale_id = :id");
        assertThat(captureUpdateParams().getValue("id")).isEqualTo(10L);
    }

    @Test
    void deleteAllRelatedInvoiceRefs_deletesByParentId() {
        repo.deleteAllRelatedInvoiceRefs(10L);

        assertThat(captureUpdateSql())
                .contains("DELETE FROM THE.coastal_log_sale_rltd_invc WHERE coastal_log_sale_id = :id");
        assertThat(captureUpdateParams().getValue("id")).isEqualTo(10L);
    }

    @Test
    void deleteAllIncomingRelatedInvoiceRefs_deletesByRelatedId() {
        repo.deleteAllIncomingRelatedInvoiceRefs(10L);

        assertThat(captureUpdateSql())
                .contains("DELETE FROM THE.coastal_log_sale_rltd_invc WHERE related_coastal_log_sale_id = :id");
        assertThat(captureUpdateParams().getValue("id")).isEqualTo(10L);
    }

    // ---------------------------------------------------------------
    // countByCspSubmissionId / countByCspSubmissionIdAndStatus
    // ---------------------------------------------------------------

    @Test
    void countByCspSubmissionId_nullId_returnsZeroWithoutQuerying() {
        assertThat(repo.countByCspSubmissionId(null)).isZero();
        verifyNoInteractions(jdbc);
    }

    @Test
    void countByCspSubmissionId_returnsCountAndBindsId() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(5);

        assertThat(repo.countByCspSubmissionId(500L)).isEqualTo(5);

        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(anyString(), paramsCaptor.capture(), eq(Integer.class));
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(500L);
    }

    @Test
    void countByCspSubmissionId_nullCount_returnsZero() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(null);

        assertThat(repo.countByCspSubmissionId(500L)).isZero();
    }

    @Test
    void countByCspSubmissionIdAndStatus_nullId_returnsZeroWithoutQuerying() {
        assertThat(repo.countByCspSubmissionIdAndStatus(null, "DFT")).isZero();
        verifyNoInteractions(jdbc);
    }

    @Test
    void countByCspSubmissionIdAndStatus_returnsCountAndBindsParams() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(3);

        assertThat(repo.countByCspSubmissionIdAndStatus(500L, "DFT")).isEqualTo(3);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(sqlCaptor.capture(), paramsCaptor.capture(), eq(Integer.class));
        assertThat(sqlCaptor.getValue())
                .contains("csp_submission_id = :id")
                .contains("log_sale_entry_status_code = :status");
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(500L);
        assertThat(paramsCaptor.getValue().getValue("status")).isEqualTo("DFT");
    }

    @Test
    void countByCspSubmissionIdAndStatus_nullCount_returnsZero() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(null);

        assertThat(repo.countByCspSubmissionIdAndStatus(500L, "DFT")).isZero();
    }

    // ---------------------------------------------------------------
    // replaceRelatedInvoices
    // ---------------------------------------------------------------

    @Test
    void replaceRelatedInvoices_nullCsv_onlyDeletesExistingRefs() {
        repo.replaceRelatedInvoices(10L, "REP", null, "00001111", "01", "user123");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(sqlCaptor.capture(), paramsCaptor.capture());
        assertThat(sqlCaptor.getValue())
                .contains("DELETE FROM THE.coastal_log_sale_rltd_invc")
                .contains("coastal_log_sale_id = :id AND csp_invoice_ref_type_code = :type");
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(10L);
        assertThat(paramsCaptor.getValue().getValue("type")).isEqualTo("REP");
    }

    @Test
    void replaceRelatedInvoices_blankCsv_onlyDeletesExistingRefs() {
        repo.replaceRelatedInvoices(10L, "ADJ", "   ", "00001111", "01", "user123");

        verify(jdbc, times(1)).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void replaceRelatedInvoices_resolvesEachNumber_insertsResolved_skipsUnresolvedAndEmptyTokens() {
        // "INV-A" resolves to id 101; "INV-B" does not resolve (defensive skip);
        // the empty token between commas is skipped without a resolve query.
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .willAnswer(inv -> {
                    MapSqlParameterSource p = inv.getArgument(1);
                    return "INV-A".equals(p.getValue("invNo")) ? List.of(101L) : List.of();
                });

        repo.replaceRelatedInvoices(10L, "REP", " INV-A , , INV-B ", "00001111", "01", "user123");

        // Two resolve lookups: INV-A and INV-B (empty token skipped)
        ArgumentCaptor<MapSqlParameterSource> resolveCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc, times(2)).queryForList(anyString(), resolveCaptor.capture(), eq(Long.class));
        assertThat(resolveCaptor.getAllValues().get(0).getValue("invNo")).isEqualTo("INV-A");
        assertThat(resolveCaptor.getAllValues().get(0).getValue("submitterClientNum")).isEqualTo("00001111");
        assertThat(resolveCaptor.getAllValues().get(0).getValue("submitterLocnCode")).isEqualTo("01");
        assertThat(resolveCaptor.getAllValues().get(1).getValue("invNo")).isEqualTo("INV-B");

        // One delete + one insert (only INV-A resolved)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc, times(2)).update(sqlCaptor.capture(), paramsCaptor.capture());

        assertThat(sqlCaptor.getAllValues().get(0)).contains("DELETE FROM THE.coastal_log_sale_rltd_invc");
        assertThat(sqlCaptor.getAllValues().get(1))
                .contains("INSERT INTO THE.coastal_log_sale_rltd_invc")
                .contains("THE.COASTAL_LOG_SALE_RTLD_INVC_SEQ.NEXTVAL");

        MapSqlParameterSource insertParams = paramsCaptor.getAllValues().get(1);
        assertThat(insertParams.getValue("parentId")).isEqualTo(10L);
        assertThat(insertParams.getValue("relatedId")).isEqualTo(101L);
        assertThat(insertParams.getValue("refTypeCode")).isEqualTo("REP");
        assertThat(insertParams.getValue("userId")).isEqualTo("user123");
    }

    // ---------------------------------------------------------------
    // replaceLogSources
    // ---------------------------------------------------------------

    @Test
    void replaceLogSources_nullValues_onlyDeletesExistingRows() {
        repo.replaceLogSources(10L, "BOOM", null, "user123");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(sqlCaptor.capture(), paramsCaptor.capture());
        assertThat(sqlCaptor.getValue())
                .contains("DELETE FROM THE.coastal_log_sale_log_source")
                .contains("coastal_log_sale_id = :id AND log_source_code = :code");
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(10L);
        assertThat(paramsCaptor.getValue().getValue("code")).isEqualTo("BOOM");
    }

    @Test
    void replaceLogSources_emptyValues_onlyDeletesExistingRows() {
        repo.replaceLogSources(10L, "MARK", List.of(), "user123");

        verify(jdbc, times(1)).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void replaceLogSources_insertsTrimmedValues_skipsNullAndBlankEntries() {
        repo.replaceLogSources(10L, "BOOM", Arrays.asList(" B1 ", null, "   ", "B2"), "user123");

        // One delete + two inserts (null and blank entries skipped)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc, times(3)).update(sqlCaptor.capture(), paramsCaptor.capture());

        assertThat(sqlCaptor.getAllValues().get(0)).contains("DELETE FROM THE.coastal_log_sale_log_source");
        assertThat(sqlCaptor.getAllValues().get(1))
                .contains("INSERT INTO THE.coastal_log_sale_log_source")
                .contains("THE.COASTAL_LOG_SALE_SOURCE_SEQ.NEXTVAL");
        assertThat(sqlCaptor.getAllValues().get(2)).contains("INSERT INTO THE.coastal_log_sale_log_source");

        MapSqlParameterSource firstInsert = paramsCaptor.getAllValues().get(1);
        assertThat(firstInsert.getValue("id")).isEqualTo(10L);
        assertThat(firstInsert.getValue("code")).isEqualTo("BOOM");
        assertThat(firstInsert.getValue("ref")).isEqualTo("B1");
        assertThat(firstInsert.getValue("userId")).isEqualTo("user123");

        MapSqlParameterSource secondInsert = paramsCaptor.getAllValues().get(2);
        assertThat(secondInsert.getValue("ref")).isEqualTo("B2");
    }
}
