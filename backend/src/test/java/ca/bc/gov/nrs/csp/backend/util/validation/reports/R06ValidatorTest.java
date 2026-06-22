package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R06ReportRequest;
import ca.bc.gov.nrs.csp.backend.service.SearchService;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class R06ValidatorTest {

    @Mock SearchService searchService;

    R06Validator validator;

    @BeforeEach
    void setUp() {
        validator = new R06Validator(searchService);
        lenient().when(searchService.findClientsByNumber(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(client("00001234")));
    }

    @Test
    void validate_withInvoiceNumbers_noDateRequired() {
        R06ReportRequest r = new R06ReportRequest();
        r.setInvoiceNumbers("INV-001");

        ValidationResult result = validator.validate(r);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_noInvoiceNumbers_missingDateFrom_addsError() {
        R06ReportRequest r = new R06ReportRequest();
        r.setDateTo("20240630");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.r06.startdate.required.error");
    }

    @Test
    void validate_noInvoiceNumbers_missingDateTo_addsError() {
        R06ReportRequest r = new R06ReportRequest();
        r.setDateFrom("20240101");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.r06.enddate.required.error");
    }

    @Test
    void validate_noInvoiceNumbers_bothDatesPresent_noRequiredErrors() {
        R06ReportRequest r = new R06ReportRequest();
        r.setDateFrom("20240101");
        r.setDateTo("20240630");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> m.messageKey().equals("report.r06.startdate.required.error"));
        assertThat(result.errors()).noneMatch(m -> m.messageKey().equals("report.r06.enddate.required.error"));
    }

    @Test
    void validate_dateRangeOutOfOrder_addsError() {
        R06ReportRequest r = new R06ReportRequest();
        r.setDateFrom("20240630");
        r.setDateTo("20240101");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.daterange.order.error");
    }

    @Test
    void validate_invoiceNumberTooLong_addsError() {
        R06ReportRequest r = new R06ReportRequest();
        r.setInvoiceNumbers("A".repeat(16));

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.r06.invoicenumber.length.error");
    }

    @Test
    void validate_invoiceNumberExactlyMaxLength_noError() {
        R06ReportRequest r = new R06ReportRequest();
        r.setInvoiceNumbers("A".repeat(15));

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> m.messageKey().equals("report.r06.invoicenumber.length.error"));
    }

    @Test
    void validate_sellerClientNotFound_addsError() {
        given(searchService.findClientsByNumber("UNKNOWN")).willReturn(List.of());
        R06ReportRequest r = new R06ReportRequest();
        r.setInvoiceNumbers("INV-001");
        r.setSellerClientNumber("UNKNOWN");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.client.number.notfound.error");
    }

    @Test
    void validate_buyerClientNotFound_addsError() {
        given(searchService.findClientsByNumber("UNKNOWN")).willReturn(List.of());
        R06ReportRequest r = new R06ReportRequest();
        r.setInvoiceNumbers("INV-001");
        r.setBuyerClientNumber("UNKNOWN");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.client.number.notfound.error");
    }

    @Test
    void validate_validRequest_noErrors() {
        R06ReportRequest r = new R06ReportRequest();
        r.setDateFrom("20240101");
        r.setDateTo("20240630");

        ValidationResult result = validator.validate(r);

        assertThat(result.hasErrors()).isFalse();
    }

    private void assertHasError(ValidationResult r, String key) {
        assertThat(r.errors().stream().map(m -> m.messageKey()).toList())
                .as("expected error key '%s'", key)
                .contains(key);
    }

    private ClientLocation client(String number) {
        return new ClientLocation(number, "Test Client", "01", "Main", "Victoria", "BC");
    }
}
