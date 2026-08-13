package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R10ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperReportRenderer;
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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class R10ServiceTest {

    @Mock
    DataSource dataSource;
    @Mock
    SearchService searchService;

    R10Service service;

    static final ClientLocation ACME = new ClientLocation("00000001", "Acme Logging", "00", "Main", "Victoria", "BC");

    @BeforeEach
    void setUp() {
        service = new R10Service(new JasperReportRenderer(dataSource), searchService, Clock.systemUTC());
        ReflectionTestUtils.setField(service, "r10TemplatePath", "/reports/R10.jrxml");
        ReflectionTestUtils.setField(service, "r10CsvTemplatePath", "/reports/R10_CSV.jrxml");
    }

    private R10ReportRequest baseRequest() {
        R10ReportRequest r = new R10ReportRequest();
        r.setReportFormat(ReportFormat.PDF);
        r.setDateFrom("20200101");
        return r;
    }

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        void shouldThrow_whenDateFromIsNull() {
            R10ReportRequest r = new R10ReportRequest();
            r.setReportFormat(ReportFormat.PDF);
            r.setDateTo("20201231");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(m -> m.messageKey())
                    .anyMatch(key -> key.contains("report.startdate.required.error"));
        }

        @Test
        void shouldThrow_whenNeitherDateToNorTimeFrameProvided() {
            R10ReportRequest r = baseRequest();

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(m -> m.messageKey())
                    .anyMatch(key -> key.contains("report.r10.enddate.or.timeframe.required.error"));
        }

        @Test
        void shouldThrow_whenDateFromIsAfterDateTo() {
            R10ReportRequest r = baseRequest();
            r.setDateFrom("20201231");
            r.setDateTo("20200101");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(m -> m.messageKey())
                    .anyMatch(key -> key.contains("report.daterange.order.error"));
        }

        @Test
        void shouldAccept_whenDateToProvided() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");

            // Validation passes; the empty fill against the mock DataSource then yields
            // "no data" (ResourceNotFoundException), which is not a validation failure.
            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldAccept_whenTimeFrameProvided() {
            R10ReportRequest r = baseRequest();
            r.setTimeFrame("3");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldThrow_whenTimeFrameIsNonNumeric() {
            R10ReportRequest r = baseRequest();
            r.setTimeFrame("abc");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("timeFrame must be a numeric value");
        }

        @Test
        void shouldThrow_whenSellerClientNumberNotFound() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            r.setSellerClientNumber("9999");
            given(searchService.findClientsByNumber("9999")).willReturn(List.of());

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(m -> m.messageKey())
                    .anyMatch(key -> key.contains("report.client.number.notfound.error"));
        }

        @Test
        void shouldThrow_whenBuyerClientNumberNotFound() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            r.setBuyerClientNumber("9999");
            given(searchService.findClientsByNumber("9999")).willReturn(List.of());

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(m -> m.messageKey())
                    .anyMatch(key -> key.contains("report.client.number.notfound.error"));
        }

        @Test
        void shouldAccept_whenSellerClientNumberExists() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            r.setSellerClientNumber("00000001");
            given(searchService.findClientsByNumber("00000001")).willReturn(List.of(ACME));

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("buildParams() date handling")
    class DateHandling {

        private Map<String, Object> buildParams(R10ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "buildParams", r);
        }

        @Test
        void shouldRoundDatesToMonthBoundaries() {
            R10ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setDateTo("20200320");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200331");
        }

        @Test
        void shouldComputeDateToInclusivelyFromTimeFrame() {
            R10ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("3");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200331");
        }

        @Test
        void shouldUseEndOfStartMonth_whenTimeFrameIsOne() {
            R10ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("1");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200131");
        }
    }

    @Nested
    @DisplayName("buildParams() optional criteria")
    class OptionalCriteria {

        private Map<String, Object> buildParams(R10ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "buildParams", r);
        }

        @Test
        void shouldIncludeAllOptionalCriteria_whenProvided() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            r.setSellerClientNumber("00000001");
            r.setSellerLocnCode("00");
            r.setBuyerClientNumber("00000002");
            r.setBuyerLocnCode("01");
            r.setMaturityCodes("M,I");
            r.setInvoiceTypeCode("SI");

            Map<String, Object> params = buildParams(r);

            // SELLER_CLIENT_LOCN_CODE / BUYER_CLIENT_LOCN_CODE are the jrxml/stored-proc parameter
            // names (verified against R10.jrxml and the original pre-JR7 export) — the previous
            // short names here were a pre-existing bug that silently dropped these filters.
            assertThat(params)
                    .containsEntry("SELLER_CLIENT_NUMBER", "00000001")
                    .containsEntry("SELLER_CLIENT_LOCN_CODE", "00")
                    .containsEntry("BUYER_CLIENT_NUMBER", "00000002")
                    .containsEntry("BUYER_CLIENT_LOCN_CODE", "01")
                    .containsEntry("MATURITY", "M,I")
                    .containsEntry("INVOICE_TYPE_CODE", "SI");
        }

        @Test
        void shouldOmitOptionalCriteria_whenNotProvided() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .doesNotContainKeys("SELLER_CLIENT_NUMBER", "SELLER_CLIENT_LOCN_CODE",
                            "BUYER_CLIENT_NUMBER", "BUYER_CLIENT_LOCN_CODE",
                            "MATURITY", "INVOICE_TYPE_CODE", "TIME_FRAME", "USER_ID");
        }

        @Test
        void shouldIncludeTimeFrameParam_whenProvided() {
            R10ReportRequest r = baseRequest();
            r.setTimeFrame("3");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("TIME_FRAME", "3");
        }

        @Test
        void shouldUseRequestUserId_whenNoAuthenticatedUser() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            r.setUserId("REQUEST_USER");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("USER_ID", "REQUEST_USER");
        }

        @Test
        void shouldPreferAuthenticatedUser_overRequestUserId() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("IDIR_USER", null, List.of()));
            try {
                R10ReportRequest r = baseRequest();
                r.setDateTo("20201231");
                r.setUserId("REQUEST_USER");

                Map<String, Object> params = buildParams(r);

                assertThat(params).containsEntry("USER_ID", "IDIR_USER");
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    @Nested
    @DisplayName("generateReport() — compile/fill failures")
    class GenerateReportFailures {

        @Test
        void shouldThrowReportGenerationException_whenTemplateNotFoundOnClasspath() {
            ReflectionTestUtils.setField(service, "r10TemplatePath", "/reports/DOES_NOT_EXIST.jrxml");
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to compile JRXML");
        }

        @Test
        void shouldThrowReportGenerationException_whenConnectionCannotBeObtained() throws Exception {
            given(dataSource.getConnection()).willThrow(new SQLException("boom"));
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to fill R10 report from database")
                    .hasCauseInstanceOf(SQLException.class);
        }

        @Test
        void shouldThrowResourceNotFound_whenReportHasNoPages() {
            // The mock DataSource yields no JDBC connection, so the fill produces no data rows
            // and the report (whenNoDataType = NoPages) ends up with zero pages. This is also the
            // regression guard for the R10.jrxml <query language="plsql"> registration: if the
            // "plsql" -> JRJdbcQueryExecuterFactory alias in resources/jasperreports.properties
            // were missing, this would instead throw a ReportGenerationException wrapping a
            // JRException ("No suitable query executer factory found for language plsql") rather
            // than reaching ResourceNotFoundException.
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R10");
        }

        @Test
        void shouldCompileCsvTemplateWithoutError_whenFormatIsCsv() {
            R10ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            r.setDateTo("20201231");

            // Proves R10_CSV.jrxml compiles and reaches the fill stage (same no-pages outcome as
            // the PDF template, since the mock DataSource yields no connection either way).
            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R10");
        }
    }

    /**
     * Exercises the real {@code Connection} → {@code CallableStatement} binding path that every
     * other test in this class bypasses (they leave {@code dataSource.getConnection()}
     * unstubbed, which returns {@code null} and short-circuits before JasperReports ever prepares
     * a statement). R10.jrxml's {@code <query language="plsql">} is a real Oracle stored-procedure
     * call with a {@code REPORT_CURSOR} OUT parameter — this is exactly the path that silently
     * broke in production (JasperReports' built-in {@code OracleProcedureCallHandlerFactory}
     * reflectively loads a class that doesn't exist in the open-source jar, returns null, and the
     * REPORT_CURSOR parameter then fails to bind with {@code ORA-17004: Invalid column type: 2000}
     * — see {@code RefCursorProcedureCallHandlerFactory}'s Javadoc). A mocked {@code DataSource}
     * can never prove the report renders correct data, but it CAN prove the JDBC binding sequence
     * itself is wired correctly — which is exactly the class of bug this regression guard exists
     * to catch without needing a live Oracle connection.
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

            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");

            // No rows from the (mocked) cursor -> zero pages -> ResourceNotFoundException is the
            // correct, successful outcome here; the point of this test is that binding succeeds
            // at all, not that it returns data.
            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(connection).prepareCall(anyString());
            verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
            verify(callableStatement).execute();
        }
    }
}
