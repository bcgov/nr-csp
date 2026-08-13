package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R06ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
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
class R06ServiceTest {

    @Mock
    DataSource dataSource;
    @Mock
    SearchService searchService;

    R06Service service;

    static final ClientLocation ACME = new ClientLocation("00000001", "Acme Logging", "00", "Main", "Victoria", "BC");

    @BeforeEach
    void setUp() {
        service = new R06Service(new JasperReportRenderer(dataSource), searchService, Clock.systemUTC());
        ReflectionTestUtils.setField(service, "r06TemplatePath", "/reports/R06.jrxml");
        ReflectionTestUtils.setField(service, "r06CsvTemplatePath", "/reports/R06_CSV.jrxml");
        ReflectionTestUtils.setField(service, "r06Subreport1Path", "/reports/r06_subreport1.jrxml");
        ReflectionTestUtils.setField(service, "r06Subreport1CsvPath", "/reports/r06_subreport1_CSV.jrxml");
        ReflectionTestUtils.setField(service, "r06Subreport2Path", "/reports/r06_subreport2.jrxml");
        ReflectionTestUtils.setField(service, "r06Subreport2CsvPath", "/reports/r06_subreport2_CSV.jrxml");
    }

    private R06ReportRequest baseRequest() {
        R06ReportRequest r = new R06ReportRequest();
        r.setReportFormat(ReportFormat.PDF);
        r.setDateFrom("20200101");
        r.setDateTo("20200131");
        return r;
    }

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        void shouldThrow_whenDateFromIsAfterDateTo() {
            R06ReportRequest r = baseRequest();
            r.setDateFrom("20201231");
            r.setDateTo("20200101");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.daterange.order.error"));
        }

        @Test
        void shouldAccept_whenDateFromEqualsDateTo() {
            R06ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setDateTo("20200101");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldThrow_whenNoDatesAndNoInvoiceNumbers() {
            R06ReportRequest r = new R06ReportRequest();
            r.setReportFormat(ReportFormat.PDF);

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.r06.startdate.required.error"));
        }

        @Test
        void shouldAccept_whenInvoiceNumbersProvidedWithoutDates() {
            R06ReportRequest r = new R06ReportRequest();
            r.setReportFormat(ReportFormat.PDF);
            r.setInvoiceNumbers("12345");

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldThrow_whenSellerClientNumberNotFound() {
            R06ReportRequest r = baseRequest();
            r.setSellerClientNumber("9999");
            given(searchService.findClientsByNumber("9999")).willReturn(List.of());

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.client.number.notfound.error"));
        }

        @Test
        void shouldThrow_whenBuyerClientNumberNotFound() {
            R06ReportRequest r = baseRequest();
            r.setBuyerClientNumber("9999");
            given(searchService.findClientsByNumber("9999")).willReturn(List.of());

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.client.number.notfound.error"));
        }

        @Test
        void shouldAccept_whenSellerClientNumberExists() {
            R06ReportRequest r = baseRequest();
            r.setSellerClientNumber("00000001");
            given(searchService.findClientsByNumber("00000001")).willReturn(List.of(ACME));

            assertThatThrownBy(() -> service.generateReport(r))
                    .isNotInstanceOf(ValidationException.class);
        }

        @Test
        void shouldThrow_whenInvoiceNumberExceedsMaxLength() {
            R06ReportRequest r = baseRequest();
            r.setInvoiceNumbers("ABCDEFGHIJKLMNOP,ABCDEFGHIJKLMNOP");

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.r06.invoicenumber.length.error"));
        }
    }

    @Nested
    @DisplayName("generateReport() — compile/fill failures")
    class GenerateReportFailures {

        @Test
        void shouldThrowReportGenerationException_whenMainTemplateNotFoundOnClasspath() {
            ReflectionTestUtils.setField(service, "r06TemplatePath", "/reports/DOES_NOT_EXIST.jrxml");
            R06ReportRequest r = baseRequest();

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to compile JRXML");
        }

        @Test
        void shouldThrowReportGenerationException_whenSubreportTemplateNotFoundOnClasspath() {
            ReflectionTestUtils.setField(service, "r06Subreport2Path", "/reports/DOES_NOT_EXIST.jrxml");
            R06ReportRequest r = baseRequest();

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to compile JRXML");
        }

        @Test
        void shouldThrowReportGenerationException_whenConnectionCannotBeObtained() throws Exception {
            given(dataSource.getConnection()).willThrow(new SQLException("boom"));
            R06ReportRequest r = baseRequest();

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to fill R06 report from database")
                    .hasCauseInstanceOf(SQLException.class);
        }

        @Test
        void shouldThrowResourceNotFound_whenReportHasNoPages() {
            // Also exercises the full real compile+subreport-wiring path (main + both subreports)
            // against a mocked DataSource that yields no JDBC connection — the fill produces no
            // data rows and the report ends up with zero pages.
            R06ReportRequest r = baseRequest();

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R06");
        }

        @Test
        void shouldCompileCsvTemplateChainWithoutError_whenFormatIsCsv() {
            R06ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R06");
        }
    }

    /**
     * Exercises the real {@code Connection} → {@code CallableStatement} binding path that every
     * other test in this class bypasses (they leave {@code dataSource.getConnection()}
     * unstubbed, which returns {@code null} and short-circuits before JasperReports ever prepares
     * a statement). See {@code R10ServiceTest.RealProcedureCallBinding} for the full rationale.
     *
     * <p>Only the main report's {@code CallableStatement} is exercised here: R06's subreport
     * element lives inside a {@code groupHeader} band gated on {@code $F{COASTAL_LOG_SALE_ID}},
     * which only evaluates when the main query returns at least one row — with a mocked cursor
     * returning zero rows (as below), the subreport chain is never reached, so this test only
     * proves the top-level {@code CSP_SP_RPT_06} call binds correctly, not the subreports'.</p>
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

            R06ReportRequest r = baseRequest();

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(connection).prepareCall(anyString());
            verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
            verify(callableStatement).execute();
        }
    }

    @Nested
    @DisplayName("buildParams()")
    class BuildParams {

        private Map<String, Object> buildParams(R06ReportRequest r) {
            return ReflectionTestUtils.invokeMethod(service, "buildParams", r);
        }

        @Test
        void shouldUppercaseInvoiceNumbersInParams() {
            R06ReportRequest r = baseRequest();
            r.setInvoiceNumbers("abc123,abc456");

            Map<String, Object> params = buildParams(r);

            assertThat(params).containsEntry("CLIENT_INVOICE_NO", "ABC123,ABC456");
        }

        @Test
        void shouldIncludeAllOptionalCriteriaInParams() {
            R06ReportRequest r = baseRequest();
            r.setSellerClientNumber("00000001");
            r.setSellerLocCode("00");
            r.setBuyerClientNumber("00000002");
            r.setBuyerLocCode("01");
            r.setSubmissionId(1234567890L);
            r.setMaturityCodes("C,M");
            r.setLogSaleEntryStatusCode("A");
            r.setCspInvoiceTypeCode("S");

            Map<String, Object> params = buildParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_FROM", "20200101")
                    .containsEntry("INVOICE_TO", "20200131")
                    .containsEntry("SELLER_CLIENT_NUMBER", "00000001")
                    .containsEntry("SELLER_CLIENT_LOC_CODE", "00")
                    .containsEntry("BUYER_CLIENT_NUMBER", "00000002")
                    .containsEntry("BUYER_CLIENT_LOC_CODE", "01")
                    .containsEntry("SUBMISSION_ID", 1234567890L)
                    .containsEntry("LOG_SALE_TYPE_CODE_MATURITY", "C,M")
                    .containsEntry("LOG_SALE_ENTRY_STATUS_CODE", "A")
                    .containsEntry("CSP_INVOICE_TYPE_CODE", "S");
        }

        @Test
        void shouldOmitOptionalCriteriaAndUserId_whenNotProvided() {
            R06ReportRequest r = baseRequest();

            Map<String, Object> params = buildParams(r);

            assertThat(params).doesNotContainKeys(
                    "SELLER_CLIENT_NUMBER", "SELLER_CLIENT_LOC_CODE",
                    "BUYER_CLIENT_NUMBER", "BUYER_CLIENT_LOC_CODE",
                    "SUBMISSION_ID", "CLIENT_INVOICE_NO",
                    "LOG_SALE_TYPE_CODE_MATURITY", "LOG_SALE_ENTRY_STATUS_CODE",
                    "CSP_INVOICE_TYPE_CODE", "USER_ID");
        }

        @Test
        void shouldUseRequestUserId_whenNoAuthenticatedUser() {
            R06ReportRequest r = baseRequest();
            r.setUserId("CLIENTUSER");

            assertThat(buildParams(r)).containsEntry("USER_ID", "CLIENTUSER");
        }

        @Test
        void shouldPreferAuthenticatedUser_overRequestUserId() {
            R06ReportRequest r = baseRequest();
            r.setUserId("CLIENTUSER");
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("JDOE", null, List.of()));

            try {
                assertThat(buildParams(r)).containsEntry("USER_ID", "JDOE");
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
