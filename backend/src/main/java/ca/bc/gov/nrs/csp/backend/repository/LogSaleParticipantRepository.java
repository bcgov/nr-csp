package ca.bc.gov.nrs.csp.backend.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CRUD for {@code THE.log_sale_participant} — the table that stores manual "other party"
 * details (name/city/province) for invoices where the other party isn't a registered
 * client in {@code client_location}.
 */
@Repository
public class LogSaleParticipantRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public LogSaleParticipantRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long insert(String name, String city, String province, String userId) {
        String sql = """
                INSERT INTO THE.log_sale_participant (
                    log_sale_participant_id,
                    name, city, province,
                    revision_count,
                    entry_userid, entry_timestamp, update_userid, update_timestamp
                ) VALUES (
                    THE.CSP_LOG_SALE_PARTICIPANT_SEQ.NEXTVAL,
                    :name, :city, :province,
                    0,
                    :userId, SYSDATE, :userId, SYSDATE
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("city", city)
                .addValue("province", province)
                .addValue("userId", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"LOG_SALE_PARTICIPANT_ID"});
        return ((Number) keyHolder.getKeys().get("LOG_SALE_PARTICIPANT_ID")).longValue();
    }

    /**
     * The participant with this name, city and province, inserted if there is none. Legacy reuses
     * these rows across invoices (SubmissionTransferRules.getAndSaveOtherPartiFromDTO), so a row is
     * never edited in place. As in legacy, a null field never matches, so it gets its own row.
     */
    public Long findOrInsert(String name, String city, String province, String userId) {
        String sql = """
                SELECT log_sale_participant_id FROM THE.log_sale_participant
                WHERE name = :name AND city = :city AND province = :province
                ORDER BY log_sale_participant_id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("city", city)
                .addValue("province", province);
        List<Long> ids = jdbc.queryForList(sql, params, Long.class);
        return ids.isEmpty() ? insert(name, city, province, userId) : ids.get(0);
    }

    /** Deletes the participant only when no invoice refers to it as buyer or seller any more. */
    public void deleteIfUnreferenced(Long id) {
        if (id == null) return;
        String sql = """
                DELETE FROM THE.log_sale_participant p
                WHERE p.log_sale_participant_id = :id
                  AND NOT EXISTS (
                      SELECT 1 FROM THE.coastal_log_sale cls
                      WHERE cls.buyer_log_sale_participant_id = p.log_sale_participant_id
                         OR cls.seller_log_sale_participant_id = p.log_sale_participant_id)
                """;
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }
}
