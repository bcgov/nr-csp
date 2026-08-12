package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R07ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperReportRenderer;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R07Validator;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class R07Service {

    private static final Logger log = LoggerFactory.getLogger(R07Service.class);

    private final JasperReportRenderer renderer;
    private final SearchService searchService;
    private final CspSubmissionRepository cspSubmissionRepository;

    /** Cache of compiled JasperReport objects keyed by template classpath path. */
    private final Map<String, JasperReport> compiledReportCache = new ConcurrentHashMap<>();

    @Value("${jasper.report.r07.template:/reports/R07.jrxml}")
    private String r07TemplatePath;

    @Value("${jasper.report.r07.csv.template:/reports/R07_CSV.jrxml}")
    private String r07CsvTemplatePath;

    public R07Service(JasperReportRenderer renderer, SearchService searchService,
                      CspSubmissionRepository cspSubmissionRepository) {
        this.renderer = renderer;
        this.searchService = searchService;
        this.cspSubmissionRepository = cspSubmissionRepository;
    }

    public ReportResult generateReport(R07ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R07 report format={}", format);
        String ext = "CSV".equalsIgnoreCase(format) ? "csv" : "pdf";
        String templatePath = "CSV".equalsIgnoreCase(format) ? r07CsvTemplatePath : r07TemplatePath;

        JasperReport jasperReport = compiledReportCache.computeIfAbsent(templatePath, path -> {
            log.info("Compiling JRXML: {}", path);
            return renderer.compileFromClasspath(path);
        });

        Map<String, Object> params = buildParams(request);
        JasperPrint jasperPrint = renderer.fillReport(jasperReport, params, "R07");

        if (jasperPrint.getPages().isEmpty()) {
            throw new ResourceNotFoundException("The R07 report returned no data for the given parameters.");
        }

        byte[] data = renderer.exportReport(jasperPrint, format, "R07");
        String filename = String.format("R07_%s.%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")), ext);
        return new ReportResult(data, filename);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(R07ReportRequest r) {
        ValidationResult result = new R07Validator(cspSubmissionRepository, searchService).validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R07 report failed validation.", result);
        }
    }

    // ── Parameter building ─────────────────────────────────────────────────────

    private Map<String, Object> buildParams(R07ReportRequest r) {
        Map<String, Object> p = new HashMap<>();
        if (r.getYear() != null)  p.put("YEAR", String.valueOf(r.getYear()));
        if (r.getMonth() != null) p.put("MONTH", String.format("%02d", r.getMonth()));

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
        if (r.getShowReplacesAdjusts() != null) p.put("SHOW_REPLACES_ADJUSTS", String.valueOf(r.getShowReplacesAdjusts()));

        // MATURITY is the only maturity parameter R07.jrxml declares — the stored proc computes
        // its own description text; there is no TYPE_CODE_MATURITY/TYPE_CODE_MATURITY_DESCRIPTION
        // parameter in the report design (verified against both the JR7-converted and the
        // original pre-conversion export).
        p.put("MATURITY", r.getMaturityCodes() != null ? r.getMaturityCodes() : "O,S,M");
        p.put("INVOICE_TYPE",      r.getInvoiceType() != null ? r.getInvoiceType() : "ADJ,CAN,PUR,SAL");
        p.put("INVOICE_STATUS",    r.getInvoiceStatus() != null ? r.getInvoiceStatus() : "PRO,UNA,APP,CAN,DFT,DVF,REJ,VER");
        p.put("SUBMISSION_STATUS", r.getSubmissionStatus() != null ? r.getSubmissionStatus() : "COM,INB,LOB,REJ");

        if (r.getSubmissionNumber() != null)    p.put("SUBMISSION_NUMBER", r.getSubmissionNumber());
        if (r.getSubmissionYearMonth() != null) p.put("SUBMISSION_MONTH_YEAR", r.getSubmissionYearMonth());
        // Prefer the authenticated user (IDIR) from the validated JWT over any client-supplied value.
        String idir = SecurityContextUtils.currentUsername().orElse(r.getUserId());
        if (idir != null)                       p.put("USER_ID", idir);
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
