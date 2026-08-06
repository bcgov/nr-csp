package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R06ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.SubreportInjector;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R06Validator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class R06Service {

    private static final Logger log = LoggerFactory.getLogger(R06Service.class);

    private final DataSource dataSource;
    private final SearchService searchService;

    /** Cache of compiled report bundles (main + both subreports) keyed by main template path. */
    private final Map<String, ReportBundle> compiledReportCache = new ConcurrentHashMap<>();

    @Value("${jasper.report.r06.template:/reports/R06.jrxml}")
    private String r06TemplatePath;
    @Value("${jasper.report.r06.csv.template:/reports/R06_CSV.jrxml}")
    private String r06CsvTemplatePath;
    @Value("${jasper.report.r06.subreport1.template:/reports/r06_subreport1.jrxml}")
    private String r06Subreport1Path;
    @Value("${jasper.report.r06.subreport1.csv.template:/reports/r06_subreport1_CSV.jrxml}")
    private String r06Subreport1CsvPath;
    @Value("${jasper.report.r06.subreport2.template:/reports/r06_subreport2.jrxml}")
    private String r06Subreport2Path;
    @Value("${jasper.report.r06.subreport2.csv.template:/reports/r06_subreport2_CSV.jrxml}")
    private String r06Subreport2CsvPath;

    public R06Service(DataSource dataSource, SearchService searchService) {
        this.dataSource = dataSource;
        this.searchService = searchService;
    }

    public ReportResult generateReport(R06ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R06 report format={}", format);
        boolean csv = "CSV".equalsIgnoreCase(format);
        String ext = csv ? "csv" : "pdf";
        String mainPath = csv ? r06CsvTemplatePath : r06TemplatePath;
        String sub1Path = csv ? r06Subreport1CsvPath : r06Subreport1Path;
        String sub2Path = csv ? r06Subreport2CsvPath : r06Subreport2Path;
        String sub1RepoName = csv ? "r06_subreport1_CSV.jrxml" : "r06_subreport1.jrxml";
        String sub2RepoName = csv ? "r06_subreport2_CSV.jrxml" : "r06_subreport2.jrxml";

        ReportBundle bundle = compiledReportCache.computeIfAbsent(mainPath,
                path -> compileBundle(path, sub1Path, sub2Path, sub1RepoName, sub2RepoName));

        Map<String, Object> params = buildParams(request);
        params.put("SUBREPORT_R06_1", bundle.sub1());
        params.put("SUBREPORT_R06_2", bundle.sub2());
        JasperPrint jasperPrint = fillReport(bundle.main(), params);

        if (jasperPrint.getPages().isEmpty()) {
            throw new ResourceNotFoundException("The R06 report returned no data for the given parameters.");
        }

        byte[] data = exportReport(jasperPrint, format);
        String filename = String.format("R06_%s.%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")), ext);
        return new ReportResult(data, filename);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(R06ReportRequest r) {
        ValidationResult result = new R06Validator(searchService).validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R06 report failed validation.", result);
        }
    }

    // ── JRXML loading, compilation and subreport wiring ────────────────────────

    /**
     * Compiles the main report and both of its subreports, rewriting the JasperReports
     * Server-only {@code "repo:name.jrxml"} subreport expressions to reference compiled-report
     * parameters instead (see {@link SubreportInjector}). Compiled deepest-first: subreport2 has
     * no nested subreports of its own, so it compiles unmodified; subreport1 hosts subreport2, so
     * its own subreport element is rewritten to {@code $P{SUBREPORT_R06_2}}; the main report hosts
     * subreport1 ({@code $P{SUBREPORT_R06_1}}) and additionally needs a pass-through mapping so
     * {@code SUBREPORT_R06_2} — declared but not directly used at the main-report level — reaches
     * subreport1 when filled.
     */
    private ReportBundle compileBundle(String mainPath, String sub1Path, String sub2Path,
                                        String sub1RepoName, String sub2RepoName) {
        try {
            JasperReport sub2 = compileReport(loadTemplate(sub2Path));

            String sub1Jrxml = SubreportInjector.rewriteSubreportExpression(
                    loadTemplate(sub1Path), sub2RepoName, "SUBREPORT_R06_2");
            JasperReport sub1 = compileReport(sub1Jrxml);

            String mainJrxml = SubreportInjector.rewriteSubreportExpression(
                    loadTemplate(mainPath), sub1RepoName, "SUBREPORT_R06_1");
            mainJrxml = SubreportInjector.addPassThroughParameter(mainJrxml, "SUBREPORT_R06_1", "SUBREPORT_R06_2");
            JasperReport main = compileReport(mainJrxml);

            return new ReportBundle(main, sub1, sub2);
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to compile JRXML template.", e);
        }
    }

    private String loadTemplate(String templatePath) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(templatePath)) {
            if (stream == null) {
                throw new IOException("JRXML template not found on classpath: " + templatePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private JasperReport compileReport(String jrxmlContent) throws JRException {
        try (InputStream stream = new ByteArrayInputStream(jrxmlContent.getBytes(StandardCharsets.UTF_8))) {
            return JasperCompileManager.compileReport(stream);
        } catch (IOException e) {
            throw new JRException("Failed to compile JRXML template", e);
        }
    }

    // ── Report fill / export ───────────────────────────────────────────────────

    private JasperPrint fillReport(JasperReport report, Map<String, Object> params) {
        try (Connection conn = dataSource.getConnection()) {
            return JasperFillManager.fillReport(report, params, conn);
        } catch (JRException | SQLException e) {
            throw new ReportGenerationException("Failed to fill R06 report from database", e);
        }
    }

    private byte[] exportReport(JasperPrint jasperPrint, String format) {
        try {
            return switch (format.toLowerCase()) {
                case "pdf" -> JasperExportManager.exportReportToPdf(jasperPrint);
                case "csv" -> exportToCsv(jasperPrint);
                default -> throw new ReportGenerationException("Unsupported report format: " + format, null);
            };
        } catch (JRException e) {
            throw new ReportGenerationException("Failed to export R06 report to " + format, e);
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

    // ── Parameter building ─────────────────────────────────────────────────────

    private Map<String, Object> buildParams(R06ReportRequest r) {
        Map<String, Object> p = new HashMap<>();
        if (r.getDateFrom() != null)               p.put("INVOICE_FROM", r.getDateFrom());
        if (r.getDateTo() != null)                 p.put("INVOICE_TO", r.getDateTo());
        if (r.getSellerClientNumber() != null)     p.put("SELLER_CLIENT_NUMBER", r.getSellerClientNumber());
        if (r.getSellerLocCode() != null)          p.put("SELLER_CLIENT_LOC_CODE", r.getSellerLocCode());
        if (r.getBuyerClientNumber() != null)      p.put("BUYER_CLIENT_NUMBER", r.getBuyerClientNumber());
        if (r.getBuyerLocCode() != null)           p.put("BUYER_CLIENT_LOC_CODE", r.getBuyerLocCode());
        if (r.getSubmissionId() != null)           p.put("SUBMISSION_ID", r.getSubmissionId());
        if (r.getInvoiceNumbers() != null)         p.put("CLIENT_INVOICE_NO", r.getInvoiceNumbers().toUpperCase());
        if (r.getMaturityCodes() != null)          p.put("LOG_SALE_TYPE_CODE_MATURITY", r.getMaturityCodes());
        if (r.getLogSaleEntryStatusCode() != null) p.put("LOG_SALE_ENTRY_STATUS_CODE", r.getLogSaleEntryStatusCode());
        if (r.getCspInvoiceTypeCode() != null)     p.put("CSP_INVOICE_TYPE_CODE", r.getCspInvoiceTypeCode());
        // Prefer the authenticated user (IDIR) from the validated JWT over any client-supplied value.
        String idir = SecurityContextUtils.currentUsername().orElse(r.getUserId());
        if (idir != null)                          p.put("USER_ID", idir);
        return p;
    }

    private record ReportBundle(JasperReport main, JasperReport sub1, JasperReport sub2) {}
}
