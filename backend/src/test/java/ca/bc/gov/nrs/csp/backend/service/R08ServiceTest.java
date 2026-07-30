package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R08ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperServerService;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class R08ServiceTest {

    @Mock
    JasperServerService jasperServerService;
    @Mock
    SearchService searchService;

    R08Service service;

    static final ClientLocation ACME = new ClientLocation("00000001", "Acme Logging", "00", "Main", "Victoria", "BC");
    static final ClientLocation ACME_BRANCH = new ClientLocation("00000001", "Acme Logging", "01", "Branch", "Nanaimo", "BC");

    @BeforeEach
    void setUp() {
        service = new R08Service(jasperServerService, searchService);
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
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }

        @Test
        void shouldAccept_whenSubmissionNumberProvided() {
            R08ReportRequest r = baseRequest();
            r.setSubmissionNumber("12345");
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }

        @Test
        void shouldAccept_whenSubmissionYearMonthProvided() {
            R08ReportRequest r = baseRequest();
            r.setSubmissionYearMonth("202001");
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }
    }

    @Nested
    @DisplayName("generateReport()")
    class GenerateReport {

        @Test
        void shouldReturnResult_whenJasperReturnsData() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{42});

            ReportResult result = service.generateReport(r);

            assertThat(result.data()).isEqualTo(new byte[]{42});
            assertThat(result.filename()).startsWith("R08_").endsWith(".pdf");
        }

        @Test
        void shouldReturnCsvFilename_whenFormatIsCsv() {
            R08ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            r.setDateFrom("20200101");
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r).filename()).endsWith(".csv");
        }

        @Test
        void shouldThrowResourceNotFound_whenJasperReturnsEmpty() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[0]);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R08");
        }

        @Test
        void shouldThrowResourceNotFound_whenJasperReturnsNull() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(null);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R08");
        }
    }

    @Nested
    @DisplayName("resolveClient()")
    class ResolveClient {

        @Test
        void shouldResolveSellerNameFromNumber() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("1");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME));
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
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
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
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
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
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
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
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
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params)
                    .containsEntry("SELLER_NUMBER", "00000001")
                    .containsEntry("SELLER_NAME", "Acme Logging");
        }

        @Test
        void shouldNotSetSellerParams_whenSellerNameIsBlank() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientName("   ");
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params)
                    .doesNotContainKey("SELLER_NUMBER")
                    .doesNotContainKey("SELLER_NAME");
        }

        @Test
        void shouldNotSetSellerParams_whenNumberLookupBecomesEmptyAfterValidation() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("1");
            // Validation lookup succeeds, but the second lookup inside buildParams returns empty.
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME), List.of());
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params)
                    .doesNotContainKey("SELLER_NUMBER")
                    .doesNotContainKey("SELLER_NAME");
        }

        @Test
        void shouldNotSetSellerParams_whenNameLookupBecomesEmptyAfterValidation() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientName("Acme");
            // Validation lookup succeeds, but the second lookup inside buildParams returns empty.
            given(searchService.findClientsByName("Acme")).willReturn(List.of(ACME), List.of());
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
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
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params).containsEntry("SELLER_CLIENT_LOCN_CODE", "01");
        }

        @Test
        void shouldDefaultToLocationZeroZero_whenLocCodeIsBlank() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("1");
            r.setSellerLocCode("   ");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME_BRANCH, ACME));
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params).containsEntry("SELLER_CLIENT_LOCN_CODE", "00");
        }

        @Test
        void shouldFallBackToFirstLocation_whenLocCodeMatchesNothing() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setSellerClientNumber("1");
            r.setSellerLocCode("99");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME_BRANCH, ACME));
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params).containsEntry("SELLER_CLIENT_LOCN_CODE", "01");
        }
    }

    @Nested
    @DisplayName("buildParams()")
    class BuildParams {

        @SuppressWarnings("unchecked")
        private Map<String, Object> captureParams(R08ReportRequest r) {
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());
            return captor.getValue();
        }

        @Test
        void shouldDeriveYearAndMonthFromSubmissionYearMonth() {
            R08ReportRequest r = baseRequest();
            r.setSubmissionYearMonth("202003");

            Map<String, Object> params = captureParams(r);

            assertThat(params)
                    .containsEntry("YEAR", "2020")
                    .containsEntry("MONTH", "03");
        }

        @Test
        void shouldExtendInvoiceDateToByTimeFrameMonths() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setTimeFrame("3");

            Map<String, Object> params = captureParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200430");
        }

        @Test
        void shouldEndAtStartMonth_whenNoTimeFrameOrEndDate() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            Map<String, Object> params = captureParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200131");
        }

        @Test
        void shouldUseYearAndMonthParams_whenProvided() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setYear(2021);
            r.setMonth(5);

            Map<String, Object> params = captureParams(r);

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

            Map<String, Object> params = captureParams(r);

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

            Map<String, Object> params = captureParams(r);

            assertThat(params)
                    .containsEntry("YEAR", "2020")
                    .containsEntry("MONTH", "03");
        }

        @Test
        void shouldUseProvidedDateTo_whenDateToPresent() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setDateTo("20200315");

            Map<String, Object> params = captureParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200315");
        }

        @Test
        void shouldTreatBlankTimeFrameAsAbsent() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("   ");

            Map<String, Object> params = captureParams(r);

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
            r.setSubmissionStatus("COM");

            Map<String, Object> params = captureParams(r);

            assertThat(params)
                    .containsEntry("MATURITY", "O,C")
                    .containsEntry("TYPE_CODE_MATURITY", "O,C")
                    .containsEntry("TYPE_CODE_MATURITY_DESCRIPTION", "Old Growth, Cants")
                    .containsEntry("INVOICE_TYPE", "PUR")
                    .containsEntry("INVOICE_STATUS", "APP")
                    .containsEntry("SUBMISSION_STATUS", "COM");
        }

        @Test
        void shouldUseDefaultFilterCodes_whenNoneProvided() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            Map<String, Object> params = captureParams(r);

            assertThat(params)
                    .containsEntry("MATURITY", "O,S,M")
                    .containsEntry("TYPE_CODE_MATURITY_DESCRIPTION",
                            "Old Growth, Second Growth, Mixed Growth")
                    .containsEntry("INVOICE_TYPE", "ADJ,CAN,PUR,SAL")
                    .containsEntry("INVOICE_STATUS", "PRO,UNA,APP,CAN,DFT,DVF,REJ,VER")
                    .containsEntry("SUBMISSION_STATUS", "COM,INB,LOB,REJ");
        }

        @Test
        void shouldSkipUnknownMaturityCodes() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setMaturityCodes("O,X,C");

            Map<String, Object> params = captureParams(r);

            assertThat(params).containsEntry("TYPE_CODE_MATURITY_DESCRIPTION", "Old Growth, Cants");
        }

        @Test
        void shouldReturnEmptyMaturityDescription_whenMaturityCodesBlank() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setMaturityCodes("");

            Map<String, Object> params = captureParams(r);

            assertThat(params)
                    .containsEntry("MATURITY", "")
                    .containsEntry("TYPE_CODE_MATURITY_DESCRIPTION", "");
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

        @SuppressWarnings("unchecked")
        private Map<String, Object> captureParams(R08ReportRequest r) {
            given(jasperServerService.generateReport(eq("R08"), any())).willReturn(new byte[]{1});
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R08"), captor.capture());
            return captor.getValue();
        }

        @Test
        void shouldPreferAuthenticatedUsernameOverRequestUserId() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("TESTUSER", null, List.of()));
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setUserId("CLIENTUSER");

            Map<String, Object> params = captureParams(r);

            assertThat(params).containsEntry("USER_ID", "TESTUSER");
        }

        @Test
        void shouldFallBackToRequestUserId_whenNoAuthenticatedUser() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setUserId("IDIRUSER");

            Map<String, Object> params = captureParams(r);

            assertThat(params).containsEntry("USER_ID", "IDIRUSER");
        }

        @Test
        void shouldOmitUserId_whenNoUserAvailable() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            Map<String, Object> params = captureParams(r);

            assertThat(params).doesNotContainKey("USER_ID");
        }
    }
}
