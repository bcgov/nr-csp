package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R13ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R13ShowOptions;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class R13ValidatorTest {

    @Mock SearchService searchService;

    R13Validator validator;

    @BeforeEach
    void setUp() {
        validator = new R13Validator(searchService);
        lenient().when(searchService.findClientsByNumber(anyString())).thenReturn(List.of(client("00001234", "Test Client")));
        lenient().when(searchService.findClientsByName(anyString())).thenReturn(List.of(client("00001234", "Test Client")));
    }

    // ── Show options ──────────────────────────────────────────────────────────

    @Test
    void validate_lessThanTwoShowOptions_addsError() {
        R13ShowOptions opts = new R13ShowOptions();
        opts.setShowInvoiceNumber(true); // only 1
        R13ReportRequest r = validRequest();
        r.setShowOptions(opts);

        ValidationResult result = validator.validate(r);

        assertHasError(result, "R13 requires at least 2 output columns to be selected in showOptions");
    }

    @Test
    void validate_twoShowOptions_noShowError() {
        R13ShowOptions opts = new R13ShowOptions();
        opts.setShowInvoiceNumber(true);
        opts.setShowSellerName(true);
        R13ReportRequest r = validRequest();
        r.setShowOptions(opts);

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> m.messageKey().contains("showOptions"));
    }

    @Test
    void validate_nullShowOptions_addsError() {
        R13ReportRequest r = validRequest();
        r.setShowOptions(null);

        ValidationResult result = validator.validate(r);

        assertHasError(result, "R13 requires at least 2 output columns to be selected in showOptions");
    }

    // ── Filter requirement ────────────────────────────────────────────────────

    @Test
    void validate_noFiltersAtAll_addsFilterRequiredError() {
        R13ReportRequest r = new R13ReportRequest();
        r.setShowOptions(twoColumnOptions());

        ValidationResult result = validator.validate(r);

        assertHasError(result, "R13 requires at least one of: invoice date range, submission number/month-year/entry user, or seller/buyer client");
    }

    @Test
    void validate_withInvoiceDateFrom_noFilterRequiredError() {
        R13ReportRequest r = validRequest();

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> m.messageKey().contains("requires at least one of"));
    }

    @Test
    void validate_withSubmissionNumber_noFilterRequiredError() {
        R13ReportRequest r = new R13ReportRequest();
        r.setShowOptions(twoColumnOptions());
        r.setSubmissionNumber("12345");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> m.messageKey().contains("requires at least one of"));
    }

    // ── Date ordering ─────────────────────────────────────────────────────────

    @Test
    void validate_invoiceDateFromAfterDateTo_addsError() {
        R13ReportRequest r = validRequest();
        r.setInvoiceDateFrom("20240630");
        r.setInvoiceDateTo("20240101");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "invoiceDateFrom must not be after invoiceDateTo");
    }

    @Test
    void validate_invoiceDateFromEqualsDateTo_noError() {
        R13ReportRequest r = validRequest();
        r.setInvoiceDateFrom("20240101");
        r.setInvoiceDateTo("20240101");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "invoiceDateFrom must not be after invoiceDateTo".equals(m.messageKey()));
    }

    // ── Client validation ─────────────────────────────────────────────────────

    @Test
    void validate_sellerNumberNotFound_addsError() {
        given(searchService.findClientsByNumber("UNKNOWN")).willReturn(List.of());
        R13ReportRequest r = validRequest();
        r.setSellerNumbers(List.of("UNKNOWN"));

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).anyMatch(m -> m.messageKey().contains("cannot be found in CSP"));
    }

    @Test
    void validate_sellerNameNotFound_addsError() {
        given(searchService.findClientsByName("Nobody")).willReturn(List.of());
        R13ReportRequest r = validRequest();
        r.setSellerName("Nobody");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).anyMatch(m -> m.messageKey().contains("cannot be found in CSP"));
    }

    @Test
    void validate_sellerNumberWithNameMismatch_addsError() {
        given(searchService.findClientsByNumber("00001234")).willReturn(List.of(client("00001234", "Different Name")));
        R13ReportRequest r = validRequest();
        r.setSellerNumbers(List.of("00001234"));
        r.setSellerName("Exact Name");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).anyMatch(m -> m.messageKey().contains("cannot be found in CSP"));
    }

    @Test
    void validate_validRequest_noErrors() {
        R13ReportRequest r = validRequest();

        ValidationResult result = validator.validate(r);

        assertThat(result.hasErrors()).isFalse();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private R13ReportRequest validRequest() {
        R13ReportRequest r = new R13ReportRequest();
        r.setShowOptions(twoColumnOptions());
        r.setInvoiceDateFrom("20240101");
        r.setInvoiceDateTo("20240630");
        return r;
    }

    private R13ShowOptions twoColumnOptions() {
        R13ShowOptions opts = new R13ShowOptions();
        opts.setShowInvoiceNumber(true);
        opts.setShowSellerName(true);
        return opts;
    }

    private void assertHasError(ValidationResult r, String key) {
        assertThat(r.errors().stream().map(m -> m.messageKey()).toList())
                .as("expected error key '%s'", key)
                .contains(key);
    }

    private ClientLocation client(String number, String name) {
        return new ClientLocation(number, name, "01", "Main", "Victoria", "BC");
    }
}
