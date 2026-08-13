package ca.bc.gov.nrs.csp.backend.service.reporting;

import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleCsvExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Shared JasperReports compile/fill/export primitives used by every locally-rendered report
 * service (R06-R12). Extracted from what were byte-for-byte identical private methods on each
 * {@code RXXService} — the only thing that ever varied between reports was the exception message
 * text, which is preserved here via the {@code reportCode} parameter (e.g. {@code "R10"}).
 *
 * <p>Reports with subreports (R06, R11) call {@link #loadTemplate} / {@link #compileReport}
 * directly instead of {@link #compileFromClasspath}, since they need to interleave
 * {@link SubreportInjector} DOM rewriting between loading and compiling each piece of their
 * subreport chain — that per-report wiring stays in {@code R06Service}/{@code R11Service}, not
 * here.</p>
 */
@Component
public class JasperReportRenderer {

    private final DataSource dataSource;

    public JasperReportRenderer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Reads a JRXML template's raw content from the classpath. */
    public String loadTemplate(String templatePath) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(templatePath)) {
            if (stream == null) {
                throw new IOException("JRXML template not found on classpath: " + templatePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Compiles raw JRXML content (e.g. after {@link SubreportInjector} rewriting) into a {@link JasperReport}. */
    public JasperReport compileReport(String jrxmlContent) throws JRException {
        try (InputStream stream = new ByteArrayInputStream(jrxmlContent.getBytes(StandardCharsets.UTF_8))) {
            return JasperCompileManager.compileReport(stream);
        } catch (IOException e) {
            throw new JRException("Failed to compile JRXML template", e);
        }
    }

    /**
     * Loads and compiles a template directly from the classpath in one step, wrapping load/compile
     * failures in an unchecked {@link ReportGenerationException}. This is the common case for
     * self-contained reports (R07, R08, R10, R12) with no subreport rewriting between load and
     * compile.
     */
    public JasperReport compileFromClasspath(String templatePath) {
        try {
            return compileReport(loadTemplate(templatePath));
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to compile JRXML template.", e);
        }
    }

    /** Fills a compiled report against this app's own database connection. */
    public JasperPrint fillReport(JasperReport report, Map<String, Object> params, String reportCode) {
        try (Connection conn = dataSource.getConnection()) {
            return JasperFillManager.fillReport(report, params, conn);
        } catch (JRException | SQLException e) {
            throw new ReportGenerationException("Failed to fill " + reportCode + " report from database", e);
        }
    }

    /** Exports a filled report to PDF or CSV. */
    public byte[] exportReport(JasperPrint jasperPrint, String format, String reportCode) {
        try {
            return switch (format.toLowerCase()) {
                case "pdf" -> JasperExportManager.exportReportToPdf(jasperPrint);
                case "csv" -> exportToCsv(jasperPrint);
                default -> throw new ReportGenerationException("Unsupported report format: " + format, null);
            };
        } catch (JRException e) {
            throw new ReportGenerationException("Failed to export " + reportCode + " report to " + format, e);
        }
    }

    private byte[] exportToCsv(JasperPrint jasperPrint) throws JRException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JRCsvExporter exporter = new JRCsvExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(out));
        exporter.setConfiguration(new SimpleCsvExporterConfiguration());
        exporter.exportReport();
        return out.toByteArray();
    }
}
