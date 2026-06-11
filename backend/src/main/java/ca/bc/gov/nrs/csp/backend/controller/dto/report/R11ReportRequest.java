package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import ca.bc.gov.nrs.csp.backend.util.ToStringUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class R11ReportRequest {

    @NotNull
    private ReportFormat reportFormat = ReportFormat.PDF;

    @Pattern(regexp = "^\\d{8}$", message = "dateFrom must be in format yyyyMMdd")
    private String dateFrom;

    @Pattern(regexp = "^\\d{8}$", message = "dateTo must be in format yyyyMMdd")
    private String dateTo;

    @Pattern(regexp = "^\\d+$", message = "timeFrame must be numeric")
    private String timeFrame;
    private Boolean blended;
    private String modelingCode;
    private String maturityCodes;
    private String maturityDescriptions;
    private String userId;

    public ReportFormat getReportFormat() { return reportFormat; }
    public void setReportFormat(ReportFormat reportFormat) { this.reportFormat = reportFormat; }
    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
    public String getTimeFrame() { return timeFrame; }
    public void setTimeFrame(String timeFrame) { this.timeFrame = timeFrame; }
    public Boolean getBlended() { return blended; }
    public void setBlended(Boolean blended) { this.blended = blended; }
    public String getModelingCode() { return modelingCode; }
    public void setModelingCode(String modelingCode) { this.modelingCode = modelingCode; }
    public String getMaturityCodes() { return maturityCodes; }
    public void setMaturityCodes(String maturityCodes) { this.maturityCodes = maturityCodes; }
    public String getMaturityDescriptions() { return maturityDescriptions; }
    public void setMaturityDescriptions(String maturityDescriptions) { this.maturityDescriptions = maturityDescriptions; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String toString() {
        return ToStringUtils.toJson(this);
    }
}
