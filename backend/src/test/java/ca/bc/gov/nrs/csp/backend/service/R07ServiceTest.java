package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R07ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperReportRenderer;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
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
class R07ServiceTest {

    @Mock
    DataSource dataSource;
    @Mock
    SearchService searchService;
    @Mock
    CspSubmissionRepository cspSubmissionRepository;

    R07Service service;

    static final ClientLocation ACME = new ClientLocation("00000001", "Acme Logging", "00", "Main", "Victoria", "BC");

    @BeforeEach
    void setUp() {
        service = new R07Service(new JasperReportRenderer(dataSource), searchService, cspSubmissionRepository);
        ReflectionTestUtils.setField(service, "r07TemplatePath", "/reports/R07.jrxml");
        ReflectionTestUtils.setField(service, "r07CsvTemplatePath", "/reports/R07_CSV.jrxml");
    }

    private R07ReportRequest baseRequest() {
        R07ReportRequest r = new R07ReportRequest();
        r.setReportFormat(ReportFormat.PDF);
        return r;
    }

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        void shouldThrow_whenNoFilterProvided() {
            R07ReportRequest r = baseRequest();

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.r07.filter.required.error"));
        }

        @Test
        void shouldThrow_whenDateFromIsAfterDateTo() {
            R07ReportRequest r = baseRequest();
            r.setDateFrom("20201231");
            r.setDateTo("20200101");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.daterange.order.error"));
        }

        @Test
        void shouldAccept_whenYearAndMonthProvided() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(6);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldAccept_whenDateFromProvided() {
            R07ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldAccept_whenSellerClientNumberProvided() {
            R07ReportRequest r = baseRequest();
            r.setSellerClientNumber("12345");
            given(searchService.findClientsByNumber("12345")).willReturn(List.of(ACME));

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldAccept_whenSubmissionNumberProvided() {
            R07ReportRequest r = baseRequest();
            r.setSubmissionNumber("999");
            given(cspSubmissionRepository.existsBySubmissionNumber(999L)).willReturn(true);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldThrow_whenSubmissionNumberNotFound() {
            R07ReportRequest r = baseRequest();
            r.setSubmissionNumber("999");
            given(cspSubmissionRepository.existsBySubmissionNumber(999L)).willReturn(false);

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.submissionnumber.notfound.error"));
        }

        @Test
        void shouldThrow_whenTimeFrameIsNonNumeric() {
            R07ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setTimeFrame("abc");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("timeFrame must be a numeric value");
        }
    }

    @Nested
    @DisplayName("generateReport() — compile/fill failures")
    class GenerateReportFailures {

        @Test
        void shouldThrowReportGenerationException_whenTemplateNotFoundOnClasspath() {
            ReflectionTestUtils.setField(service, "r07TemplatePath", "/reports/DOES_NOT_EXIST.jrxml");
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to compile JRXML");
        }

        @Test
        void shouldThrowReportGenerationException_whenConnectionCannotBeObtained() throws Exception {
            given(dataSource.getConnection()).willThrow(new SQLException("boom"));
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to fill R07 report from database")
                    .hasCauseInstanceOf(SQLException.class);
        }

        @Test
        void shouldThrowResourceNotFound_whenReportHasNoPages() {
            // The mock DataSource yields no JDBC connection, so the fill produces no data rows
            // and the report ends up with zero pages. Also the plsql-query-executer regression
            // guard: without the jasperreports.properties alias this would instead throw a
            // ReportGenerationException wrapping a JRException.
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R07");
        }

        @Test
        void shouldCompileCsvTemplateWithoutError_whenFormatIsCsv() {
            R07ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            r.setYear(2020);
            r.setMonth(1);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R07");
        }
    }

    /**
     * Exercises the real {@code Connection} → {@code CallableStatement} binding path that every
     * other test in this class bypasses (they leave {@code dataSource.getConnection()}
     * unstubbed, which returns {@code null} and short-circuits before JasperReports ever prepares
     * a statement). See {@code R10ServiceTest.RealProcedureCallBinding} for the full rationale —
     * this is the same regression guard, retrofitted here since R07's stored-procedure call is
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

            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(connection).prepareCall(anyString());
            verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
            verify(callableStatement).execute();
        }
    }

    @Nested
    @DisplayName("resolveClient()")
    class ResolveClient {

        private Map<String, Object> buildParams(R07ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "buildParams", r);
        }

        @Test
        void shouldResolveSellerNameFromNumber() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
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
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
            r.setSellerClientName("Acme");
            given(searchService.findClientsByName("Acme")).willReturn(List.of(ACME));

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .containsEntry("SELLER_NUMBER", "00000001")
                    .containsEntry("SELLER_NAME", "Acme Logging");
        }

        @Test
        void shouldResolveBuyerNameFromNumber() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
            r.setBuyerClientNumber("1");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME));

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .containsEntry("BUYER_NUMBER", "00000001")
                    .containsEntry("BUYER_NAME", "Acme Logging")
                    .containsEntry("BUYER_CLIENT_LOCN_CODE", "00");
        }

        @Test
        void shouldPreferLocCodeMatchWhenResolvingByNumber() {
            ClientLocation loc01 = new ClientLocation("00000001", "Acme Logging", "01", "Branch", "Kelowna", "BC");
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
            r.setSellerClientNumber("1");
            r.setSellerLocCode("01");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME, loc01));

            assertThat(buildParams(r)).containsEntry("SELLER_CLIENT_LOCN_CODE", "01");
        }

        @Test
        void shouldThrow_whenSellerNumberNotFound() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
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
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
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
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
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
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .doesNotContainKey("SELLER_NUMBER")
                    .doesNotContainKey("BUYER_NUMBER");
        }

        @Test
        void shouldOmitSellerParams_whenNumberLookupReturnsEmpty() {
            R07ReportRequest r = baseRequest();
            r.setSellerClientNumber("1");
            given(searchService.findClientsByNumber("1")).willReturn(List.of());

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .doesNotContainKey("SELLER_NUMBER")
                    .doesNotContainKey("SELLER_NAME");
        }

        @Test
        void shouldOmitSellerParams_whenNameLookupReturnsEmpty() {
            R07ReportRequest r = baseRequest();
            r.setSellerClientName("Acme");
            given(searchService.findClientsByName("Acme")).willReturn(List.of());

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .doesNotContainKey("SELLER_NUMBER")
                    .doesNotContainKey("SELLER_NAME");
        }

        @Test
        void shouldFallBackToNameLookup_whenClientNumberIsBlank() {
            R07ReportRequest r = baseRequest();
            r.setSellerClientNumber("   ");
            r.setSellerClientName("Acme");
            given(searchService.findClientsByName("Acme")).willReturn(List.of(ACME));

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .containsEntry("SELLER_NUMBER", "00000001")
                    .containsEntry("SELLER_NAME", "Acme Logging");
        }

        @Test
        void shouldNotLookupClient_whenNameIsBlank() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
            r.setSellerClientName("   ");

            Map<String, Object> params = buildParams(r);
            assertThat(params)
                    .doesNotContainKey("SELLER_NUMBER")
                    .doesNotContainKey("SELLER_NAME");
        }

        @Test
        void shouldDefaultBlankLocCodeToDoubleZero() {
            ClientLocation loc01 = new ClientLocation("00000001", "Acme Logging", "01", "Branch", "Kelowna", "BC");
            R07ReportRequest r = baseRequest();
            r.setSellerClientNumber("1");
            r.setSellerLocCode("   ");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(loc01, ACME));

            assertThat(buildParams(r)).containsEntry("SELLER_CLIENT_LOCN_CODE", "00");
        }

        @Test
        void shouldFallBackToFirstResult_whenNoLocationMatches() {
            ClientLocation loc01 = new ClientLocation("00000001", "Acme Logging", "01", "Branch", "Kelowna", "BC");
            R07ReportRequest r = baseRequest();
            r.setSellerClientNumber("1");
            r.setSellerLocCode("05");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(loc01, ACME));

            assertThat(buildParams(r)).containsEntry("SELLER_CLIENT_LOCN_CODE", "01");
        }
    }

    @Nested
    @DisplayName("buildParams()")
    class BuildParams {

        private Map<String, Object> buildParams(R07ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "buildParams", r);
        }

        @Test
        void shouldSetOptionalParams_whenProvided() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(6);
            r.setShowReplacesAdjusts(true);
            r.setInvoiceType("PUR");
            r.setInvoiceStatus("APP");
            r.setSubmissionStatus("COM");
            r.setSubmissionYearMonth("202006");
            r.setUserId("TESTUSER");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("YEAR", "2020")
                    .containsEntry("MONTH", "06")
                    .containsEntry("SHOW_REPLACES_ADJUSTS", "true")
                    .containsEntry("INVOICE_TYPE", "PUR")
                    .containsEntry("INVOICE_STATUS", "APP")
                    .containsEntry("SUBMISSION_STATUS", "COM")
                    .containsEntry("SUBMISSION_MONTH_YEAR", "202006")
                    .containsEntry("USER_ID", "TESTUSER");
        }

        @Test
        void shouldUseDefaults_whenOptionalParamsMissing() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .doesNotContainKey("SHOW_REPLACES_ADJUSTS")
                    .doesNotContainKey("SUBMISSION_MONTH_YEAR")
                    .doesNotContainKey("USER_ID")
                    .containsEntry("MATURITY", "O,S,M")
                    .containsEntry("INVOICE_TYPE", "ADJ,CAN,PUR,SAL")
                    .containsEntry("INVOICE_STATUS", "PRO,UNA,APP,CAN,DFT,DVF,REJ,VER")
                    .containsEntry("SUBMISSION_STATUS", "COM,INB,LOB,REJ");
        }

        @Test
        void shouldUseCustomMaturityCodes() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
            r.setMaturityCodes("S,C");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("MATURITY", "S,C");
        }

        @Test
        void shouldUseProvidedDateTo_whenDateToGiven() {
            R07ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setDateTo("20200630");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200630");
        }

        @Test
        void shouldComputeDateTo_byAddingTimeFrameMonths() {
            R07ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("2");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200331");
        }

        @Test
        void shouldComputeDateTo_asEndOfDateFromMonth_whenTimeFrameBlank() {
            R07ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("   ");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200131");
        }
    }
}
