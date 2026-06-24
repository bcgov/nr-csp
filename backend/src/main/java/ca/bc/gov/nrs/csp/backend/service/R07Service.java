package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R07ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperServerService;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R07Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class R07Service {

    private static final Logger log = LoggerFactory.getLogger(R07Service.class);

    private final JasperServerService jasperServerService;
    private final SearchService searchService;
    private final CspSubmissionRepository cspSubmissionRepository;

    public R07Service(JasperServerService jasperServerService, SearchService searchService,
                      CspSubmissionRepository cspSubmissionRepository) {
        this.jasperServerService = jasperServerService;
        this.searchService = searchService;
        this.cspSubmissionRepository = cspSubmissionRepository;
    }

    public ReportResult generateReport(R07ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R07 report format={}", format);

        Map<String, Object> params = buildParams(request);
        params.put("RUN_OUTPUT_FORMAT", format);

        byte[] data = jasperServerService.generateReport("R07", params);
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("The R07 report returned no data.");
        }

        String filename = String.format("R07_%s.%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
                request.getReportFormat().getExtension());
        return new ReportResult(data, filename);
    }

    private void validate(R07ReportRequest r) {
        ValidationResult result = new R07Validator(cspSubmissionRepository, searchService).validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R07 report failed validation.", result);
        }
    }

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

        String maturity = r.getMaturityCodes() != null ? r.getMaturityCodes() : "O,S,M";
        p.put("MATURITY", maturity);
        p.put("TYPE_CODE_MATURITY", maturity);
        p.put("TYPE_CODE_MATURITY_DESCRIPTION", buildMaturityDescription(maturity));
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

    private static String buildMaturityDescription(String codes) {
        if (codes == null || codes.isBlank()) return "";
        Map<String, String> map = new LinkedHashMap<>();
        map.put("O", "Old Growth"); map.put("S", "Second Growth");
        map.put("M", "Mixed Growth"); map.put("C", "Cants");
        StringBuilder sb = new StringBuilder();
        for (String code : codes.split(",")) {
            String label = map.get(code.trim());
            if (label != null) { if (sb.length() > 0) sb.append(", "); sb.append(label); }
        }
        return sb.toString();
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
