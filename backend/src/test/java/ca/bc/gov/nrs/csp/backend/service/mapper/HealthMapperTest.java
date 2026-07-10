package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.health.HealthResponse;
import ca.bc.gov.nrs.csp.backend.service.model.Health;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HealthMapperTest {

    private final HealthMapper mapper = new HealthMapperImpl();

    @Test
    void toResponse_mapsAllFields() {
        Instant now = Instant.parse("2026-07-08T12:34:56Z");
        Health health = new Health("UP", now);

        HealthResponse response = mapper.toResponse(health);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.timestamp()).isEqualTo(now);
    }

    @Test
    void toResponse_nullFields_areMappedAsNull() {
        HealthResponse response = mapper.toResponse(new Health(null, null));

        assertThat(response).isNotNull();
        assertThat(response.status()).isNull();
        assertThat(response.timestamp()).isNull();
    }

    @Test
    void toResponse_nullInput_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
