package ca.bc.gov.nrs.csp.backend.service.reporting;

import ca.bc.gov.nrs.csp.backend.config.JasperServerProperties;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JasperServerServiceTest {

    private static final String REPORT_UUID = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    StubHttpServer server;
    JasperServerService service;

    @BeforeEach
    void setUp() throws IOException {
        server = StubHttpServer.start();
        service = new JasperServerService(props(server.baseUrl()));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private static JasperServerProperties props(String baseUrl) {
        return new JasperServerProperties(
                baseUrl + "/jasperserver/login",
                baseUrl + "/jasperserver/report/",
                baseUrl + "/jasperserver/report",
                "/reports/CSP/",
                "csp user",
                "p&ss word",
                true,
                5);
    }

    private static Map<String, Object> pdfParams() {
        return Map.of("RUN_OUTPUT_FORMAT", "PDF");
    }

    private static String putResponse(int totalPages, String uuid) {
        return "<report><uuid>" + uuid + "</uuid><totalPages>" + totalPages + "</totalPages></report>";
    }

    private void enqueueLoginOk() {
        server.enqueue(200, "", "Set-Cookie: JSESSIONID=ABC123; Path=/jasperserver; HttpOnly");
    }

    private void enqueuePutAndFetchOk(String reportContent) {
        server.enqueue(201, putResponse(3, REPORT_UUID));
        server.enqueue(200, reportContent);
    }

    @Nested
    @DisplayName("generateReport() happy path")
    class HappyPath {

        @Test
        void shouldReturnReportBytes_andSendExpectedRequests() {
            enqueueLoginOk();
            enqueuePutAndFetchOk("PDF-BYTES");

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("RUN_OUTPUT_FORMAT", "PDF");
            params.put("DATE_FROM", "20200101");

            byte[] result = service.generateReport("R06", params);

            assertThat(result).isEqualTo("PDF-BYTES".getBytes(StandardCharsets.UTF_8));
            assertThat(server.requestCount()).isEqualTo(3);

            StubHttpServer.RecordedRequest login = server.request(0);
            assertThat(login.method()).isEqualTo("POST");
            assertThat(login.path()).isEqualTo("/jasperserver/login");
            assertThat(login.header("Content-Type")).isEqualTo("application/x-www-form-urlencoded");
            // credentials go in the POST body, URL-encoded
            assertThat(login.body()).isEqualTo("j_username=csp+user&j_password=p%26ss+word");

            StubHttpServer.RecordedRequest put = server.request(1);
            assertThat(put.method()).isEqualTo("PUT");
            assertThat(put.path()).isEqualTo("/jasperserver/report?RUN_OUTPUT_FORMAT=pdf");
            assertThat(put.header("Content-Type")).isEqualTo("application/xml");
            assertThat(put.header("Cookie")).isEqualTo("JSESSIONID=ABC123");
            assertThat(put.body())
                    .contains("<resourceDescriptor name=\"R06\" wsType=\"reportUnit\""
                            + " uriString=\"/reports/CSP/R06\">")
                    .contains("<parameter name=\"DATE_FROM\">20200101</parameter>")
                    // RUN_OUTPUT_FORMAT travels as a query parameter, not a report parameter
                    .doesNotContain("name=\"RUN_OUTPUT_FORMAT\"");

            StubHttpServer.RecordedRequest fetch = server.request(2);
            assertThat(fetch.method()).isEqualTo("GET");
            assertThat(fetch.path()).isEqualTo("/jasperserver/report/" + REPORT_UUID + "?file=report");
            assertThat(fetch.header("Cookie")).isEqualTo("JSESSIONID=ABC123");
        }

        @Test
        void shouldJoinListParametersWithCommas_andEscapeXmlValues() {
            enqueueLoginOk();
            enqueuePutAndFetchOk("CSV-BYTES");

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("RUN_OUTPUT_FORMAT", "CSV");
            params.put("MARKS", List.of("AB1", "CD2"));
            params.put("NOTE", "<A & \"B\" 'C'>");

            service.generateReport("R13", params);

            StubHttpServer.RecordedRequest put = server.request(1);
            assertThat(put.path()).endsWith("?RUN_OUTPUT_FORMAT=csv");
            assertThat(put.body())
                    .contains("<parameter name=\"MARKS\">AB1,CD2</parameter>")
                    .contains("<parameter name=\"NOTE\">&lt;A &amp; &quot;B&quot; &apos;C&apos;&gt;</parameter>")
                    .contains("uriString=\"/reports/CSP/R13\"");
        }
    }

    @Nested
    @DisplayName("session handling")
    class SessionHandling {

        @Test
        void shouldReuseSessionCookie_onSecondCall() {
            enqueueLoginOk();
            enqueuePutAndFetchOk("FIRST");
            enqueuePutAndFetchOk("SECOND");

            service.generateReport("R06", pdfParams());
            byte[] second = service.generateReport("R06", pdfParams());

            assertThat(second).isEqualTo("SECOND".getBytes(StandardCharsets.UTF_8));
            // login + (put + fetch) x 2 — no second login
            assertThat(server.requestCount()).isEqualTo(5);
            assertThat(server.request(3).method()).isEqualTo("PUT");
            assertThat(server.request(3).header("Cookie")).isEqualTo("JSESSIONID=ABC123");
        }

        @Test
        void shouldReauthenticate_whenSessionExpired() {
            enqueueLoginOk();
            enqueuePutAndFetchOk("FIRST");
            service.generateReport("R06", pdfParams());

            ReflectionTestUtils.setField(service, "sessionExpiresAt", Instant.EPOCH);

            server.enqueue(200, "", "Set-Cookie: JSESSIONID=NEW999; Path=/jasperserver");
            enqueuePutAndFetchOk("SECOND");
            service.generateReport("R06", pdfParams());

            assertThat(server.requestCount()).isEqualTo(6);
            assertThat(server.request(3).method()).isEqualTo("POST");
            assertThat(server.request(3).path()).isEqualTo("/jasperserver/login");
            assertThat(server.request(4).header("Cookie")).isEqualTo("JSESSIONID=NEW999");
        }

        @Test
        void shouldJoinMultipleSessionCookies_strippingAttributes() {
            server.enqueue(200, "",
                    "Set-Cookie: JSESSIONID=ABC; Path=/jasperserver; HttpOnly",
                    "Set-Cookie: userLocale=en_US; Secure");
            enqueuePutAndFetchOk("DATA");

            service.generateReport("R06", pdfParams());

            String cookie = server.request(1).header("Cookie");
            assertThat(cookie)
                    .contains("JSESSIONID=ABC")
                    .contains("userLocale=en_US")
                    .doesNotContain("Path=")
                    .doesNotContain("HttpOnly")
                    .doesNotContain("Secure");
        }

        @Test
        void shouldAcceptRedirectLoginResponse_withSessionCookie() {
            server.enqueue(302, "",
                    "Set-Cookie: JSESSIONID=REDIR; Path=/jasperserver",
                    "Location: /jasperserver/home");
            enqueuePutAndFetchOk("DATA");

            byte[] result = service.generateReport("R06", pdfParams());

            assertThat(result).isEqualTo("DATA".getBytes(StandardCharsets.UTF_8));
            assertThat(server.request(1).header("Cookie")).isEqualTo("JSESSIONID=REDIR");
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        void shouldThrow_whenLoginFails() {
            server.enqueue(401, "unauthorized");
            Map<String, Object> params = pdfParams();

            assertThatThrownBy(() -> service.generateReport("R06", params))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("login failed with HTTP 401");
        }

        @Test
        void shouldThrow_whenLoginSucceedsWithoutSessionCookie() {
            server.enqueue(200, "");
            Map<String, Object> params = pdfParams();

            assertThatThrownBy(() -> service.generateReport("R06", params))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("no session cookie");
        }

        @Test
        void shouldThrow_whenPutIsNotCreated() {
            enqueueLoginOk();
            server.enqueue(500, "server error");
            Map<String, Object> params = pdfParams();

            assertThatThrownBy(() -> service.generateReport("R06", params))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("PUT failed with status 500")
                    .hasMessageContaining("R06");
        }

        @Test
        void shouldThrowResourceNotFound_whenReportHasZeroPages() {
            enqueueLoginOk();
            server.enqueue(201, putResponse(0, REPORT_UUID));
            Map<String, Object> params = pdfParams();

            assertThatThrownBy(() -> service.generateReport("R06", params))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("returned no data");
        }

        @Test
        void shouldThrow_whenUuidMissingFromPutResponse() {
            enqueueLoginOk();
            server.enqueue(201, "<report><totalPages>2</totalPages></report>");
            Map<String, Object> params = pdfParams();

            assertThatThrownBy(() -> service.generateReport("R06", params))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Could not extract UUID");
        }

        @Test
        void shouldThrow_whenFetchFails() {
            enqueueLoginOk();
            server.enqueue(201, putResponse(1, REPORT_UUID));
            server.enqueue(404, "gone");
            Map<String, Object> params = pdfParams();

            assertThatThrownBy(() -> service.generateReport("R06", params))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("GET failed with status 404")
                    .hasMessageContaining(REPORT_UUID);
        }

        @Test
        void shouldWrapIoException_whenServerUnreachable() throws IOException {
            int unusedPort;
            try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                unusedPort = socket.getLocalPort();
            }
            JasperServerService unreachable =
                    new JasperServerService(props("http://127.0.0.1:" + unusedPort));
            Map<String, Object> params = pdfParams();

            assertThatThrownBy(() -> unreachable.generateReport("R06", params))
                    .isInstanceOf(ReportGenerationException.class)
                    .hasMessageContaining("Failed to generate report 'R06'")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    @Nested
    @DisplayName("SSL configuration")
    class SslConfiguration {

        @Test
        void shouldUseDefaultHostnameVerifier_whenSslVerifyEnabled() {
            assertThat(ReflectionTestUtils.getField(service, "hostnameVerifier"))
                    .isSameAs(HttpsURLConnection.getDefaultHostnameVerifier());
            assertThat(ReflectionTestUtils.getField(service, "sslContext")).isNotNull();
        }

        @Test
        void shouldTrustAllHostnames_whenSslVerifyDisabled() {
            JasperServerService insecure = new JasperServerService(new JasperServerProperties(
                    "https://jasper.example.com/login",
                    "https://jasper.example.com/fetch/",
                    "https://jasper.example.com/put",
                    "/reports/CSP/",
                    "user", "pass", false, 5));

            HostnameVerifier verifier =
                    (HostnameVerifier) ReflectionTestUtils.getField(insecure, "hostnameVerifier");
            assertThat(verifier.verify("any-host.example.com", null)).isTrue();
            assertThat(ReflectionTestUtils.getField(insecure, "sslContext")).isNotNull();
        }
    }
}
