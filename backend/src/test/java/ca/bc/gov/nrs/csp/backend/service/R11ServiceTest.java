package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R11ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperServerService;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class R11ServiceTest {

    @Mock
    JasperServerService jasperServerService;

    R11Service service;

    @BeforeEach
    void setUp() {
        service = new R11Service(jasperServerService);
    }

    private R11ReportRequest baseRequest() {
        R11ReportRequest r = new R11ReportRequest();
        r.setReportFormat(ReportFormat.PDF);
        r.setDateFrom("20200101");
        r.setModelingCode("1");
        return r;
    }

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        void shouldThrow_whenDateFromIsAfterDateTo() {
            R11ReportRequest r = baseRequest();
            r.setDateFrom("20201231");
            r.setDateTo("20200101");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.daterange.order.error"));
        }

        @Test
        void shouldThrow_whenModelingCodeMissing() {
            R11ReportRequest r = baseRequest();
            r.setModelingCode(null);

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.r11.reporttype.required.error"));
        }

        @Test
        void shouldThrow_whenDateFromIsNull() {
            R11ReportRequest r = new R11ReportRequest();
            r.setReportFormat(ReportFormat.PDF);
            r.setModelingCode("1");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.startdate.required.error"));
        }

        @Test
        void shouldAccept_whenDateFromEqualsDateTo() {
            R11ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setDateTo("20200101");
            given(jasperServerService.generateReport(eq("R11"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }

        @Test
        void shouldThrow_whenTimeFrameIsNonNumeric() {
            R11ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setTimeFrame("abc");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("timeFrame must be a numeric value");
        }
    }

    @Nested
    @DisplayName("generateReport()")
    class GenerateReport {

        @Test
        void shouldReturnResult_whenJasperReturnsData() {
            R11ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setDateTo("20201231");
            given(jasperServerService.generateReport(eq("R11"), any())).willReturn(new byte[]{7, 8});

            ReportResult result = service.generateReport(r);

            assertThat(result.data()).isEqualTo(new byte[]{7, 8});
            assertThat(result.filename()).startsWith("R11_").endsWith(".pdf");
        }

        @Test
        void shouldReturnCsvFilename_whenFormatIsCsv() {
            R11ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            given(jasperServerService.generateReport(eq("R11"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r).filename()).endsWith(".csv");
        }

        @Test
        void shouldThrowResourceNotFound_whenJasperReturnsEmpty() {
            R11ReportRequest r = baseRequest();
            given(jasperServerService.generateReport(eq("R11"), any())).willReturn(null);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R11");
        }

        @Test
        void shouldDefaultMaturityCodes_whenNotProvided() {
            R11ReportRequest r = baseRequest();
            given(jasperServerService.generateReport(eq("R11"), any())).willReturn(new byte[]{1});

            ReportResult result = service.generateReport(r);

            assertThat(result).isNotNull();
        }
    }
}
