package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R12ReportRequest;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class R12ValidatorTest {

    R12Validator validator;

    @BeforeEach
    void setUp() {
        validator = new R12Validator();
    }

    @Test
    void validate_noYearAndMissingDateFrom_addsStartDateError() {
        R12ReportRequest r = new R12ReportRequest();
        r.setDateTo("20240630");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.r12.startdate.required.error");
    }

    @Test
    void validate_noYearAndMissingDateToAndTimeFrame_addsEndDateError() {
        R12ReportRequest r = new R12ReportRequest();
        r.setDateFrom("20240101");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.r12.enddate.or.timeframe.required.error");
    }

    @Test
    void validate_yearProvided_noDateRequiredErrors() {
        R12ReportRequest r = new R12ReportRequest();
        r.setYear(2024);

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r12.startdate.required.error".equals(m.messageKey()));
        assertThat(result.errors()).noneMatch(m -> "report.r12.enddate.or.timeframe.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_withDateFromAndDateTo_noRequiredErrors() {
        R12ReportRequest r = new R12ReportRequest();
        r.setDateFrom("20240101");
        r.setDateTo("20240630");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r12.startdate.required.error".equals(m.messageKey()));
        assertThat(result.errors()).noneMatch(m -> "report.r12.enddate.or.timeframe.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_withDateFromAndTimeFrame_noEndDateError() {
        R12ReportRequest r = new R12ReportRequest();
        r.setDateFrom("20240101");
        r.setTimeFrame("3");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.r12.enddate.or.timeframe.required.error".equals(m.messageKey()));
    }

    @Test
    void validate_dateRangeOutOfOrder_addsError() {
        R12ReportRequest r = new R12ReportRequest();
        r.setDateFrom("20240630");
        r.setDateTo("20240101");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.daterange.order.error");
    }

    @Test
    void validate_validRequest_noErrors() {
        R12ReportRequest r = new R12ReportRequest();
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
}
