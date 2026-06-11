package ca.bc.gov.nrs.csp.backend.testsupport;

import org.testcontainers.oracle.OracleContainer;

/**
 * Singleton Testcontainers Oracle container shared across all IT classes in the same JVM.
 * Uses the Apache-2.0 licensed gvenzl/oracle-free image.
 *
 * Usage in an IT class:
 * <pre>
 *   {@literal @}BeforeAll
 *   static void setup() {
 *       RequiresOracle.assumeDockerAvailable();
 *       OracleTestContainer.start();
 *       // wire datasource URL/user/password from OracleTestContainer.getJdbcUrl() etc.
 *   }
 * </pre>
 */
public final class OracleTestContainer {

    private static final OracleContainer CONTAINER = new OracleContainer("gvenzl/oracle-free:23.26.1")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    private OracleTestContainer() {}

    public static void start() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
    }

    public static String getJdbcUrl()  { return CONTAINER.getJdbcUrl(); }
    public static String getUsername() { return CONTAINER.getUsername(); }
    public static String getPassword() { return CONTAINER.getPassword(); }
}
