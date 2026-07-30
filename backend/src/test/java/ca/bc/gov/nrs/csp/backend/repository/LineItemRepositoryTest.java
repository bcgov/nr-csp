package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for LineItemRepository SQL construction, parameter binding,
 * row mapping and generated-key retrieval.
 *
 * <p>The NamedParameterJdbcTemplate is mocked; row mappers are executed
 * against a mocked ResultSet, and generated keys are simulated by populating
 * the KeyHolder inside the stubbed update call.</p>
 */
@ExtendWith(MockitoExtension.class)
class LineItemRepositoryTest {

    @Mock NamedParameterJdbcTemplate jdbc;

    LineItemRepository repo;

    @BeforeEach
    void setUp() {
        repo = new LineItemRepository(jdbc);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void stubQueryMapsRow(ResultSet rs) {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willAnswer(inv -> {
                    RowMapper<Object> rowMapper = inv.getArgument(2);
                    return Collections.singletonList(rowMapper.mapRow(rs, 0));
                });
    }

    private void stubGeneratedKey(long key) {
        given(jdbc.update(anyString(), any(MapSqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .willAnswer(inv -> {
                    KeyHolder keyHolder = inv.getArgument(2);
                    keyHolder.getKeyList().add(Map.of("COASTAL_LOG_SALE_DETAIL_ID", BigDecimal.valueOf(key)));
                    return 1;
                });
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

    private MapSqlParameterSource captureInsertParams() {
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), paramsCaptor.capture(), any(KeyHolder.class), any(String[].class));
        return paramsCaptor.getValue();
    }

    private static LineItem lineItem(String clientSecondarySort) {
        return new LineItem(
                null, null, "SORT01", clientSecondarySort, "SPC1", null, "G1",
                50, new BigDecimal("25.00"), new BigDecimal("6.25"), new BigDecimal("21.25"), null);
    }

    // ---------------------------------------------------------------
    // findByInvoiceId
    // ---------------------------------------------------------------

    @Test
    void findByInvoiceId_nullId_returnsEmptyListWithoutQuerying() {
        assertThat(repo.findByInvoiceId(null)).isEmpty();
        verifyNoInteractions(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByInvoiceId_mapsEveryColumnAndComputesAmount() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getBigDecimal("price")).willReturn(new BigDecimal("25.00"));
        given(rs.getBigDecimal("volume")).willReturn(new BigDecimal("6.25"));
        given(rs.getObject("line_item_id", Long.class)).willReturn(10L);
        given(rs.getObject("invoice_id", Long.class)).willReturn(12345L);
        given(rs.getString("second_sort")).willReturn("SORT01");
        given(rs.getString("client_secondary_sort")).willReturn("CLIENT01");
        given(rs.getString("species")).willReturn("SPC1");
        given(rs.getString("species_description")).willReturn("Cedar");
        given(rs.getString("grade")).willReturn("G1");
        given(rs.getObject("pieces", Integer.class)).willReturn(50);
        given(rs.getBigDecimal("converted_price")).willReturn(new BigDecimal("21.25"));
        stubQueryMapsRow(rs);

        List<LineItem> items = repo.findByInvoiceId(12345L);

        assertThat(items).hasSize(1);
        LineItem item = items.get(0);
        assertThat(item.lineItemID()).isEqualTo(10L);
        assertThat(item.invoiceID()).isEqualTo(12345L);
        assertThat(item.secondSort()).isEqualTo("SORT01");
        assertThat(item.clientSecondarySort()).isEqualTo("CLIENT01");
        assertThat(item.species()).isEqualTo("SPC1");
        assertThat(item.speciesDescription()).isEqualTo("Cedar");
        assertThat(item.grade()).isEqualTo("G1");
        assertThat(item.numOfPieces()).isEqualTo(50);
        assertThat(item.price()).isEqualByComparingTo("25.00");
        assertThat(item.volume()).isEqualByComparingTo("6.25");
        assertThat(item.convertedPrice()).isEqualByComparingTo("21.25");
        // amount = price * volume
        assertThat(item.amount()).isEqualByComparingTo("156.25");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
                .contains("WHERE d.coastal_log_sale_id = :id")
                .contains("ORDER BY d.coastal_log_sale_detail_id");
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(12345L);
    }

    @Test
    void findByInvoiceId_nullPrice_amountIsNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Explicit null stub: the mapper reads "price" first, and strict stubs
        // reject the argument mismatch against the "volume"-only stubbing.
        given(rs.getBigDecimal("price")).willReturn(null);
        given(rs.getBigDecimal("volume")).willReturn(new BigDecimal("6.25"));
        stubQueryMapsRow(rs);

        List<LineItem> items = repo.findByInvoiceId(12345L);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).price()).isNull();
        assertThat(items.get(0).volume()).isEqualByComparingTo("6.25");
        assertThat(items.get(0).amount()).isNull();
    }

    @Test
    void findByInvoiceId_nullVolume_amountIsNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getBigDecimal("price")).willReturn(new BigDecimal("25.00"));
        stubQueryMapsRow(rs);

        List<LineItem> items = repo.findByInvoiceId(12345L);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).volume()).isNull();
        assertThat(items.get(0).amount()).isNull();
    }

    // ---------------------------------------------------------------
    // insertLineItem
    // ---------------------------------------------------------------

    @Test
    void insertLineItem_returnsGeneratedKeyAndBindsAllParams() {
        stubGeneratedKey(456L);

        Long id = repo.insertLineItem(12345L, lineItem("CLIENT01"), "USER1");

        assertThat(id).isEqualTo(456L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String[]> keyColumnsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(jdbc).update(sqlCaptor.capture(), any(MapSqlParameterSource.class),
                any(KeyHolder.class), keyColumnsCaptor.capture());
        assertThat(sqlCaptor.getValue())
                .contains("INSERT INTO THE.coastal_log_sale_detail")
                .contains("THE.COASTAL_LOG_SALE_DETAIL_SEQ.NEXTVAL")
                .contains("SELECT csp_species_grade_xref_id FROM THE.csp_species_grade_xref");
        assertThat(keyColumnsCaptor.getValue()).containsExactly("COASTAL_LOG_SALE_DETAIL_ID");

        MapSqlParameterSource params = captureInsertParams();
        assertThat(params.getValue("invoiceId")).isEqualTo(12345L);
        assertThat(params.getValue("species")).isEqualTo("SPC1");
        assertThat(params.getValue("grade")).isEqualTo("G1");
        assertThat(params.getValue("sortCode")).isEqualTo("SORT01");
        // Client-supplied secondary sort is used when present.
        assertThat(params.getValue("clientSortCode")).isEqualTo("CLIENT01");
        assertThat(params.getValue("pieces")).isEqualTo(50);
        assertThat(params.getValue("volume")).isEqualTo(new BigDecimal("6.25"));
        assertThat(params.getValue("price")).isEqualTo(new BigDecimal("25.00"));
        assertThat(params.getValue("convertedPrice")).isEqualTo(new BigDecimal("21.25"));
        assertThat(params.getValue("userId")).isEqualTo("USER1");
    }

    @Test
    void insertLineItem_blankClientSort_fallsBackToSecondSort() {
        stubGeneratedKey(456L);

        repo.insertLineItem(12345L, lineItem("   "), "USER1");

        MapSqlParameterSource params = captureInsertParams();
        assertThat(params.getValue("clientSortCode")).isEqualTo("SORT01");
    }

    // ---------------------------------------------------------------
    // updateLineItem
    // ---------------------------------------------------------------

    @Test
    void updateLineItem_bindsAllParamsAndBumpsRevisionCount() {
        repo.updateLineItem(10L, lineItem("CLIENT01"), "USER1");

        String sql = captureUpdateSql();
        assertThat(sql)
                .contains("UPDATE THE.coastal_log_sale_detail SET")
                .contains("revision_count = revision_count + 1")
                .contains("WHERE coastal_log_sale_detail_id = :id");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("id")).isEqualTo(10L);
        assertThat(params.getValue("species")).isEqualTo("SPC1");
        assertThat(params.getValue("grade")).isEqualTo("G1");
        assertThat(params.getValue("sortCode")).isEqualTo("SORT01");
        assertThat(params.getValue("clientSortCode")).isEqualTo("CLIENT01");
        assertThat(params.getValue("pieces")).isEqualTo(50);
        assertThat(params.getValue("volume")).isEqualTo(new BigDecimal("6.25"));
        assertThat(params.getValue("price")).isEqualTo(new BigDecimal("25.00"));
        assertThat(params.getValue("convertedPrice")).isEqualTo(new BigDecimal("21.25"));
        assertThat(params.getValue("userId")).isEqualTo("USER1");
    }

    @Test
    void updateLineItem_nullClientSort_fallsBackToSecondSort() {
        repo.updateLineItem(10L, lineItem(null), "USER1");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("clientSortCode")).isEqualTo("SORT01");
    }

    // ---------------------------------------------------------------
    // updateConvertedPrice — must not touch user columns or revision count
    // ---------------------------------------------------------------

    @Test
    void updateConvertedPrice_bindsOnlyConversionColumnsAndSkipsRevisionCount() {
        repo.updateConvertedPrice(10L, new BigDecimal("19.50"), "USER1");

        String sql = captureUpdateSql();
        assertThat(sql)
                .contains("converted_price = :convertedPrice")
                .contains("WHERE coastal_log_sale_detail_id = :id")
                .doesNotContain("revision_count")
                .doesNotContain(":price")
                .doesNotContain(":volume");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("convertedPrice")).isEqualTo(new BigDecimal("19.50"));
        assertThat(params.getValue("userId")).isEqualTo("USER1");
        assertThat(params.getValue("id")).isEqualTo(10L);
    }

    // ---------------------------------------------------------------
    // deleteLineItem / deleteAllByInvoiceId
    // ---------------------------------------------------------------

    @Test
    void deleteLineItem_deletesByDetailId() {
        repo.deleteLineItem(10L);

        String sql = captureUpdateSql();
        assertThat(sql).contains("DELETE FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_detail_id = :id");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("id")).isEqualTo(10L);
    }

    @Test
    void deleteAllByInvoiceId_deletesByInvoiceId() {
        repo.deleteAllByInvoiceId(12345L);

        String sql = captureUpdateSql();
        assertThat(sql).contains("DELETE FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = :id");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("id")).isEqualTo(12345L);
    }

    // ---------------------------------------------------------------
    // findIdsByInvoiceId
    // ---------------------------------------------------------------

    @Test
    void findIdsByInvoiceId_returnsIdsAndBindsInvoiceId() {
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .willReturn(List.of(1L, 2L, 3L));

        List<Long> ids = repo.findIdsByInvoiceId(12345L);

        assertThat(ids).containsExactly(1L, 2L, 3L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(sqlCaptor.capture(), paramsCaptor.capture(), eq(Long.class));
        assertThat(sqlCaptor.getValue())
                .contains("SELECT coastal_log_sale_detail_id FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = :id");
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(12345L);
    }
}
