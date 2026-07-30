package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R12ReportRequest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class R12ServiceTest {

    @Mock
    JasperServerService jasperServerService;

    R12Service service;

    @BeforeEach
    void setUp() {
        service = new R12Service(jasperServerService);
    }

    private R12ReportRequest baseRequest() {
        R12ReportRequest r = new R12ReportRequest();
        r.setReportFormat(ReportFormat.PDF);
        return r;
    }

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        void shouldThrow_whenNoYearAndNoDateFrom() {
            R12ReportRequest r = baseRequest();

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.r12.startdate.required.error"));
        }

        @Test
        void shouldThrow_whenNoYearAndDateFromButNoDateToOrTimeFrame() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.r12.enddate.or.timeframe.required.error"));
        }

        @Test
        void shouldThrow_whenDateFromIsAfterDateTo() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20201231");
            r.setDateTo("20200101");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.daterange.order.error"));
        }

        @Test
        void shouldAccept_whenYearProvided() {
            R12ReportRequest r = baseRequest();
            r.setYear(2020);
            given(jasperServerService.generateReport(eq("R12"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }

        @Test
        void shouldAccept_whenYearAndMonthProvided() {
            R12ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(6);
            given(jasperServerService.generateReport(eq("R12"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }

        @Test
        void shouldAccept_whenDateFromAndDateToProvided() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setDateTo("20201231");
            given(jasperServerService.generateReport(eq("R12"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }

        @Test
        void shouldAccept_whenDateFromAndTimeFrameProvided() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setTimeFrame("3");
            given(jasperServerService.generateReport(eq("R12"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }

        @Test
        void shouldThrow_whenTimeFrameIsNonNumeric() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setTimeFrame("abc");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("timeFrame must be a numeric value");
        }
    }

    @Nested
    @DisplayName("buildParams() date handling")
    class DateHandling {

        @SuppressWarnings("unchecked")
        private Map<String, Object> capturedParams(R12ReportRequest r) {
            given(jasperServerService.generateReport(eq("R12"), any())).willReturn(new byte[]{1});
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R12"), captor.capture());
            return captor.getValue();
        }

        @Test
        void shouldRoundDatesToMonthBoundaries() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setDateTo("20200320");

            Map<String, Object> params = capturedParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200331");
        }

        @Test
        void shouldComputeDateToInclusivelyFromTimeFrame() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("3");

            Map<String, Object> params = capturedParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200331");
        }

        @Test
        void shouldUseEndOfStartMonth_whenTimeFrameIsOne() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("1");

            Map<String, Object> params = capturedParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200131");
        }
    }

    @Nested
    @DisplayName("generateReport()")
    class GenerateReport {

        @Test
        void shouldReturnResult_whenJasperReturnsData() {
            R12ReportRequest r = baseRequest();
            r.setYear(2020);
            given(jasperServerService.generateReport(eq("R12"), any())).willReturn(new byte[]{9});

            ReportResult result = service.generateReport(r);

            assertThat(result.data()).isEqualTo(new byte[]{9});
            assertThat(result.filename()).startsWith("R12_").endsWith(".pdf");
        }

        @Test
        void shouldReturnCsvFilename_whenFormatIsCsv() {
            R12ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            r.setYear(2020);
            given(jasperServerService.generateReport(eq("R12"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r).filename()).endsWith(".csv");
        }

        @Test
        void shouldThrowResourceNotFound_whenJasperReturnsEmpty() {
            R12ReportRequest r = baseRequest();
            r.setYear(2020);
            given(jasperServerService.generateReport(eq("R12"), any())).willReturn(new byte[0]);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R12");
        }
    }
}
