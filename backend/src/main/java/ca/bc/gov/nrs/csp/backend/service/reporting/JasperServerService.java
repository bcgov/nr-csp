package ca.bc.gov.nrs.csp.backend.service.reporting;

import ca.bc.gov.nrs.csp.backend.config.JasperServerProperties;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Client for the JasperReports Server REST API.
 * Handles authentication (session cookie), report submission (PUT), and report fetch (GET).
 */
@Service
public class JasperServerService {

    private static final Logger log = LogManager.getLogger(JasperServerService.class);

    private static final long SESSION_TTL_SECONDS = 20 * 60;

    private final JasperServerProperties props;
    private final SSLContext sslContext;
    private final HostnameVerifier hostnameVerifier;

    private volatile String cachedSessionCookie;
    private volatile Instant sessionExpiresAt = Instant.EPOCH;

    public JasperServerService(JasperServerProperties props) {
        this.props = props;
        try {
            this.sslContext = buildSslContext(props.sslVerify());
            this.hostnameVerifier = buildHostnameVerifier(props.sslVerify());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new ReportGenerationException("Failed to initialise SSL context for JasperReports Server", e);
        }
    }

    /**
     * Generates a report via JasperReports Server: authenticate → submit → fetch.
     *
     * @param reportName the server-side report name (e.g. "R06" or "R06_CSV")
     * @param parameters report parameters; include {@code "RUN_OUTPUT_FORMAT" = "PDF"} or {@code "CSV"}
     * @return raw report bytes
     */
    public byte[] generateReport(String reportName, Map<String, Object> parameters) {
        try {
            String sessionCookie = getSessionCookie();
            String format = (String) parameters.get("RUN_OUTPUT_FORMAT");
            String reportUuid = submitReport(reportName, parameters, sessionCookie, format);
            return fetchReport(reportUuid, sessionCookie);
        } catch (ResourceNotFoundException | ReportGenerationException e) {
            throw e;
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate report '" + reportName + "' via JasperReports Server", e);
        }
    }

    private synchronized String getSessionCookie() throws IOException {
        if (cachedSessionCookie == null || Instant.now().isAfter(sessionExpiresAt)) {
            cachedSessionCookie = authenticate();
            sessionExpiresAt = Instant.now().plusSeconds(SESSION_TTL_SECONDS);
        }
        return cachedSessionCookie;
    }

    private String authenticate() throws IOException {
        // Credentials are sent in the POST body (not the URL) so they do not appear in
        // proxy/load-balancer/server access logs on the path to JasperReports Server.
        String form = "j_username=" + encode(props.username())
                + "&j_password=" + encode(props.password());

        log.info("Authenticating with JasperReports Server");
        HttpURLConnection conn = openConnection(props.loginUrl());
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(form.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        log.info("JasperReports Server login response: {}", status);

        if (status < 200 || status >= 400) {
            throw new ReportGenerationException("JasperReports Server login failed with HTTP " + status, null);
        }

        Set<String> cookies = new LinkedHashSet<>();
        List<String> setCookies = conn.getHeaderFields().get("Set-Cookie");
        if (setCookies != null) {
            setCookies.stream().map(c -> c.split(";")[0]).forEach(cookies::add);
        }

        if (cookies.isEmpty()) {
            throw new ReportGenerationException("JasperReports Server login returned HTTP " + status + " but no session cookie was set.", null);
        }
        return String.join("; ", cookies);
    }

    private String submitReport(String reportName, Map<String, Object> parameters,
                                String sessionCookie, String format) throws IOException {
        String putUrl = props.putUrl() + "?RUN_OUTPUT_FORMAT=" + format.toLowerCase();
        log.info("Submitting report '{}' to JasperReports Server", reportName);

        HttpURLConnection conn = openConnection(putUrl);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/xml");
        conn.setRequestProperty("Cookie", sessionCookie);
        conn.setDoOutput(true);

        String paramXml = parameters.entrySet().stream()
                .filter(e -> !"RUN_OUTPUT_FORMAT".equalsIgnoreCase(e.getKey()))
                .map(e -> {
                    Object val = e.getValue();
                    String text = (val instanceof List<?>)
                            ? ((List<?>) val).stream().map(Object::toString).collect(Collectors.joining(","))
                            : val.toString();
                    return "    <parameter name=\"" + e.getKey() + "\">" + escapeXml(text) + "</parameter>";
                })
                .collect(Collectors.joining("\n"));

        String body = "<resourceDescriptor name=\"" + reportName
                + "\" wsType=\"reportUnit\" uriString=\"" + props.reportUriBase() + reportName + "\">\n"
                + paramXml + "\n</resourceDescriptor>";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_CREATED) {
            throw new ReportGenerationException(
                    "JasperReports Server PUT failed with status " + status + " for report '" + reportName + "'", null);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseBody = br.lines().collect(Collectors.joining(System.lineSeparator()));

            Pattern totalPages = Pattern.compile("<totalPages>(\\d+)</totalPages>");
            Matcher tpMatcher = totalPages.matcher(responseBody);
            if (tpMatcher.find() && Integer.parseInt(tpMatcher.group(1)) == 0) {
                throw new ResourceNotFoundException("The provided parameters returned no data for report '" + reportName + "'.");
            }

            Matcher uuidMatcher = Pattern.compile("<uuid>([a-fA-F0-9\\-]+)</uuid>").matcher(responseBody);
            if (!uuidMatcher.find()) {
                throw new ReportGenerationException("Could not extract UUID from JasperReports Server response.", null);
            }
            return uuidMatcher.group(1);
        }
    }

    private byte[] fetchReport(String uuid, String sessionCookie) throws IOException {
        String getUrl = props.fetchUrl() + uuid + "?file=report";
        log.info("Fetching report UUID {} from JasperReports Server", uuid);

        HttpURLConnection conn = openConnection(getUrl);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", sessionCookie);

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new ReportGenerationException(
                    "JasperReports Server GET failed with status " + status + " for UUID " + uuid, null);
        }

        try (InputStream in = conn.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private HttpURLConnection openConnection(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10_000);   // 10s to establish the connection
        conn.setReadTimeout(120_000);     // 2min to read the response (PDFs can be large)
        if (conn instanceof HttpsURLConnection https) {
            https.setSSLSocketFactory(sslContext.getSocketFactory());
            https.setHostnameVerifier(hostnameVerifier);
        }
        return conn;
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static SSLContext buildSslContext(boolean verify)
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        if (verify) {
            ctx.init(null, null, new java.security.SecureRandom());
        } else {
            log.warn("SSL certificate verification is DISABLED — do not use in production.");
            TrustManager[] trustAll = {new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }};
            ctx.init(null, trustAll, new java.security.SecureRandom());
        }
        return ctx;
    }

    private static HostnameVerifier buildHostnameVerifier(boolean verify) {
        if (verify) return HttpsURLConnection.getDefaultHostnameVerifier();
        log.warn("Hostname verification is DISABLED — do not use in production.");
        return (hostname, session) -> true;
    }
}
