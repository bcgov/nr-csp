package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R11ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperServerService;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R11Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class R11Service {

    private static final Logger log = LoggerFactory.getLogger(R11Service.class);

    private final JasperServerService jasperServerService;

    public R11Service(JasperServerService jasperServerService) {
        this.jasperServerService = jasperServerService;
    }

    public ReportResult generateReport(R11ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R11 report format={}", format);

        Map<String, Object> params = buildParams(request);
        params.put("RUN_OUTPUT_FORMAT", format);

        byte[] data = jasperServerService.generateReport("R11", params);
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("The R11 report returned no data.");
        }

        String filename = String.format("R11_%s.%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
                request.getReportFormat().getExtension());
        return new ReportResult(data, filename);
    }

    private void validate(R11ReportRequest r) {
        ValidationResult result = new R11Validator().validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R11 report failed validation.", result);
        }
    }

    private Map<String, Object> buildParams(R11ReportRequest r) {
        Map<String, Object> p = new HashMap<>();
        String effectiveDateTo = autoDateTo(r.getDateFrom(), r.getDateTo(), r.getTimeFrame());
        if (r.getDateFrom() != null) p.put("INVOICE_DATE_FROM", r.getDateFrom());
        if (effectiveDateTo != null) p.put("INVOICE_DATE_TO", effectiveDateTo);

        p.put("BLENDED", r.getBlended() != null ? String.valueOf(r.getBlended()) : "false");
        if (r.getModelingCode() != null) p.put("MODELING_CODE", r.getModelingCode());

        String maturity = r.getMaturityCodes() != null ? r.getMaturityCodes() : "O,S,M";
        String maturityDesc = r.getMaturityDescriptions() != null ? r.getMaturityDescriptions() : buildMaturityDescription(maturity);
        p.put("TYPE_CODE_MATURITY", maturity);
        p.put("TYPE_CODE_MATURITY_DESCRIPTION", maturityDesc);

        if (r.getUserId() != null) p.put("USER_ID", r.getUserId());
        return p;
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
