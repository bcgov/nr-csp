package ca.bc.gov.nrs.csp.backend.config;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for {@code resources/fonts.xml} + {@code resources/jasperreports_extension.properties}.
 *
 * <p>Without those files, JasperReports' PDF export has no mapping from "SansSerif + bold" (the
 * engine's default font name, used by every report in this codebase since none declare an
 * explicit {@code fontName}) to a distinct PDF font — it falls back to the single global
 * {@code net.sf.jasperreports.default.pdf.font.name=Helvetica} for every style, silently
 * rendering bold text identically to regular text. Confirmed by hand against a real Oracle-rendered
 * PDF: every bold header/table-header in R06 (and, on inspection, R13 too — this predates the
 * R06-R12 local-rendering migration and affects every locally-rendered report) rendered with no
 * bold weight at all until this fix was added.</p>
 *
 * <p>This test proves the fix directly at the PDF-export level — no database, no specific report,
 * just a minimal synthetic report with one bold and one regular static text element — by checking
 * that the exported PDF contains distinct {@code /BaseFont} declarations for each.</p>
 */
class PdfFontExtensionTest {

    @Test
    void boldAndRegularTextShouldUseDistinctBaseFonts() throws Exception {
        String jrxml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <jasperReport name="boldtest" pageWidth="200" pageHeight="100" columnWidth="200"
                              leftMargin="0" rightMargin="0" topMargin="0" bottomMargin="0">
                    <detail>
                        <band height="40">
                            <element kind="staticText" x="0" y="0" width="150" height="20" bold="false">
                                <text><![CDATA[Regular Text]]></text>
                            </element>
                            <element kind="staticText" x="0" y="20" width="150" height="20" bold="true">
                                <text><![CDATA[Bold Text]]></text>
                            </element>
                        </band>
                    </detail>
                </jasperReport>
                """;

        JasperReport report;
        try (ByteArrayInputStream in = new ByteArrayInputStream(jrxml.getBytes(StandardCharsets.UTF_8))) {
            report = JasperCompileManager.compileReport(in);
        }

        JasperPrint print = JasperFillManager.fillReport(report, new HashMap<>(), new JREmptyDataSource());
        byte[] pdfBytes = JasperExportManager.exportReportToPdf(print);

        String pdfAsLatin1 = new String(pdfBytes, StandardCharsets.ISO_8859_1);
        Matcher matcher = Pattern.compile("/BaseFont\\s*/(\\S+?)/").matcher(pdfAsLatin1);
        java.util.Set<String> baseFonts = new java.util.HashSet<>();
        while (matcher.find()) {
            baseFonts.add(matcher.group(1));
        }

        assertThat(baseFonts)
                .as("PDF should embed distinct base fonts for regular and bold text")
                .contains("Helvetica", "Helvetica-Bold");
    }
}
