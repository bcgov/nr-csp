package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.service.model.Health;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HealthServiceTest {

    private final HealthService service = new HealthService();

    @Test
    void getHealth_returnsUpStatus() {
        Health health = service.getHealth();

        assertThat(health.status()).isEqualTo("UP");
    }

    @Test
    void getHealth_returnsRecentTimestamp() {
        Instant before = Instant.now();
        Health health = service.getHealth();
        Instant after = Instant.now();

        assertThat(health.timestamp()).isBetween(before, after);
    }
}
