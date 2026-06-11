package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R06ReportRequest;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class R06ServiceTest {

    @Mock
    JasperServerService jasperServerService;
    @Mock
    SearchService searchService;

    R06Service service;

    static final ClientLocation ACME = new ClientLocation("00000001", "Acme Logging", "00", "Main", "Victoria", "BC");

    @BeforeEach
    void setUp() {
        service = new R06Service(jasperServerService, searchService);
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
            given(jasperServerService.generateReport(eq("R06"), any())).willReturn(new byte[]{1});

            ReportResult result = service.generateReport(r);

            assertThat(result.filename()).startsWith("R06_").endsWith(".pdf");
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
            given(jasperServerService.generateReport(eq("R06"), any())).willReturn(new byte[]{1});

            ReportResult result = service.generateReport(r);

            assertThat(result).isNotNull();
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
            given(jasperServerService.generateReport(eq("R06"), any())).willReturn(new byte[]{1});

            assertThat(service.generateReport(r)).isNotNull();
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

        @Test
        void shouldUppercaseInvoiceNumbersInParams() {
            R06ReportRequest r = baseRequest();
            r.setInvoiceNumbers("abc123,abc456");
            given(jasperServerService.generateReport(eq("R06"), any())).willReturn(new byte[]{1});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            service.generateReport(r);
            verify(jasperServerService).generateReport(eq("R06"), captor.capture());

            assertThat(captor.getValue()).containsEntry("CLIENT_INVOICE_NO", "ABC123,ABC456");
        }
    }

    @Nested
    @DisplayName("generateReport()")
    class GenerateReport {

        @Test
        void shouldReturnResult_whenJasperReturnsData() {
            R06ReportRequest r = baseRequest();
            given(jasperServerService.generateReport(eq("R06"), any())).willReturn(new byte[]{1, 2, 3});

            ReportResult result = service.generateReport(r);

            assertThat(result.data()).isEqualTo(new byte[]{1, 2, 3});
            assertThat(result.filename()).startsWith("R06_").endsWith(".pdf");
        }

        @Test
        void shouldReturnCsvFilename_whenFormatIsCsv() {
            R06ReportRequest r = baseRequest();
            r.setReportFormat(ReportFormat.CSV);
            given(jasperServerService.generateReport(eq("R06"), any())).willReturn(new byte[]{1});

            ReportResult result = service.generateReport(r);

            assertThat(result.filename()).endsWith(".csv");
        }

        @Test
        void shouldThrowResourceNotFound_whenJasperReturnsEmpty() {
            R06ReportRequest r = baseRequest();
            given(jasperServerService.generateReport(eq("R06"), any())).willReturn(new byte[0]);

            assertThatThrownBy(() -> service.generateReport(r))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("R06");
        }
    }
}
