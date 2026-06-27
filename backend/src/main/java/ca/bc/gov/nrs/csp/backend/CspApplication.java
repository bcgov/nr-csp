package ca.bc.gov.nrs.csp.backend;

import ca.bc.gov.nrs.csp.backend.config.JasperServerProperties;
import ca.bc.gov.nrs.csp.backend.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.File;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, JasperServerProperties.class})
public class CspApplication {

    public static void main(String[] args) {
        // The certextractor init container writes the Oracle server's self-signed cert
        // to /cert/jssecacerts (JKS).  Point Java's JSSE at it so PKIX validation passes.
        // Read the password from the environment so it stays off the process table.
        String jssecacerts = "/cert/jssecacerts";
        if (new File(jssecacerts).exists()) {
            System.setProperty("javax.net.ssl.trustStore", jssecacerts);
            String pw = System.getenv("KEYSTORE_SECRET");
            if (pw != null && !pw.isEmpty()) {
                System.setProperty("javax.net.ssl.trustStorePassword", pw);
            }
        }
        SpringApplication.run(CspApplication.class, args);
    }
}
