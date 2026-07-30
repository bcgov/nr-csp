package ca.bc.gov.nrs.csp.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Supplies a single application {@link Clock} so date/time logic is evaluated in
 * a defined business time zone (BC / Pacific) rather than the server's default
 * zone, and can be replaced with a fixed clock in tests.
 */
@Configuration
public class ClockConfig {

  /** BC business time zone — submission/invoice dates are interpreted in Pacific time. */
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Vancouver");

  @Bean
  public Clock clock() {
    return Clock.system(BUSINESS_ZONE);
  }
}
