package ca.bc.gov.nrs.csp.backend.service.reporting;

import ca.bc.gov.nrs.csp.backend.config.JasperServerProperties;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    DataSource dataSource;
    @Mock
    Connection connection;

    private ReportService serviceWith(String fetchUrl) {
        JasperServerProperties props = new JasperServerProperties(
                "http://localhost/jasperserver/login",
                fetchUrl,
                "http://localhost/jasperserver/put",
                "/reports/CSP/",
                "user",
                "pass",
                true,
                5);
        return new ReportService(dataSource, props);
    }

    @Nested
    @DisplayName("renderLocal()")
    class RenderLocal {

        @Test
        void shouldThrow_whenTemplateNotFoundOnClasspath() throws Exception {
            given(dataSource.getConnection()).willReturn(connection);
            ReportService service = serviceWith("http://localhost/fetch/");

            assertThatThrownBy(() -> service.renderLocal("reports/DoesNotExist.jrxml", Map.of()))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Report template not found on classpath");

            // the connection was opened by try-with-resources and must be released
            verify(connection).close();
        }

        @Test
        void shouldWrapCompileFailure_whenTemplateIsNotValidJrxml() throws Exception {
            given(dataSource.getConnection()).willReturn(connection);
            ReportService service = serviceWith("http://localhost/fetch/");

            // an existing classpath resource that is XML but not a JasperReports template
            assertThatThrownBy(() -> service.renderLocal("log4j2-test.xml", Map.of()))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to render local report")
                    .cause().isNotNull();

            verify(connection).close();
        }

        @Test
        void shouldWrapSqlException_whenConnectionCannotBeObtained() throws Exception {
            given(dataSource.getConnection()).willThrow(new SQLException("db down"));
            ReportService service = serviceWith("http://localhost/fetch/");

            assertThatThrownBy(() -> service.renderLocal("reports/R13.jrxml", Map.of()))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to render local report")
                    .hasCauseInstanceOf(SQLException.class);
        }
    }

    @Nested
    @DisplayName("renderViaServer()")
    class RenderViaServer {

        @Test
        void shouldReturnBytes_andAppendQueryParameters() throws Exception {
            try (StubHttpServer server = StubHttpServer.start()) {
                server.enqueue(200, "REPORT-BYTES", "Content-Type: application/pdf");
                ReportService service = serviceWith(server.baseUrl() + "/rest/reports/");

                byte[] result = service.renderViaServer("R06", Map.of("FORMAT", "pdf"));

                assertThat(result).isEqualTo("REPORT-BYTES".getBytes(StandardCharsets.UTF_8));
                StubHttpServer.RecordedRequest request = server.request(0);
                assertThat(request.method()).isEqualTo("GET");
                assertThat(request.path()).isEqualTo("/rest/reports/R06?FORMAT=pdf");
            }
        }

        @Test
        void shouldWrapHttpError_whenServerReturns5xx() throws Exception {
            try (StubHttpServer server = StubHttpServer.start()) {
                server.enqueue(500, "boom");
                ReportService service = serviceWith(server.baseUrl() + "/rest/reports/");

                assertThatThrownBy(() -> service.renderViaServer("R06", Map.of()))
                        .isInstanceOf(ReportGenerationException.class)
                        .hasMessageContaining("Failed to fetch report from server")
                        .cause().isInstanceOf(RestClientException.class);
            }
        }

        @Test
        void shouldWrapConnectionFailure_whenServerUnreachable() throws Exception {
            String deadBaseUrl;
            try (StubHttpServer server = StubHttpServer.start()) {
                deadBaseUrl = server.baseUrl();
            }
            ReportService service = serviceWith(deadBaseUrl + "/rest/reports/");

            assertThatThrownBy(() -> service.renderViaServer("R06", Map.of()))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to fetch report from server")
                    .cause().isNotNull();
        }
    }
}
