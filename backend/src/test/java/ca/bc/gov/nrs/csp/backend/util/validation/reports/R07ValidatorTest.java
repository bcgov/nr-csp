package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R07ReportRequest;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class R07ValidatorTest {

    @Mock CspSubmissionRepository cspSubmissionRepo;
    @Mock SearchService searchService;

    R07Validator validator;

    @BeforeEach
    void setUp() {
        validator = new R07Validator(cspSubmissionRepo, searchService);
        lenient().when(cspSubmissionRepo.existsBySubmissionNumber(anyLong())).thenReturn(true);
        lenient().when(searchService.findClientsByNumber(anyString())).thenReturn(List.of(client("00001234")));
        lenient().when(searchService.findClientsByName(anyString())).thenReturn(List.of(client("00001234")));
    }

    @Test
    void validate_noFiltersAtAll_addsRequiredFilterError() {
        R07ReportRequest r = new R07ReportRequest();

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.r07.filter.required.error");
    }

    @Test
    void validate_withYearAndMonth_noRequiredFilterError() {
        R07ReportRequest r = new R07ReportRequest();
        r.setYear(2024);
        r.setMonth(6);

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r07.filter.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_withDateFrom_noRequiredFilterError() {
        R07ReportRequest r = new R07ReportRequest();
        r.setDateFrom("20240101");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r07.filter.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_withSellerClientNumber_noRequiredFilterError() {
        R07ReportRequest r = new R07ReportRequest();
        r.setSellerClientNumber("00001234");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r07.filter.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_nonNumericSubmissionNumber_addsError() {
        R07ReportRequest r = new R07ReportRequest();
        r.setSubmissionNumber("ABC");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.submissionnumber.numeric.error");
    }

    @Test
    void validate_submissionNumberNotFound_addsError() {
        given(cspSubmissionRepo.existsBySubmissionNumber(anyLong())).willReturn(false);
        R07ReportRequest r = new R07ReportRequest();
        r.setSubmissionNumber("99999");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.submissionnumber.notfound.error");
    }

    @Test
    void validate_sellerClientNumberNotFound_addsError() {
        given(searchService.findClientsByNumber("UNKNOWN")).willReturn(List.of());
        R07ReportRequest r = new R07ReportRequest();
        r.setSellerClientNumber("UNKNOWN");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.client.number.notfound.error");
    }

    @Test
    void validate_sellerClientNameNotFound_addsError() {
        given(searchService.findClientsByName("No Such Client")).willReturn(List.of());
        R07ReportRequest r = new R07ReportRequest();
        r.setSellerClientName("No Such Client");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.client.name.notfound.error");
    }

    @Test
    void validate_sellerClientNameNoMatch_addsError() {
        ClientLocation cl = new ClientLocation("00001234", "Different Name", "01", "Main", "Victoria", "BC");
        given(searchService.findClientsByName("Exact Name")).willReturn(List.of(cl));
        R07ReportRequest r = new R07ReportRequest();
        r.setSellerClientName("Exact Name");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.client.name.nomatch.error");
    }

    @Test
    void validate_dateRangeOutOfOrder_addsError() {
        R07ReportRequest r = new R07ReportRequest();
        r.setDateFrom("20240630");
        r.setDateTo("20240101");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.daterange.order.error");
    }

    @Test
    void validate_validYearMonthRequest_noErrors() {
        R07ReportRequest r = new R07ReportRequest();
        r.setYear(2024);
        r.setMonth(6);

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
