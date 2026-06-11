package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R08ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.reporting.JasperServerService;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class R08ServiceTest {

    @Mock
    JasperServerService jasperServerService;
    @Mock
    SearchService searchService;

    R08Service service;

    static final ClientLocation ACME = new ClientLocation("00000001", "Acme Logging", "00", "Main", "Victoria", "BC");

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
            org.mockito.Mockito.verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params.get("SELLER_NUMBER")).isEqualTo("00000001");
            assertThat(params.get("SELLER_NAME")).isEqualTo("Acme Logging");
            assertThat(params.get("SELLER_CLIENT_LOCN_CODE")).isEqualTo("00");
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
            org.mockito.Mockito.verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params.get("SELLER_NUMBER")).isEqualTo("00000001");
            assertThat(params.get("SELLER_NAME")).isEqualTo("Acme Logging");
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
            org.mockito.Mockito.verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params.get("BUYER_NUMBER")).isEqualTo("00000001");
            assertThat(params.get("BUYER_NAME")).isEqualTo("Acme Logging");
            assertThat(params.get("BUYER_CLIENT_LOCN_CODE")).isEqualTo("00");
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
            org.mockito.Mockito.verify(jasperServerService).generateReport(eq("R08"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params).doesNotContainKey("SELLER_NUMBER");
            assertThat(params).doesNotContainKey("BUYER_NUMBER");
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
            org.mockito.Mockito.verify(jasperServerService).generateReport(eq("R08"), captor.capture());
            return captor.getValue();
        }

        @Test
        void shouldDeriveYearAndMonthFromSubmissionYearMonth() {
            R08ReportRequest r = baseRequest();
            r.setSubmissionYearMonth("202003");

            Map<String, Object> params = captureParams(r);

            assertThat(params.get("YEAR")).isEqualTo("2020");
            assertThat(params.get("MONTH")).isEqualTo("03");
        }

        @Test
        void shouldExtendInvoiceDateToByTimeFrameMonths() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            r.setTimeFrame("3");

            Map<String, Object> params = captureParams(r);

            assertThat(params.get("INVOICE_DATE_TO")).isEqualTo("20200430");
        }

        @Test
        void shouldEndAtStartMonth_whenNoTimeFrameOrEndDate() {
            R08ReportRequest r = baseRequest();
            r.setDateFrom("20200101");

            Map<String, Object> params = captureParams(r);

            assertThat(params.get("INVOICE_DATE_TO")).isEqualTo("20200131");
        }
    }
}
