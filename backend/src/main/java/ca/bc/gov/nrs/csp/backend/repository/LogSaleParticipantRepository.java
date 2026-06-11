package ca.bc.gov.nrs.csp.backend.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

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

    public void update(Long id, String name, String city, String province, String userId) {
        String sql = """
                UPDATE THE.log_sale_participant SET
                    name = :name,
                    city = :city,
                    province = :province,
                    revision_count = revision_count + 1,
                    update_userid = :userId,
                    update_timestamp = SYSDATE
                WHERE log_sale_participant_id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", name)
                .addValue("city", city)
                .addValue("province", province)
                .addValue("userId", userId);
        jdbc.update(sql, params);
    }

    public void delete(Long id) {
        if (id == null) return;
        jdbc.update("DELETE FROM THE.log_sale_participant WHERE log_sale_participant_id = :id",
                new MapSqlParameterSource("id", id));
    }
}
