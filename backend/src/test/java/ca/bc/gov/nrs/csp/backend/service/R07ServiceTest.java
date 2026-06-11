package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R07ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
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
class R07ServiceTest {

    @Mock
    JasperServerService jasperServerService;
    @Mock
    SearchService searchService;
    @Mock
    CspSubmissionRepository cspSubmissionRepository;

    R07Service service;

    static final ClientLocation ACME = new ClientLocation("00000001", "Acme Logging", "00", "Main", "Victoria", "BC");

    @BeforeEach
    void setUp() {
        service = new R07Service(jasperServerService, searchService, cspSubmissionRepository);
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
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1});

            ReportResult result = service.generateReport(r);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldAccept_whenDateFromProvided() {
            R07ReportRequest r = baseRequest();
            r.setDateFrom("20200101");
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }

        @Test
        void shouldAccept_whenSellerClientNumberProvided() {
            R07ReportRequest r = baseRequest();
            r.setSellerClientNumber("12345");
            given(searchService.findClientsByNumber("12345")).willReturn(List.of(ACME));
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }

        @Test
        void shouldAccept_whenSubmissionNumberProvided() {
            R07ReportRequest r = baseRequest();
            r.setSubmissionNumber("999");
            given(cspSubmissionRepository.existsBySubmissionNumber(999L)).willReturn(true);
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
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
    @DisplayName("generateReport()")
    class GenerateReport {

        @Test
        void shouldReturnResult_whenJasperReturnsData() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1, 2});

            ReportResult result = service.generateReport(r);

            assertThat(result.data()).isEqualTo(new byte[]{1, 2});
            assertThat(result.filename()).startsWith("R07_").endsWith(".pdf");
        }

        @Test
        void shouldReturnCsvFilename_whenFormatIsCsv() {
            R07ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            r.setYear(2020);
            r.setMonth(1);
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r).filename()).endsWith(".csv");
        }

        @Test
        void shouldThrowResourceNotFound_whenJasperReturnsEmpty() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(null);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R07");
        }
    }

    @Nested
    @DisplayName("resolveClient()")
    class ResolveClient {

        @Test
        void shouldResolveSellerNameFromNumber() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
            r.setSellerClientNumber("1");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME));
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            org.mockito.Mockito.verify(jasperServerService).generateReport(eq("R07"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params.get("SELLER_NUMBER")).isEqualTo("00000001");
            assertThat(params.get("SELLER_NAME")).isEqualTo("Acme Logging");
            assertThat(params.get("SELLER_CLIENT_LOCN_CODE")).isEqualTo("00");
        }

        @Test
        void shouldResolveSellerNumberFromName() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
            r.setSellerClientName("Acme");
            given(searchService.findClientsByName("Acme")).willReturn(List.of(ACME));
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            org.mockito.Mockito.verify(jasperServerService).generateReport(eq("R07"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params.get("SELLER_NUMBER")).isEqualTo("00000001");
            assertThat(params.get("SELLER_NAME")).isEqualTo("Acme Logging");
        }

        @Test
        void shouldResolveBuyerNameFromNumber() {
            R07ReportRequest r = baseRequest();
            r.setYear(2020);
            r.setMonth(1);
            r.setBuyerClientNumber("1");
            given(searchService.findClientsByNumber("1")).willReturn(List.of(ACME));
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            org.mockito.Mockito.verify(jasperServerService).generateReport(eq("R07"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params.get("BUYER_NUMBER")).isEqualTo("00000001");
            assertThat(params.get("BUYER_NAME")).isEqualTo("Acme Logging");
            assertThat(params.get("BUYER_CLIENT_LOCN_CODE")).isEqualTo("00");
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
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            org.mockito.Mockito.verify(jasperServerService).generateReport(eq("R07"), captor.capture());

            assertThat(captor.getValue().get("SELLER_CLIENT_LOCN_CODE")).isEqualTo("01");
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
            given(jasperServerService.generateReport(eq("R07"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            org.mockito.Mockito.verify(jasperServerService).generateReport(eq("R07"), captor.capture());

            Map<String, Object> params = captor.getValue();
            assertThat(params).doesNotContainKey("SELLER_NUMBER");
            assertThat(params).doesNotContainKey("BUYER_NUMBER");
        }
    }
}
