package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.CreateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItemRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItemResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.UpdateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationMessageResponse;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InvoiceMapperTest {

    private static final LocalDate INVOICE_DATE = LocalDate.of(2026, Month.MAY, 19);

    @Mock MessageSource messageSource;

    InvoiceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new InvoiceMapper(messageSource);
    }

    // ---------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------

    private static CreateInvoiceRequest createRequest(List<String> booms, List<String> marks, List<String> slips) {
        return new CreateInvoiceRequest(
                "INV-2026-001",             // invNumber
                INVOICE_DATE,               // invoiceDate
                "SAL",                      // invType
                "M",                        // maturity
                "FOB01",                    // fobCode
                "A",                        // primarySortCode
                new BigDecimal("1250.75"),  // totalAmt
                100,                        // totalPieces
                new BigDecimal("12.5"),     // totalVol
                "00001234",                 // submitterClientNum
                "00",                       // submitterLocation
                "Seller",                   // submittedBy
                "00001234",                 // clientNumber
                "01",                       // clientLocation
                "00005678",                 // otherClientNum
                "02",                       // otherClientLocation
                "ABC Logging Ltd.",         // otherClientName
                "Nanaimo",                  // otherClientCity
                "BC",                       // otherClientProvState
                booms,                      // boomNumbers
                marks,                      // timberMarks
                slips,                      // weightSlips
                "INV-OLD-1",                // replaceInvNum
                "INV-OLD-2",                // adjustInvNum
                "please review",            // reviewComments
                "submitted via UI",         // submitComments
                true,                       // manual
                List.of()                   // lineItems (not consumed by toDetails)
        );
    }

    private static UpdateInvoiceRequest updateRequest(List<String> booms, List<String> marks, List<String> slips) {
        return new UpdateInvoiceRequest(
                "INV-2026-002",             // invNumber
                INVOICE_DATE,               // invoiceDate
                "TRD",                      // invType
                "I",                        // maturity
                "FOB02",                    // fobCode
                "B",                        // primarySortCode
                new BigDecimal("999.99"),   // totalAmt
                50,                         // totalPieces
                new BigDecimal("7.25"),     // totalVol
                "00009999",                 // submitterClientNum
                "03",                       // submitterLocation
                "Buyer",                    // submittedBy
                "00008888",                 // clientNumber
                "04",                       // clientLocation
                "00007777",                 // otherClientNum
                "05",                       // otherClientLocation
                "XYZ Timber Inc.",          // otherClientName
                "Victoria",                 // otherClientCity
                "AB",                       // otherClientProvState
                booms,                      // boomNumbers
                marks,                      // timberMarks
                slips,                      // weightSlips
                "INV-R-1",                  // replaceInvNum
                "INV-A-1",                  // adjustInvNum
                "updated review",           // reviewComments
                "updated submit",           // submitComments
                false,                      // manual
                List.of()                   // lineItems (not consumed by toDetails)
        );
    }

    private static LineItemRequest lineItemRequest() {
        return new LineItemRequest(
                1L,                         // lineItemID
                "SORT01",                   // secondSort
                "CLIENT-SORT",              // clientSecondarySort
                "SP1",                      // species
                "G1",                       // grade
                50,                         // numOfPieces
                new BigDecimal("25.00"),    // price
                new BigDecimal("6.25"),     // volume
                new BigDecimal("30.00")     // convertedPrice
        );
    }

    private static LineItem lineItem() {
        return new LineItem(
                1L,                         // lineItemID
                12345L,                     // invoiceID
                "SORT01",                   // secondSort
                "CLIENT-SORT",              // clientSecondarySort
                "SP1",                      // species
                "Cedar",                    // speciesDescription
                "G1",                       // grade
                50,                         // numOfPieces
                new BigDecimal("25.00"),    // price
                new BigDecimal("6.25"),     // volume
                new BigDecimal("30.00"),    // convertedPrice
                new BigDecimal("156.25")    // amount
        );
    }

    private static InvoiceDetails invoiceDetails() {
        return new InvoiceDetails(
                12345L,                     // invID
                "INV-2026-001",             // invNumber
                INVOICE_DATE,               // invoiceDate
                "DFT",                      // invStatus
                "SAL",                      // invType
                "M",                        // maturity
                "FOB01",                    // fobCode
                "A",                        // primarySortCode
                new BigDecimal("1250.75"),  // totalAmt
                100,                        // totalPieces
                new BigDecimal("12.5"),     // totalVol
                "00001234",                 // submitterClientNum
                "00",                       // submitterLocation
                "Seller",                   // submittedBy
                "00001234",                 // clientNumber
                "01",                       // clientLocation
                "00005678",                 // otherClientNum
                "02",                       // otherClientLocation
                "ABC Logging Ltd.",         // otherClientName
                "Nanaimo",                  // otherClientCity
                "BC",                       // otherClientProvState
                List.of("B123", "B124"),    // boomNumbers
                List.of("TM1"),             // timberMarks
                List.of("WS1"),             // weightSlips
                "INV-OLD-1",                // replaceInvNum
                "INV-OLD-2",                // adjustInvNum
                "please review",            // reviewComments
                "submitted via UI",         // submitComments
                "user123"                   // entryUserID
        );
    }

    // ---------------------------------------------------------------
    // toDetails(CreateInvoiceRequest, entryUserID)
    // ---------------------------------------------------------------

    @Test
    @SuppressWarnings("java:S5961")
    void toDetails_fromCreateRequest_mapsAllFields() {
        CreateInvoiceRequest req = createRequest(List.of("B123", "B124"), List.of("TM1"), List.of("WS1"));

        InvoiceDetails details = mapper.toDetails(req, "user123");

        assertThat(details)
                .isNotNull()
                .extracting(InvoiceDetails::invNumber, InvoiceDetails::entryUserID)
                .containsExactly("INV-2026-001", "user123");
    }

    @Test
    void toDetails_fromCreateRequest_nullCollections_becomeEmptyLists() {
        InvoiceDetails details = mapper.toDetails(createRequest(null, null, null), "user123");

        assertThat(details.boomNumbers()).isEmpty();
        assertThat(details.timberMarks()).isEmpty();
        assertThat(details.weightSlips()).isEmpty();
    }

    // ---------------------------------------------------------------
    // toDetails(UpdateInvoiceRequest, invID, existingStatus, existingEntryUserID)
    // ---------------------------------------------------------------

    @Test
    @SuppressWarnings("java:S5961")
    void toDetails_fromUpdateRequest_mapsAllFieldsAndCarriesExistingValues() {
        UpdateInvoiceRequest req = updateRequest(List.of("B200"), List.of("TM2", "TM3"), List.of("WS2"));

        InvoiceDetails details = mapper.toDetails(req, 777L, "DFT", "originalUser");

        assertThat(details)
                .isNotNull()
                .extracting(InvoiceDetails::invID, InvoiceDetails::invStatus, InvoiceDetails::entryUserID)
                .containsExactly(777L, "DFT", "originalUser");
    }

    @Test
    void toDetails_fromUpdateRequest_nullCollections_becomeEmptyLists() {
        InvoiceDetails details = mapper.toDetails(updateRequest(null, null, null), 777L, "DFT", "originalUser");

        assertThat(details.boomNumbers()).isEmpty();
        assertThat(details.timberMarks()).isEmpty();
        assertThat(details.weightSlips()).isEmpty();
    }

    // ---------------------------------------------------------------
    // toLineItem() / toLineItems()
    // ---------------------------------------------------------------

    @Test
    void toLineItem_mapsAllFieldsAndComputesAmount() {
        LineItem line = mapper.toLineItem(lineItemRequest(), 12345L);

        assertThat(line).isNotNull();
        assertThat(line.lineItemID()).isEqualTo(1L);
        assertThat(line.invoiceID()).isEqualTo(12345L);
        assertThat(line.secondSort()).isEqualTo("SORT01");
        assertThat(line.clientSecondarySort()).isEqualTo("CLIENT-SORT");
        assertThat(line.species()).isEqualTo("SP1");
        assertThat(line.speciesDescription()).isNull();             // resolved server-side, null inbound
        assertThat(line.grade()).isEqualTo("G1");
        assertThat(line.numOfPieces()).isEqualTo(50);
        assertThat(line.price()).isEqualByComparingTo("25.00");
        assertThat(line.volume()).isEqualByComparingTo("6.25");
        assertThat(line.convertedPrice()).isEqualByComparingTo("30.00");
        assertThat(line.amount()).isEqualByComparingTo("156.25");   // volume x price
    }

    @Test
    void toLineItem_nullRequest_returnsNull() {
        assertThat(mapper.toLineItem(null, 12345L)).isNull();
    }

    @Test
    void toLineItem_nullVolume_yieldsNullAmount() {
        LineItemRequest req = new LineItemRequest(
                null, "SORT01", null, "SP1", "G1", 50, new BigDecimal("25.00"), null, null);

        LineItem line = mapper.toLineItem(req, null);

        assertThat(line.amount()).isNull();
        assertThat(line.invoiceID()).isNull();
        assertThat(line.lineItemID()).isNull();
    }

    @Test
    void toLineItem_nullPrice_yieldsNullAmount() {
        LineItemRequest req = new LineItemRequest(
                2L, "SORT01", null, "SP1", "G1", 50, null, new BigDecimal("6.25"), null);

        assertThat(mapper.toLineItem(req, 1L).amount()).isNull();
    }

    @Test
    void toLineItems_mapsEveryElementWithInvoiceId() {
        List<LineItem> lines = mapper.toLineItems(List.of(lineItemRequest(), lineItemRequest()), 42L);

        assertThat(lines)
                .hasSize(2)
                .allSatisfy(line -> assertThat(line.invoiceID()).isEqualTo(42L))
                .extracting(LineItem::species)
                .containsOnly("SP1");
    }

    @Test
    void toLineItems_nullInput_returnsEmptyList() {
        assertThat(mapper.toLineItems(null, 42L)).isEmpty();
    }

    @Test
    void toLineItems_emptyInput_returnsEmptyList() {
        assertThat(mapper.toLineItems(List.of(), 42L)).isEmpty();
    }

    // ---------------------------------------------------------------
    // toLineItemResponse() / toLineItemResponses()
    // ---------------------------------------------------------------

    @Test
    void toLineItemResponse_mapsAllFields() {
        LineItemResponse response = mapper.toLineItemResponse(lineItem());

        assertThat(response.lineItemID()).isEqualTo(1L);
        assertThat(response.invoiceID()).isEqualTo(12345L);
        assertThat(response.secondSort()).isEqualTo("SORT01");
        assertThat(response.clientSecondarySort()).isEqualTo("CLIENT-SORT");
        assertThat(response.species()).isEqualTo("SP1");
        assertThat(response.speciesDescription()).isEqualTo("Cedar");
        assertThat(response.grade()).isEqualTo("G1");
        assertThat(response.numOfPieces()).isEqualTo(50);
        assertThat(response.price()).isEqualByComparingTo("25.00");
        assertThat(response.volume()).isEqualByComparingTo("6.25");
        assertThat(response.convertedPrice()).isEqualByComparingTo("30.00");
        assertThat(response.amount()).isEqualByComparingTo("156.25");
    }

    @Test
    void toLineItemResponses_mapsEveryElement() {
        List<LineItemResponse> responses = mapper.toLineItemResponses(List.of(lineItem()));

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().speciesDescription()).isEqualTo("Cedar");
    }

    @Test
    void toLineItemResponses_nullInput_returnsEmptyList() {
        assertThat(mapper.toLineItemResponses(null)).isEmpty();
    }

    // ---------------------------------------------------------------
    // toMessageResponse() — message resolution
    // ---------------------------------------------------------------

    @Test
    void toMessageResponse_resolvesTextViaMessageSource() {
        Object[] args = {"1234.50"};
        given(messageSource.getMessage(eq("invoice.totalamount.dismatch.warning"), eq(args), any(Locale.class)))
                .willReturn("Submitted total amount 1234.50 does not match.");

        ValidationMessageResponse response = mapper.toMessageResponse(
                new ValidationMessage("invoice.totalamount.dismatch.warning", args, MessageType.WARNING));

        assertThat(response.messageKey()).isEqualTo("invoice.totalamount.dismatch.warning");
        assertThat(response.args()).isSameAs(args);
        assertThat(response.type()).isEqualTo("WARNING");
        assertThat(response.message()).isEqualTo("Submitted total amount 1234.50 does not match.");
    }

    @Test
    void toMessageResponse_missingBundleKey_fallsBackToKeyItself() {
        given(messageSource.getMessage(eq("missing.key"), any(), any(Locale.class)))
                .willThrow(new NoSuchMessageException("missing.key"));

        ValidationMessageResponse response = mapper.toMessageResponse(
                new ValidationMessage("missing.key", null, MessageType.ERROR));

        assertThat(response.messageKey()).isEqualTo("missing.key");
        assertThat(response.type()).isEqualTo("ERROR");
        assertThat(response.message()).isEqualTo("missing.key");
    }

    @Test
    void toMessageResponse_nullKey_resolvesToEmptyStringWithoutHittingMessageSource() {
        ValidationMessageResponse response = mapper.toMessageResponse(
                new ValidationMessage(null, null, MessageType.ERROR));

        assertThat(response.messageKey()).isNull();
        assertThat(response.message()).isEmpty();
        verifyNoInteractions(messageSource);
    }

    @Test
    void toMessageResponse_blankKey_resolvesToEmptyStringWithoutHittingMessageSource() {
        ValidationMessageResponse response = mapper.toMessageResponse(
                new ValidationMessage("   ", null, MessageType.WARNING));

        assertThat(response.messageKey()).isEqualTo("   ");
        assertThat(response.message()).isEmpty();
        verifyNoInteractions(messageSource);
    }

    // ---------------------------------------------------------------
    // toWarningResponses() / toErrorResponses()
    // ---------------------------------------------------------------

    @Test
    void toWarningResponses_filtersWarningsOnly() {
        given(messageSource.getMessage(any(String.class), any(), any(Locale.class)))
                .willAnswer(inv -> "resolved:" + inv.getArgument(0));
        ValidationResult result = new ValidationResult(List.of(
                new ValidationMessage("warn.one", null, MessageType.WARNING),
                new ValidationMessage("error.one", null, MessageType.ERROR),
                new ValidationMessage("warn.two", null, MessageType.WARNING)
        ));

        List<ValidationMessageResponse> warnings = mapper.toWarningResponses(result);

        assertThat(warnings).extracting(ValidationMessageResponse::messageKey)
                .containsExactly("warn.one", "warn.two");
        assertThat(warnings).extracting(ValidationMessageResponse::type)
                .containsOnly("WARNING");
        assertThat(warnings.getFirst().message()).isEqualTo("resolved:warn.one");
    }

    @Test
    void toWarningResponses_nullResult_returnsEmptyList() {
        assertThat(mapper.toWarningResponses(null)).isEmpty();
    }

    @Test
    void toErrorResponses_filtersErrorsOnly() {
        given(messageSource.getMessage(any(String.class), any(), any(Locale.class)))
                .willAnswer(inv -> "resolved:" + inv.getArgument(0));
        ValidationResult result = new ValidationResult(List.of(
                new ValidationMessage("warn.one", null, MessageType.WARNING),
                new ValidationMessage("error.one", null, MessageType.ERROR)
        ));

        List<ValidationMessageResponse> errors = mapper.toErrorResponses(result);

        assertThat(errors).extracting(ValidationMessageResponse::messageKey)
                .containsExactly("error.one");
        assertThat(errors.getFirst().type()).isEqualTo("ERROR");
        assertThat(errors.getFirst().message()).isEqualTo("resolved:error.one");
    }

    @Test
    void toErrorResponses_nullResult_returnsEmptyList() {
        assertThat(mapper.toErrorResponses(null)).isEmpty();
    }

    // ---------------------------------------------------------------
    // toResponse()
    // ---------------------------------------------------------------

    @Test
    @SuppressWarnings("java:S5961")
    void toResponse_mapsAllFieldsIncludingNestedCollections() {
        given(messageSource.getMessage(any(String.class), any(), any(Locale.class)))
                .willAnswer(inv -> "resolved:" + inv.getArgument(0));
        ValidationResult validation = new ValidationResult(List.of(
                new ValidationMessage("warn.key", null, MessageType.WARNING),
                new ValidationMessage("error.key", null, MessageType.ERROR)
        ));

        InvoiceResponse response = mapper.toResponse(
                invoiceDetails(), 67890L, 55555L, List.of(lineItem()), validation);

        assertThat(response)
                .isNotNull()
                .extracting(InvoiceResponse::invID, InvoiceResponse::submissionId, InvoiceResponse::submissionNumber)
                .containsExactly(12345L, 67890L, 55555L);
        assertThat(response.lineItems()).hasSize(1);
        assertThat(response.warnings()).hasSize(1);
        assertThat(response.errors()).hasSize(1);
    }

    @Test
    void toResponse_nullLineItemsAndNullValidation_yieldEmptyLists() {
        InvoiceResponse response = mapper.toResponse(invoiceDetails(), null, null, null, null);

        assertThat(response.submissionId()).isNull();
        assertThat(response.submissionNumber()).isNull();
        assertThat(response.lineItems()).isEmpty();
        assertThat(response.warnings()).isEmpty();
        assertThat(response.errors()).isEmpty();
    }
}
