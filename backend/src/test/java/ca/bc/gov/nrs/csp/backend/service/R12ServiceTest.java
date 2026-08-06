package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R12ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.base.JRBasePrintPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class R12ServiceTest {

    @Mock
    DataSource dataSource;

    R12Service service;

    @BeforeEach
    void setUp() {
        service = new R12Service(dataSource);
        ReflectionTestUtils.setField(service, "r12TemplatePath", "/reports/R12.jrxml");
        ReflectionTestUtils.setField(service, "r12CsvTemplatePath", "/reports/R12_CSV.jrxml");
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
                    .extracting(m -> m.messageKey())
                    .anyMatch(key -> key.contains("report.r12.startdate.required.error"));
        }

        @Test
        void shouldThrow_whenNoYearAndDateFromButNoDateToOrTimeFrame() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(m -> m.messageKey())
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
                    .extracting(m -> m.messageKey())
                    .anyMatch(key -> key.contains("report.daterange.order.error"));
        }

        @Test
        void shouldAccept_whenYearProvided() {
            R12ReportRequest r = baseRequest();
            r.setYear(2020);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldAccept_whenYearAndMonthProvided() {
            R12ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(6);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldAccept_whenDateFromAndDateToProvided() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setDateTo("20201231");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldAccept_whenDateFromAndTimeFrameProvided() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setTimeFrame("3");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
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

        private Map<String, Object> buildParams(R12ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "buildParams", r);
        }

        @Test
        void shouldRoundDatesToMonthBoundaries() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setDateTo("20200320");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200331");
        }

        @Test
        void shouldComputeDateToInclusivelyFromTimeFrame() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("3");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200331");
        }

        @Test
        void shouldUseEndOfStartMonth_whenTimeFrameIsOne() {
            R12ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("1");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200131");
        }
    }

    @Nested
    @DisplayName("generateReport() — compile/fill failures")
    class GenerateReportFailures {

        @Test
        void shouldThrowReportGenerationException_whenTemplateNotFoundOnClasspath() {
            ReflectionTestUtils.setField(service, "r12TemplatePath", "/reports/DOES_NOT_EXIST.jrxml");
            R12ReportRequest r = baseRequest();
            r.setYear(2020);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to compile JRXML");
        }

        @Test
        void shouldThrowReportGenerationException_whenConnectionCannotBeObtained() throws Exception {
            given(dataSource.getConnection()).willThrow(new SQLException("boom"));
            R12ReportRequest r = baseRequest();
            r.setYear(2020);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to fill R12 report from database")
                    .hasCauseInstanceOf(SQLException.class);
        }

        @Test
        void shouldThrowResourceNotFound_whenReportHasNoPages() {
            R12ReportRequest r = baseRequest();
            r.setYear(2020);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R12");
        }

        @Test
        void shouldCompileCsvTemplateWithoutError_whenFormatIsCsv() {
            R12ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            r.setYear(2020);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R12");
        }
    }

    @Nested
    @DisplayName("exportReport() — output formats")
    class ExportReport {

        private JasperPrint printWithOnePage() {
            JasperPrint print = new JasperPrint();
            print.setName("R12Test");
            print.setPageWidth(612);
            print.setPageHeight(792);
            print.addPage(new JRBasePrintPage());
            return print;
        }

        @Test
        void shouldExportPdf() {
            byte[] data = ReflectionTestUtils.invokeMethod(service, "exportReport", printWithOnePage(), "PDF");

            assertThat(data).isNotEmpty();
            assertThat(new String(data, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        }

        @Test
        void shouldExportCsv() {
            byte[] data = ReflectionTestUtils.invokeMethod(service, "exportReport", printWithOnePage(), "CSV");

            assertThat(data).isNotNull();
        }

        @Test
        void shouldThrowReportGenerationException_whenFormatUnsupported() {
            JasperPrint print = printWithOnePage();

            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "exportReport", print, "XLSX"))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Unsupported report format: XLSX");
        }
    }
}
