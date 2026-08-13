package ca.bc.gov.nrs.csp.backend.service.reporting;

import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.base.JRBasePrintPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Direct unit tests for the shared compile/fill/export component used by every locally-rendered
 * report service (R06-R12). Each {@code RXXServiceTest} still exercises this renderer end-to-end
 * against its own report's real jrxml (constructing a real {@code JasperReportRenderer} wrapping
 * a mocked {@code DataSource}, exactly as before this class existed) — this file instead tests
 * the renderer's own logic in isolation, once, rather than duplicating it per report.
 */
@ExtendWith(MockitoExtension.class)
class JasperReportRendererTest {

    @Mock
    DataSource dataSource;

    JasperReportRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new JasperReportRenderer(dataSource);
    }

    @Nested
    @DisplayName("loadTemplate()")
    class LoadTemplate {

        @Test
        void shouldReturnJrxmlContent_whenTemplateExistsOnClasspath() throws IOException {
            String content = renderer.loadTemplate("/reports/R10.jrxml");

            assertThat(content).contains("<jasperReport");
        }

        @Test
        void shouldThrowIOException_whenTemplateNotFoundOnClasspath() {
            assertThatThrownBy(() -> renderer.loadTemplate("/reports/DOES_NOT_EXIST.jrxml"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("JRXML template not found on classpath: /reports/DOES_NOT_EXIST.jrxml");
        }
    }

    @Nested
    @DisplayName("compileReport()")
    class CompileReport {

        @Test
        void shouldCompileValidJrxmlContent() throws Exception {
            String jrxml = renderer.loadTemplate("/reports/R10.jrxml");

            JasperReport report = renderer.compileReport(jrxml);

            assertThat(report).isNotNull();
        }

        @Test
        void shouldThrowJRException_whenContentIsNotValidJrxml() {
            assertThatThrownBy(() -> renderer.compileReport("<not-a-jrxml/>"))
                    .isInstanceOf(JRException.class);
        }
    }

    @Nested
    @DisplayName("compileFromClasspath()")
    class CompileFromClasspath {

        @Test
        void shouldLoadAndCompileInOneStep() {
            JasperReport report = renderer.compileFromClasspath("/reports/R10.jrxml");

            assertThat(report).isNotNull();
        }

        @Test
        void shouldWrapMissingTemplateFailure_inReportGenerationException() {
            assertThatThrownBy(() -> renderer.compileFromClasspath("/reports/DOES_NOT_EXIST.jrxml"))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to compile JRXML template.")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    @Nested
    @DisplayName("fillReport()")
    class FillReport {

        @Test
        void shouldWrapConnectionFailure_withReportCodeSpecificMessage() throws SQLException {
            given(dataSource.getConnection()).willThrow(new SQLException("boom"));
            JasperReport report = renderer.compileFromClasspath("/reports/R10.jrxml");

            assertThatThrownBy(() -> renderer.fillReport(report, new HashMap<>(), "R10"))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to fill R10 report from database")
                    .hasCauseInstanceOf(SQLException.class);
        }

        @Test
        void shouldUseDifferentReportCodeInMessage_forDifferentReport() throws SQLException {
            given(dataSource.getConnection()).willThrow(new SQLException("boom"));
            JasperReport report = renderer.compileFromClasspath("/reports/R10.jrxml");

            assertThatThrownBy(() -> renderer.fillReport(report, new HashMap<>(), "R12"))
                    .hasMessageContaining("Failed to fill R12 report from database");
        }
    }

    /**
     * Exercises the real {@code Connection} → {@code CallableStatement} binding path that every
     * test above bypasses (they leave {@code dataSource.getConnection()} unstubbed, which returns
     * {@code null} and short-circuits before JasperReports ever prepares a statement). This is the
     * generic version of the regression guard formerly duplicated as {@code RealProcedureCallBinding}
     * in every {@code RXXServiceTest} — the JDBC binding mechanism it proves
     * ({@code RefCursorProcedureCallHandlerFactory} registering a REF CURSOR OUT parameter) lives
     * entirely in {@link #fillReport} and doesn't vary by report, so one thorough test here covers
     * it; each {@code RXXServiceTest} additionally keeps its own instance of this test using its
     * own report's real compiled jrxml, to prove that specific report's own {@code {call ...}}
     * query text survives the real pipeline (a generic synthetic report here couldn't catch e.g. a
     * wrong parameter count in one specific report's stored-procedure call).
     */
    @Nested
    @DisplayName("fillReport() — real Connection/CallableStatement binding")
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
            JasperReport report = renderer.compileFromClasspath("/reports/R10.jrxml");

            JasperPrint print = renderer.fillReport(report, new HashMap<>(), "R10");

            // No rows from the (mocked) cursor -> zero pages; the point of this test is that
            // binding succeeds at all, not that it returns data.
            assertThat(print.getPages()).isEmpty();
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
            print.setName("JasperReportRendererTest");
            print.setPageWidth(612);
            print.setPageHeight(792);
            print.addPage(new JRBasePrintPage());
            return print;
        }

        @Test
        void shouldExportPdf() {
            byte[] data = renderer.exportReport(printWithOnePage(), "PDF", "R10");

            assertThat(data).isNotEmpty();
            assertThat(new String(data, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        }

        @Test
        void shouldExportCsv() {
            byte[] data = renderer.exportReport(printWithOnePage(), "CSV", "R10");

            assertThat(data).isNotNull();
        }

        @Test
        void shouldThrowReportGenerationException_whenFormatUnsupported() {
            JasperPrint print = printWithOnePage();

            assertThatThrownBy(() -> renderer.exportReport(print, "XLSX", "R10"))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Unsupported report format: XLSX");
        }

        @Test
        void shouldIncludeReportCodeInUnsupportedFormatExceptionContext() {
            JasperPrint print = printWithOnePage();

            // The "Unsupported report format" message itself has no report code (matches the
            // original per-service behavior), but exportReport() is still exercised for a second
            // report code here to confirm that omission is deliberate, not an oversight.
            assertThatThrownBy(() -> renderer.exportReport(print, "XLSX", "R06"))
                    .hasMessageContaining("Unsupported report format: XLSX")
                    .hasMessageNotContaining("R06");
        }
    }
}
