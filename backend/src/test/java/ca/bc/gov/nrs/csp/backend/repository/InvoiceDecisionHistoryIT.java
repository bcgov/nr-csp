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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the approve/unapprove columns and the submission-close SQL against real Oracle, in the order InvoiceService uses them. */
class InvoiceDecisionHistoryIT {

    private static final long SUBMISSION_ID = 920L;
    private static final long FIRST = 921L;
    private static final long SECOND = 922L;

    private static NamedParameterJdbcTemplate jdbc;
    private static InvoiceRepository invoiceRepo;
    private static CspSubmissionRepository submissionRepo;
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
        submissionRepo = new CspSubmissionRepository(jdbc);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void seedUploadWithTwoProcessingInvoices() {
        execute("DELETE FROM THE.coastal_log_sale WHERE csp_submission_id = 920");
        execute("DELETE FROM THE.csp_submission WHERE csp_submission_id = 920");
        execute("INSERT INTO THE.csp_submission (csp_submission_id, csp_submission_status_code, revision_count, update_timestamp)"
                + " VALUES (920, 'INB', 0, SYSDATE)");
        seedInvoice(FIRST, "PRO");
        seedInvoice(SECOND, "PRO");
    }

    @Test
    void approveUnapproveReapprove_keepsEachDecisionWithItsOwnStaff() {
        invoiceRepo.updateStatus(FIRST, "APP", "IDIR\\APPROVER1");
        invoiceRepo.updateStatus(FIRST, "UNA", "IDIR\\UNAPPROVER");
        Map<String, Object> afterUnapprove = decisionColumns(FIRST);
        invoiceRepo.updateStatus(FIRST, "APP", "IDIR\\APPROVER2");

        Map<String, Object> row = decisionColumns(FIRST);
        assertThat(afterUnapprove.get("APPROVED_USERID")).isEqualTo("IDIR\\APPROVER1");
        assertThat(row.get("APPROVED_USERID")).isEqualTo("IDIR\\APPROVER2");
        assertThat(row.get("UNAPPROVED_USERID")).isEqualTo("IDIR\\UNAPPROVER");
        assertThat(row.get("APPROVED_DATE")).isNotNull();
        assertThat(row.get("UNAPPROVED_DATE")).isEqualTo(afterUnapprove.get("UNAPPROVED_DATE"));
        assertThat(row.get("UPDATE_USERID")).isEqualTo("IDIR\\APPROVER2");
    }

    @Test
    void rejectAndCancel_leaveDecisionColumnsEmpty() {
        invoiceRepo.updateStatus(FIRST, "REJ", "IDIR\\REVIEWER");
        invoiceRepo.updateStatus(SECOND, "CAN", "IDIR\\REVIEWER");

        assertThat(decisionColumns(FIRST)).containsEntry("APPROVED_USERID", null).containsEntry("UNAPPROVED_USERID", null);
        assertThat(decisionColumns(SECOND)).containsEntry("APPROVED_USERID", null).containsEntry("UNAPPROVED_USERID", null);
    }

    @Test
    void undecidedCount_includesDraftProcessingAndUnapprovedSiblings() {
        seedInvoice(923L, "DFT");
        seedInvoice(924L, "UNA");
        seedInvoice(925L, "APP");
        seedInvoice(926L, "CAN");
        seedInvoice(927L, "REJ");

        assertThat(invoiceRepo.countUndecidedByCspSubmissionId(SUBMISSION_ID)).isEqualTo(4);
    }

    @Test
    void concurrentLastDecisions_theSecondToLockSeesTheFirstAndClosesTheSubmission() throws Exception {
        CountDownLatch firstHoldsLock = new CountDownLatch(1);
        CountDownLatch secondDecided = new CountDownLatch(1);
        // Two threads of its own: the common pool can be a single thread on a small runner.
        ExecutorService pool = Executors.newFixedThreadPool(2);

        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> transaction.execute(status -> {
            invoiceRepo.updateStatus(FIRST, "APP", "IDIR\\APPROVER1");
            submissionRepo.lockForUpdate(SUBMISSION_ID);
            firstHoldsLock.countDown();
            await(secondDecided);
            return invoiceRepo.countUndecidedByCspSubmissionId(SUBMISSION_ID);
        }), pool);
        CompletableFuture<Integer> second = CompletableFuture.supplyAsync(() -> transaction.execute(status -> {
            await(firstHoldsLock);
            invoiceRepo.updateStatus(SECOND, "REJ", "IDIR\\APPROVER2");
            secondDecided.countDown();
            // Waits here until the first commits.
            submissionRepo.lockForUpdate(SUBMISSION_ID);
            return invoiceRepo.countUndecidedByCspSubmissionId(SUBMISSION_ID);
        }), pool);

        // The first can't see the second's uncommitted decision, so it leaves the submission open.
        assertThat(first.get(30, TimeUnit.SECONDS)).isEqualTo(1);
        // The second counts only after the first commits, so it is the one that closes the submission.
        assertThat(second.get(30, TimeUnit.SECONDS)).isZero();
        pool.shutdown();
    }

    private static void seedInvoice(long id, String status) {
        jdbc.update("INSERT INTO THE.coastal_log_sale (coastal_log_sale_id, csp_submission_id, log_sale_entry_status_code,"
                        + " revision_count, update_timestamp) VALUES (:id, 920, :status, 0, SYSDATE)",
                new MapSqlParameterSource().addValue("id", id).addValue("status", status));
    }

    private static Map<String, Object> decisionColumns(long id) {
        return jdbc.queryForMap("SELECT approved_userid, approved_date, unapproved_userid, unapproved_date, update_userid"
                + " FROM THE.coastal_log_sale WHERE coastal_log_sale_id = :id", new MapSqlParameterSource("id", id));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the other transaction");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void execute(String sql) {
        jdbc.update(sql, new MapSqlParameterSource());
    }
}
