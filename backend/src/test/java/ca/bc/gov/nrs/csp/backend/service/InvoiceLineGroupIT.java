package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItemGroupUpdateRequest;
import ca.bc.gov.nrs.csp.backend.exception.ConflictException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.invoice.manual.InvoiceValidator;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository.LoadedInvoice;
import ca.bc.gov.nrs.csp.backend.repository.LineItemRepository;
import ca.bc.gov.nrs.csp.backend.repository.LogSaleParticipantRepository;
import ca.bc.gov.nrs.csp.backend.service.mapper.InvoiceMapper;
import ca.bc.gov.nrs.csp.backend.testsupport.OracleTestContainer;
import ca.bc.gov.nrs.csp.backend.testsupport.RequiresOracle;
import ca.bc.gov.nrs.csp.backend.util.constants.Roles;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Runs the group edit and delete against real Oracle, each in one transaction as @Transactional does.
 * A refused group must leave every line, the header totals and the delete audit as they were.
 */
class InvoiceLineGroupIT {

    private static final long INVOICE_ID = 920L;
    private static final String USER = "IDIR\\GROUPER";
    private static final ValidationResult VALID = new ValidationResult(List.of());

    private static NamedParameterJdbcTemplate jdbc;
    private static TransactionTemplate transaction;

    private LineItemRepository lineItemRepo;
    private InvoiceService service;
    private InvoiceValidator validator;

    @BeforeAll
    static void startOracle() {
        RequiresOracle.assumeDockerAvailable();
        OracleTestContainer.start();
        OracleTestContainer.bootstrap();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                OracleTestContainer.getJdbcUrl(), OracleTestContainer.getUsername(), OracleTestContainer.getPassword());
        jdbc = new NamedParameterJdbcTemplate(dataSource);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void seedManualDraftWithThreeLines() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                USER, null, AuthorityUtils.createAuthorityList(Roles.APPROVE)));
        execute("DELETE FROM THE.coastal_log_sale_detail_audit WHERE coastal_log_sale_id = 920");
        execute("DELETE FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = 920");
        execute("DELETE FROM THE.coastal_log_sale WHERE coastal_log_sale_id = 920");
        execute("""
                INSERT INTO THE.coastal_log_sale (
                    coastal_log_sale_id, csp_submission_id, client_invoice_no, client_invoice_date,
                    log_sale_entry_status_code, csp_invoice_type_code, revision_count,
                    client_total_invoice_pieces, client_total_log_pieces, client_total_invoice_volume,
                    client_total_invoice_amt, update_userid, update_timestamp
                ) VALUES (
                    920, 77, 'TEST-920', DATE '2026-11-01', 'DFT', 'SAL', 4,
                    35, 35, 13.75, 1140, 'IDIR\\EDITOR', DATE '2026-11-03'
                )
                """);
        insertLine(9201, 10, "5.5", "100", "01", 1);
        insertLine(9202, 20, "7.25", "80", "01", 1);
        insertLine(9203, 5, "1", "10", "02", 2);

        InvoiceRepository invoiceRepo = spy(new InvoiceRepository(jdbc));
        // The header read joins tables the bootstrap leaves out; the lines and the writes are real.
        doReturn(Optional.of(loadedManualDraft())).when(invoiceRepo).findById(INVOICE_ID);
        lineItemRepo = spy(new LineItemRepository(jdbc));
        service = spy(new InvoiceService(invoiceRepo, lineItemRepo, mock(CspSubmissionRepository.class),
                mock(LogSaleParticipantRepository.class), mock(CommonValidation.class),
                mock(PriceConversionService.class), mock(InvoiceMapper.class), Clock.systemUTC()));
        validator = mock(InvoiceValidator.class);
        doReturn(validator).when(service).newValidator();
        when(validator.validate(any(), any(), anyBoolean(), any())).thenReturn(VALID);
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void groupEdit_movesEveryLineInOneGo() {
        transaction.executeWithoutResult(status -> service.updateLineItemGroup(INVOICE_ID, currentVersion(),
                new LineItemGroupUpdateRequest(List.of(9201L, 9202L), "03", "BAL")));

        assertThat(jdbc.queryForList("SELECT log_sale_sort_code || '/' || csp_species_grade_xref_id "
                        + "FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = 920 "
                        + "ORDER BY coastal_log_sale_detail_id", new MapSqlParameterSource(), String.class))
                .containsExactly("03/3", "03/3", "02/2");
    }

    @Test
    void groupEdit_thatFailsValidation_changesNothing() {
        when(validator.validate(any(), any(), anyBoolean(), any())).thenReturn(new ValidationResult(List.of(
                new ValidationMessage("invoice.species.grade.invalid.error", null, MessageType.ERROR))));
        Map<String, Object> before = invoiceState();

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> service.updateLineItemGroup(
                INVOICE_ID, currentVersion(), new LineItemGroupUpdateRequest(List.of(9201L, 9202L), "03", "BAL"))))
                .isInstanceOf(ValidationException.class);

        assertThat(invoiceState()).isEqualTo(before);
    }

    @Test
    void groupEditAndDelete_fromAStaleVersion_changeNothing() {
        Map<String, Object> before = invoiceState();

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> service.updateLineItemGroup(
                INVOICE_ID, "stale", new LineItemGroupUpdateRequest(List.of(9201L, 9202L), "03", "BAL"))))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status ->
                service.deleteLineItemGroup(INVOICE_ID, "stale", List.of(9201L, 9202L))))
                .isInstanceOf(ConflictException.class);

        assertThat(invoiceState()).isEqualTo(before);
    }

    @Test
    void groupDelete_failingOnItsSecondLine_rollsBackTheFirst() {
        doAnswer(invocation -> {
            if (invocation.getArgument(0).equals(9202L)) {
                throw new IllegalStateException("forced failure on the second line");
            }
            return invocation.callRealMethod();
        }).when(lineItemRepo).auditAndDeleteLineItem(anyLong(), anyString());
        Map<String, Object> before = invoiceState();

        assertThatThrownBy(() -> transaction.executeWithoutResult(status ->
                service.deleteLineItemGroup(INVOICE_ID, currentVersion(), List.of(9201L, 9202L))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(invoiceState()).isEqualTo(before);
    }

    @Test
    void groupDelete_auditsEveryLineAndRecalculatesTheTotals() {
        transaction.executeWithoutResult(status ->
                service.deleteLineItemGroup(INVOICE_ID, currentVersion(), List.of(9201L, 9202L)));

        assertThat(jdbc.queryForList("SELECT coastal_log_sale_detail_id FROM THE.coastal_log_sale_detail "
                + "WHERE coastal_log_sale_id = 920", new MapSqlParameterSource(), Long.class)).containsExactly(9203L);
        assertThat(jdbc.queryForList("SELECT coastal_log_sale_detail_id FROM THE.coastal_log_sale_detail_audit "
                        + "WHERE coastal_log_sale_id = 920 AND csp_audit_action_type_code = 'D' "
                        + "ORDER BY coastal_log_sale_detail_id", new MapSqlParameterSource(), Long.class))
                .containsExactly(9201L, 9202L);
        Map<String, Object> header = jdbc.queryForMap("SELECT client_total_invoice_pieces, client_total_invoice_amt "
                + "FROM THE.coastal_log_sale WHERE coastal_log_sale_id = 920", new MapSqlParameterSource());
        assertThat(((Number) header.get("CLIENT_TOTAL_INVOICE_PIECES")).intValue()).isEqualTo(5);
        assertThat(new BigDecimal(header.get("CLIENT_TOTAL_INVOICE_AMT").toString())).isEqualByComparingTo("10");
    }

    // Every line, the header totals and revision, and the delete audit: what a refused group must not touch.
    private static Map<String, Object> invoiceState() {
        return Map.of(
                "lines", jdbc.queryForList("SELECT coastal_log_sale_detail_id || ':' || log_sale_sort_code || ':' "
                                + "|| csp_species_grade_xref_id || ':' || pieces || ':' || revision_count "
                                + "FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = 920 "
                                + "ORDER BY coastal_log_sale_detail_id", new MapSqlParameterSource(), String.class),
                "header", jdbc.queryForList("SELECT revision_count || ':' || client_total_invoice_pieces || ':' "
                                + "|| client_total_invoice_volume || ':' || client_total_invoice_amt || ':' "
                                + "|| TO_CHAR(update_timestamp, 'YYYYMMDDHH24MISS') "
                                + "FROM THE.coastal_log_sale WHERE coastal_log_sale_id = 920",
                        new MapSqlParameterSource(), String.class),
                "audit", jdbc.queryForObject("SELECT COUNT(*) FROM THE.coastal_log_sale_detail_audit "
                        + "WHERE coastal_log_sale_id = 920", new MapSqlParameterSource(), Integer.class));
    }

    private static String currentVersion() {
        return new InvoiceRepository(jdbc).findVersion(INVOICE_ID);
    }

    private static LoadedInvoice loadedManualDraft() {
        InvoiceDetails details = new InvoiceDetails(
                INVOICE_ID, "TEST-920", LocalDate.of(2026, 11, 1), "DFT", "SAL", "O", "V", null, null,
                new BigDecimal("1140"), 35, new BigDecimal("13.75"),
                "00001234", "00", "Seller", "00001234", "00", "00005678", "00", null, null, null,
                List.of("B1"), List.of(), List.of(), null, null, null, null, "IDIR\\CREATOR");
        return new LoadedInvoice(details, 77L, null, null, null, null);
    }

    private static void insertLine(long id, int pieces, String volume, String price, String sort, int xrefId) {
        jdbc.update("""
                INSERT INTO THE.coastal_log_sale_detail (
                    coastal_log_sale_detail_id, coastal_log_sale_id, revision_count, log_sale_sort_code,
                    csp_species_grade_xref_id, pieces, volume, price, entry_userid, entry_timestamp,
                    update_userid, update_timestamp
                ) VALUES (:id, 920, 0, :sort, :xrefId, :pieces, :volume, :price, 'IDIR\\CREATOR',
                    DATE '2026-11-02', 'IDIR\\EDITOR', DATE '2026-11-03')
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("sort", sort)
                .addValue("xrefId", xrefId)
                .addValue("pieces", pieces)
                .addValue("volume", new BigDecimal(volume))
                .addValue("price", new BigDecimal(price)));
    }

    private static void execute(String sql) {
        jdbc.update(sql, new MapSqlParameterSource());
    }
}
