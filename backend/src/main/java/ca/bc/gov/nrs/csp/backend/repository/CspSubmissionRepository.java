package ca.bc.gov.nrs.csp.backend.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CspSubmissionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CspSubmissionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record CspSubmission(
            Long submissionId,
            String clientNumber,
            String clientLocnCode,
            String statusCode,
            String monthCompleteInd
    ) {}

    public Optional<CspSubmission> findById(Long submissionId) {
        if (submissionId == null) return Optional.empty();
        String sql = """
                SELECT csp_submission_id, client_number, client_locn_code,
                       csp_submission_status_code, month_complete_ind
                FROM THE.csp_submission
                WHERE csp_submission_id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("id", submissionId);
        List<CspSubmission> rows = jdbc.query(sql, params, (rs, rowNum) -> new CspSubmission(
                rs.getLong("csp_submission_id"),
                rs.getString("client_number"),
                rs.getString("client_locn_code"),
                rs.getString("csp_submission_status_code"),
                rs.getString("month_complete_ind")
        ));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public boolean existsBySubmissionNumber(Long submissionNumber) {
        if (submissionNumber == null) return false;
        String sql = "SELECT COUNT(*) FROM THE.csp_submission WHERE submission_id = :num";
        MapSqlParameterSource params = new MapSqlParameterSource("num", submissionNumber);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public Long insertSubmission(String clientNumber, String clientLocnCode, String statusCode, String userId) {
        String sql = """
                INSERT INTO THE.csp_submission (
                    csp_submission_id, csp_submission_status_code,
                    client_number, client_locn_code,
                    month_complete_ind, number_invoices_submitted,
                    revision_count,
                    entry_userid, entry_timestamp, update_userid, update_timestamp
                ) VALUES (
                    THE.CSP_SUBMISSION_SEQ.NEXTVAL, :status,
                    :clientNumber, :clientLocnCode,
                    'N', 1,
                    0,
                    :userId, SYSDATE, :userId, SYSDATE
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", statusCode)
                .addValue("clientNumber", clientNumber)
                .addValue("clientLocnCode", clientLocnCode)
                .addValue("userId", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"CSP_SUBMISSION_ID"});
        return ((Number) keyHolder.getKeys().get("CSP_SUBMISSION_ID")).longValue();
    }

    public void delete(Long submissionId) {
        if (submissionId == null) return;
        jdbc.update("DELETE FROM THE.csp_submission WHERE csp_submission_id = :id",
                new MapSqlParameterSource("id", submissionId));
    }

    public void updateSubmissionStatus(Long submissionId, String statusCode, String userId) {
        String sql = """
                UPDATE THE.csp_submission SET
                    csp_submission_status_code = :status,
                    revision_count = revision_count + 1,
                    update_userid = :userId,
                    update_timestamp = SYSDATE
                WHERE csp_submission_id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("status", statusCode)
                .addValue("userId", userId);
        jdbc.update(sql, params);
    }
}
