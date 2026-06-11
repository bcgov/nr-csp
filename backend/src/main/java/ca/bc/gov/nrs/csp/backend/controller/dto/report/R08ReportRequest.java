package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import ca.bc.gov.nrs.csp.backend.util.ToStringUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class R08ReportRequest {

    @NotNull
    private ReportFormat reportFormat = ReportFormat.PDF;

    private Integer year;
    private Integer month;

    @Pattern(regexp = "^\\d{8}$", message = "dateFrom must be in format yyyyMMdd")
    private String dateFrom;

    @Pattern(regexp = "^\\d{8}$", message = "dateTo must be in format yyyyMMdd")
    private String dateTo;

    @Pattern(regexp = "^\\d+$", message = "timeFrame must be numeric")
    private String timeFrame;

    private String sellerClientNumber;
    private String sellerClientName;
    private String sellerLocCode;
    private String buyerClientNumber;
    private String buyerClientName;
    private String buyerLocCode;
    private String maturityCodes;
    private String invoiceType;
    private String invoiceStatus;
    private String submissionStatus;

    @Pattern(regexp = "^\\d{1,10}$", message = "submissionNumber must be numeric with at most 10 digits")
    private String submissionNumber;

    @Pattern(regexp = "^\\d{6}$", message = "submissionYearMonth must be in format yyyyMM")
    private String submissionYearMonth;
    private String userId;

    public ReportFormat getReportFormat() { return reportFormat; }
    public void setReportFormat(ReportFormat reportFormat) { this.reportFormat = reportFormat; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
    public String getTimeFrame() { return timeFrame; }
    public void setTimeFrame(String timeFrame) { this.timeFrame = timeFrame; }
    public String getSellerClientNumber() { return sellerClientNumber; }
    public void setSellerClientNumber(String sellerClientNumber) { this.sellerClientNumber = sellerClientNumber; }
    public String getSellerClientName() { return sellerClientName; }
    public void setSellerClientName(String sellerClientName) { this.sellerClientName = sellerClientName; }
    public String getSellerLocCode() { return sellerLocCode; }
    public void setSellerLocCode(String sellerLocCode) { this.sellerLocCode = sellerLocCode; }
    public String getBuyerClientNumber() { return buyerClientNumber; }
    public void setBuyerClientNumber(String buyerClientNumber) { this.buyerClientNumber = buyerClientNumber; }
    public String getBuyerClientName() { return buyerClientName; }
    public void setBuyerClientName(String buyerClientName) { this.buyerClientName = buyerClientName; }
    public String getBuyerLocCode() { return buyerLocCode; }
    public void setBuyerLocCode(String buyerLocCode) { this.buyerLocCode = buyerLocCode; }
    public String getMaturityCodes() { return maturityCodes; }
    public void setMaturityCodes(String maturityCodes) { this.maturityCodes = maturityCodes; }
    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String invoiceType) { this.invoiceType = invoiceType; }
    public String getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(String invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    public String getSubmissionStatus() { return submissionStatus; }
    public void setSubmissionStatus(String submissionStatus) { this.submissionStatus = submissionStatus; }
    public String getSubmissionNumber() { return submissionNumber; }
    public void setSubmissionNumber(String submissionNumber) { this.submissionNumber = submissionNumber; }
    public String getSubmissionYearMonth() { return submissionYearMonth; }
    public void setSubmissionYearMonth(String submissionYearMonth) { this.submissionYearMonth = submissionYearMonth; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String toString() {
        return ToStringUtils.toJson(this);
    }
}
