package ca.bc.gov.nrs.csp.backend.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Wraps a datasource and pre-flights the connection at startup with a validation query.
 * Throws on failure so misconfigured credentials surface at boot time rather than at first request.
 */
public class ValidatingDataSource extends DelegatingDataSource {

    private static final Logger log = LogManager.getLogger(ValidatingDataSource.class);

    private static final String VALIDATION_QUERY = "SELECT 1 FROM DUAL";

    public ValidatingDataSource(DataSource targetDataSource) {
        super(targetDataSource);
        validate();
    }

    private void validate() {
        try (Connection conn = getTargetDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(VALIDATION_QUERY);
            log.info("Datasource validation successful.");
        } catch (SQLException e) {
            throw new IllegalStateException("Datasource validation failed. Check SPRING_DATASOURCE_URL/USERNAME/PASSWORD.", e);
        }
    }
}
