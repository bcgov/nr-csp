package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R12ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperServerService;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R12Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class R12Service {

    private static final Logger log = LoggerFactory.getLogger(R12Service.class);

    private final JasperServerService jasperServerService;

    public R12Service(JasperServerService jasperServerService) {
        this.jasperServerService = jasperServerService;
    }

    public ReportResult generateReport(R12ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R12 report format={}", format);

        Map<String, Object> params = buildParams(request);
        params.put("RUN_OUTPUT_FORMAT", format);

        byte[] data = jasperServerService.generateReport("R12", params);
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("The R12 report returned no data.");
        }

        String filename = String.format("R12_%s.%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
                request.getReportFormat().getExtension());
        return new ReportResult(data, filename);
    }

    private void validate(R12ReportRequest r) {
        ValidationResult result = new R12Validator().validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R12 report failed validation.", result);
        }
    }

    private Map<String, Object> buildParams(R12ReportRequest r) {
        Map<String, Object> p = new HashMap<>();
        if (r.getYear() != null) {
            p.put("YEAR", String.valueOf(r.getYear()));
            if (r.getMonth() != null) p.put("MONTH", String.format("%02d", r.getMonth()));
        } else {
            String effectiveDateTo = autoDateTo(r.getDateFrom(), r.getDateTo(), r.getTimeFrame());
            if (r.getDateFrom() != null) p.put("INVOICE_DATE_FROM", firstDayOfMonth(r.getDateFrom()));
            if (effectiveDateTo != null) p.put("INVOICE_DATE_TO", lastDayOfMonth(effectiveDateTo));
            if (r.getTimeFrame() != null) p.put("TIME_FRAME", r.getTimeFrame());
        }
        if (r.getLogSaleTypeCode() != null) p.put("LOG_SALE_TYPE_CODE", r.getLogSaleTypeCode());
        // Prefer the authenticated user (IDIR) from the validated JWT over any client-supplied value.
        String idir = SecurityContextUtils.currentUsername().orElse(r.getUserId());
        if (idir != null)                   p.put("USER_ID", idir);
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
