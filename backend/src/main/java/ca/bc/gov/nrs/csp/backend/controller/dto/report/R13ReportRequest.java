package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import ca.bc.gov.nrs.csp.backend.util.ToStringUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class R13ReportRequest {

    @NotNull
    @Size(min = 1, max = 100)
    private String reportName;

    @NotNull
    private ReportFormat reportFormat = ReportFormat.PDF;

    private R13ShowOptions showOptions;

    @Pattern(regexp = "^\\d{8}$", message = "must be in yyyyMMdd format")
    private String invoiceDateFrom;

    @Pattern(regexp = "^\\d{8}$", message = "must be in yyyyMMdd format")
    private String invoiceDateTo;

    @Pattern(regexp = "^(0?[1-9]|1[0-2])$", message = "timeFrame must be between 01 and 12")
    private String timeFrame;

    private List<@Size(max = 15) String> invoiceNumbers = new ArrayList<>();
    @Size(max = 15) private String invoiceNumberFrom;
    @Size(max = 15) private String invoiceNumberTo;
    private List<String> invoiceStatus = new ArrayList<>();
    private List<@Size(max = 15) String> invoiceReplacesAdjusts = new ArrayList<>();
    @Size(max = 15) private String invoiceReplacesAdjustsFrom;
    @Size(max = 15) private String invoiceReplacesAdjustsTo;
    private List<@Size(max = 15) String> invoiceBoomNumbers = new ArrayList<>();
    @Size(max = 15) private String invoiceBoomNumberFrom;
    @Size(max = 15) private String invoiceBoomNumberTo;
    private List<@Size(max = 15) String> invoiceTimberMarks = new ArrayList<>();
    @Size(max = 15) private String invoiceTimberMarkFrom;
    @Size(max = 15) private String invoiceTimberMarkTo;
    private List<@Size(max = 15) String> invoiceWeighSlips = new ArrayList<>();
    @Size(max = 15) private String invoiceWeighSlipFrom;
    @Size(max = 15) private String invoiceWeighSlipTo;
    private List<String> invoiceTypes = new ArrayList<>();
    private List<String> maturityCodes = new ArrayList<>();
    private List<String> species = new ArrayList<>();
    private List<String> sortCodes = new ArrayList<>();
    private List<String> grades = new ArrayList<>();
    private String submissionMonthYear;
    private List<String> submissionStatus = new ArrayList<>();
    @Pattern(regexp = "^\\d+$", message = "submissionNumber must be numeric")
    private String submissionNumber;
    private String entryUserId;
    private List<@Pattern(regexp = "Electronic|Manual", message = "must be 'Electronic' or 'Manual'") String> submissionTypes = new ArrayList<>();
    private String approvalMonthYear;
    private List<String> approvedBy = new ArrayList<>();
    private String sellerName;
    private String buyerName;
    private List<String> sellerNumbers = new ArrayList<>();
    private List<String> buyerNumbers = new ArrayList<>();
    private List<String> sellerClientLocnCodes = new ArrayList<>();
    private List<String> buyerClientLocnCodes = new ArrayList<>();
    private String userId;
    private String userName;

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public ReportFormat getReportFormat() { return reportFormat; }
    public void setReportFormat(ReportFormat reportFormat) { this.reportFormat = reportFormat; }
    public R13ShowOptions getShowOptions() { return showOptions; }
    public void setShowOptions(R13ShowOptions showOptions) { this.showOptions = showOptions; }
    public String getInvoiceDateFrom() { return invoiceDateFrom; }
    public void setInvoiceDateFrom(String invoiceDateFrom) { this.invoiceDateFrom = invoiceDateFrom; }
    public String getInvoiceDateTo() { return invoiceDateTo; }
    public void setInvoiceDateTo(String invoiceDateTo) { this.invoiceDateTo = invoiceDateTo; }
    public String getTimeFrame() { return timeFrame; }
    public void setTimeFrame(String timeFrame) { this.timeFrame = timeFrame; }
    public List<String> getInvoiceNumbers() { return invoiceNumbers; }
    public void setInvoiceNumbers(List<String> invoiceNumbers) { this.invoiceNumbers = invoiceNumbers; }
    public String getInvoiceNumberFrom() { return invoiceNumberFrom; }
    public void setInvoiceNumberFrom(String invoiceNumberFrom) { this.invoiceNumberFrom = invoiceNumberFrom; }
    public String getInvoiceNumberTo() { return invoiceNumberTo; }
    public void setInvoiceNumberTo(String invoiceNumberTo) { this.invoiceNumberTo = invoiceNumberTo; }
    public List<String> getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(List<String> invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    public List<String> getInvoiceReplacesAdjusts() { return invoiceReplacesAdjusts; }
    public void setInvoiceReplacesAdjusts(List<String> invoiceReplacesAdjusts) { this.invoiceReplacesAdjusts = invoiceReplacesAdjusts; }
    public String getInvoiceReplacesAdjustsFrom() { return invoiceReplacesAdjustsFrom; }
    public void setInvoiceReplacesAdjustsFrom(String invoiceReplacesAdjustsFrom) { this.invoiceReplacesAdjustsFrom = invoiceReplacesAdjustsFrom; }
    public String getInvoiceReplacesAdjustsTo() { return invoiceReplacesAdjustsTo; }
    public void setInvoiceReplacesAdjustsTo(String invoiceReplacesAdjustsTo) { this.invoiceReplacesAdjustsTo = invoiceReplacesAdjustsTo; }
    public List<String> getInvoiceBoomNumbers() { return invoiceBoomNumbers; }
    public void setInvoiceBoomNumbers(List<String> invoiceBoomNumbers) { this.invoiceBoomNumbers = invoiceBoomNumbers; }
    public String getInvoiceBoomNumberFrom() { return invoiceBoomNumberFrom; }
    public void setInvoiceBoomNumberFrom(String invoiceBoomNumberFrom) { this.invoiceBoomNumberFrom = invoiceBoomNumberFrom; }
    public String getInvoiceBoomNumberTo() { return invoiceBoomNumberTo; }
    public void setInvoiceBoomNumberTo(String invoiceBoomNumberTo) { this.invoiceBoomNumberTo = invoiceBoomNumberTo; }
    public List<String> getInvoiceTimberMarks() { return invoiceTimberMarks; }
    public void setInvoiceTimberMarks(List<String> invoiceTimberMarks) { this.invoiceTimberMarks = invoiceTimberMarks; }
    public String getInvoiceTimberMarkFrom() { return invoiceTimberMarkFrom; }
    public void setInvoiceTimberMarkFrom(String invoiceTimberMarkFrom) { this.invoiceTimberMarkFrom = invoiceTimberMarkFrom; }
    public String getInvoiceTimberMarkTo() { return invoiceTimberMarkTo; }
    public void setInvoiceTimberMarkTo(String invoiceTimberMarkTo) { this.invoiceTimberMarkTo = invoiceTimberMarkTo; }
    public List<String> getInvoiceWeighSlips() { return invoiceWeighSlips; }
    public void setInvoiceWeighSlips(List<String> invoiceWeighSlips) { this.invoiceWeighSlips = invoiceWeighSlips; }
    public String getInvoiceWeighSlipFrom() { return invoiceWeighSlipFrom; }
    public void setInvoiceWeighSlipFrom(String invoiceWeighSlipFrom) { this.invoiceWeighSlipFrom = invoiceWeighSlipFrom; }
    public String getInvoiceWeighSlipTo() { return invoiceWeighSlipTo; }
    public void setInvoiceWeighSlipTo(String invoiceWeighSlipTo) { this.invoiceWeighSlipTo = invoiceWeighSlipTo; }
    public List<String> getInvoiceTypes() { return invoiceTypes; }
    public void setInvoiceTypes(List<String> invoiceTypes) { this.invoiceTypes = invoiceTypes; }
    public List<String> getMaturityCodes() { return maturityCodes; }
    public void setMaturityCodes(List<String> maturityCodes) { this.maturityCodes = maturityCodes; }
    public List<String> getSpecies() { return species; }
    public void setSpecies(List<String> species) { this.species = species; }
    public List<String> getSortCodes() { return sortCodes; }
    public void setSortCodes(List<String> sortCodes) { this.sortCodes = sortCodes; }
    public List<String> getGrades() { return grades; }
    public void setGrades(List<String> grades) { this.grades = grades; }
    public String getSubmissionMonthYear() { return submissionMonthYear; }
    public void setSubmissionMonthYear(String submissionMonthYear) { this.submissionMonthYear = submissionMonthYear; }
    public List<String> getSubmissionStatus() { return submissionStatus; }
    public void setSubmissionStatus(List<String> submissionStatus) { this.submissionStatus = submissionStatus; }
    public String getSubmissionNumber() { return submissionNumber; }
    public void setSubmissionNumber(String submissionNumber) { this.submissionNumber = submissionNumber; }
    public String getEntryUserId() { return entryUserId; }
    public void setEntryUserId(String entryUserId) { this.entryUserId = entryUserId; }
    public List<String> getSubmissionTypes() { return submissionTypes; }
    public void setSubmissionTypes(List<String> submissionTypes) { this.submissionTypes = submissionTypes; }
    public String getApprovalMonthYear() { return approvalMonthYear; }
    public void setApprovalMonthYear(String approvalMonthYear) { this.approvalMonthYear = approvalMonthYear; }
    public List<String> getApprovedBy() { return approvedBy; }
    public void setApprovedBy(List<String> approvedBy) { this.approvedBy = approvedBy; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public List<String> getSellerNumbers() { return sellerNumbers; }
    public void setSellerNumbers(List<String> sellerNumbers) { this.sellerNumbers = sellerNumbers; }
    public List<String> getBuyerNumbers() { return buyerNumbers; }
    public void setBuyerNumbers(List<String> buyerNumbers) { this.buyerNumbers = buyerNumbers; }
    public List<String> getSellerClientLocnCodes() { return sellerClientLocnCodes; }
    public void setSellerClientLocnCodes(List<String> sellerClientLocnCodes) { this.sellerClientLocnCodes = sellerClientLocnCodes; }
    public List<String> getBuyerClientLocnCodes() { return buyerClientLocnCodes; }
    public void setBuyerClientLocnCodes(List<String> buyerClientLocnCodes) { this.buyerClientLocnCodes = buyerClientLocnCodes; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    @Override
    public String toString() {
        return ToStringUtils.toJson(this);
    }
}
