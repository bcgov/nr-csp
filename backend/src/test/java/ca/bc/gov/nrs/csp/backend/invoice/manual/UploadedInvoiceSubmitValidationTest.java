package ca.bc.gov.nrs.csp.backend.invoice.manual;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository.RelatedInvoice;
import ca.bc.gov.nrs.csp.backend.util.constants.ActionType;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Staff Submit validates a loaded invoice with manual=true; legacy validated ESF invoices with
 * manual=false. Loads each upload shape through the real row mapping and checks Submit does not block it.
 */
@ExtendWith(MockitoExtension.class)
class UploadedInvoiceSubmitValidationTest {

    private static final String SELLER = "00001111";
    private static final String BUYER = "00002222";
    private static final String LOCATION = "00";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Vancouver");
    private static final Clock CLOCK =
            Clock.fixed(LocalDate.of(2026, 11, 25).atStartOfDay(BUSINESS_ZONE).toInstant(), BUSINESS_ZONE);

    @Mock NamedParameterJdbcTemplate jdbc;
    @Mock InvoiceRepository lookups;
    @Mock CommonValidation commonValidation;

    private InvoiceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InvoiceValidator(lookups, commonValidation, CLOCK);
        lenient().when(commonValidation.isValidInvoiceType(any(), any())).thenReturn(true);
        lenient().when(commonValidation.isValidMaturity(any(), any())).thenReturn(true);
        lenient().when(commonValidation.isValidSortCode(any(), any())).thenReturn(true);
        lenient().when(commonValidation.isValidSpeciesGradeCombination(any(), any())).thenReturn(true);
        lenient().when(commonValidation.isValidClientLocation(any(), any())).thenReturn(true);
        lenient().when(lookups.findByClientInvoiceNo(any())).thenReturn(List.of());
        lenient().when(lookups.findByInvoiceNoAndClient(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void sellerUpload_registeredBuyer_passesSubmit() {
        InvoiceDetails loaded = load(new Row(SELLER, ConstantsCode.INVTYPE_SALE)
                .seller(SELLER).buyer(BUYER));

        assertThat(loaded.submittedBy()).isEqualTo(ConstantsCode.INVOICE_SUBMITTEDBY_SELLER);
        assertSubmitMatchesIntake(loaded);
    }

    @Test
    void buyerUpload_registeredSeller_passesSubmit() {
        InvoiceDetails loaded = load(new Row(BUYER, ConstantsCode.INVTYPE_PURCHASE)
                .seller(SELLER).buyer(BUYER));

        assertThat(loaded.submittedBy()).isEqualTo(ConstantsCode.INVOICE_SUBMITTEDBY_BUYER);
        assertSubmitMatchesIntake(loaded);
    }

    @Test
    void sellerUpload_freeTextBuyer_passesSubmit() {
        InvoiceDetails loaded = load(new Row(SELLER, ConstantsCode.INVTYPE_SALE)
                .seller(SELLER).freeTextBuyer());

        assertThat(loaded.otherClientNum()).isNull();
        assertThat(loaded.otherClientName()).isEqualTo("TEST MILL");
        assertSubmitMatchesIntake(loaded);
    }

    @Test
    void buyerUpload_freeTextSeller_passesSubmit() {
        InvoiceDetails loaded = load(new Row(BUYER, ConstantsCode.INVTYPE_PURCHASE)
                .buyer(BUYER).freeTextSeller());

        assertThat(loaded.submittedBy()).isEqualTo(ConstantsCode.INVOICE_SUBMITTEDBY_BUYER);
        assertThat(loaded.otherClientName()).isEqualTo("TEST MILL");
        assertSubmitMatchesIntake(loaded);
    }

    @Test
    void sellerReplacementOfABuyerSubmittedOriginal_passesSubmit() {
        // The lookup answers for the seller because the seller is a party to the original.
        given(lookups.findByInvoiceNoAndClient("TEST-ORIG", SELLER, LOCATION))
                .willReturn(List.of(new RelatedInvoice(920L, ConstantsCode.INVENTRYSTATUS_APPROVED)));
        InvoiceDetails loaded = load(new Row(SELLER, ConstantsCode.INVTYPE_SALE)
                .seller(SELLER).buyer(BUYER).replaces("TEST-ORIG"));

        assertThat(loaded.replaceInvNum()).isEqualTo("TEST-ORIG");
        assertSubmitMatchesIntake(loaded);
    }

    private void assertSubmitMatchesIntake(InvoiceDetails loaded) {
        List<LineItem> lines = List.of(new LineItem(null, null, "01", null, "FI", null, "J", 10,
                new BigDecimal("100.00"), new BigDecimal("5.000"), null, null));

        ValidationResult submit = validator.validate(loaded, lines, true, ActionType.SUBMIT);
        List<String> submitErrors = errorKeys(submit);
        ValidationResult intake = validator.validate(loaded, lines, false, ActionType.SUBMIT);

        assertThat(submitErrors).isEmpty();
        assertThat(errorKeys(intake)).isEmpty();
    }

    private static List<String> errorKeys(ValidationResult result) {
        return result.messages().stream()
                .filter(m -> m.type() == MessageType.ERROR)
                .map(ValidationMessage::messageKey)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private InvoiceDetails load(Row row) {
        ResultSet rs = row.toResultSet();
        given(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .willAnswer(inv -> List.of(((RowMapper<Object>) inv.getArgument(2)).mapRow(rs, 0)));
        given(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .willAnswer(inv -> {
                    MapSqlParameterSource p = inv.getArgument(1);
                    if (p.hasValue("code")) {
                        return "BOOM".equals(p.getValue("code")) ? List.of("TESTBOOM1") : List.of();
                    }
                    boolean replaces = ConstantsCode.INVRELATETYPE_REPLACE.equals(p.getValue("refTypeCode"));
                    return replaces && row.replaces != null ? List.of(row.replaces) : List.of();
                });
        return new InvoiceRepository(jdbc).findById(10L).orElseThrow().details();
    }

    /** One coastal_log_sale row as CspSubmissionPersistenceService writes an upload. */
    private static final class Row {
        final String submissionClient;
        final String invoiceType;
        String seller;
        String buyer;
        boolean freeTextBuyer;
        boolean freeTextSeller;
        String replaces;

        Row(String submissionClient, String invoiceType) {
            this.submissionClient = submissionClient;
            this.invoiceType = invoiceType;
        }

        Row seller(String client) { this.seller = client; return this; }
        Row buyer(String client) { this.buyer = client; return this; }
        Row freeTextBuyer() { this.freeTextBuyer = true; return this; }
        Row freeTextSeller() { this.freeTextSeller = true; return this; }
        Row replaces(String invoiceNumber) { this.replaces = invoiceNumber; return this; }

        ResultSet toResultSet() {
            ResultSet rs = mock(ResultSet.class);
            try {
                lenient().when(rs.getObject("submission_id", Long.class)).thenReturn(500L);
                lenient().when(rs.getObject("submission_number", Long.class)).thenReturn(9001L);
                lenient().when(rs.getString("submitter_client_num")).thenReturn(submissionClient);
                lenient().when(rs.getString("submitter_location")).thenReturn(LOCATION);
                lenient().when(rs.getString("seller_client_number")).thenReturn(seller);
                lenient().when(rs.getString("seller_client_locn_code")).thenReturn(seller == null ? null : LOCATION);
                lenient().when(rs.getString("buyer_client_number")).thenReturn(buyer);
                lenient().when(rs.getString("buyer_client_locn_code")).thenReturn(buyer == null ? null : LOCATION);
                if (freeTextBuyer) {
                    lenient().when(rs.getString("buyer_participant_name")).thenReturn("TEST MILL");
                    lenient().when(rs.getString("buyer_participant_city")).thenReturn("TEST CITY");
                    lenient().when(rs.getString("buyer_participant_province")).thenReturn("BC");
                }
                if (freeTextSeller) {
                    lenient().when(rs.getString("seller_participant_name")).thenReturn("TEST MILL");
                    lenient().when(rs.getString("seller_participant_city")).thenReturn("TEST CITY");
                    lenient().when(rs.getString("seller_participant_province")).thenReturn("BC");
                }
                lenient().when(rs.getObject("inv_id", Long.class)).thenReturn(10L);
                lenient().when(rs.getString("inv_number")).thenReturn("TEST-INV1");
                lenient().when(rs.getDate("invoice_date")).thenReturn(Date.valueOf(LocalDate.of(2026, 11, 1)));
                lenient().when(rs.getString("inv_status")).thenReturn(ConstantsCode.INVENTRYSTATUS_DRAFT);
                lenient().when(rs.getString("inv_type")).thenReturn(invoiceType);
                lenient().when(rs.getString("maturity")).thenReturn("O");
                lenient().when(rs.getString("fob_code")).thenReturn("TEST");
                lenient().when(rs.getString("primary_sort_code")).thenReturn("01");
                lenient().when(rs.getBigDecimal("total_amt")).thenReturn(new BigDecimal("500.00"));
                lenient().when(rs.getObject("total_pieces", Integer.class)).thenReturn(10);
                lenient().when(rs.getBigDecimal("total_vol")).thenReturn(new BigDecimal("5.000"));
                lenient().when(rs.getString("entry_userid")).thenReturn("TEST-UPLOADER");
                lenient().when(rs.getString("invoice_version")).thenReturn("1");
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException(e);
            }
            return rs;
        }
    }
}
