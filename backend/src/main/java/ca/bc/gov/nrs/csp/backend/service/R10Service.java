package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R10ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperServerService;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R10Validator;
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
public class R10Service {

    private static final Logger log = LoggerFactory.getLogger(R10Service.class);

    private final JasperServerService jasperServerService;

    public R10Service(JasperServerService jasperServerService) {
        this.jasperServerService = jasperServerService;
    }

    public ReportResult generateReport(R10ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R10 report format={}", format);

        Map<String, Object> params = buildParams(request);
        params.put("RUN_OUTPUT_FORMAT", format);

        byte[] data = jasperServerService.generateReport("R10", params);
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("The R10 report returned no data.");
        }

        String filename = String.format("R10_%s.%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
                request.getReportFormat().getExtension());
        return new ReportResult(data, filename);
    }

    private void validate(R10ReportRequest r) {
        ValidationResult result = new R10Validator().validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R10 report failed validation.", result);
        }
    }

    private Map<String, Object> buildParams(R10ReportRequest r) {
        Map<String, Object> p = new HashMap<>();
        String effectiveDateTo = autoDateTo(r.getDateFrom(), r.getDateTo(), r.getTimeFrame());
        if (r.getDateFrom() != null)           p.put("INVOICE_DATE_FROM", r.getDateFrom());
        if (effectiveDateTo != null)           p.put("INVOICE_DATE_TO", effectiveDateTo);
        if (r.getTimeFrame() != null)          p.put("TIME_FRAME", r.getTimeFrame());
        if (r.getSellerClientNumber() != null) p.put("SELLER_CLIENT_NUMBER", r.getSellerClientNumber());
        if (r.getSellerLocnCode() != null)     p.put("SELLER_LOCN_CODE", r.getSellerLocnCode());
        if (r.getBuyerClientNumber() != null)  p.put("BUYER_CLIENT_NUMBER", r.getBuyerClientNumber());
        if (r.getBuyerLocnCode() != null)      p.put("BUYER_LOCN_CODE", r.getBuyerLocnCode());
        if (r.getMaturityCodes() != null)      p.put("MATURITY", r.getMaturityCodes());
        if (r.getInvoiceTypeCode() != null)    p.put("INVOICE_TYPE_CODE", r.getInvoiceTypeCode());
        if (r.getUserId() != null)             p.put("USER_ID", r.getUserId());
        return p;
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
