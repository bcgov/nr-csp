package ca.bc.gov.nrs.csp.backend.testsupport;

import org.junit.jupiter.api.Assumptions;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit 5 assumption helper that skips Oracle integration tests when Docker is unavailable.
 *
 * Call {@link #assumeDockerAvailable()} in a {@code @BeforeAll} method of any {@code *IT.java} class:
 * <pre>
 *   {@literal @}BeforeAll
 *   static void setup() {
 *       RequiresOracle.assumeDockerAvailable();
 *       OracleTestContainer.start();
 *   }
 * </pre>
 */
public final class RequiresOracle {

    private RequiresOracle() {}

    public static void assumeDockerAvailable() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available — skipping Oracle integration test.");
    }
}
