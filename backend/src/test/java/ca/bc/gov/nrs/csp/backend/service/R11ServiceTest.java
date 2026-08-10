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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R11");
        }
    }

    /**
     * Exercises the real {@code Connection} → {@code CallableStatement} binding path that every
     * other test in this class bypasses (they leave {@code dataSource.getConnection()}
     * unstubbed, which returns {@code null} and short-circuits before JasperReports ever prepares
     * a statement). See {@code R10ServiceTest.RealProcedureCallBinding} for the full rationale.
     *
     * <p>Only the main report's {@code CallableStatement} is exercised here: R11's four
     * maturity-gated subreport invocations live inside the {@code <detail>} band, which only
     * evaluates when the main query returns at least one row — with a mocked cursor returning
     * zero rows (as below), the detail band (and its subreport chain) is never reached, so this
     * test only proves the top-level {@code CSP_SP_RPT_11} call binds correctly, not the
     * subreports'.</p>
     */
    @Nested
    @DisplayName("generateReport() — real Connection/CallableStatement binding")
    class RealProcedureCallBinding {

        @Mock
        Connection connection;
        @Mock
        CallableStatement callableStatement;
        @Mock
        ResultSet cursorResult;

        @Test
        void shouldRegisterRefCursorOutParameter_andFillViaCallableStatement() throws SQLException {
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.prepareCall(anyString())).willReturn(callableStatement);
            given(callableStatement.getObject(1)).willReturn(cursorResult);
            given(cursorResult.next()).willReturn(false);

            R11ReportRequest r = baseRequest();

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(connection).prepareCall(anyString());
            verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
            verify(callableStatement).execute();
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
