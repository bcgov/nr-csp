package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R11ReportRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperReportRenderer;
import ca.bc.gov.nrs.csp.backend.service.reporting.ReportFilenames;
import ca.bc.gov.nrs.csp.backend.service.reporting.SubreportInjector;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R11Validator;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class R11Service {

    private static final Logger log = LoggerFactory.getLogger(R11Service.class);

    private static final String PARAM_SUBREPORT_R11_SUB = "SUBREPORT_R11_SUB";
    private static final String PARAM_SUBREPORT_R11_XTAB = "SUBREPORT_R11_XTAB";

    private final JasperReportRenderer renderer;
    private final Clock clock;

    /** Cache of compiled report bundles (main + subreport + xtab subreport) keyed by main template path. */
    private final Map<String, ReportBundle> compiledReportCache = new ConcurrentHashMap<>();

    @Value("${jasper.report.r11.template:/reports/R11.jrxml}")
    private String r11TemplatePath;
    @Value("${jasper.report.r11.csv.template:/reports/R11_CSV.jrxml}")
    private String r11CsvTemplatePath;
    @Value("${jasper.report.r11.subreport.template:/reports/r11_subreport.jrxml}")
    private String r11SubreportPath;
    @Value("${jasper.report.r11.subreport.csv.template:/reports/r11_subreport_CSV.jrxml}")
    private String r11SubreportCsvPath;
    @Value("${jasper.report.r11.xtabsubreport.template:/reports/r11_xtab_subreport.jrxml}")
    private String r11XtabSubreportPath;
    @Value("${jasper.report.r11.xtabsubreport.csv.template:/reports/r11_xtab_subreport_CSV.jrxml}")
    private String r11XtabSubreportCsvPath;

    public R11Service(JasperReportRenderer renderer, Clock clock) {
        this.renderer = renderer;
        this.clock = clock;
    }

    public ReportResult generateReport(R11ReportRequest request) {
        validate(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R11 report format={}", format);
        boolean csv = "CSV".equalsIgnoreCase(format);
        String ext = csv ? "csv" : "pdf";
        String mainPath = csv ? r11CsvTemplatePath : r11TemplatePath;
        String subPath = csv ? r11SubreportCsvPath : r11SubreportPath;
        String xtabPath = csv ? r11XtabSubreportCsvPath : r11XtabSubreportPath;
        String subRepoName = csv ? "r11_subreport_CSV.jrxml" : "r11_subreport.jrxml";
        String xtabRepoName = csv ? "r11_xtab_subreport_CSV.jrxml" : "r11_xtab_subreport.jrxml";

        ReportBundle bundle = compiledReportCache.computeIfAbsent(mainPath,
                path -> compileBundle(path, subPath, xtabPath, subRepoName, xtabRepoName));

        Map<String, Object> params = buildParams(request);
        params.put(PARAM_SUBREPORT_R11_SUB, bundle.sub());
        params.put(PARAM_SUBREPORT_R11_XTAB, bundle.xtab());
        JasperPrint jasperPrint = renderer.fillReport(bundle.main(), params, "R11");

        if (jasperPrint.getPages().isEmpty()) {
            throw new ResourceNotFoundException("The R11 report returned no data for the given parameters.");
        }

        byte[] data = renderer.exportReport(jasperPrint, format, "R11");
        String filename = ReportFilenames.timestamped("R11", ext, clock);
        return new ReportResult(data, filename);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(R11ReportRequest r) {
        ValidationResult result = new R11Validator().validate(r);
        if (result.hasErrors()) {
            throw new ValidationException("R11 report failed validation.", result);
        }
    }

    // ── JRXML loading, compilation and subreport wiring ────────────────────────

    /**
     * Compiles the main report and its two-level subreport chain, rewriting the JasperReports
     * Server-only {@code "repo:name.jrxml"} subreport expressions to reference compiled-report
     * parameters instead (see {@link SubreportInjector}). Compiled deepest-first: the crosstab
     * subreport has no nested subreports of its own, so it compiles unmodified; the mid-level
     * subreport hosts the crosstab subreport, so its own subreport element is rewritten to
     * {@code $P{SUBREPORT_R11_XTAB}}; the main report invokes the mid-level subreport <b>four
     * times</b> (once per maturity-code/blended detail band, each with a different literal
     * {@code TYPE_CODE_MATURITY} override) — {@link SubreportInjector} rewrites every occurrence
     * to the shared {@code $P{SUBREPORT_R11_SUB}} and adds the {@code SUBREPORT_R11_XTAB}
     * pass-through mapping to all four, since {@code SUBREPORT_R11_XTAB} is only used one level
     * deeper but must reach the mid-level subreport through each invocation.
     */
    private ReportBundle compileBundle(String mainPath, String subPath, String xtabPath,
                                        String subRepoName, String xtabRepoName) {
        try {
            JasperReport xtab = renderer.compileReport(renderer.loadTemplate(xtabPath));

            String subJrxml = SubreportInjector.rewriteSubreportExpression(
                    renderer.loadTemplate(subPath), xtabRepoName, PARAM_SUBREPORT_R11_XTAB);
            JasperReport sub = renderer.compileReport(subJrxml);

            String mainJrxml = SubreportInjector.rewriteSubreportExpression(
                    renderer.loadTemplate(mainPath), subRepoName, PARAM_SUBREPORT_R11_SUB);
            mainJrxml = SubreportInjector.addPassThroughParameter(mainJrxml, PARAM_SUBREPORT_R11_SUB, PARAM_SUBREPORT_R11_XTAB);
            JasperReport main = renderer.compileReport(mainJrxml);

            return new ReportBundle(main, sub, xtab);
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to compile JRXML template.", e);
        }
    }

    // ── Parameter building ─────────────────────────────────────────────────────

    private Map<String, Object> buildParams(R11ReportRequest r) {
        Map<String, Object> p = new HashMap<>();
        String effectiveDateTo = autoDateTo(r.getDateFrom(), r.getDateTo(), r.getTimeFrame());
        if (r.getDateFrom() != null) p.put("INVOICE_DATE_FROM", r.getDateFrom());
        if (effectiveDateTo != null) p.put("INVOICE_DATE_TO", effectiveDateTo);

        p.put("BLENDED", r.getBlended() != null ? String.valueOf(r.getBlended()) : "false");
        if (r.getModelingCode() != null) p.put("MODELING_CODE", r.getModelingCode());

        String maturity = r.getMaturityCodes() != null ? r.getMaturityCodes() : "O,S,M";
        String maturityDesc = r.getMaturityDescriptions() != null ? r.getMaturityDescriptions() : buildMaturityDescription(maturity);
        p.put("TYPE_CODE_MATURITY", maturity);
        p.put("TYPE_CODE_MATURITY_DESCRIPTION", maturityDesc);

        // Prefer the authenticated user (IDIR) from the validated JWT over any client-supplied value.
        String idir = SecurityContextUtils.currentUsername().orElse(r.getUserId());
        if (idir != null) p.put("USER_ID", idir);
        return p;
    }

    private static String buildMaturityDescription(String codes) {
        if (codes == null || codes.isBlank()) return "";
        Map<String, String> map = new LinkedHashMap<>();
        map.put("O", "Old Growth"); map.put("S", "Second Growth");
        map.put("M", "Mixed Growth"); map.put("C", "Cants");
        StringBuilder sb = new StringBuilder();
        for (String code : codes.split(",")) {
            String label = map.get(code.trim());
            if (label != null) { if (sb.length() > 0) sb.append(", "); sb.append(label); }
        }
        return sb.toString();
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

    private record ReportBundle(JasperReport main, JasperReport sub, JasperReport xtab) {}
}
