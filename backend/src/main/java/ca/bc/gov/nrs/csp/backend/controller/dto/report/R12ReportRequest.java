package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import ca.bc.gov.nrs.csp.backend.util.ToStringUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class R12ReportRequest {

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
    private String logSaleTypeCode;
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
    public String getLogSaleTypeCode() { return logSaleTypeCode; }
    public void setLogSaleTypeCode(String logSaleTypeCode) { this.logSaleTypeCode = logSaleTypeCode; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String toString() {
        return ToStringUtils.toJson(this);
    }
}
