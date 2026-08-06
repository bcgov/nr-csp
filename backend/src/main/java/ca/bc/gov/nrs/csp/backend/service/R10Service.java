package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R10ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R10Validator;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class R10Service {

    private static final Logger log = LoggerFactory.getLogger(R10Service.class);

    private final DataSource dataSource;
    private final SearchService searchService;

    /** Cache of compiled JasperReport objects keyed by template classpath path. */
    private final Map<String, JasperReport> compiledReportCache = new ConcurrentHashMap<>();

    @Value("${jasper.report.r10.template:/reports/R10.jrxml}")
    private String r10TemplatePath;

    @Value("${jasper.report.r10.csv.template:/reports/R10_CSV.jrxml}")
    private String r10CsvTemplatePath;

    public R10Service(DataSource dataSource, SearchService searchService) {
        this.dataSource = dataSource;
        this.searchService = searchService;
    }

    public ReportResult generateReport(R10ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R10 report format={}", format);
        String ext = "CSV".equalsIgnoreCase(format) ? "csv" : "pdf";
        String templatePath = "CSV".equalsIgnoreCase(format) ? r10CsvTemplatePath : r10TemplatePath;

        JasperReport jasperReport = compiledReportCache.computeIfAbsent(templatePath, path -> {
            try {
                log.info("Compiling JRXML: {}", path);
                return compileReport(loadTemplate(path));
            } catch (Exception e) {
                throw new ReportGenerationException("Failed to compile JRXML template.", e);
            }
        });

        Map<String, Object> params = buildParams(request);
        JasperPrint jasperPrint = fillReport(jasperReport, params);

        if (jasperPrint.getPages().isEmpty()) {
            throw new ResourceNotFoundException("The R10 report returned no data for the given parameters.");
        }

        byte[] data = exportReport(jasperPrint, format);
        String filename = String.format("R10_%s.%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")), ext);
        return new ReportResult(data, filename);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(R10ReportRequest r) {
        ValidationResult result = new R10Validator(searchService).validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R10 report failed validation.", result);
        }
    }

    // ── JRXML loading and compilation ─────────────────────────────────────────

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
            throw new ReportGenerationException("Failed to fill R10 report from database", e);
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
            throw new ReportGenerationException("Failed to export R10 report to " + format, e);
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

    private Map<String, Object> buildParams(R10ReportRequest r) {
        Map<String, Object> p = new HashMap<>();
        String effectiveDateTo = autoDateTo(r.getDateFrom(), r.getDateTo(), r.getTimeFrame());
        if (r.getDateFrom() != null)           p.put("INVOICE_DATE_FROM", firstDayOfMonth(r.getDateFrom()));
        if (effectiveDateTo != null)           p.put("INVOICE_DATE_TO", lastDayOfMonth(effectiveDateTo));
        if (r.getTimeFrame() != null)          p.put("TIME_FRAME", r.getTimeFrame());
        if (r.getSellerClientNumber() != null) p.put("SELLER_CLIENT_NUMBER", r.getSellerClientNumber());
        // The stored proc / jrxml parameter is SELLER_CLIENT_LOCN_CODE / BUYER_CLIENT_LOCN_CODE
        // (confirmed against both the JR7-converted and the original JR6 report designs) — the
        // short names previously used here were a pre-existing mismatch that silently dropped
        // these filters (JasperReports/JasperServer both fall back to a param's null default
        // when the supplied map key doesn't match a declared parameter name).
        if (r.getSellerLocnCode() != null)     p.put("SELLER_CLIENT_LOCN_CODE", r.getSellerLocnCode());
        if (r.getBuyerClientNumber() != null)  p.put("BUYER_CLIENT_NUMBER", r.getBuyerClientNumber());
        if (r.getBuyerLocnCode() != null)      p.put("BUYER_CLIENT_LOCN_CODE", r.getBuyerLocnCode());
        if (r.getMaturityCodes() != null)      p.put("MATURITY", r.getMaturityCodes());
        if (r.getInvoiceTypeCode() != null)    p.put("INVOICE_TYPE_CODE", r.getInvoiceTypeCode());
        // Prefer the authenticated user (IDIR) from the validated JWT over any client-supplied value.
        String idir = SecurityContextUtils.currentUsername().orElse(r.getUserId());
        if (idir != null)                      p.put("USER_ID", idir);
        return p;
    }

    private static String autoDateTo(String dateFrom, String dateTo, String timeFrame) {
        if (dateTo != null || dateFrom == null) return dateTo;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate from = LocalDate.parse(dateFrom, fmt);
        LocalDate end;
        if (timeFrame != null && !timeFrame.isBlank()) {
            try {
                end = from.plusMonths(Math.max(Integer.parseInt(timeFrame) - 1, 0));
            } catch (NumberFormatException e) {
                throw new BadRequestException("timeFrame must be a numeric value");
            }
        } else {
            end = from;
        }
        return YearMonth.from(end).atEndOfMonth().format(fmt);
    }

    private static String firstDayOfMonth(String date) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        return YearMonth.from(LocalDate.parse(date, fmt)).atDay(1).format(fmt);
    }

    private static String lastDayOfMonth(String date) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        return YearMonth.from(LocalDate.parse(date, fmt)).atEndOfMonth().format(fmt);
    }
}
