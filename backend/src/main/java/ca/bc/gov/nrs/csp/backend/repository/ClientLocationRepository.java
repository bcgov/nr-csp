package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ClientLocationRepository {

    private static final String BASE_QUERY = """
            SELECT fc.client_number,
                   fc.client_name,
                   cl.client_locn_code,
                   cl.client_locn_name,
                   cl.city,
                   cl.province
            FROM THE.forest_client fc
            JOIN THE.client_location cl ON fc.client_number = cl.client_number
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ClientLocationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ClientLocation> findByName(String name) {
        String sql = BASE_QUERY + "WHERE UPPER(fc.client_name) LIKE UPPER(:name) AND cl.client_locn_code = '00' ORDER BY fc.client_name FETCH FIRST 50 ROWS ONLY";
        MapSqlParameterSource params = new MapSqlParameterSource("name", "%" + name + "%");
        return jdbc.query(sql, params, this::mapRow);
    }

    public List<ClientLocation> findByNumber(String number) {
        String sql = BASE_QUERY + "WHERE fc.client_number = :number ORDER BY cl.client_locn_code FETCH FIRST 50 ROWS ONLY";
        MapSqlParameterSource params = new MapSqlParameterSource("number", number);
        return jdbc.query(sql, params, this::mapRow);
    }

    private ClientLocation mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ClientLocation(
                rs.getString("client_number"),
                rs.getString("client_name"),
                rs.getString("client_locn_code"),
                rs.getString("client_locn_name"),
                rs.getString("city"),
                rs.getString("province")
        );
    }
}
