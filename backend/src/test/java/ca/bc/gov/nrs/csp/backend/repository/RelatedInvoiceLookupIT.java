package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository.RelatedInvoice;
import ca.bc.gov.nrs.csp.backend.testsupport.OracleTestContainer;
import ca.bc.gov.nrs.csp.backend.testsupport.RequiresOracle;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the replace/adjust original lookup and link insert against real Oracle. */
class RelatedInvoiceLookupIT {

    private static final String INVOICE_NUMBER = "TEST-ORIG";
    private static final String SELLER = "00009001";
    private static final String BUYER = "00009002";
    private static final String UNRELATED = "00009003";
    private static final String LOCATION = "00";
    private static final long ORIGINAL_ID = 920L;
    private static final long LATER_ID = 921L;
    private static final long REPLACEMENT_ID = 930L;
    private static final long ADJUSTMENT_ID = 931L;

    private static NamedParameterJdbcTemplate jdbc;
    private static InvoiceRepository invoiceRepo;

    @BeforeAll
    static void startOracle() {
        RequiresOracle.assumeDockerAvailable();
        OracleTestContainer.start();
        OracleTestContainer.bootstrap();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                OracleTestContainer.getJdbcUrl(), OracleTestContainer.getUsername(), OracleTestContainer.getPassword());
        jdbc = new NamedParameterJdbcTemplate(dataSource);
        invoiceRepo = new InvoiceRepository(jdbc);
    }

    @BeforeEach
    void seedOriginalSubmittedByTheBuyer() {
        execute("DELETE FROM THE.coastal_log_sale_rltd_invc WHERE coastal_log_sale_id IN (930, 931)");
        execute("DELETE FROM THE.coastal_log_sale WHERE coastal_log_sale_id BETWEEN 920 AND 931");
        // The buyer submitted it, so its submission client is the buyer; the seller only appears as seller.
        insertInvoice(ORIGINAL_ID, INVOICE_NUMBER, SELLER, BUYER, ConstantsCode.INVENTRYSTATUS_APPROVED);
        insertInvoice(REPLACEMENT_ID, "TEST-REPL", SELLER, BUYER, ConstantsCode.INVENTRYSTATUS_DRAFT);
        insertInvoice(ADJUSTMENT_ID, "TEST-ADJ", SELLER, BUYER, ConstantsCode.INVENTRYSTATUS_DRAFT);
    }

    @Test
    void lookup_findsTheOriginalForEitherParty() {
        assertThat(ids(invoiceRepo.findByInvoiceNoAndClient(INVOICE_NUMBER, SELLER, LOCATION)))
                .containsExactly(ORIGINAL_ID);
        assertThat(ids(invoiceRepo.findByInvoiceNoAndClient(INVOICE_NUMBER, BUYER, LOCATION)))
                .containsExactly(ORIGINAL_ID);
    }

    @Test
    void lookup_refusesAClientThatIsNotAParty() {
        assertThat(invoiceRepo.findByInvoiceNoAndClient(INVOICE_NUMBER, UNRELATED, LOCATION)).isEmpty();
        assertThat(invoiceRepo.findByInvoiceNoAndClient(INVOICE_NUMBER, SELLER, "01")).isEmpty();
    }

    @Test
    void lookup_returnsEveryMatchOldestFirstWithItsStatus() {
        insertInvoice(LATER_ID, INVOICE_NUMBER, SELLER, UNRELATED, ConstantsCode.INVENTRYSTATUS_CANCELLED);

        List<RelatedInvoice> matches = invoiceRepo.findByInvoiceNoAndClient(INVOICE_NUMBER, SELLER, LOCATION);

        assertThat(ids(matches)).containsExactly(ORIGINAL_ID, LATER_ID);
        assertThat(matches.get(1).invoiceStatusCode()).isEqualTo(ConstantsCode.INVENTRYSTATUS_CANCELLED);
        assertThat(ids(invoiceRepo.findByInvoiceNoAndClient(INVOICE_NUMBER, BUYER, LOCATION)))
                .containsExactly(ORIGINAL_ID);
    }

    @Test
    void sellerReplacementAndBuyerAdjustment_linkToTheExactOriginal() {
        assertThat(invoiceRepo.replaceRelatedInvoices(REPLACEMENT_ID, ConstantsCode.INVRELATETYPE_REPLACE,
                INVOICE_NUMBER, SELLER, LOCATION, "TEST-USER")).containsExactly(ORIGINAL_ID);
        assertThat(invoiceRepo.replaceRelatedInvoices(ADJUSTMENT_ID, ConstantsCode.INVRELATETYPE_ADJUST,
                INVOICE_NUMBER, BUYER, LOCATION, "TEST-USER")).containsExactly(ORIGINAL_ID);

        assertThat(linkedOriginal(REPLACEMENT_ID, ConstantsCode.INVRELATETYPE_REPLACE)).containsExactly(ORIGINAL_ID);
        assertThat(linkedOriginal(ADJUSTMENT_ID, ConstantsCode.INVRELATETYPE_ADJUST)).containsExactly(ORIGINAL_ID);
    }

    @Test
    void unrelatedClient_linksNothing() {
        assertThat(invoiceRepo.replaceRelatedInvoices(REPLACEMENT_ID, ConstantsCode.INVRELATETYPE_REPLACE,
                INVOICE_NUMBER, UNRELATED, LOCATION, "TEST-USER")).isEmpty();

        assertThat(linkedOriginal(REPLACEMENT_ID, ConstantsCode.INVRELATETYPE_REPLACE)).isEmpty();
    }

    private static List<Long> ids(List<RelatedInvoice> invoices) {
        return invoices.stream().map(RelatedInvoice::coastalLogSaleId).toList();
    }

    private static List<Long> linkedOriginal(long invoiceId, String refTypeCode) {
        return jdbc.queryForList("SELECT related_coastal_log_sale_id FROM THE.coastal_log_sale_rltd_invc "
                        + "WHERE coastal_log_sale_id = :id AND csp_invoice_ref_type_code = :type",
                new MapSqlParameterSource().addValue("id", invoiceId).addValue("type", refTypeCode), Long.class);
    }

    private static void insertInvoice(long id, String invoiceNumber, String seller, String buyer, String status) {
        jdbc.update("""
                INSERT INTO THE.coastal_log_sale (
                    coastal_log_sale_id, csp_submission_id, client_invoice_no, client_invoice_date,
                    log_sale_entry_status_code, csp_invoice_type_code, revision_count,
                    seller_client_number, seller_client_locn_code, buyer_client_number, buyer_client_locn_code,
                    entry_userid, entry_timestamp, update_userid, update_timestamp
                ) VALUES (
                    :id, 77, :invoiceNumber, DATE '2026-11-01', :status, 'PUR', 0,
                    :seller, '00', :buyer, '00',
                    'TEST-USER', SYSDATE, 'TEST-USER', SYSDATE
                )
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("invoiceNumber", invoiceNumber)
                .addValue("status", status)
                .addValue("seller", seller)
                .addValue("buyer", buyer));
    }

    private static void execute(String sql) {
        jdbc.update(sql, new MapSqlParameterSource());
    }
}
