package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R08ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.base.JRBasePrintPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class R08ServiceTest {

    @Mock
    DataSource dataSource;
    @Mock
    SearchService searchService;

    R08Service service;

    static final ClientLocation ACME = new ClientLocation("00000001", "Acme Logging", "00", "Main", "Victoria", "BC");
    static final ClientLocation ACME_BRANCH = new ClientLocation("00000001", "Acme Logging", "01", "Branch", "Nanaimo", "BC");

    @BeforeEach
    void setUp() {
        service = new R08Service(dataSource, searchService);
        ReflectionTestUtils.setField(service, "r08TemplatePath", "/reports/R08.jrxml");
        ReflectionTestUtils.setField(service, "r08CsvTemplatePath", "/reports/R08_CSV.jrxml");
    }

    private R08ReportRequest baseRequest() {
        R08ReportRequest r = new R08ReportRequest();
        r.setReportFormat(ReportFormat.PDF);
        return r;
    }

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        void shouldThrow_whenNeitherDateRangeNorSubmissionProvided() {
            R08ReportRequest r = baseRequest();

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.r08.filter.required.error"));
        }

        @Test
        void shouldThrow_whenDateFromIsAfterDateTo() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20201231");
            r.setDateTo("20200101");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.daterange.order.error"));
        }

        @Test
        void shouldAccept_whenDateFromProvided() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldAccept_whenSubmissionNumberProvided() {
            R08ReportRequest r = baseRequest();
            r.setSubmissionNumber("12345");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldAccept_whenSubmissionYearMonthProvided() {
            R08ReportRequest r = baseRequest();
            r.setSubmissionYearMonth("202001");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("generateReport() — compile/fill failures")
    class GenerateReportFailures {

        @Test
        void shouldThrowReportGenerationException_whenTemplateNotFoundOnClasspath() {
            ReflectionTestUtils.setField(service, "r08TemplatePath", "/reports/DOES_NOT_EXIST.jrxml");
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to compile JRXML");
        }

        @Test
        void shouldThrowReportGenerationException_whenConnectionCannotBeObtained() throws Exception {
            given(dataSource.getConnection()).willThrow(new SQLException("boom"));
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to fill R08 report from database")
                    .hasCauseInstanceOf(SQLException.class);
        }

        @Test
        void shouldThrowResourceNotFound_whenReportHasNoPages() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R08");
        }

        @Test
        void shouldCompileCsvTemplateWithoutError_whenFormatIsCsv() {
            R08ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            r.setDateFrom("20200101");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R08");
        }
    }

    /**
     * Exercises the real {@code Connection} → {@code CallableStatement} binding path that every
     * other test in this class bypasses (they leave {@code dataSource.getConnection()}
     * unstubbed, which returns {@code null} and short-circuits before JasperReports ever prepares
     * a statement). See {@code R10ServiceTest.RealProcedureCallBinding} for the full rationale —
     * this is the same regression guard, retrofitted here since R08's stored-procedure call is
     * subject to the identical {@code RefCursorProcedureCallHandlerFactory} binding mechanism.
     */
    @Nested
    @DisplayName("generateReport() — real Connection/CallableStatement binding")
    class RealProcedureCallBinding {

        @Mock
        Connection connection;
        @Mock
        CallableStatement callableStatement;
        @Mock
        ResultSet cursorResult;

        @Test
        void shouldRegisterRefCursorOutParameter_andFillViaCallableStatement() throws SQLException {
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.prepareCall(anyString())).willReturn(callableStatement);
            given(callableStatement.getObject(1)).willReturn(cursorResult);
            given(cursorResult.next()).willReturn(false);

            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(connection).prepareCall(anyString());
            verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
            verify(callableStatement).execute();
        }
    }

    @Nested
    @DisplayName("exportReport() — output formats")
    class ExportReport {

        private JasperPrint printWithOnePage() {
            JasperPrint print = new JasperPrint();
            print.setName("R08Test");
            print.setPageWidth(612);
            print.setPageHeight(792);
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
    @DisplayName("resolveClient()")
    class ResolveClient {

        private Map<String, Object> buildParams(R08ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "buildParams", r);
        }

        @Test
        void shouldResolveSellerNameFromNumber() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("1");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME));

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .containsEntry("SELLER_NUMBER", "00000001")
                    .containsEntry("SELLER_NAME", "Acme Logging")
                    .containsEntry("SELLER_CLIENT_LOCN_CODE", "00");
        }

        @Test
        void shouldResolveSellerNumberFromName() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientName("Acme");
            given(searchService.findClientsByName("Acme")).willReturn(List.of(ACME));

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .containsEntry("SELLER_NUMBER", "00000001")
                    .containsEntry("SELLER_NAME", "Acme Logging");
        }

        @Test
        void shouldResolveBuyerNameFromNumber() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setBuyerClientNumber("1");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME));

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .containsEntry("BUYER_NUMBER", "00000001")
                    .containsEntry("BUYER_NAME", "Acme Logging")
                    .containsEntry("BUYER_CLIENT_LOCN_CODE", "00");
        }

        @Test
        void shouldThrow_whenSellerNumberNotFound() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("9999");
            given(searchService.findClientsByNumber("9999")).willReturn(List.of());

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.client.number.notfound.error"));
        }

        @Test
        void shouldThrow_whenBuyerNameNotFound() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setBuyerClientName("Unknown Corp");
            given(searchService.findClientsByName("Unknown Corp")).willReturn(List.of());

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.client.name.notfound.error"));
        }

        @Test
        void shouldThrow_whenSellerNameDoesNotPrefixMatchResolvedClient() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientName("Zenith");
            given(searchService.findClientsByName("Zenith")).willReturn(List.of(ACME));

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.client.name.nomatch.error"));
        }

        @Test
        void shouldNotSetClientParams_whenNoClientProvided() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .doesNotContainKey("SELLER_NUMBER")
                    .doesNotContainKey("BUYER_NUMBER");
        }

        @Test
        void shouldResolveSellerByName_whenSellerNumberIsBlank() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("   ");
            r.setSellerClientName("Acme");
            given(searchService.findClientsByName("Acme")).willReturn(List.of(ACME));

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .containsEntry("SELLER_NUMBER", "00000001")
                    .containsEntry("SELLER_NAME", "Acme Logging");
        }

        @Test
        void shouldNotSetSellerParams_whenSellerNameIsBlank() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientName("   ");

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .doesNotContainKey("SELLER_NUMBER")
                    .doesNotContainKey("SELLER_NAME");
        }

        @Test
        void shouldNotSetSellerParams_whenNumberLookupReturnsEmpty() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("1");
            given(searchService.findClientsByNumber("1")).willReturn(List.of());

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .doesNotContainKey("SELLER_NUMBER")
                    .doesNotContainKey("SELLER_NAME");
        }

        @Test
        void shouldNotSetSellerParams_whenNameLookupReturnsEmpty() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientName("Acme");
            given(searchService.findClientsByName("Acme")).willReturn(List.of());

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .doesNotContainKey("SELLER_NUMBER")
                    .doesNotContainKey("SELLER_NAME");
        }

        @Test
        void shouldSelectLocationMatchingLocCode() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("1");
            r.setSellerLocCode("01");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME, ACME_BRANCH));

            Map<String, Object> params = buildParams(r);
            assertThat(params).containsEntry("SELLER_CLIENT_LOCN_CODE", "01");
        }

        @Test
        void shouldDefaultToLocationZeroZero_whenLocCodeIsBlank() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("1");
            r.setSellerLocCode("   ");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME_BRANCH, ACME));

            Map<String, Object> params = buildParams(r);
            assertThat(params).containsEntry("SELLER_CLIENT_LOCN_CODE", "00");
        }

        @Test
        void shouldFallBackToFirstLocation_whenLocCodeMatchesNothing() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("1");
            r.setSellerLocCode("99");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME_BRANCH, ACME));

            Map<String, Object> params = buildParams(r);
            assertThat(params).containsEntry("SELLER_CLIENT_LOCN_CODE", "01");
        }
    }

    @Nested
    @DisplayName("buildParams()")
    class BuildParams {

        private Map<String, Object> buildParams(R08ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "buildParams", r);
        }

        @Test
        void shouldDeriveYearAndMonthFromSubmissionYearMonth() {
            R08ReportRequest r = baseRequest();
            r.setSubmissionYearMonth("202003");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("YEAR", "2020")
                    .containsEntry("MONTH", "03");
        }

        @Test
        void shouldExtendInvoiceDateToByTimeFrameMonths() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setTimeFrame("3");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200430");
        }

        @Test
        void shouldEndAtStartMonth_whenNoTimeFrameOrEndDate() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200131");
        }

        @Test
        void shouldUseYearAndMonthParams_whenProvided() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setYear(2021);
            r.setMonth(5);

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("YEAR", "2021")
                    .containsEntry("MONTH", "05");
        }

        @Test
        void shouldKeepYearAndMonth_whenSubmissionYearMonthIsBlank() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setYear(2021);
            r.setMonth(5);
            r.setSubmissionYearMonth("");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("YEAR", "2021")
                    .containsEntry("MONTH", "05");
        }

        @Test
        void shouldPreferSubmissionYearMonthOverExplicitYearAndMonth() {
            R08ReportRequest r = baseRequest();
            r.setYear(2019);
            r.setMonth(7);
            r.setSubmissionYearMonth("202003");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("YEAR", "2020")
                    .containsEntry("MONTH", "03");
        }

        @Test
        void shouldUseProvidedDateTo_whenDateToPresent() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setDateTo("20200315");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200315");
        }

        @Test
        void shouldTreatBlankTimeFrameAsAbsent() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("   ");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200131");
        }

        @Test
        void shouldThrowBadRequest_whenTimeFrameIsNotNumeric() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setTimeFrame("abc");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("timeFrame");
        }

        @Test
        void shouldUseProvidedFilterCodes() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setMaturityCodes("O,C");
            r.setInvoiceType("PUR");
            r.setInvoiceStatus("APP");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("MATURITY", "O,C")
                    .containsEntry("INVOICE_TYPE", "PUR")
                    .containsEntry("INVOICE_STATUS", "APP")
                    // R08.jrxml declares no SUBMISSION_STATUS parameter — the request field is
                    // still bean-validated but never reaches the stored proc; not sent as a param.
                    .doesNotContainKey("SUBMISSION_STATUS");
        }

        @Test
        void shouldUseDefaultFilterCodes_whenNoneProvided() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("MATURITY", "O,S,M")
                    .containsEntry("INVOICE_TYPE", "ADJ,CAN,PUR,SAL")
                    .containsEntry("INVOICE_STATUS", "PRO,UNA,APP,CAN,DFT,DVF,REJ,VER")
                    .doesNotContainKey("SUBMISSION_STATUS");
        }
    }

    @Nested
    @DisplayName("USER_ID parameter")
    class UserIdParam {

        @BeforeEach
        void clearContextBefore() {
            SecurityContextHolder.clearContext();
        }

        @AfterEach
        void clearContextAfter() {
            SecurityContextHolder.clearContext();
        }

        private Map<String, Object> buildParams(R08ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "buildParams", r);
        }

        @Test
        void shouldPreferAuthenticatedUsernameOverRequestUserId() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("TESTUSER", null, List.of()));
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setUserId("CLIENTUSER");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("USER_ID", "TESTUSER");
        }

        @Test
        void shouldFallBackToRequestUserId_whenNoAuthenticatedUser() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setUserId("IDIRUSER");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("USER_ID", "IDIRUSER");
        }

        @Test
        void shouldOmitUserId_whenNoUserAvailable() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            Map<String, Object> params = buildParams(r);

            assertThat(params).doesNotContainKey("USER_ID");
        }
    }
}
