package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.testsupport.OracleTestContainer;
import ca.bc.gov.nrs.csp.backend.testsupport.RequiresOracle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the invoice version and lock SQL against real Oracle, including the legacy CSP write patterns. */
class InvoiceVersionIT {

    private static final long INVOICE_ID = 900L;

    private static NamedParameterJdbcTemplate jdbc;
    private static InvoiceRepository repo;
    private static TransactionTemplate transaction;

    @BeforeAll
    static void startOracle() {
        RequiresOracle.assumeDockerAvailable();
        OracleTestContainer.start();
        OracleTestContainer.bootstrap();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                OracleTestContainer.getJdbcUrl(), OracleTestContainer.getUsername(), OracleTestContainer.getPassword());
        jdbc = new NamedParameterJdbcTemplate(dataSource);
        repo = new InvoiceRepository(jdbc);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void seedInvoiceWithOneLine() {
        execute("DELETE FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = 900");
        execute("DELETE FROM THE.coastal_log_sale WHERE coastal_log_sale_id = 900");
        execute("INSERT INTO THE.coastal_log_sale (coastal_log_sale_id, revision_count, update_timestamp) "
                + "VALUES (900, 3, TO_DATE('2026-11-17 14:00:00', 'YYYY-MM-DD HH24:MI:SS'))");
        execute("INSERT INTO THE.coastal_log_sale_detail (coastal_log_sale_detail_id, coastal_log_sale_id, revision_count) "
                + "VALUES (9001, 900, 1)");
    }

    @Test
    void version_combinesHeaderRevisionTimestampAndLines() {
        assertThat(repo.findVersion(INVOICE_ID)).isEqualTo("3.20261117140000.1.1");
    }

    @Test
    void lockVersion_returnsTheVersionInsideATransaction() {
        Optional<String> locked = transaction.execute(status -> repo.lockVersion(INVOICE_ID));

        Optional<String> missing = transaction.execute(status -> repo.lockVersion(404L));

        assertThat(locked).contains("3.20261117140000.1.1");
        assertThat(missing).isEmpty();
    }

    @Test
    void legacyLineAdd_withNoHeaderWrite_changesVersion() {
        String before = repo.findVersion(INVOICE_ID);

        execute("INSERT INTO THE.coastal_log_sale_detail (coastal_log_sale_detail_id, coastal_log_sale_id, revision_count) "
                + "VALUES (9002, 900, 0)");

        assertThat(repo.findVersion(INVOICE_ID)).isNotEqualTo(before);
    }

    @Test
    void legacyLineEdit_withHeaderTimestampOnly_changesVersion() {
        String before = repo.findVersion(INVOICE_ID);

        execute("UPDATE THE.coastal_log_sale_detail SET revision_count = revision_count + 1 WHERE coastal_log_sale_detail_id = 9001");
        execute("UPDATE THE.coastal_log_sale SET update_timestamp = SYSDATE WHERE coastal_log_sale_id = 900");

        assertThat(repo.findVersion(INVOICE_ID)).isNotEqualTo(before);
    }

    @Test
    void incrementRevisionCount_changesVersion() {
        String before = repo.findVersion(INVOICE_ID);

        repo.incrementRevisionCount(INVOICE_ID, "it-user");

        assertThat(repo.findVersion(INVOICE_ID)).isNotEqualTo(before);
    }

    private static void execute(String sql) {
        jdbc.update(sql, new MapSqlParameterSource());
    }
}
