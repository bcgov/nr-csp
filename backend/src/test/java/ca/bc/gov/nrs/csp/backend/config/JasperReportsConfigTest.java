package ca.bc.gov.nrs.csp.backend.config;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JasperReportsConfigTest {

    @Test
    void configure_setsJr7DefaultNumberFormatPatterns() {
        new JasperReportsConfig().configure();

        JRPropertiesUtil propsUtil =
                JRPropertiesUtil.getInstance(DefaultJasperReportsContext.getInstance());
        assertThat(propsUtil.getProperty("net.sf.jasperreports.text.pattern.number"))
                .isEqualTo("#,##0.00");
        assertThat(propsUtil.getProperty("net.sf.jasperreports.text.pattern.integer"))
                .isEqualTo("#,##0");
    }

    @Test
    void configure_isIdempotent_whenCalledMultipleTimes() {
        JasperReportsConfig config = new JasperReportsConfig();

        assertThatCode(() -> {
            config.configure();
            config.configure();
        }).doesNotThrowAnyException();

        JRPropertiesUtil propsUtil =
                JRPropertiesUtil.getInstance(DefaultJasperReportsContext.getInstance());
        assertThat(propsUtil.getProperty("net.sf.jasperreports.text.pattern.number"))
                .isEqualTo("#,##0.00");
    }

    @Test
    void configure_neverPropagatesClasspathDiscoveryFailures() {
        // The compiler-classpath discovery is wrapped in a broad catch: whatever the
        // runtime layout (m2 repo JARs, exploded JAR, fat JAR), configure() must not throw.
        assertThatCode(() -> new JasperReportsConfig().configure()).doesNotThrowAnyException();
    }
}
