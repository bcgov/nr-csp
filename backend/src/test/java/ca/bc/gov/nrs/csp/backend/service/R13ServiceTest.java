package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R13ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R13ShowOptions;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.base.JRBasePrintPage;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class R13ServiceTest {

    @Mock
    DataSource dataSource;

    @Mock
    LookupService lookupService;

    @Mock
    SearchService searchService;

    R13Service service;

    static final ClientLocation VALID_CLIENT =
            new ClientLocation("00012345", "ACME FOREST LTD", "00", "Head Office", "Victoria", "BC");

    @BeforeEach
    void setUp() {
        service = new R13Service(dataSource, lookupService, searchService);
        ReflectionTestUtils.setField(service, "r13TemplatePath", "/reports/R13.jrxml");
        ReflectionTestUtils.setField(service, "r13CsvTemplatePath", "/reports/R13_CSV.jrxml");
    }

    private R13ReportRequest baseRequest() {
        R13ReportRequest r = new R13ReportRequest();
        r.setReportFormat(ReportFormat.PDF);
        r.setReportName("Test Report");
        r.setInvoiceDateFrom("20200101");
        R13ShowOptions opts = new R13ShowOptions();
        opts.setShowInvoiceNumber(true);
        opts.setShowSellerName(true);
        r.setShowOptions(opts);
        return r;
    }

    private static R13ShowOptions allShown() {
        R13ShowOptions opts = new R13ShowOptions();
        opts.setShowSubmissionStatus(true);
        opts.setShowSubmissionNumber(true);
        opts.setShowApprovedBy(true);
        opts.setShowSubmissionMonthYear(true);
        opts.setShowApprovalMonthYear(true);
        opts.setShowSubmissionType(true);
        opts.setShowClientInvoiceDate(true);
        opts.setShowInvoiceNumber(true);
        opts.setShowInvoiceReplacesAdjusts(true);
        opts.setShowInvoiceBoomNumber(true);
        opts.setShowInvoiceTimberMark(true);
        opts.setShowInvoiceWeighSlip(true);
        opts.setShowInvoiceType(true);
        opts.setShowInvoiceStatus(true);
        opts.setShowSellerName(true);
        opts.setShowSellerNumber(true);
        opts.setShowBuyerName(true);
        opts.setShowBuyerNumber(true);
        opts.setShowMaturity(true);
        opts.setShowSpecies(true);
        opts.setShowSortCodeSecondary(true);
        opts.setShowGrade(true);
        opts.setShowFobPoint(true);
        opts.setShowPieces(true);
        opts.setShowVolume(true);
        opts.setShowAmount(true);
        opts.setShowSortCodePrimary(true);
        opts.setShowFlatPrice(true);
        opts.setShowSpreadPrice(true);
        opts.setShowPrice(true);
        opts.setShowReviewer(true);
        opts.setShowComments(true);
        opts.setShowEntryUserid(true);
        return opts;
    }

    @Nested
    @DisplayName("validate() — show options")
    class ValidateShowOptions {

        @Test
        void shouldThrow_whenFewerThanTwoColumnsSelected() {
            R13ReportRequest r = baseRequest();
            R13ShowOptions opts = new R13ShowOptions();
            opts.setShowInvoiceNumber(true);
            r.setShowOptions(opts);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("at least 2 output columns");
        }

        @Test
        void shouldThrow_whenShowOptionsIsNullAndDefaultsToZeroSelected() {
            R13ReportRequest r = baseRequest();
            r.setShowOptions(null);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("at least 2 output columns");
        }
    }

    @Nested
    @DisplayName("validate() — filter requirement")
    class ValidateFilterRequirement {

        @Test
        void shouldThrow_whenNoDateRangeOrSubmissionOrClientProvided() {
            R13ReportRequest r = new R13ReportRequest();
            r.setReportFormat(ReportFormat.PDF);
            r.setReportName("Test");
            R13ShowOptions opts = new R13ShowOptions();
            opts.setShowInvoiceNumber(true);
            opts.setShowSellerName(true);
            r.setShowOptions(opts);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("R13 requires at least one of");
        }

        @Test
        void shouldAccept_whenSellerNumbersProvided() {
            given(searchService.findClientsByNumber(any())).willReturn(List.of(VALID_CLIENT));

            R13ReportRequest r = new R13ReportRequest();
            r.setReportFormat(ReportFormat.PDF);
            r.setReportName("Test");
            r.setSellerNumbers(List.of("12345"));
            R13ShowOptions opts = new R13ShowOptions();
            opts.setShowInvoiceNumber(true);
            opts.setShowSellerName(true);
            r.setShowOptions(opts);

            // Validation passes; the empty fill against the mock DataSource then yields
            // "no data" (ResourceNotFoundException), which is not a validation failure.
            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldAccept_whenSubmissionNumberProvided() {
            R13ReportRequest r = new R13ReportRequest();
            r.setReportFormat(ReportFormat.PDF);
            r.setReportName("Test");
            r.setSubmissionNumber("99999");
            R13ShowOptions opts = new R13ShowOptions();
            opts.setShowInvoiceNumber(true);
            opts.setShowSellerName(true);
            r.setShowOptions(opts);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("validate() — date ordering")
    class ValidateDateOrdering {

        @Test
        void shouldThrow_whenInvoiceDateFromIsAfterInvoiceDateTo() {
            R13ReportRequest r = baseRequest();
            r.setInvoiceDateFrom("20201231");
            r.setInvoiceDateTo("20200101");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("invoiceDateFrom must not be after invoiceDateTo");
        }

        @Test
        void shouldAccept_whenInvoiceDateFromEqualsInvoiceDateTo() {
            R13ReportRequest r = baseRequest();
            r.setInvoiceDateFrom("20200101");
            r.setInvoiceDateTo("20200101");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("validate() — entry user ID filter")
    class ValidateEntryUserId {

        @Test
        void shouldAccept_whenOnlyEntryUserIdProvided() {
            R13ReportRequest r = new R13ReportRequest();
            r.setReportFormat(ReportFormat.PDF);
            r.setReportName("Test");
            r.setEntryUserId("jsmith");
            R13ShowOptions opts = new R13ShowOptions();
            opts.setShowInvoiceNumber(true);
            opts.setShowSellerName(true);
            r.setShowOptions(opts);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("resolveInvoiceDateTo() — date auto-population")
    class ResolveDateTo {

        private String resolve(R13ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "resolveInvoiceDateTo", r);
        }

        @Test
        void shouldReturnNull_whenNoDatesProvided() {
            R13ReportRequest r = new R13ReportRequest();
            assertThat(resolve(r)).isNull();
        }

        @Test
        void shouldReturnExplicitDateTo_whenProvided() {
            R13ReportRequest r = new R13ReportRequest();
            r.setInvoiceDateFrom("20200101");
            r.setInvoiceDateTo("20200615");
            assertThat(resolve(r)).isEqualTo("20200615");
        }

        @Test
        void shouldReturnEndOfMonth_whenOnlyDateFromProvided() {
            R13ReportRequest r = new R13ReportRequest();
            r.setInvoiceDateFrom("20200115");
            assertThat(resolve(r)).isEqualTo("20200131");
        }

        @Test
        void shouldReturnEndOfOffsetMonth_whenTimeFrameProvided() {
            // Jan + 3 months = Apr, last day = 30
            R13ReportRequest r = new R13ReportRequest();
            r.setInvoiceDateFrom("20200101");
            r.setTimeFrame("3");
            assertThat(resolve(r)).isEqualTo("20200430");
        }
    }

    @Nested
    @DisplayName("validate() — client validation")
    class ValidateClientInfo {

        @Test
        void shouldThrow_whenSellerNumberNotFoundInCSP() {
            given(searchService.findClientsByNumber(any())).willReturn(Collections.emptyList());

            R13ReportRequest r = baseRequest();
            r.setSellerNumbers(List.of("99999"));

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cannot be found in CSP");
        }

        @Test
        void shouldThrow_whenSellerNameDoesNotMatchResolvedClient() {
            ClientLocation client = new ClientLocation("00099999", "BIG TIMBER INC", "00", "Head Office", "Victoria", "BC");
            given(searchService.findClientsByNumber(any())).willReturn(List.of(client));

            R13ReportRequest r = baseRequest();
            r.setSellerNumbers(List.of("99999"));
            r.setSellerName("WRONG NAME");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("The Client Name (WRONG NAME) cannot be found in CSP");
        }

        @Test
        void shouldThrow_whenSellerNameNotFoundByNameLookup() {
            given(searchService.findClientsByName(any())).willReturn(Collections.emptyList());

            R13ReportRequest r = baseRequest();
            r.setSellerName("UNKNOWN CO");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("The Client Name (UNKNOWN CO) cannot be found in CSP");
        }

        @Test
        void shouldThrow_whenBuyerNumberNotFoundInCSP() {
            given(searchService.findClientsByNumber(any())).willReturn(Collections.emptyList());

            R13ReportRequest r = baseRequest();
            r.setBuyerNumbers(List.of("88888"));

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cannot be found in CSP");
        }

        @Test
        void shouldAccept_whenSellerNumberExistsAndNameMatches() {
            ClientLocation client = new ClientLocation("00012345", "ACME FOREST LTD", "00", "Head Office", "Victoria", "BC");
            given(searchService.findClientsByNumber(any())).willReturn(List.of(client));

            R13ReportRequest r = baseRequest();
            r.setSellerNumbers(List.of("12345"));
            r.setSellerName("ACME");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("generateReport() — compile/fill failures")
    class GenerateReportFailures {

        @Test
        void shouldThrowReportGenerationException_whenTemplateNotFoundOnClasspath() {
            ReflectionTestUtils.setField(service, "r13TemplatePath", "/reports/DOES_NOT_EXIST.jrxml");
            R13ReportRequest req = baseRequest();

            assertThatThrownBy(() -> service.generateReport(req))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to compile JRXML")
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        void shouldThrowReportGenerationException_whenNoDataSourceConfigured() {
            R13Service noDbService = new R13Service(null, lookupService, searchService);
            ReflectionTestUtils.setField(noDbService, "r13TemplatePath", "/reports/R13.jrxml");
            ReflectionTestUtils.setField(noDbService, "r13CsvTemplatePath", "/reports/R13_CSV.jrxml");
            R13ReportRequest req = baseRequest();

            assertThatThrownBy(() -> noDbService.generateReport(req))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("requires a database connection");
        }

        @Test
        void shouldThrowReportGenerationException_whenConnectionCannotBeObtained() throws Exception {
            given(dataSource.getConnection()).willThrow(new SQLException("boom"));
            R13ReportRequest req = baseRequest();
            assertThatThrownBy(() -> service.generateReport(req))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to fill R13 report from database")
                    .hasCauseInstanceOf(SQLException.class);
        }

        @Test
        void shouldThrowResourceNotFound_whenReportHasNoPages() {
            // The mock DataSource yields no JDBC connection, so the fill produces no data rows
            // and the report (whenNoDataType = NoPages) ends up with zero pages.
            R13ReportRequest req = baseRequest();
            assertThatThrownBy(() -> service.generateReport(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("no data");
        }

        @Test
        void shouldUseCsvTemplateUnmodified_whenCsvRequestedWithAllColumnsShown() {
            R13ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            r.setShowOptions(allShown());

            // All columns shown -> template is compiled as-is; empty fill -> no pages.
            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("no data");
        }
    }

    @Nested
    @DisplayName("exportReport() — output formats")
    class ExportReport {

        private JasperPrint printWithOnePage() {
            JasperPrint print = new JasperPrint();
            print.setName("R13Test");
            print.setPageWidth(842);
            print.setPageHeight(595);
            print.addPage(new JRBasePrintPage());
            return print;
        }

        @Test
        void shouldExportPdf() {
            byte[] data = ReflectionTestUtils.invokeMethod(service, "exportReport", printWithOnePage(), "PDF");

            assertThat(data).isNotEmpty();
            assertThat(new String(data, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        }

        @Test
        void shouldExportCsv() {
            byte[] data = ReflectionTestUtils.invokeMethod(service, "exportReport", printWithOnePage(), "CSV");

            assertThat(data).isNotNull();
        }

        @Test
        void shouldThrowReportGenerationException_whenFormatUnsupported() {
            JasperPrint print = printWithOnePage();

            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "exportReport", print, "XLSX"))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Unsupported report format: XLSX");
        }
    }

    @Nested
    @DisplayName("buildReportParameters() — parameter map construction")
    class BuildReportParameters {

        private String invokeBuildRangeDesc(List<String> values, String from, String to) {
            try {
                var method = R13Service.class.getDeclaredMethod("buildRangeDesc", List.class, String.class, String.class);
                method.setAccessible(true);
                return (String) method.invoke(service, values, from, to);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        private String invokeBuildRangeSql(String column, List<String> values, String from, String to) {
            try {
                var method = R13Service.class.getDeclaredMethod("buildRangeSql", String.class, List.class, String.class, String.class);
                method.setAccessible(true);
                return (String) method.invoke(service, column, values, from, to);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void shouldNormaliseFiltersAndBuildDescriptionsAndSql() {
            R13ReportRequest r = new R13ReportRequest();
            r.setUserId("tester");
            r.setUserName("Tester T");
            r.setReportName("My R13");
            r.setInvoiceDateFrom("20200101");
            r.setInvoiceNumbers(List.of(" inv1 "));
            r.setInvoiceNumberFrom("a1");
            r.setInvoiceNumberTo("z9");
            r.setInvoiceStatus(List.of("APP"));
            r.setInvoiceReplacesAdjusts(List.of("o'brien"));
            r.setInvoiceBoomNumberFrom("b1");
            r.setInvoiceTimberMarkTo("t9");
            r.setInvoiceWeighSlips(List.of("ws1", "ws2"));
            r.setInvoiceWeighSlipFrom("w1");
            r.setInvoiceWeighSlipTo("w9");
            r.setInvoiceTypes(List.of("LOG"));
            r.setMaturityCodes(List.of("O", "S"));
            r.setSpecies(List.of("FI"));
            r.setSortCodes(List.of("01"));
            r.setGrades(List.of("A"));
            r.setSubmissionMonthYear("202001");
            r.setSubmissionStatus(List.of("SUB"));
            r.setSubmissionNumber("123");
            r.setEntryUserId("jdoe");
            r.setSubmissionTypes(List.of("Manual"));
            r.setApprovalMonthYear("202002");
            r.setApprovedBy(List.of("APPROVER"));
            r.setSellerName("ACME");
            r.setBuyerName("BUYCO");
            r.setSellerNumbers(List.of("12345"));
            r.setBuyerNumbers(List.of("67890"));
            r.setSellerClientLocnCodes(List.of("00"));
            r.setBuyerClientLocnCodes(List.of("01"));

            Map<String, Object> p = ReflectionTestUtils.invokeMethod(service, "buildReportParameters", r, "20200131");

            // consolidated assertions below
            assertThat(p)
                    .isNotNull()
                    // Range SQL: values only (with single-quote escaping)
                    .containsEntry("INVOICE_REPLACES_ADJUSTS_SQL", "rep_adj.rep_adj_inv_id IN ('O''BRIEN')")
                    // Range SQL: from only
                    .containsEntry("INVOICE_BOOM_NUMBER_SQL", "csp_reports_pkg.match_range(boom_nums.boom_num_src, 'B1', null) = 'Y'")
                    // Range SQL: to only
                    .containsEntry("INVOICE_TIMBER_MARK_SQL", "csp_reports_pkg.match_range(tmbr_mks.tmbr_mks_src, null, 'T9') = 'Y'")
                    // Range SQL: values plus from/to range
                    .containsEntry("INVOICE_WEIGH_SLIP_SQL", "weigh_sl.weigh_slips_src IN ('WS1','WS2')"
                            + " AND csp_reports_pkg.match_range(weigh_sl.weigh_slips_src, 'W1', 'W9') = 'Y'")
                    .containsEntry("SUBMISSION_MONTH_YEAR", "202001")
                    .containsEntry("SUBMISSION_NUMBER", "123")
                    .containsEntry("ENTRY_USERID", "jdoe")
                    .containsEntry("APPROVAL_MONTH_YEAR", "202002")
                    .containsEntry("APPROVED_BY", List.of("APPROVER"))
                    .containsEntry("SELLER_NAME", "ACME")
                    .containsEntry("BUYER_NAME", "BUYCO")
                    .containsEntry("SELLER_NUMBER", List.of("12345"))
                    .containsEntry("BUYER_NUMBER", List.of("67890"))
                    .containsEntry("SELLER_CLIENT_LOCN_CODE", List.of("00"))
                    .containsEntry("BUYER_CLIENT_LOCN_CODE", List.of("01"))
                    // FOB_POINT is always null (never exposed as a filter in the legacy UI)
                    .containsEntry("FOB_POINT", null);
        }

        @Test
        void shouldFallBackToUserId_whenUserNameMissing() {
            R13ReportRequest r = new R13ReportRequest();
            r.setUserId("abc");

            Map<String, Object> p = ReflectionTestUtils.invokeMethod(service, "buildReportParameters", r, "20200131");

            assertThat(p).isNotNull()
                    .containsEntry("USER_ID", "abc")
                    .containsEntry("USER_NAME", "abc")
                    // Empty multi-selects that are not DB lookups get their fixed defaults
                    .containsEntry("MATURITY", List.of("O", "S", "M", "C"))
                    .containsEntry("SUBMISSION_TYPE", List.of("Electronic", "Manual"))
                    // Empty filters map to null parameters and match-all SQL
                    .containsEntry("INVOICE_NUMBER", null)
                    .containsEntry("INVOICE_NUMBER_DESC", null)
                    .containsEntry("INVOICE_BOOM_NUMBER_SQL", "1=1");
        }

        @Test
        void buildRangeDesc_shouldStripTrailingComma_whenOnlySingleValueProvided() {
            String desc = invokeBuildRangeDesc(List.of("ABC"), null, null);

            assertThat(desc).isEqualTo("ABC");
        }

        @Test
        void buildRangeDesc_shouldSkipBlankSingleValue() {
            String desc = invokeBuildRangeDesc(List.of("  "), "A1", null);

            assertThat(desc).isEqualTo("from A1");
        }

        @Test
        void buildRangeDesc_shouldReturnNull_whenNothingProvided() {
            String desc = invokeBuildRangeDesc(Collections.emptyList(), null, null);

            assertThat(desc).isNull();
        }

        @Test
        void buildRangeSql_shouldEscapeQuotesInRangeBounds() {
            String sql = invokeBuildRangeSql("col", null, "o'b", null);

            assertThat(sql).isEqualTo("csp_reports_pkg.match_range(col, 'o''b', null) = 'Y'");
        }
    }

    @Nested
    @DisplayName("modifyTemplateContent() / relocateFields() — column show/hide")
    class TemplateModification {

        private Element find(Document doc, String section, String kind, String key) {
            Element band = doc.getRootElement().element(section).element("band");
            for (Element el : band.elements("element")) {
                if (kind.equals(el.attributeValue("kind")) && key.equals(el.attributeValue("key"))) {
                    return el;
                }
            }
            return null;
        }

        @Test
        void shouldRemoveHiddenColumnsAndTheirTotals() {
            String jrxml = """
                    <jasperReport>
                      <columnHeader><band>
                        <element kind="staticText" key="VOLUME" x="0" width="30"/>
                        <element kind="staticText" key="TOTAL_VOLUME" x="30" width="30"/>
                        <element kind="staticText" key="PRICE" x="60" width="30"/>
                      </band></columnHeader>
                      <detail><band>
                        <element kind="textField" key="VOLUME" x="0"/>
                        <element kind="textField" key="TOTAL_VOLUME" x="30"/>
                        <element kind="textField" key="PRICE" x="60"/>
                      </band></detail>
                    </jasperReport>
                    """;
            Map<String, Boolean> showParams = new LinkedHashMap<>();
            showParams.put("SHOW_VOLUME", false);
            showParams.put("SHOW_PRICE", true);
            showParams.put("SHOW_BOGUS", false); // no matching node -> warn branch

            String result = ReflectionTestUtils.invokeMethod(service, "modifyTemplateContent", jrxml, showParams);

            assertThat(result)
                    .doesNotContain("key=\"VOLUME\"")
                    .doesNotContain("key=\"TOTAL_VOLUME\"")
                    .contains("key=\"PRICE\"");
        }

        @Test
        void relocateFields_shouldRedistributeColumnsAndMoveTotals() throws Exception {
            String jrxml = """
                    <jasperReport>
                      <columnHeader><band>
                        <element kind="staticText" key="PIECES" x="50"/>
                        <element kind="staticText" key="VOLUME" x="100"/>
                        <element kind="staticText" key="PRICE" x="200"/>
                        <element kind="staticText" key="CONVERSION_FACTOR" x="210"/>
                        <element kind="staticText" key="AMOUNT" x="300"/>
                        <element kind="staticText" key="BAD" x="abc"/>
                        <element kind="staticText" key="NOX"/>
                      </band></columnHeader>
                      <detail><band>
                        <element kind="textField" key="PIECES" x="50"/>
                        <element kind="textField" key="VOLUME" x="100"/>
                        <element kind="textField" key="PRICE" x="200"/>
                        <element kind="textField" key="CONVERSION_FACTOR" x="210"/>
                        <element kind="textField" key="TOTAL_VOLUME" x="400"/>
                      </band></detail>
                      <summary><band>
                        <element kind="textField" key="PIECES" x="50"/>
                        <element kind="textField" key="AMOUNT" x="300"/>
                        <element kind="staticText" key="TOTAL_LABEL" x="10" width="60"/>
                      </band></summary>
                    </jasperReport>
                    """;
            Document doc = DocumentHelper.parseText(jrxml);

            Document result = ReflectionTestUtils.invokeMethod(service, "relocateFields", doc, 800);

            assertThat(result).isNotNull();
            // 4 columns remain (CONVERSION_FACTOR merges into PRICE; BAD/NOX skipped) -> width 200 each
            Element ePieces = find(result, "columnHeader", "staticText", "PIECES");
            assertThat(ePieces).isNotNull();
            assertThat(ePieces.attributeValue("x")).isEqualTo("0");

            Element eVolume = find(result, "columnHeader", "staticText", "VOLUME");
            assertThat(eVolume).isNotNull();
            assertThat(eVolume.attributeValue("x")).isEqualTo("200");

            Element ePrice = find(result, "columnHeader", "staticText", "PRICE");
            assertThat(ePrice).isNotNull();
            assertThat(ePrice.attributeValue("x")).isEqualTo("400");

            // CONVERSION_FACTOR follows PRICE
            Element eConvHead = find(result, "columnHeader", "staticText", "CONVERSION_FACTOR");
            assertThat(eConvHead).isNotNull();
            assertThat(eConvHead.attributeValue("x")).isEqualTo("400");

            Element eConvDetail = find(result, "detail", "textField", "CONVERSION_FACTOR");
            assertThat(eConvDetail).isNotNull();
            assertThat(eConvDetail.attributeValue("x")).isEqualTo("400");

            Element eAmount = find(result, "columnHeader", "staticText", "AMOUNT");
            assertThat(eAmount).isNotNull();
            assertThat(eAmount.attributeValue("x")).isEqualTo("600");

            // TOTAL_ companion column moves with its base column
            Element eTotalVol = find(result, "detail", "textField", "TOTAL_VOLUME");
            assertThat(eTotalVol).isNotNull();
            assertThat(eTotalVol.attributeValue("x")).isEqualTo("200");

            // Summary fields are relocated too
            Element sPieces = find(result, "summary", "textField", "PIECES");
            assertThat(sPieces).isNotNull();
            assertThat(sPieces.attributeValue("x")).isEqualTo("0");

            Element sAmount = find(result, "summary", "textField", "AMOUNT");
            assertThat(sAmount).isNotNull();
            assertThat(sAmount.attributeValue("x")).isEqualTo("600");

            // TOTAL_LABEL sits immediately left of the leftmost summary field (0 - width 60)
            Element totalLabel = find(result, "summary", "staticText", "TOTAL_LABEL");
            assertThat(totalLabel).isNotNull();
            assertThat(totalLabel.attributeValue("x")).isEqualTo("-60");
        }

        @Test
        void relocateFields_shouldIgnoreTotalLabelWithUnparseableWidth() throws Exception {
            String jrxml = """
                    <jasperReport>
                      <columnHeader><band>
                        <element kind="staticText" key="A" x="10"/>
                        <element kind="staticText" key="B" x="20"/>
                      </band></columnHeader>
                      <summary><band>
                        <element kind="textField" key="B" x="99"/>
                        <element kind="staticText" key="TOTAL_LABEL" x="5" width="NaN"/>
                      </band></summary>
                    </jasperReport>
                    """;
            Document doc = DocumentHelper.parseText(jrxml);

            Document result = ReflectionTestUtils.invokeMethod(service, "relocateFields", doc, 800);

            assertThat(result).isNotNull();
            Element a = find(result, "columnHeader", "staticText", "A");
            assertThat(a).isNotNull();
            assertThat(a.attributeValue("x")).isEqualTo("0");
            // B's summary field moves to its new column position
            Element b = find(result, "summary", "textField", "B");
            assertThat(b).isNotNull();
            assertThat(b.attributeValue("x")).isEqualTo("400");
            // Unparseable width -> label position left untouched
            Element totalLabel = find(result, "summary", "staticText", "TOTAL_LABEL");
            assertThat(totalLabel).isNotNull();
            assertThat(totalLabel.attributeValue("x")).isEqualTo("5");
        }
    }
}
