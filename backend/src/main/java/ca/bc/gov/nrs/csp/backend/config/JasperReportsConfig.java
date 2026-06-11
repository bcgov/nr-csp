package ca.bc.gov.nrs.csp.backend.config;

import jakarta.annotation.PostConstruct;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * One-time JasperReports 7 bootstrap, applied at application startup.
 *
 * <p>JR7 removed two default number-format patterns that were present in JR6,
 * and requires an explicit compiler classpath when running from an exploded JAR
 * (Tomcat WAR or Spring Boot JarLauncher). Both are wired here so that no
 * individual service needs to carry this logic.</p>
 */
@Configuration
public class JasperReportsConfig {

    private static final Logger log = LogManager.getLogger(JasperReportsConfig.class);

    @PostConstruct
    public void configure() {
        try {
            JRPropertiesUtil propsUtil = JRPropertiesUtil.getInstance(DefaultJasperReportsContext.getInstance());

            // JR7 removed default number patterns — set them to avoid NPE on unpatternned numeric fields.
            propsUtil.setProperty("net.sf.jasperreports.text.pattern.number",  "#,##0.00");
            propsUtil.setProperty("net.sf.jasperreports.text.pattern.integer", "#,##0");

            // Build the compiler classpath from the directory that contains the JasperReports JAR.
            // Only meaningful for Tomcat WAR / exploded Spring Boot JarLauncher (file: URLs);
            // skipped for an unexploded fat JAR (jar: URLs) where File paths are not usable.
            URL jasperJarUrl = JasperReport.class.getProtectionDomain().getCodeSource().getLocation();
            if ("file".equals(jasperJarUrl.getProtocol())) {
                File libDir = new File(jasperJarUrl.toURI()).getParentFile();
                File[] jars = libDir.listFiles(f -> f.getName().endsWith(".jar"));

                if (jars != null && jars.length > 1) {
                    String jarPaths = Arrays.stream(jars)
                            .map(File::getAbsolutePath)
                            .collect(Collectors.joining(File.pathSeparator));
                    File classesDir = new File(libDir.getParentFile(), "classes");
                    String classpath = classesDir.isDirectory()
                            ? classesDir.getAbsolutePath() + File.pathSeparator + jarPaths
                            : jarPaths;
                    propsUtil.setProperty("net.sf.jasperreports.compiler.classpath", classpath);
                    log.debug("JasperReports compiler classpath set ({} JARs)", jars.length);
                }
            }
        } catch (Exception e) {
            log.warn("Could not auto-configure JasperReports compiler classpath", e);
        }
    }
}
