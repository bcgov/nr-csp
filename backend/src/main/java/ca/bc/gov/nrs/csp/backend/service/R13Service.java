package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R13ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R13ShowOptions;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.reports.R13Validator;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleCsvExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class R13Service {

    private static final Logger log = LoggerFactory.getLogger(R13Service.class);

    private static final int DEFAULT_TABLE_WIDTH = 800;

    private final DataSource dataSource;
    private final LookupService lookupService;
    private final SearchService searchService;

    /** Cache of compiled JasperReport objects keyed by {@code templatePath:showOptionsBitmask}. */
    private final Map<String, JasperReport> compiledReportCache = new ConcurrentHashMap<>();

    @Value("${jasper.report.r13.template:/reports/R13.jrxml}")
    private String r13TemplatePath;

    @Value("${jasper.report.r13.csv.template:/reports/R13_CSV.jrxml}")
    private String r13CsvTemplatePath;

    public R13Service(@org.springframework.beans.factory.annotation.Autowired(required = false) DataSource dataSource,
                      LookupService lookupService,
                      SearchService searchService) {
        this.dataSource = dataSource;
        this.lookupService = lookupService;
        this.searchService = searchService;
    }

    public ReportResult generateReport(R13ReportRequest request) {
        validate(request);
        String resolvedDateTo = resolveInvoiceDateTo(request);
        String format = request.getReportFormat().getValue();
        log.info("Generating R13 report format={}", format);
        String ext = "CSV".equalsIgnoreCase(format) ? "csv" : "pdf";
        String filename = String.format("R13_%s.%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")), ext);

        R13ShowOptions showOptions = request.getShowOptions() != null
                ? request.getShowOptions() : new R13ShowOptions();
        String templatePath = "CSV".equalsIgnoreCase(format) ? r13CsvTemplatePath : r13TemplatePath;
        String cacheKey = templatePath + ":" + showOptions.toCacheKey();

        JasperReport jasperReport = compiledReportCache.computeIfAbsent(cacheKey, key -> {
            try {
                log.info("Compiling JRXML for cache key: {}", key);
                String jrxml = loadTemplate(templatePath);
                jrxml = modifyTemplateContent(jrxml, showOptions.toShowMap());
                return compileReport(jrxml);
            } catch (Exception e) {
                throw new ReportGenerationException("Failed to compile JRXML template.", e);
            }
        });

        Map<String, Object> params = buildReportParameters(request, resolvedDateTo);
        JasperPrint jasperPrint = fillReport(jasperReport, params);

        if (jasperPrint.getPages().isEmpty()) {
            throw new ResourceNotFoundException("The R13 report returned no data for the given parameters.");
        }

        byte[] data = exportReport(jasperPrint, format);
        return new ReportResult(data, filename);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(R13ReportRequest r) {
        ValidationResult result = new R13Validator(searchService).validate(r);
        if (result.hasErrors()) {
            throw new ValidationException(result.errors().get(0).messageKey(), result);
        }
    }

    private String resolveInvoiceDateTo(R13ReportRequest r) {
        if (r.getInvoiceDateTo() != null) {
            return r.getInvoiceDateTo();
        }
        if (r.getInvoiceDateFrom() == null) {
            return null;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate from = LocalDate.parse(r.getInvoiceDateFrom(), fmt);
        if (r.getTimeFrame() != null && !r.getTimeFrame().isBlank()) {
            return from.plusMonths(Integer.parseInt(r.getTimeFrame()))
                       .with(TemporalAdjusters.lastDayOfMonth())
                       .format(fmt);
        }
        return from.with(TemporalAdjusters.lastDayOfMonth()).format(fmt);
    }

    // ── JRXML loading and compilation ─────────────────────────────────────────

    private String loadTemplate(String templatePath) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(templatePath)) {
            if (stream == null) {
                throw new IOException("JRXML template not found on classpath: " + templatePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private JasperReport compileReport(String jrxmlContent) throws JRException {
        try (InputStream stream = new ByteArrayInputStream(jrxmlContent.getBytes(StandardCharsets.UTF_8))) {
            return JasperCompileManager.compileReport(stream);
        } catch (IOException e) {
            throw new JRException("Failed to compile JRXML template", e);
        }
    }

    // ── JRXML column show/hide ──────────────────────────────────────────────

    private String modifyTemplateContent(String jrxmlContent, Map<String, Boolean> showParams)
            throws Exception {
        Document doc = DocumentHelper.parseText(jrxmlContent);
        for (Map.Entry<String, Boolean> entry : showParams.entrySet()) {
            String fieldName = entry.getKey().replaceFirst("^SHOW_", "");
            if (Boolean.FALSE.equals(entry.getValue())) {
                doc = removeUnshownField(doc, fieldName);
            }
        }
        return relocateFields(doc, DEFAULT_TABLE_WIDTH).asXML();
    }

    private Document removeUnshownField(Document doc, String fieldName) {
        List<Element> nodes = new ArrayList<>();
        nodes.addAll(findReportElements(doc, "columnHeader", "staticText", fieldName));
        nodes.addAll(findReportElements(doc, "detail",       "textField",  fieldName));
        nodes.addAll(findReportElements(doc, "summary",      "textField",  fieldName));

        if ("VOLUME".equals(fieldName) || "PIECES".equals(fieldName) || "AMOUNT".equals(fieldName)) {
            String totalKey = "TOTAL_" + fieldName;
            nodes.addAll(findReportElements(doc, "columnHeader", "staticText", totalKey));
            nodes.addAll(findReportElements(doc, "detail",       "textField",  totalKey));
            nodes.addAll(findReportElements(doc, "summary",      "textField",  totalKey));
        }

        if (nodes.isEmpty()) log.warn("No JRXML node found for key '{}'", fieldName);
        for (Element node : nodes) {
            Element parent = node.getParent();
            if (parent != null) parent.remove(node);
        }
        return doc;
    }

    private Document relocateFields(Document doc, int width) {
        List<Element> allHeaderREs = findReportElements(doc, "columnHeader", "staticText", null);
        List<Object[]> pairs = new ArrayList<>();
        for (Element re : allHeaderREs) {
            String key = re.attributeValue("key");
            String xStr = re.attributeValue("x");
            if (key != null && xStr != null) {
                try { pairs.add(new Object[]{key, Integer.parseInt(xStr)}); }
                catch (NumberFormatException ignored) {}
            }
        }
        pairs.sort((a, b) -> Integer.compare((Integer) a[1], (Integer) b[1]));

        int m = pairs.isEmpty() ? 1 : pairs.size();
        int colWidth = width / m;
        int index = 0;
        int leftmostSummaryX = 2 * width;

        for (Object[] pair : pairs) {
            String key = (String) pair[0];
            List<Element> headerNs  = findReportElements(doc, "columnHeader", "staticText", key);
            List<Element> detailNs  = findReportElements(doc, "detail",       "textField",  key);
            List<Element> summaryNs = findReportElements(doc, "summary",      "textField",  key);

            List<Element> allNs = new ArrayList<>();
            allNs.addAll(headerNs); allNs.addAll(detailNs); allNs.addAll(summaryNs);

            if ("VOLUME".equals(key) || "PIECES".equals(key) || "AMOUNT".equals(key)) {
                String totalKey = "TOTAL_" + key;
                allNs.addAll(findReportElements(doc, "columnHeader", "staticText", totalKey));
                allNs.addAll(findReportElements(doc, "detail",       "textField",  totalKey));
                allNs.addAll(findReportElements(doc, "summary",      "textField",  totalKey));
            }

            for (Element n : allNs) {
                int newX = colWidth * index;
                if (!summaryNs.contains(n)) {
                    n.addAttribute("x", String.valueOf(newX));
                } else {
                    if (newX > 0) {
                        n.addAttribute("x", String.valueOf(newX));
                        if (leftmostSummaryX > newX) leftmostSummaryX = newX;
                    } else {
                        n.addAttribute("x", "0");
                        leftmostSummaryX = 0;
                    }
                }
            }
            index++;
        }

        List<Element> totalLabelNs = findReportElements(doc, "summary", "staticText", "TOTAL_LABEL");
        if (!totalLabelNs.isEmpty()) {
            Element labelEl = totalLabelNs.get(0);
            String wStr = labelEl.attributeValue("width");
            if (wStr != null) {
                try { labelEl.addAttribute("x", String.valueOf(leftmostSummaryX - Integer.parseInt(wStr))); }
                catch (NumberFormatException ignored) {}
            }
        }
        return doc;
    }

    private List<Element> findReportElements(Document doc, String section, String elementKind, String key) {
        List<Element> result = new ArrayList<>();
        Element root = doc.getRootElement();
        for (Element sec : childrenByName(root, section)) {
            collectKindElements(sec, elementKind, key, result);
            for (Element band : childrenByName(sec, "band")) {
                collectKindElements(band, elementKind, key, result);
            }
        }
        return result;
    }

    private void collectKindElements(Element parent, String elementKind, String key, List<Element> result) {
        for (java.util.Iterator<Element> it = parent.elementIterator(); it.hasNext(); ) {
            Element child = it.next();
            if ("element".equals(child.getName()) && elementKind.equals(child.attributeValue("kind"))) {
                String childKey = child.attributeValue("key");
                if (key == null ? childKey != null : key.equals(childKey)) {
                    result.add(child);
                }
            }
        }
    }

    private List<Element> childrenByName(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        for (java.util.Iterator<Element> it = parent.elementIterator(); it.hasNext(); ) {
            Element child = it.next();
            if (localName.equals(child.getName())) result.add(child);
        }
        return result;
    }

    // ── Report fill / export ───────────────────────────────────────────────────

    private JasperPrint fillReport(JasperReport report, Map<String, Object> params) {
        if (dataSource == null) {
            throw new ReportGenerationException("R13 report requires a database connection. No DataSource configured.", null);
        }
        try (Connection conn = dataSource.getConnection()) {
            return JasperFillManager.fillReport(report, params, conn);
        } catch (JRException | SQLException e) {
            throw new ReportGenerationException("Failed to fill R13 report from database", e);
        }
    }

    private byte[] exportReport(JasperPrint jasperPrint, String format) {
        try {
            return switch (format.toLowerCase()) {
                case "pdf" -> JasperExportManager.exportReportToPdf(jasperPrint);
                case "csv" -> exportToCsv(jasperPrint);
                default -> throw new ReportGenerationException("Unsupported report format: " + format, null);
            };
        } catch (JRException e) {
            throw new ReportGenerationException("Failed to export R13 report to " + format, e);
        }
    }

    private byte[] exportToCsv(JasperPrint jasperPrint) throws JRException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JRCsvExporter exporter = new JRCsvExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(out));
        exporter.setConfiguration(new SimpleCsvExporterConfiguration());
        exporter.exportReport();
        return out.toByteArray();
    }

    // ── Parameter building ─────────────────────────────────────────────────────

    private Map<String, Object> buildReportParameters(R13ReportRequest r, String resolvedDateTo) {
        // Normalise free-text search identifiers to uppercase to match DB storage
        List<String> invoiceNums = upperNullIfEmpty(r.getInvoiceNumbers());
        String invNumFrom        = upperNullIfBlank(r.getInvoiceNumberFrom());
        String invNumTo          = upperNullIfBlank(r.getInvoiceNumberTo());
        List<String> repAdj      = upperNullIfEmpty(r.getInvoiceReplacesAdjusts());
        String repAdjFrom        = upperNullIfBlank(r.getInvoiceReplacesAdjustsFrom());
        String repAdjTo          = upperNullIfBlank(r.getInvoiceReplacesAdjustsTo());
        List<String> boomNums    = upperNullIfEmpty(r.getInvoiceBoomNumbers());
        String boomNumFrom       = upperNullIfBlank(r.getInvoiceBoomNumberFrom());
        String boomNumTo         = upperNullIfBlank(r.getInvoiceBoomNumberTo());
        List<String> timberMarks = upperNullIfEmpty(r.getInvoiceTimberMarks());
        String timberMarkFrom    = upperNullIfBlank(r.getInvoiceTimberMarkFrom());
        String timberMarkTo      = upperNullIfBlank(r.getInvoiceTimberMarkTo());
        List<String> weighSlips  = upperNullIfEmpty(r.getInvoiceWeighSlips());
        String weighSlipFrom     = upperNullIfBlank(r.getInvoiceWeighSlipFrom());
        String weighSlipTo       = upperNullIfBlank(r.getInvoiceWeighSlipTo());

        Map<String, Object> p = new HashMap<>();

        // Prefer the authenticated user (IDIR) from the validated JWT over any client-supplied value.
        String idir = SecurityContextUtils.currentUsername().orElse(r.getUserId());
        p.put("USER_ID",      idir);
        p.put("USER_NAME",    r.getUserName() != null ? r.getUserName() : idir);
        p.put("REPORT_NAME",  r.getReportName());

        p.put("INVOICE_DATE_FROM", r.getInvoiceDateFrom());
        p.put("INVOICE_DATE_TO",   resolvedDateTo);

        p.put("INVOICE_NUMBER",      invoiceNums);
        p.put("INVOICE_NUMBER_FROM", invNumFrom);
        p.put("INVOICE_NUMBER_TO",   invNumTo);
        p.put("INVOICE_NUMBER_DESC", buildRangeDesc(invoiceNums, invNumFrom, invNumTo));

        // When a multi-select filter is empty the user did not restrict by that field, which
        // means "all values".  We populate with every valid code from the lookup table so that
        // (a) the Jasper IN-clause still matches all rows, and (b) the report header shows the
        // full list of values exactly as the legacy app did.
        p.put("INVOICE_STATUS", allCodesIfEmpty(r.getInvoiceStatus(), lookupService::getInvoiceStatuses));

        p.put("INVOICE_REPLACES_ADJUSTS",      repAdj);
        p.put("INVOICE_REPLACES_ADJUSTS_FROM", repAdjFrom);
        p.put("INVOICE_REPLACES_ADJUSTS_TO",   repAdjTo);
        p.put("INVOICE_REPLACES_ADJUSTS_DESC", buildRangeDesc(repAdj, repAdjFrom, repAdjTo));
        p.put("INVOICE_REPLACES_ADJUSTS_SQL",  buildRangeSql("rep_adj.rep_adj_inv_id", repAdj, repAdjFrom, repAdjTo));

        p.put("INVOICE_BOOM_NUMBER",      boomNums);
        p.put("INVOICE_BOOM_NUMBER_FROM", boomNumFrom);
        p.put("INVOICE_BOOM_NUMBER_TO",   boomNumTo);
        p.put("INVOICE_BOOM_NUMBER_DESC", buildRangeDesc(boomNums, boomNumFrom, boomNumTo));
        p.put("INVOICE_BOOM_NUMBER_SQL",  buildRangeSql("boom_nums.boom_num_src", boomNums, boomNumFrom, boomNumTo));

        p.put("INVOICE_TIMBER_MARK",      timberMarks);
        p.put("INVOICE_TIMBER_MARK_FROM", timberMarkFrom);
        p.put("INVOICE_TIMBER_MARK_TO",   timberMarkTo);
        p.put("INVOICE_TIMBER_MARK_DESC", buildRangeDesc(timberMarks, timberMarkFrom, timberMarkTo));
        p.put("INVOICE_TIMBER_MARK_SQL",  buildRangeSql("tmbr_mks.tmbr_mks_src", timberMarks, timberMarkFrom, timberMarkTo));

        p.put("INVOICE_WEIGH_SLIP",      weighSlips);
        p.put("INVOICE_WEIGH_SLIP_FROM", weighSlipFrom);
        p.put("INVOICE_WEIGH_SLIP_TO",   weighSlipTo);
        p.put("INVOICE_WEIGH_SLIP_DESC", buildRangeDesc(weighSlips, weighSlipFrom, weighSlipTo));
        p.put("INVOICE_WEIGH_SLIP_SQL",  buildRangeSql("weigh_sl.weigh_slips_src", weighSlips, weighSlipFrom, weighSlipTo));

        p.put("INVOICE_TYPE", allCodesIfEmpty(r.getInvoiceTypes(), lookupService::getInvoiceTypes));
        // Maturity codes are hardcoded in the legacy app (AbstractInvoiceReportBean.init) —
        // the DB table is NOT used. R13 always includes Cants ("C"), and "E" is excluded.
        // Order must match legacy: O, S, M, C.
        p.put("MATURITY",     defaultIfEmpty(r.getMaturityCodes(),  List.of("O", "S", "M", "C")));
        p.put("SPECIES",      allCodesIfEmpty(r.getSpecies(),        lookupService::getSpeciesCodes));
        p.put("SORT_CODE",    allCodesIfEmpty(r.getSortCodes(),      lookupService::getSortCodes));
        p.put("GRADE",        allCodesIfEmpty(r.getGrades(),         lookupService::getGradeCodes));

        p.put("SUBMISSION_MONTH_YEAR", nullIfBlank(r.getSubmissionMonthYear()));
        p.put("SUBMISSION_STATUS",     allCodesIfEmpty(r.getSubmissionStatus(), lookupService::getSubmissionStatuses));
        p.put("SUBMISSION_NUMBER",     nullIfBlank(r.getSubmissionNumber()));
        p.put("ENTRY_USERID",          nullIfBlank(r.getEntryUserId()));
        // Submission type is not a DB lookup (valid values are fixed: Electronic / Manual).
        // Default to both when none are selected, so the header always shows a value.
        p.put("SUBMISSION_TYPE",       defaultIfEmpty(r.getSubmissionTypes(), List.of("Electronic", "Manual")));
        p.put("APPROVAL_MONTH_YEAR",   nullIfBlank(r.getApprovalMonthYear()));
        p.put("APPROVED_BY",           nullIfEmpty(r.getApprovedBy()));

        p.put("SELLER_NAME",             nullIfBlank(r.getSellerName()));
        p.put("BUYER_NAME",              nullIfBlank(r.getBuyerName()));
        p.put("SELLER_NUMBER",           nullIfEmpty(r.getSellerNumbers()));
        p.put("BUYER_NUMBER",            nullIfEmpty(r.getBuyerNumbers()));
        p.put("SELLER_CLIENT_LOCN_CODE", nullIfEmpty(r.getSellerClientLocnCodes()));
        p.put("BUYER_CLIENT_LOCN_CODE",  nullIfEmpty(r.getBuyerClientLocnCodes()));
        // FOB_POINT is declared in the JRXML but was never exposed as a filter in the legacy UI.
        // The old app always passed null here, so we preserve that — null causes the JRXML
        // $X{IN} clause to evaluate as "column IS NULL", matching the same rows it always did.
        p.put("FOB_POINT", null);
        return p;
    }

    /**
     * Returns {@code selected} if it contains at least one value (the user filtered by specific
     * codes).  Otherwise fetches every active code from {@code allCodes} and returns that full
     * list, mirroring the legacy behaviour where an empty multi-select means "all values".
     */
    private List<String> allCodesIfEmpty(List<String> selected, Supplier<List<LookupItem>> allCodes) {
        if (selected != null && !selected.isEmpty()) {
            return selected;
        }
        return allCodes.get().stream()
                .map(LookupItem::code)
                .collect(Collectors.toList());
    }

    /** Returns {@code selected} when non-empty, otherwise {@code defaults}. */
    private List<String> defaultIfEmpty(List<String> selected, List<String> defaults) {
        return (selected != null && !selected.isEmpty()) ? selected : new ArrayList<>(defaults);
    }

    private <T> List<T> nullIfEmpty(List<T> list) {
        return (list != null && !list.isEmpty()) ? list : null;
    }

    private String nullIfBlank(String s) {
        return (s != null && !s.isBlank()) ? s : null;
    }

    private String upperNullIfBlank(String s) {
        return (s != null && !s.isBlank()) ? s.trim().toUpperCase() : null;
    }

    private List<String> upperNullIfEmpty(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream().map(s -> s.trim().toUpperCase()).collect(Collectors.toList());
    }

    private String buildRangeDesc(List<String> single, String from, String to) {
        StringBuilder sb = new StringBuilder();
        if (single != null && !single.isEmpty()) {
            String val = single.get(0);
            if (val != null && !val.isBlank()) sb.append(val).append(",");
        }
        if (from != null && !from.isBlank()) sb.append(" from ").append(from);
        if (to != null && !to.isBlank())     sb.append(" to ").append(to);
        if (sb.isEmpty()) return null;
        String result = sb.toString().trim();
        if (result.endsWith(",")) result = result.substring(0, result.length() - 1).trim();
        return result.isEmpty() ? null : result;
    }

    private String buildRangeSql(String column, List<String> values, String from, String to) {
        StringBuilder sql = new StringBuilder();
        if (values != null && !values.isEmpty()) {
            sql.append(column).append(" IN (");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("'").append(values.get(i).replace("'", "''")).append("'");
            }
            sql.append(")");
        }
        boolean hasFrom = from != null && !from.isBlank();
        boolean hasTo   = to   != null && !to.isBlank();
        if (hasFrom || hasTo) {
            if (sql.length() > 0) sql.append(" AND ");
            sql.append("csp_reports_pkg.match_range(").append(column).append(", ")
               .append(hasFrom ? "'" + from.replace("'", "''") + "'" : "null").append(", ")
               .append(hasTo   ? "'" + to.replace("'", "''")   + "'" : "null").append(") = 'Y'");
        }
        return sql.isEmpty() ? "1=1" : sql.toString();
    }
}
