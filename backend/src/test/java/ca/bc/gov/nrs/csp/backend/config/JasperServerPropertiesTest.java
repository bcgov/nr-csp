package ca.bc.gov.nrs.csp.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JasperServerPropertiesTest {

    private static JasperServerProperties sample() {
        return new JasperServerProperties(
                "https://jasper.example.com/login",
                "https://jasper.example.com/fetch/",
                "https://jasper.example.com/put",
                "/reports/CSP/",
                "jasper-user",
                "jasper-pass",
                true,
                60);
    }

    @Test
    void accessors_exposeConstructorValues() {
        JasperServerProperties props = sample();

        assertThat(props.loginUrl()).isEqualTo("https://jasper.example.com/login");
        assertThat(props.fetchUrl()).isEqualTo("https://jasper.example.com/fetch/");
        assertThat(props.putUrl()).isEqualTo("https://jasper.example.com/put");
        assertThat(props.reportUriBase()).isEqualTo("/reports/CSP/");
        assertThat(props.username()).isEqualTo("jasper-user");
        assertThat(props.password()).isEqualTo("jasper-pass");
        assertThat(props.sslVerify()).isTrue();
        assertThat(props.readTimeoutSeconds()).isEqualTo(60);
    }

    @Test
    void equalsAndHashCode_followRecordSemantics() {
        assertThat(sample())
                .isEqualTo(sample())
                .hasSameHashCodeAs(sample());

        JasperServerProperties different = new JasperServerProperties(
                "https://other.example.com/login",
                "https://jasper.example.com/fetch/",
                "https://jasper.example.com/put",
                "/reports/CSP/",
                "jasper-user",
                "jasper-pass",
                false,
                30);
        assertThat(sample()).isNotEqualTo(different);
    }

    @Test
    void isAnnotatedWithJasperServerPrefix() {
        ConfigurationProperties annotation =
                JasperServerProperties.class.getAnnotation(ConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("jasper.server");
    }

    @Test
    void bindsFromKebabCaseConfigurationProperties() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "jasper.server.login-url", "https://jasper.example.com/login",
                "jasper.server.fetch-url", "https://jasper.example.com/fetch/",
                "jasper.server.put-url", "https://jasper.example.com/put",
                "jasper.server.report-uri-base", "/reports/CSP/",
                "jasper.server.username", "jasper-user",
                "jasper.server.password", "jasper-pass",
                "jasper.server.ssl-verify", "true",
                "jasper.server.read-timeout-seconds", "60"));

        JasperServerProperties bound = new Binder(source)
                .bind("jasper.server", JasperServerProperties.class)
                .get();

        assertThat(bound).isEqualTo(sample());
    }
}
