package ca.bc.gov.nrs.csp.backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for LogSaleParticipantRepository SQL construction, parameter
 * binding and generated-key retrieval, using a mocked
 * NamedParameterJdbcTemplate.
 */
@ExtendWith(MockitoExtension.class)
class LogSaleParticipantRepositoryTest {

    @Mock NamedParameterJdbcTemplate jdbc;

    LogSaleParticipantRepository repo;

    @BeforeEach
    void setUp() {
        repo = new LogSaleParticipantRepository(jdbc);
    }

    // ---------------------------------------------------------------
    // insert
    // ---------------------------------------------------------------

    @Test
    void insert_returnsGeneratedKeyAndBindsAllParams() {
        given(jdbc.update(anyString(), any(MapSqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .willAnswer(inv -> {
                    KeyHolder keyHolder = inv.getArgument(2);
                    keyHolder.getKeyList().add(Map.of("LOG_SALE_PARTICIPANT_ID", BigDecimal.valueOf(321L)));
                    return 1;
                });

        Long id = repo.insert("ACME Logging", "Victoria", "BC", "USER1");

        assertThat(id).isEqualTo(321L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        ArgumentCaptor<String[]> keyColumnsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(jdbc).update(sqlCaptor.capture(), paramsCaptor.capture(),
                any(KeyHolder.class), keyColumnsCaptor.capture());

        assertThat(sqlCaptor.getValue())
                .contains("INSERT INTO THE.log_sale_participant")
                .contains("THE.CSP_LOG_SALE_PARTICIPANT_SEQ.NEXTVAL");
        assertThat(keyColumnsCaptor.getValue()).containsExactly("LOG_SALE_PARTICIPANT_ID");

        MapSqlParameterSource params = paramsCaptor.getValue();
        assertThat(params.getValue("name")).isEqualTo("ACME Logging");
        assertThat(params.getValue("city")).isEqualTo("Victoria");
        assertThat(params.getValue("province")).isEqualTo("BC");
        assertThat(params.getValue("userId")).isEqualTo("USER1");
    }

    // ---------------------------------------------------------------
    // findOrInsert
    // ---------------------------------------------------------------

    @Test
    void findOrInsert_reusesTheFirstParticipantWithTheSameDetails() {
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .willReturn(List.of(12L, 34L));

        assertThat(repo.findOrInsert("ACME Logging", "Victoria", "BC", "USER1")).isEqualTo(12L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(sqlCaptor.capture(), paramsCaptor.capture(), eq(Long.class));
        assertThat(sqlCaptor.getValue())
                .contains("WHERE name = :name AND city = :city AND province = :province")
                .contains("ORDER BY log_sale_participant_id");
        assertThat(paramsCaptor.getValue().getValue("name")).isEqualTo("ACME Logging");
        verify(jdbc, never()).update(anyString(), any(MapSqlParameterSource.class), any(KeyHolder.class), any(String[].class));
    }

    @Test
    void findOrInsert_insertsWhenNoParticipantMatches() {
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(Long.class))).willReturn(List.of());
        given(jdbc.update(anyString(), any(MapSqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .willAnswer(inv -> {
                    KeyHolder keyHolder = inv.getArgument(2);
                    keyHolder.getKeyList().add(Map.of("LOG_SALE_PARTICIPANT_ID", BigDecimal.valueOf(321L)));
                    return 1;
                });

        assertThat(repo.findOrInsert("ACME Logging", "Victoria", "BC", "USER1")).isEqualTo(321L);
    }

    // ---------------------------------------------------------------
    // deleteIfUnreferenced
    // ---------------------------------------------------------------

    @Test
    void deleteIfUnreferenced_nullId_doesNothing() {
        repo.deleteIfUnreferenced(null);
        verifyNoInteractions(jdbc);
    }

    @Test
    void deleteIfUnreferenced_skipsAParticipantAnyInvoiceStillUses() {
        repo.deleteIfUnreferenced(321L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(sqlCaptor.capture(), paramsCaptor.capture());

        assertThat(sqlCaptor.getValue())
                .contains("DELETE FROM THE.log_sale_participant p")
                .contains("NOT EXISTS")
                .contains("cls.buyer_log_sale_participant_id = p.log_sale_participant_id")
                .contains("cls.seller_log_sale_participant_id = p.log_sale_participant_id");
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(321L);
    }
}
