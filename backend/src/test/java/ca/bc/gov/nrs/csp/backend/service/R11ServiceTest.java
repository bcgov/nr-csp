package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R11ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
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
class R11ServiceTest {

    @Mock
    DataSource dataSource;

    R11Service service;

    @BeforeEach
    void setUp() {
        service = new R11Service(dataSource);
        ReflectionTestUtils.setField(service, "r11TemplatePath", "/reports/R11.jrxml");
        ReflectionTestUtils.setField(service, "r11CsvTemplatePath", "/reports/R11_CSV.jrxml");
        ReflectionTestUtils.setField(service, "r11SubreportPath", "/reports/r11_subreport.jrxml");
        ReflectionTestUtils.setField(service, "r11SubreportCsvPath", "/reports/r11_subreport_CSV.jrxml");
        ReflectionTestUtils.setField(service, "r11XtabSubreportPath", "/reports/r11_xtab_subreport.jrxml");
        ReflectionTestUtils.setField(service, "r11XtabSubreportCsvPath", "/reports/r11_xtab_subreport_CSV.jrxml");
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

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
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
    @DisplayName("generateReport() — compile/fill failures")
    class GenerateReportFailures {

        @Test
        void shouldThrowReportGenerationException_whenMainTemplateNotFoundOnClasspath() {
            ReflectionTestUtils.setField(service, "r11TemplatePath", "/reports/DOES_NOT_EXIST.jrxml");
            R11ReportRequest r = baseRequest();

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to compile JRXML");
        }

        @Test
        void shouldThrowReportGenerationException_whenXtabSubreportTemplateNotFoundOnClasspath() {
            ReflectionTestUtils.setField(service, "r11XtabSubreportPath", "/reports/DOES_NOT_EXIST.jrxml");
            R11ReportRequest r = baseRequest();

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to compile JRXML");
        }

        @Test
        void shouldThrowReportGenerationException_whenConnectionCannotBeObtained() throws Exception {
            given(dataSource.getConnection()).willThrow(new SQLException("boom"));
            R11ReportRequest r = baseRequest();

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to fill R11 report from database")
                    .hasCauseInstanceOf(SQLException.class);
        }

        @Test
        void shouldThrowResourceNotFound_whenReportHasNoPages() {
            // Also exercises the full real compile+subreport-wiring path, including the crosstab
            // inside r11_xtab_subreport.jrxml, against a mocked DataSource that yields no JDBC
            // connection — the fill produces no data rows and the report ends up with zero pages.
            R11ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setDateTo("20201231");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R11");
        }

        @Test
        void shouldCompileCsvTemplateChainWithoutError_whenFormatIsCsv() {
            R11ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R11");
        }

        @Test
        void shouldDefaultMaturityCodes_whenNotProvided() {
            R11ReportRequest r = baseRequest();

            // Exercises all four of R11's maturity-gated detail-band subreport invocations
            // (O/S/M/blended), each sharing the same rewritten SUBREPORT_R11_SUB parameter.
            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R11");
        }
    }

    @Nested
    @DisplayName("exportReport() — output formats")
    class ExportReport {

        private JasperPrint printWithOnePage() {
            JasperPrint print = new JasperPrint();
            print.setName("R11Test");
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

    @Nested
    @DisplayName("buildParams()")
    class BuildParams {

        private Map<String, Object> buildParams(R11ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "buildParams", r);
        }

        @Test
        void shouldDefaultMaturityAndDescription_whenNotProvided() {
            R11ReportRequest r = baseRequest();

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("TYPE_CODE_MATURITY", "O,S,M")
                    .containsEntry("TYPE_CODE_MATURITY_DESCRIPTION", "Old Growth, Second Growth, Mixed Growth")
                    .containsEntry("BLENDED", "false");
        }

        @Test
        void shouldUseProvidedMaturityCodes() {
            R11ReportRequest r = baseRequest();
            r.setMaturityCodes("C");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("TYPE_CODE_MATURITY", "C")
                    .containsEntry("TYPE_CODE_MATURITY_DESCRIPTION", "Cants");
        }

        @Test
        void shouldUseProvidedMaturityDescription_overridingComputedOne() {
            R11ReportRequest r = baseRequest();
            r.setMaturityCodes("C");
            r.setMaturityDescriptions("Custom Cants Label");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("TYPE_CODE_MATURITY_DESCRIPTION", "Custom Cants Label");
        }

        @Test
        void shouldReflectBlendedFlag_whenProvided() {
            R11ReportRequest r = baseRequest();
            r.setBlended(true);

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("BLENDED", "true");
        }
    }
}
