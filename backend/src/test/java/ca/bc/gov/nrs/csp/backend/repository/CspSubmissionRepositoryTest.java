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
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for CspSubmissionRepository SQL construction, parameter binding,
 * row mapping and generated-key retrieval, using a mocked
 * NamedParameterJdbcTemplate.
 */
@ExtendWith(MockitoExtension.class)
class CspSubmissionRepositoryTest {

    @Mock NamedParameterJdbcTemplate jdbc;

    CspSubmissionRepository repo;

    @BeforeEach
    void setUp() {
        repo = new CspSubmissionRepository(jdbc);
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

    // ---------------------------------------------------------------
    // findById
    // ---------------------------------------------------------------

    @Test
    void findById_nullId_returnsEmptyWithoutQuerying() {
        assertThat(repo.findById(null)).isEmpty();
        verifyNoInteractions(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_found_mapsEveryColumnAndBindsId() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getLong("csp_submission_id")).willReturn(42L);
        given(rs.getString("client_number")).willReturn("00012345");
        given(rs.getString("client_locn_code")).willReturn("00");
        given(rs.getString("csp_submission_status_code")).willReturn("INB");
        given(rs.getString("month_complete_ind")).willReturn("N");
        stubQueryMapsRow(rs);

        Optional<CspSubmissionRepository.CspSubmission> result = repo.findById(42L);

        assertThat(result).isPresent();
        CspSubmissionRepository.CspSubmission submission = result.get();
        assertThat(submission.submissionId()).isEqualTo(42L);
        assertThat(submission.clientNumber()).isEqualTo("00012345");
        assertThat(submission.clientLocnCode()).isEqualTo("00");
        assertThat(submission.statusCode()).isEqualTo("INB");
        assertThat(submission.monthCompleteInd()).isEqualTo("N");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
                .contains("FROM THE.csp_submission")
                .contains("WHERE csp_submission_id = :id");
        assertThat(paramsCaptor.getValue().getValue("id")).isEqualTo(42L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_noRows_returnsEmpty() {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());

        assertThat(repo.findById(42L)).isEmpty();
    }

    // ---------------------------------------------------------------
    // existsBySubmissionNumber
    // ---------------------------------------------------------------

    @Test
    void existsBySubmissionNumber_nullNumber_returnsFalseWithoutQuerying() {
        assertThat(repo.existsBySubmissionNumber(null)).isFalse();
        verifyNoInteractions(jdbc);
    }

    @Test
    void existsBySubmissionNumber_positiveCount_returnsTrue() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(1);

        assertThat(repo.existsBySubmissionNumber(9001L)).isTrue();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(sqlCaptor.capture(), paramsCaptor.capture(), eq(Integer.class));
        assertThat(sqlCaptor.getValue()).contains("WHERE submission_id = :num");
        assertThat(paramsCaptor.getValue().getValue("num")).isEqualTo(9001L);
    }

    @Test
    void existsBySubmissionNumber_zeroCount_returnsFalse() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(0);

        assertThat(repo.existsBySubmissionNumber(9001L)).isFalse();
    }

    @Test
    void existsBySubmissionNumber_nullCount_returnsFalse() {
        given(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .willReturn(null);

        assertThat(repo.existsBySubmissionNumber(9001L)).isFalse();
    }

    // ---------------------------------------------------------------
    // insertSubmission
    // ---------------------------------------------------------------

    @Test
    void insertSubmission_returnsGeneratedKeyAndBindsAllParams() {
        given(jdbc.update(anyString(), any(MapSqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .willAnswer(inv -> {
                    KeyHolder keyHolder = inv.getArgument(2);
                    keyHolder.getKeyList().add(Map.of("CSP_SUBMISSION_ID", BigDecimal.valueOf(777L)));
                    return 1;
                });

        Long id = repo.insertSubmission("00012345", "00", "INB", "USER1");

        assertThat(id).isEqualTo(777L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        ArgumentCaptor<String[]> keyColumnsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(jdbc).update(sqlCaptor.capture(), paramsCaptor.capture(),
                any(KeyHolder.class), keyColumnsCaptor.capture());

        assertThat(sqlCaptor.getValue())
                .contains("INSERT INTO THE.csp_submission")
                .contains("THE.CSP_SUBMISSION_SEQ.NEXTVAL");
        assertThat(keyColumnsCaptor.getValue()).containsExactly("CSP_SUBMISSION_ID");

        MapSqlParameterSource params = paramsCaptor.getValue();
        assertThat(params.getValue("status")).isEqualTo("INB");
        assertThat(params.getValue("clientNumber")).isEqualTo("00012345");
        assertThat(params.getValue("clientLocnCode")).isEqualTo("00");
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
        repo.delete(42L);

        String sql = captureUpdateSql();
        assertThat(sql).contains("DELETE FROM THE.csp_submission WHERE csp_submission_id = :id");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("id")).isEqualTo(42L);
    }

    // ---------------------------------------------------------------
    // updateSubmissionStatus
    // ---------------------------------------------------------------

    @Test
    void updateSubmissionStatus_bindsParamsAndBumpsRevisionCount() {
        repo.updateSubmissionStatus(42L, "COM", "USER1");

        String sql = captureUpdateSql();
        assertThat(sql)
                .contains("UPDATE THE.csp_submission SET")
                .contains("csp_submission_status_code = :status")
                .contains("revision_count = revision_count + 1")
                .contains("WHERE csp_submission_id = :id");

        MapSqlParameterSource params = captureUpdateParams();
        assertThat(params.getValue("id")).isEqualTo(42L);
        assertThat(params.getValue("status")).isEqualTo("COM");
        assertThat(params.getValue("userId")).isEqualTo("USER1");
    }
}
