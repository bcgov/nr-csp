package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import ca.bc.gov.nrs.csp.backend.util.ToStringUtils;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class R06ReportRequest {

    @NotNull
    private ReportFormat reportFormat = ReportFormat.PDF;

    @Pattern(regexp = "^\\d{8}$", message = "dateFrom must be in format yyyyMMdd")
    private String dateFrom;

    @Pattern(regexp = "^\\d{8}$", message = "dateTo must be in format yyyyMMdd")
    private String dateTo;
    private String sellerClientNumber;
    private String sellerLocCode;
    private String buyerClientNumber;
    private String buyerLocCode;
    private String maturityCodes;

    @Digits(integer = 10, fraction = 0, message = "submissionId must be at most 10 digits")
    private Long submissionId;
    private String invoiceNumbers;
    private String logSaleEntryStatusCode;
    private String cspInvoiceTypeCode;
    private String userId;

    public ReportFormat getReportFormat() { return reportFormat; }
    public void setReportFormat(ReportFormat reportFormat) { this.reportFormat = reportFormat; }
    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
    public String getSellerClientNumber() { return sellerClientNumber; }
    public void setSellerClientNumber(String sellerClientNumber) { this.sellerClientNumber = sellerClientNumber; }
    public String getSellerLocCode() { return sellerLocCode; }
    public void setSellerLocCode(String sellerLocCode) { this.sellerLocCode = sellerLocCode; }
    public String getBuyerClientNumber() { return buyerClientNumber; }
    public void setBuyerClientNumber(String buyerClientNumber) { this.buyerClientNumber = buyerClientNumber; }
    public String getBuyerLocCode() { return buyerLocCode; }
    public void setBuyerLocCode(String buyerLocCode) { this.buyerLocCode = buyerLocCode; }
    public String getMaturityCodes() { return maturityCodes; }
    public void setMaturityCodes(String maturityCodes) { this.maturityCodes = maturityCodes; }
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    public String getInvoiceNumbers() { return invoiceNumbers; }
    public void setInvoiceNumbers(String invoiceNumbers) { this.invoiceNumbers = invoiceNumbers; }
    public String getLogSaleEntryStatusCode() { return logSaleEntryStatusCode; }
    public void setLogSaleEntryStatusCode(String logSaleEntryStatusCode) { this.logSaleEntryStatusCode = logSaleEntryStatusCode; }
    public String getCspInvoiceTypeCode() { return cspInvoiceTypeCode; }
    public void setCspInvoiceTypeCode(String cspInvoiceTypeCode) { this.cspInvoiceTypeCode = cspInvoiceTypeCode; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String toString() {
        return ToStringUtils.toJson(this);
    }
}
