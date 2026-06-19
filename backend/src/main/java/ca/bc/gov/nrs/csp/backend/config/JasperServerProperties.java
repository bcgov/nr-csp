package ca.bc.gov.nrs.csp.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JasperReports Server connection properties.
 * All values are injected from application.yml (backed by env vars in deployment).
 */
@ConfigurationProperties(prefix = "jasper.server")
public record JasperServerProperties(
        String loginUrl,
        String fetchUrl,
        String putUrl,
        String reportUriBase,
        String username,
        String password,
        boolean sslVerify,
        int readTimeoutSeconds
) {}
