package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R06ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperServerService;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R06Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class R06Service {

    private static final Logger log = LoggerFactory.getLogger(R06Service.class);

    private final JasperServerService jasperServerService;
    private final SearchService searchService;

    public R06Service(JasperServerService jasperServerService, SearchService searchService) {
        this.jasperServerService = jasperServerService;
        this.searchService = searchService;
    }

    public ReportResult generateReport(R06ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R06 report format={}", format);

        Map<String, Object> params = buildParams(request);
        params.put("RUN_OUTPUT_FORMAT", format);

        byte[] data = jasperServerService.generateReport("R06", params);
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("The R06 report returned no data.");
        }

        String filename = String.format("R06_%s.%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
                request.getReportFormat().getExtension());
        return new ReportResult(data, filename);
    }

    private void validate(R06ReportRequest r) {
        ValidationResult result = new R06Validator(searchService).validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R06 report failed validation.", result);
        }
    }

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
        if (r.getUserId() != null)                 p.put("USER_ID", r.getUserId());
        return p;
    }
}
