package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R10ReportRequest;
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
class R10ValidatorTest {

    @Mock SearchService searchService;

    R10Validator validator;

    @BeforeEach
    void setUp() {
        validator = new R10Validator(searchService);
        lenient().when(searchService.findClientsByNumber(anyString())).thenReturn(List.of(client("00001234")));
    }

    @Test
    void validate_missingDateFrom_addsError() {
        R10ReportRequest r = new R10ReportRequest();
        r.setTimeFrame("3");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.startdate.required.error");
    }

    @Test
    void validate_missingDateToAndTimeFrame_addsError() {
        R10ReportRequest r = new R10ReportRequest();
        r.setDateFrom("20240101");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.r10.enddate.or.timeframe.required.error");
    }

    @Test
    void validate_withDateTo_noEndDateError() {
        R10ReportRequest r = new R10ReportRequest();
        r.setDateFrom("20240101");
        r.setDateTo("20240630");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r10.enddate.or.timeframe.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_withTimeFrame_noEndDateError() {
        R10ReportRequest r = new R10ReportRequest();
        r.setDateFrom("20240101");
        r.setTimeFrame("3");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r10.enddate.or.timeframe.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_sellerClientNotFound_addsError() {
        given(searchService.findClientsByNumber("UNKNOWN")).willReturn(List.of());
        R10ReportRequest r = new R10ReportRequest();
        r.setDateFrom("20240101");
        r.setDateTo("20240630");
        r.setSellerClientNumber("UNKNOWN");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.client.number.notfound.error");
    }

    @Test
    void validate_buyerClientNotFound_addsError() {
        given(searchService.findClientsByNumber("UNKNOWN")).willReturn(List.of());
        R10ReportRequest r = new R10ReportRequest();
        r.setDateFrom("20240101");
        r.setDateTo("20240630");
        r.setBuyerClientNumber("UNKNOWN");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.client.number.notfound.error");
    }

    @Test
    void validate_dateRangeOutOfOrder_addsError() {
        R10ReportRequest r = new R10ReportRequest();
        r.setDateFrom("20240630");
        r.setDateTo("20240101");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.daterange.order.error");
    }

    @Test
    void validate_validRequest_noErrors() {
        R10ReportRequest r = new R10ReportRequest();
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
