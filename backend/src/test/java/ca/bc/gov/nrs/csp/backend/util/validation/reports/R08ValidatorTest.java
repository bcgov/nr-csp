package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R08ReportRequest;
import ca.bc.gov.nrs.csp.backend.service.SearchService;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
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
class R08ValidatorTest {

    @Mock SearchService searchService;

    R08Validator validator;

    @BeforeEach
    void setUp() {
        validator = new R08Validator(searchService);
        lenient().when(searchService.findClientsByNumber(anyString())).thenReturn(List.of(client("00001234")));
        lenient().when(searchService.findClientsByName(anyString())).thenReturn(List.of(client("00001234")));
    }

    @Test
    void validate_noDateAndNoSubmission_addsRequiredFilterError() {
        R08ReportRequest r = new R08ReportRequest();

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.r08.filter.required.error");
    }

    @Test
    void validate_withDateFrom_noRequiredFilterError() {
        R08ReportRequest r = new R08ReportRequest();
        r.setDateFrom("20240101");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r08.filter.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_withSubmissionNumber_noRequiredFilterError() {
        R08ReportRequest r = new R08ReportRequest();
        r.setSubmissionNumber("12345");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r08.filter.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_withSubmissionYearMonth_noRequiredFilterError() {
        R08ReportRequest r = new R08ReportRequest();
        r.setSubmissionYearMonth("202406");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r08.filter.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_nonNumericSubmissionNumber_addsError() {
        R08ReportRequest r = new R08ReportRequest();
        r.setDateFrom("20240101");
        r.setSubmissionNumber("NOTNUM");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.submissionnumber.numeric.error");
    }

    @Test
    void validate_sellerClientNumberNotFound_addsError() {
        given(searchService.findClientsByNumber("UNKNOWN")).willReturn(List.of());
        R08ReportRequest r = new R08ReportRequest();
        r.setDateFrom("20240101");
        r.setSellerClientNumber("UNKNOWN");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.client.number.notfound.error");
    }

    @Test
    void validate_buyerClientNameNotFound_addsError() {
        given(searchService.findClientsByName("Unknown Buyer")).willReturn(List.of());
        R08ReportRequest r = new R08ReportRequest();
        r.setDateFrom("20240101");
        r.setBuyerClientName("Unknown Buyer");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.client.name.notfound.error");
    }

    @Test
    void validate_buyerClientNameNoMatch_addsError() {
        ClientLocation cl = new ClientLocation("00001234", "Different Name", "01", "Main", "Victoria", "BC");
        given(searchService.findClientsByName("Some Buyer")).willReturn(List.of(cl));
        R08ReportRequest r = new R08ReportRequest();
        r.setDateFrom("20240101");
        r.setBuyerClientName("Some Buyer");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.client.name.nomatch.error");
    }

    @Test
    void validate_dateRangeOutOfOrder_addsError() {
        R08ReportRequest r = new R08ReportRequest();
        r.setDateFrom("20240630");
        r.setDateTo("20240101");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.daterange.order.error");
    }

    @Test
    void validate_validRequest_noErrors() {
        R08ReportRequest r = new R08ReportRequest();
        r.setDateFrom("20240101");
        r.setDateTo("20240630");

        ValidationResult result = validator.validate(r);

        assertThat(result.hasErrors()).isFalse();
    }

    private void assertHasError(ValidationResult r, String key) {
        assertThat(r.errors()).extracting(ValidationMessage::messageKey)
                .as("expected error key '%s'", key)
                .contains(key);
    }

    private ClientLocation client(String number) {
        return new ClientLocation(number, "Test Client", "01", "Main", "Victoria", "BC");
    }
}
