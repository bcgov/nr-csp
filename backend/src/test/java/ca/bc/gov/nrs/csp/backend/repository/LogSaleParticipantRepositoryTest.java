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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
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
    // update
    // ---------------------------------------------------------------

    @Test
    void update_bindsAllParamsAndBumpsRevisionCount() {
        repo.update(321L, "ACME Logging", "Nanaimo", "BC", "USER1");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(sqlCaptor.capture(), paramsCaptor.capture());

        assertThat(sqlCaptor.getValue())
                .contains("UPDATE THE.log_sale_participant SET")
                .contains("revision_count = revision_count + 1")
                .contains("WHERE log_sale_participant_id = :id");

        MapSqlParameterSource params = paramsCaptor.getValue();
        assertThat(params.getValue("id")).isEqualTo(321L);
        assertThat(params.getValue("name")).isEqualTo("ACME Logging");
        assertThat(params.getValue("city")).isEqualTo("Nanaimo");
        assertThat(params.getValue("province")).isEqualTo("BC");
        assertThat(params.getValue("userId")).isEqualTo("USER1");
    }

    // ---------------------------------------------------------------
    // delete
    // ---------------------------------------------------------------

    @Test
    void delete_nullId_doesNothing() {
        repo.delete(null);
        verifyNoInteractions(jdbc);
    }

    @Test
    void delete_deletesByPrimaryKey() {
        repo.delete(321L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(sqlCaptor.capture(), paramsCaptor.capture());

        assertThat(sqlCaptor.getValue())
                .contains("DELETE FROM THE.log_sale_participant WHERE log_sale_participant_id = :id");
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(321L);
    }
}
