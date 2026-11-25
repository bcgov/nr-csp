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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs the invoice and line delete-audit SQL against real Oracle, in the order InvoiceService uses it. */
class InvoiceDeleteAuditIT {

    private static final long INVOICE_ID = 910L;
    private static final String DELETING_USER = "IDIR\\DELETER";

    private static NamedParameterJdbcTemplate jdbc;
    private static InvoiceRepository invoiceRepo;
    private static LineItemRepository lineItemRepo;
    private static TransactionTemplate transaction;

    @BeforeAll
    static void startOracle() {
        RequiresOracle.assumeDockerAvailable();
        OracleTestContainer.start();
        OracleTestContainer.bootstrap();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                OracleTestContainer.getJdbcUrl(), OracleTestContainer.getUsername(), OracleTestContainer.getPassword());
        jdbc = new NamedParameterJdbcTemplate(dataSource);
        invoiceRepo = new InvoiceRepository(jdbc);
        lineItemRepo = new LineItemRepository(jdbc);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void seedInvoiceWithTwoLines() {
        execute("DELETE FROM THE.coastal_log_sale_detail_audit WHERE coastal_log_sale_id = 910");
        execute("DELETE FROM THE.coastal_log_sale_audit WHERE coastal_log_sale_id = 910");
        execute("DELETE FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = 910");
        execute("DELETE FROM THE.coastal_log_sale WHERE coastal_log_sale_id = 910");
        execute("""
                INSERT INTO THE.coastal_log_sale (
                    coastal_log_sale_id, csp_submission_id, client_invoice_no, client_invoice_date,
                    log_sale_entry_status_code, csp_invoice_type_code, revision_count,
                    entry_userid, entry_timestamp, update_userid, update_timestamp,
                    submitter_notes, reviewer_notes
                ) VALUES (
                    910, 77, 'TEST-910', DATE '2026-11-01', 'DFT', 'SAL', 4,
                    'IDIR\\CREATOR', DATE '2026-11-02', 'IDIR\\EDITOR', DATE '2026-11-03',
                    'submitter note', 'reviewer note'
                )
                """);
        execute("""
                INSERT INTO THE.coastal_log_sale_detail (
                    coastal_log_sale_detail_id, coastal_log_sale_id, revision_count, log_sale_sort_code,
                    pieces, volume, price, entry_userid, entry_timestamp, update_userid, update_timestamp
                ) VALUES (9101, 910, 2, '01', 10, 5.5, 100, 'IDIR\\CREATOR', DATE '2026-11-02',
                    'IDIR\\EDITOR', DATE '2026-11-03')
                """);
        execute("""
                INSERT INTO THE.coastal_log_sale_detail (
                    coastal_log_sale_detail_id, coastal_log_sale_id, revision_count, log_sale_sort_code,
                    pieces, volume, price, entry_userid, entry_timestamp, update_userid, update_timestamp
                ) VALUES (9102, 910, 0, '01', 20, 7.25, 80, 'IDIR\\CREATOR', DATE '2026-11-02',
                    'IDIR\\EDITOR', DATE '2026-11-03')
                """);
    }

    @Test
    void invoiceDelete_writesOneHeaderAndOneLineDeleteRowPerLine() {
        transaction.executeWithoutResult(status -> deleteInvoiceWithAudit());

        assertThat(count("SELECT COUNT(*) FROM THE.coastal_log_sale WHERE coastal_log_sale_id = 910")).isZero();
        assertThat(count("SELECT COUNT(*) FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = 910")).isZero();
        assertThat(count("SELECT COUNT(*) FROM THE.coastal_log_sale_audit "
                + "WHERE coastal_log_sale_id = 910 AND csp_audit_action_type_code = 'D'")).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT coastal_log_sale_detail_id FROM THE.coastal_log_sale_detail_audit "
                        + "WHERE coastal_log_sale_id = 910 AND csp_audit_action_type_code = 'D' "
                        + "ORDER BY coastal_log_sale_detail_id", new MapSqlParameterSource(), Long.class))
                .containsExactly(9101L, 9102L);

        Map<String, Object> header = jdbc.queryForMap("SELECT * FROM THE.coastal_log_sale_audit "
                + "WHERE coastal_log_sale_id = 910", new MapSqlParameterSource());
        assertThat(header.get("CLIENT_INVOICE_NO")).isEqualTo("TEST-910");
        assertThat(header.get("ENTRY_USERID")).isEqualTo("IDIR\\CREATOR");
        assertThat(header.get("UPDATE_USERID")).isEqualTo(DELETING_USER);
        assertThat(header.get("REVIEWER_NOTES")).isEqualTo("reviewer note");
        assertThat(header.get("SUBMITTER_NOTES")).isEqualTo("submitter note");
        assertThat(count("SELECT COUNT(*) FROM THE.coastal_log_sale_audit WHERE coastal_log_sale_id = 910 "
                + "AND update_timestamp BETWEEN SYSDATE - 1/24 AND SYSDATE")).isEqualTo(1);
    }

    @Test
    void invoiceDelete_failureAfterTheAuditInsert_rollsEverythingBack() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            invoiceRepo.insertDeleteAudit(INVOICE_ID, DELETING_USER);
            lineItemRepo.auditAndDeleteAllByInvoiceId(INVOICE_ID, DELETING_USER);
            throw new IllegalStateException("forced failure after the audit insert");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(count("SELECT COUNT(*) FROM THE.coastal_log_sale WHERE coastal_log_sale_id = 910")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = 910")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM THE.coastal_log_sale_audit WHERE coastal_log_sale_id = 910")).isZero();
        assertThat(count("SELECT COUNT(*) FROM THE.coastal_log_sale_detail_audit WHERE coastal_log_sale_id = 910")).isZero();
    }

    @Test
    void lineDelete_auditsOnlyThatLine() {
        transaction.executeWithoutResult(status -> lineItemRepo.auditAndDeleteLineItem(9101L, DELETING_USER));

        assertThat(jdbc.queryForList("SELECT coastal_log_sale_detail_id FROM THE.coastal_log_sale_detail "
                + "WHERE coastal_log_sale_id = 910", new MapSqlParameterSource(), Long.class)).containsExactly(9102L);
        Map<String, Object> line = jdbc.queryForMap("SELECT * FROM THE.coastal_log_sale_detail_audit "
                + "WHERE coastal_log_sale_id = 910", new MapSqlParameterSource());
        assertThat(line.get("CSP_AUDIT_ACTION_TYPE_CODE")).isEqualTo("D");
        assertThat(((Number) line.get("COASTAL_LOG_SALE_DETAIL_ID")).longValue()).isEqualTo(9101L);
        assertThat(((Number) line.get("REVISION_COUNT")).intValue()).isEqualTo(2);
        assertThat(line.get("ENTRY_USERID")).isEqualTo("IDIR\\CREATOR");
        assertThat(line.get("UPDATE_USERID")).isEqualTo(DELETING_USER);
    }

    private static void deleteInvoiceWithAudit() {
        invoiceRepo.insertDeleteAudit(INVOICE_ID, DELETING_USER);
        lineItemRepo.auditAndDeleteAllByInvoiceId(INVOICE_ID, DELETING_USER);
        invoiceRepo.deleteInvoice(INVOICE_ID);
    }

    private static int count(String sql) {
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
        return count == null ? 0 : count;
    }

    private static void execute(String sql) {
        jdbc.update(sql, new MapSqlParameterSource());
    }
}
