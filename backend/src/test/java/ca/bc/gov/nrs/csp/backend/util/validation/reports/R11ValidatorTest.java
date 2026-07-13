package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R11ReportRequest;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class R11ValidatorTest {

    R11Validator validator;

    @BeforeEach
    void setUp() {
        validator = new R11Validator();
    }

    @Test
    void validate_missingDateFrom_addsError() {
        R11ReportRequest r = new R11ReportRequest();
        r.setModelingCode("P");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.startdate.required.error");
    }

    @Test
    void validate_missingModelingCode_addsError() {
        R11ReportRequest r = new R11ReportRequest();
        r.setDateFrom("20240101");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.r11.reporttype.required.error");
    }

    @Test
    void validate_dateRangeOutOfOrder_addsError() {
        R11ReportRequest r = new R11ReportRequest();
        r.setDateFrom("20240630");
        r.setDateTo("20240101");
        r.setModelingCode("P");

        ValidationResult result = validator.validate(r);

        assertHasError(result, "report.daterange.order.error");
    }

    @Test
    void validate_validRequest_noErrors() {
        R11ReportRequest r = new R11ReportRequest();
        r.setDateFrom("20240101");
        r.setDateTo("20240630");
        r.setModelingCode("P");

        ValidationResult result = validator.validate(r);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_nullDateTo_allowedWithValidFrom() {
        R11ReportRequest r = new R11ReportRequest();
        r.setDateFrom("20240101");
        r.setModelingCode("P");

        ValidationResult result = validator.validate(r);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_equalDates_noDateRangeError() {
        R11ReportRequest r = new R11ReportRequest();
        r.setDateFrom("20240101");
        r.setDateTo("20240101");
        r.setModelingCode("P");

        ValidationResult result = validator.validate(r);

        assertThat(result.errors()).noneMatch(m -> "report.daterange.order.error".equals(m.messageKey()));
    }

    private void assertHasError(ValidationResult r, String key) {
        assertThat(r.errors()).extracting(ValidationMessage::messageKey)
                .as("expected error key '%s'", key)
                .contains(key);
    }
}
