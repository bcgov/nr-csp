package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for ClientLocationRepository SQL construction, parameter binding
 * and row mapping, using a mocked NamedParameterJdbcTemplate.
 */
@ExtendWith(MockitoExtension.class)
class ClientLocationRepositoryTest {

    @Mock NamedParameterJdbcTemplate jdbc;

    ClientLocationRepository repo;

    @BeforeEach
    void setUp() {
        repo = new ClientLocationRepository(jdbc);
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

    private ResultSet fullRow() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        given(rs.getString("client_number")).willReturn("00012345");
        given(rs.getString("client_name")).willReturn("ACME FOREST PRODUCTS");
        given(rs.getString("client_locn_code")).willReturn("00");
        given(rs.getString("client_locn_name")).willReturn("HEAD OFFICE");
        given(rs.getString("city")).willReturn("VICTORIA");
        given(rs.getString("province")).willReturn("BC");
        return rs;
    }

    private static void assertFullRow(ClientLocation row) {
        assertThat(row.clientNumber()).isEqualTo("00012345");
        assertThat(row.clientName()).isEqualTo("ACME FOREST PRODUCTS");
        assertThat(row.clientLocnCode()).isEqualTo("00");
        assertThat(row.clientLocnName()).isEqualTo("HEAD OFFICE");
        assertThat(row.city()).isEqualTo("VICTORIA");
        assertThat(row.province()).isEqualTo("BC");
    }

    // ---------------------------------------------------------------
    // findByName
    // ---------------------------------------------------------------

    @Test
    void findByName_buildsCaseInsensitiveContainsQueryOnHeadOfficeLocation() throws SQLException {
        stubQueryMapsRow(fullRow());

        List<ClientLocation> results = repo.findByName("acme");

        assertThat(results).hasSize(1);
        assertFullRow(results.getFirst());

        String sql = captureQuerySql();
        assertThat(sql)
                .contains("JOIN THE.client_location cl ON fc.client_number = cl.client_number")
                .contains("WHERE UPPER(fc.client_name) LIKE UPPER(:name)")
                .contains("cl.client_locn_code = '00'")
                .contains("ORDER BY fc.client_name")
                .contains("FETCH FIRST 50 ROWS ONLY")
                // The raw value must not be concatenated into the SQL
                .doesNotContain("acme");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByName_wrapsNameInContainsWildcards() {
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());

        repo.findByName("acme");

        MapSqlParameterSource params = captureQueryParams();
        assertThat(params.getValue("name")).isEqualTo("%acme%");
    }

    // ---------------------------------------------------------------
    // findByNumber
    // ---------------------------------------------------------------

    @Test
    void findByNumber_bindsExactNumberAndOrdersByLocationCode() throws SQLException {
        stubQueryMapsRow(fullRow());

        List<ClientLocation> results = repo.findByNumber("00012345");

        assertThat(results).hasSize(1);
        assertFullRow(results.getFirst());

        String sql = captureQuerySql();
        assertThat(sql)
                .contains("WHERE fc.client_number = :number")
                .contains("ORDER BY cl.client_locn_code")
                .contains("FETCH FIRST 50 ROWS ONLY");

        MapSqlParameterSource params = captureQueryParams();
        assertThat(params.getValue("number")).isEqualTo("00012345");
    }

    @Test
    void findByNumber_nullColumns_mapToNullFields() {
        // All ResultSet getters default to null on an unstubbed mock.
        stubQueryMapsRow(mock(ResultSet.class));

        List<ClientLocation> results = repo.findByNumber("00012345");

        assertThat(results).hasSize(1);
        ClientLocation row = results.getFirst();
        assertThat(row.clientNumber()).isNull();
        assertThat(row.clientName()).isNull();
        assertThat(row.clientLocnCode()).isNull();
        assertThat(row.clientLocnName()).isNull();
        assertThat(row.city()).isNull();
        assertThat(row.province()).isNull();
    }
}
