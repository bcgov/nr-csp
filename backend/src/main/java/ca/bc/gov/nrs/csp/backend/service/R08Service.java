package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R08ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperReportRenderer;
import ca.bc.gov.nrs.csp.backend.service.reporting.ReportFilenames;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R08Validator;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class R08Service {

    private static final Logger log = LoggerFactory.getLogger(R08Service.class);

    private final JasperReportRenderer renderer;
    private final SearchService searchService;
    private final Clock clock;

    /** Cache of compiled JasperReport objects keyed by template classpath path. */
    private final Map<String, JasperReport> compiledReportCache = new ConcurrentHashMap<>();

    @Value("${jasper.report.r08.template:/reports/R08.jrxml}")
    private String r08TemplatePath;

    @Value("${jasper.report.r08.csv.template:/reports/R08_CSV.jrxml}")
    private String r08CsvTemplatePath;

    public R08Service(JasperReportRenderer renderer, SearchService searchService, Clock clock) {
        this.renderer = renderer;
        this.searchService = searchService;
        this.clock = clock;
    }

    public ReportResult generateReport(R08ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R08 report format={}", format);
        String ext = "CSV".equalsIgnoreCase(format) ? "csv" : "pdf";
        String templatePath = "CSV".equalsIgnoreCase(format) ? r08CsvTemplatePath : r08TemplatePath;

        JasperReport jasperReport = compiledReportCache.computeIfAbsent(templatePath, path -> {
            log.info("Compiling JRXML: {}", path);
            return renderer.compileFromClasspath(path);
        });

        Map<String, Object> params = buildParams(request);
        JasperPrint jasperPrint = renderer.fillReport(jasperReport, params, "R08");

        if (jasperPrint.getPages().isEmpty()) {
            throw new ResourceNotFoundException("The R08 report returned no data for the given parameters.");
        }

        byte[] data = renderer.exportReport(jasperPrint, format, "R08");
        String filename = ReportFilenames.timestamped("R08", ext, clock);
        return new ReportResult(data, filename);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(R08ReportRequest r) {
        ValidationResult result = new R08Validator(searchService).validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R08 report failed validation.", result);
        }
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
