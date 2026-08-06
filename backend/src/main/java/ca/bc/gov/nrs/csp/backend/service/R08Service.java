package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R08ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R08Validator;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class R08Service {

    private static final Logger log = LoggerFactory.getLogger(R08Service.class);

    private final DataSource dataSource;
    private final SearchService searchService;

    /** Cache of compiled JasperReport objects keyed by template classpath path. */
    private final Map<String, JasperReport> compiledReportCache = new ConcurrentHashMap<>();

    @Value("${jasper.report.r08.template:/reports/R08.jrxml}")
    private String r08TemplatePath;

    @Value("${jasper.report.r08.csv.template:/reports/R08_CSV.jrxml}")
    private String r08CsvTemplatePath;

    public R08Service(DataSource dataSource, SearchService searchService) {
        this.dataSource = dataSource;
        this.searchService = searchService;
    }

    public ReportResult generateReport(R08ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R08 report format={}", format);
        String ext = "CSV".equalsIgnoreCase(format) ? "csv" : "pdf";
        String templatePath = "CSV".equalsIgnoreCase(format) ? r08CsvTemplatePath : r08TemplatePath;

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
            throw new ResourceNotFoundException("The R08 report returned no data for the given parameters.");
        }

        byte[] data = exportReport(jasperPrint, format);
        String filename = String.format("R08_%s.%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")), ext);
        return new ReportResult(data, filename);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(R08ReportRequest r) {
        ValidationResult result = new R08Validator(searchService).validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R08 report failed validation.", result);
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
            throw new ReportGenerationException("Failed to fill R08 report from database", e);
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
            throw new ReportGenerationException("Failed to export R08 report to " + format, e);
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

    private Map<String, Object> buildParams(R08ReportRequest r) {
        Map<String, Object> p = new HashMap<>();
        if (r.getYear() != null)  p.put("YEAR", String.valueOf(r.getYear()));
        if (r.getMonth() != null) p.put("MONTH", String.format("%02d", r.getMonth()));
        if (r.getSubmissionYearMonth() != null && !r.getSubmissionYearMonth().isBlank()) {
            p.put("YEAR", r.getSubmissionYearMonth().substring(0, 4));
            p.put("MONTH", r.getSubmissionYearMonth().substring(4, 6));
        }

        String effectiveDateTo = autoDateTo(r.getDateFrom(), r.getDateTo(), r.getTimeFrame());
        if (r.getDateFrom() != null) p.put("INVOICE_DATE_FROM", r.getDateFrom());
        if (effectiveDateTo != null) p.put("INVOICE_DATE_TO", effectiveDateTo);

        ClientLocation seller = resolveClient(r.getSellerClientName(), r.getSellerClientNumber(), r.getSellerLocCode());
        if (seller != null) {
            p.put("SELLER_NUMBER", seller.clientNumber());
            p.put("SELLER_NAME", seller.clientName());
            p.put("SELLER_CLIENT_LOCN_CODE", seller.clientLocnCode());
        }
        ClientLocation buyer = resolveClient(r.getBuyerClientName(), r.getBuyerClientNumber(), r.getBuyerLocCode());
        if (buyer != null) {
            p.put("BUYER_NUMBER", buyer.clientNumber());
            p.put("BUYER_NAME", buyer.clientName());
            p.put("BUYER_CLIENT_LOCN_CODE", buyer.clientLocnCode());
        }

        // R08.jrxml declares MATURITY, INVOICE_TYPE and INVOICE_STATUS but has no
        // SUBMISSION_STATUS parameter (verified against both the JR7-converted and the original
        // pre-conversion export) — nor a TYPE_CODE_MATURITY/TYPE_CODE_MATURITY_DESCRIPTION
        // parameter. R08 has never supported filtering by submission status.
        p.put("MATURITY", r.getMaturityCodes() != null ? r.getMaturityCodes() : "O,S,M");
        p.put("INVOICE_TYPE",   r.getInvoiceType() != null ? r.getInvoiceType() : "ADJ,CAN,PUR,SAL");
        p.put("INVOICE_STATUS", r.getInvoiceStatus() != null ? r.getInvoiceStatus() : "PRO,UNA,APP,CAN,DFT,DVF,REJ,VER");

        if (r.getSubmissionNumber() != null) p.put("SUBMISSION_NUMBER", r.getSubmissionNumber());
        // Prefer the authenticated user (IDIR) from the validated JWT over any client-supplied value.
        String idir = SecurityContextUtils.currentUsername().orElse(r.getUserId());
        if (idir != null)                    p.put("USER_ID", idir);
        return p;
    }

    private ClientLocation resolveClient(String name, String number, String locCode) {
        if (number != null && !number.isBlank()) {
            List<ClientLocation> results = searchService.findClientsByNumber(number);
            if (results.isEmpty()) {
                return null;
            }
            String effectiveLoc = (locCode != null && !locCode.isBlank()) ? locCode : "00";
            return results.stream()
                    .filter(c -> effectiveLoc.equals(c.clientLocnCode()))
                    .findFirst()
                    .orElse(results.get(0));
        }
        if (name != null && !name.isBlank()) {
            List<ClientLocation> results = searchService.findClientsByName(name);
            return results.isEmpty() ? null : results.get(0);
        }
        return null;
    }

    private static String autoDateTo(String dateFrom, String dateTo, String timeFrame) {
        if (dateTo != null || dateFrom == null) return dateTo;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate from = LocalDate.parse(dateFrom, fmt);
        LocalDate end;
        if (timeFrame != null && !timeFrame.isBlank()) {
            try {
                end = from.plusMonths(Integer.parseInt(timeFrame));
            } catch (NumberFormatException e) {
                throw new BadRequestException("timeFrame must be a numeric value");
            }
        } else {
            end = from;
        }
        return YearMonth.from(end).atEndOfMonth().format(fmt);
    }
}
