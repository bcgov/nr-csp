package ca.bc.gov.nrs.csp.backend;

import ca.bc.gov.nrs.csp.backend.config.JasperServerProperties;
import ca.bc.gov.nrs.csp.backend.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, JasperServerProperties.class})
public class CspApplication {

    public static void main(String[] args) {
        SpringApplication.run(CspApplication.class, args);
    }
}
