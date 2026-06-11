package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import ca.bc.gov.nrs.csp.backend.util.ToStringUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class R10ReportRequest {

    @NotNull
    private ReportFormat reportFormat = ReportFormat.PDF;

    @NotBlank(message = "dateFrom is required")
    @Pattern(regexp = "^\\d{8}$", message = "dateFrom must be in format yyyyMMdd")
    private String dateFrom;

    @Pattern(regexp = "^\\d{8}$", message = "dateTo must be in format yyyyMMdd")
    private String dateTo;

    @Pattern(regexp = "^\\d+$", message = "timeFrame must be numeric")
    private String timeFrame;
    private String sellerClientNumber;
    private String sellerLocnCode;
    private String buyerClientNumber;
    private String buyerLocnCode;
    private String maturityCodes;
    private String invoiceTypeCode;
    private String userId;

    public ReportFormat getReportFormat() { return reportFormat; }
    public void setReportFormat(ReportFormat reportFormat) { this.reportFormat = reportFormat; }
    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
    public String getTimeFrame() { return timeFrame; }
    public void setTimeFrame(String timeFrame) { this.timeFrame = timeFrame; }
    public String getSellerClientNumber() { return sellerClientNumber; }
    public void setSellerClientNumber(String sellerClientNumber) { this.sellerClientNumber = sellerClientNumber; }
    public String getSellerLocnCode() { return sellerLocnCode; }
    public void setSellerLocnCode(String sellerLocnCode) { this.sellerLocnCode = sellerLocnCode; }
    public String getBuyerClientNumber() { return buyerClientNumber; }
    public void setBuyerClientNumber(String buyerClientNumber) { this.buyerClientNumber = buyerClientNumber; }
    public String getBuyerLocnCode() { return buyerLocnCode; }
    public void setBuyerLocnCode(String buyerLocnCode) { this.buyerLocnCode = buyerLocnCode; }
    public String getMaturityCodes() { return maturityCodes; }
    public void setMaturityCodes(String maturityCodes) { this.maturityCodes = maturityCodes; }
    public String getInvoiceTypeCode() { return invoiceTypeCode; }
    public void setInvoiceTypeCode(String invoiceTypeCode) { this.invoiceTypeCode = invoiceTypeCode; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String toString() {
        return ToStringUtils.toJson(this);
    }
}
