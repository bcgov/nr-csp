package ca.bc.gov.nrs.csp.backend.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigTest {

    @Test
    void clock_isSystemClockInPacificBusinessZone() {
        Clock clock = new ClockConfig().clock();

        assertThat(clock).isEqualTo(Clock.system(ZoneId.of("America/Vancouver")));
    }

    @Test
    void clock_usesVancouverZone() {
        Clock clock = new ClockConfig().clock();

        assertThat(clock.getZone()).isEqualTo(ZoneId.of("America/Vancouver"));
    }

    @Test
    void clock_tracksSystemTime() {
        Clock clock = new ClockConfig().clock();

        assertThat(clock.instant()).isNotNull();
    }
}
