package ca.bc.gov.nrs.csp.backend.service.reporting;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;

import java.time.Clock;
import java.util.Map;

/**
 * Shared orchestration for the JasperReports-Server-backed reports (R06–R12).
 * These services differ only in their report code, {@code validate}, and
 * {@code buildParams}; the submit → null-check → filename → result flow is
 * identical and lives here once rather than being duplicated in each service.
 *
 * <p>R13 is intentionally not a client — it drives the JasperReports engine
 * directly rather than the REST server.
 */
public final class JasperReportRunner {

    private JasperReportRunner() {
    }

    /**
     * Submits the report to JasperReports Server and wraps the result.
     *
     * @param jasperServerService the REST client (passed in so this stays a pure helper)
     * @param clock               business-zone clock for the filename timestamp
     * @param reportCode          report code, e.g. {@code "R06"}
     * @param format              the requested output format (PDF/CSV)
     * @param params              report parameters; {@code RUN_OUTPUT_FORMAT} is added here
     * @return the report bytes plus a timestamped filename
     * @throws ResourceNotFoundException if the server returns no data
     */
    public static ReportResult run(JasperServerService jasperServerService, Clock clock,
                                   String reportCode, ReportFormat format, Map<String, Object> params) {
        params.put("RUN_OUTPUT_FORMAT", format.getValue());

        byte[] data = jasperServerService.generateReport(reportCode, params);
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("The " + reportCode + " report returned no data.");
        }

        return new ReportResult(data, ReportFilenames.timestamped(reportCode, format.getExtension(), clock));
    }
}
