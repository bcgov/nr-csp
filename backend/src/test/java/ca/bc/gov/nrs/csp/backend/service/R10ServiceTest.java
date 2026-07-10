package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R10ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
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
class R10ServiceTest {

    @Mock
    JasperServerService jasperServerService;
    @Mock
    SearchService searchService;

    R10Service service;

    static final ClientLocation ACME = new ClientLocation("00000001", "Acme Logging", "00", "Main", "Victoria", "BC");

    @BeforeEach
    void setUp() {
        service = new R10Service(jasperServerService, searchService);
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
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.startdate.required.error"));
        }

        @Test
        void shouldThrow_whenNeitherDateToNorTimeFrameProvided() {
            R10ReportRequest r = baseRequest();

            ValidationException ex = catchThrowableOfType(
                    () -> service.generateReport(r), ValidationException.class);

            assertThat(ex.getResult().errors())
                    .extracting(ValidationMessage::messageKey)
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
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.daterange.order.error"));
        }

        @Test
        void shouldAccept_whenDateToProvided() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            given(jasperServerService.generateReport(eq("R10"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }

        @Test
        void shouldAccept_whenTimeFrameProvided() {
            R10ReportRequest r = baseRequest();
            r.setTimeFrame("3");
            given(jasperServerService.generateReport(eq("R10"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
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
                    .extracting(ValidationMessage::messageKey)
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
                    .extracting(ValidationMessage::messageKey)
                    .anyMatch(key -> key.contains("report.client.number.notfound.error"));
        }

        @Test
        void shouldAccept_whenSellerClientNumberExists() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            r.setSellerClientNumber("00000001");
            given(searchService.findClientsByNumber("00000001")).willReturn(List.of(ACME));
            given(jasperServerService.generateReport(eq("R10"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
        }
    }

    @Nested
    @DisplayName("buildParams() date handling")
    class DateHandling {

        @SuppressWarnings("unchecked")
        private Map<String, Object> capturedParams(R10ReportRequest r) {
            given(jasperServerService.generateReport(eq("R10"), any())).willReturn(new byte[]{1});
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R10"), captor.capture());
            return captor.getValue();
        }

        @Test
        void shouldRoundDatesToMonthBoundaries() {
            R10ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setDateTo("20200320");

            Map<String, Object> params = capturedParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200331");
        }

        @Test
        void shouldComputeDateToInclusivelyFromTimeFrame() {
            R10ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("3");

            Map<String, Object> params = capturedParams(r);

            assertThat(params)
                    .containsEntry("INVOICE_DATE_FROM", "20200101")
                    .containsEntry("INVOICE_DATE_TO", "20200331");
        }

        @Test
        void shouldUseEndOfStartMonth_whenTimeFrameIsOne() {
            R10ReportRequest r = baseRequest();
            r.setDateFrom("20200115");
            r.setTimeFrame("1");

            Map<String, Object> params = capturedParams(r);

            assertThat(params).containsEntry("INVOICE_DATE_TO", "20200131");
        }
    }

    @Nested
    @DisplayName("generateReport()")
    class GenerateReport {

        @Test
        void shouldReturnResult_whenJasperReturnsData() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            given(jasperServerService.generateReport(eq("R10"), any())).willReturn(new byte[]{5, 6});

            ReportResult result = service.generateReport(r);

            assertThat(result.data()).isEqualTo(new byte[]{5, 6});
            assertThat(result.filename()).startsWith("R10_").endsWith(".pdf");
        }

        @Test
        void shouldReturnCsvFilename_whenFormatIsCsv() {
            R10ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            r.setDateTo("20201231");
            given(jasperServerService.generateReport(eq("R10"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r).filename()).endsWith(".csv");
        }

        @Test
        void shouldThrowResourceNotFound_whenJasperReturnsEmpty() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            given(jasperServerService.generateReport(eq("R10"), any())).willReturn(new byte[0]);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R10");
        }

        @Test
        void shouldThrowResourceNotFound_whenJasperReturnsNull() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            given(jasperServerService.generateReport(eq("R10"), any())).willReturn(null);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R10");
        }
    }

    @Nested
    @DisplayName("buildParams() optional criteria")
    class OptionalCriteria {

        @SuppressWarnings("unchecked")
        private Map<String, Object> capturedParams(R10ReportRequest r) {
            given(jasperServerService.generateReport(eq("R10"), any())).willReturn(new byte[]{1});
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R10"), captor.capture());
            return captor.getValue();
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
            given(searchService.findClientsByNumber("00000001")).willReturn(List.of(ACME));
            given(searchService.findClientsByNumber("00000002")).willReturn(List.of(ACME));

            Map<String, Object> params = capturedParams(r);

            assertThat(params)
                    .containsEntry("SELLER_CLIENT_NUMBER", "00000001")
                    .containsEntry("SELLER_LOCN_CODE", "00")
                    .containsEntry("BUYER_CLIENT_NUMBER", "00000002")
                    .containsEntry("BUYER_LOCN_CODE", "01")
                    .containsEntry("MATURITY", "M,I")
                    .containsEntry("INVOICE_TYPE_CODE", "SI");
        }

        @Test
        void shouldOmitOptionalCriteria_whenNotProvided() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");

            Map<String, Object> params = capturedParams(r);

            assertThat(params)
                    .doesNotContainKeys("SELLER_CLIENT_NUMBER", "SELLER_LOCN_CODE",
                            "BUYER_CLIENT_NUMBER", "BUYER_LOCN_CODE",
                            "MATURITY", "INVOICE_TYPE_CODE", "TIME_FRAME", "USER_ID");
        }

        @Test
        void shouldIncludeTimeFrameParam_whenProvided() {
            R10ReportRequest r = baseRequest();
            r.setTimeFrame("3");

            Map<String, Object> params = capturedParams(r);

            assertThat(params).containsEntry("TIME_FRAME", "3");
        }

        @Test
        void shouldUseRequestUserId_whenNoAuthenticatedUser() {
            R10ReportRequest r = baseRequest();
            r.setDateTo("20201231");
            r.setUserId("REQUEST_USER");

            Map<String, Object> params = capturedParams(r);

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

                Map<String, Object> params = capturedParams(r);

                assertThat(params).containsEntry("USER_ID", "IDIR_USER");
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
