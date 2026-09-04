package ca.bc.gov.nrs.csp.backend.testsupport;

import org.testcontainers.oracle.OracleContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
 *       OracleTestContainer.bootstrap();
 *       // wire datasource URL/user/password from OracleTestContainer.getJdbcUrl() etc.
 *   }
 * </pre>
 */
public final class OracleTestContainer {

    private static final String BOOTSTRAP_SCRIPT = "/db/test-bootstrap.sql";

    private static final OracleContainer CONTAINER = new OracleContainer("gvenzl/oracle-free:23.26.1")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    private static volatile boolean bootstrapped;

    private OracleTestContainer() {}

    public static void start() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
    }

    /**
     * Runs {@value BOOTSTRAP_SCRIPT} once per container: creates the THE schema, seed data,
     * and stub CSP_SP_RPT_* procedures, then grants them to the app user. Connects as SYSTEM
     * (the gvenzl image sets the SYS/SYSTEM password to the container password). Idempotent:
     * skipped when the THE user already exists, so a reused container isn't re-bootstrapped.
     */
    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        try (Connection conn = DriverManager.getConnection(getJdbcUrl(), "system", CONTAINER.getPassword())) {
            if (!schemaExists(conn)) {
                for (String statement : loadBootstrapStatements()) {
                    execute(conn, statement);
                }
            }
            bootstrapped = true;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to bootstrap the Oracle test schema", e);
        }
    }

    public static String getJdbcUrl()  { return CONTAINER.getJdbcUrl(); }
    public static String getUsername() { return CONTAINER.getUsername(); }
    public static String getPassword() { return CONTAINER.getPassword(); }

    private static boolean schemaExists(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM all_users WHERE username = 'THE'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    /**
     * The script uses a line containing only "/" as the statement separator (never semicolons),
     * so PL/SQL bodies need no special parsing; full-line "--" comments are stripped.
     */
    private static List<String> loadBootstrapStatements() {
        String script;
        try (InputStream in = OracleTestContainer.class.getResourceAsStream(BOOTSTRAP_SCRIPT)) {
            if (in == null) {
                throw new IllegalStateException("Bootstrap script not found on classpath: " + BOOTSTRAP_SCRIPT);
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + BOOTSTRAP_SCRIPT, e);
        }
        script = script.replace("${APP_USER}", CONTAINER.getUsername());

        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.equals("/")) {
                if (!current.isEmpty()) {
                    statements.add(current.toString().trim());
                    current.setLength(0);
                }
            } else if (!trimmed.startsWith("--")) {
                current.append(line).append('\n');
            }
        }
        if (!current.toString().isBlank()) {
            statements.add(current.toString().trim());
        }
        return statements;
    }

    private static void execute(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            String head = sql.lines().findFirst().orElse(sql);
            throw new SQLException("Bootstrap statement failed [" + head + "]: " + e.getMessage(), e);
        }
    }
}
