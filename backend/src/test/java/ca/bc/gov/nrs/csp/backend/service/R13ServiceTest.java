package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R13ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R13ShowOptions;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.ReportFormat;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

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

            // Validation passes; JRXML loading from classpath will fail (expected in unit test context)
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
}
