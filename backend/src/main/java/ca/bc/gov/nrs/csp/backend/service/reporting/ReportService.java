package ca.bc.gov.nrs.csp.backend.service.reporting;

import ca.bc.gov.nrs.csp.backend.config.JasperServerProperties;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperExportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Map;

/**
 * Provides two report rendering paths:
 * <ul>
 *   <li>{@link #renderLocal} — compiles and fills a .jrxml template in-process and exports to PDF.</li>
 *   <li>{@link #renderViaServer} — delegates to a JasperReports Server REST endpoint.</li>
 * </ul>
 *
 * <p>PDF export uses {@link JasperExportManager}; requires the {@code jasperreports-pdf}
 * module (included in pom.xml). For XLS/XLSX export, uncomment the Apache POI dep.</p>
 *
 * <p>Place .jrxml templates under {@code src/main/resources/reports/}.</p>
 *
 * <p>JasperReports is bootstrapped once at startup by {@link ca.bc.gov.nrs.csp.backend.config.JasperReportsConfig}.</p>
 *
 * <p>All failures surface as {@link ReportGenerationException}, handled by
 * {@code GlobalApiExceptionHandler} → HTTP 500.</p>
 */
@Service
public class ReportService {

    private static final Logger log = LogManager.getLogger(ReportService.class);

    private final DataSource dataSource;
    private final JasperServerProperties jasperServerProperties;
    private final RestClient restClient;

    public ReportService(DataSource dataSource, JasperServerProperties jasperServerProperties) {
        this.dataSource = dataSource;
        this.jasperServerProperties = jasperServerProperties;
        this.restClient = RestClient.create();
    }

    /**
     * Renders a .jrxml template in-process against the app datasource and returns PDF bytes.
     *
     * @param templatePath classpath path to the template (e.g., {@code "reports/MyReport.jrxml"})
     * @param params       report parameters
     * @return PDF byte array
     */
    public byte[] renderLocal(String templatePath, Map<String, Object> params) {
        log.info("Rendering local report: {}", templatePath);
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(templatePath);
             Connection conn = dataSource.getConnection()) {

            if (stream == null) {
                throw new ReportGenerationException("Report template not found on classpath: " + templatePath, null);
            }

            JasperReport compiled = JasperCompileManager.compileReport(stream);
            JasperPrint print = JasperFillManager.fillReport(compiled, params, conn);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, out);
            return out.toByteArray();

        } catch (ReportGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to render local report: " + templatePath, e);
        }
    }

    /**
     * Fetches a rendered report from JasperReports Server via its REST API.
     *
     * <p>TODO: implement session authentication against {@code jasper.server.login-url}
     * before wiring the first server-side report.</p>
     *
     * @param reportUri server-side report URI (appended to {@code jasper.server.fetch-url})
     * @param params    query parameters passed to the server
     * @return response bytes
     */
    public byte[] renderViaServer(String reportUri, Map<String, String> params) {
        log.info("Fetching report from JasperReports Server: {}", reportUri);
        String baseUrl = jasperServerProperties.fetchUrl() + reportUri;
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
                        params.forEach(builder::queryParam);
                        return builder.build().toUri();
                    })
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to fetch report from server: " + reportUri, e);
        }
    }
}
