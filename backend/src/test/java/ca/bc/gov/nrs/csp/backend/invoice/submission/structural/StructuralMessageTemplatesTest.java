package ca.bc.gov.nrs.csp.backend.invoice.submission.structural;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the structural templates in the REAL {@code messages.properties}
 * (refactor doc §3.5 Step B) — every structural code must resolve, the
 * pass-through templates must substitute the diagnostic detail, and the
 * fixed sentences must survive encoding (em dash) and MessageFormat
 * quoting (apostrophes) intact. Uses the same basename + UTF-8 encoding
 * as Spring Boot's autoconfigured MessageSource.
 */
class StructuralMessageTemplatesTest {

  private final ResourceBundleMessageSource messageSource = realBundle();

  private static ResourceBundleMessageSource realBundle() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource();
    source.setBasename("messages");
    source.setDefaultEncoding("UTF-8"); // mirrors spring.messages.encoding default
    return source;
  }

  @ParameterizedTest(name = "{0} resolves from the bundle")
  @ValueSource(strings = {
      "FORMAT_UNRECOGNIZED", "XSD", "JAXB", "XML_PARSE", "XML_READ",
      "ENVELOPE_PARSE_ERROR", "ENVELOPE_UNRECOGNIZED", "ENVELOPE_MISSING_CONTENT",
      "ENVELOPE_NO_BODY", "ENVELOPE_EXTRACTION_FAILED"
  })
  void every_structural_code_has_a_bundle_entry(String code) {
    String resolved = messageSource.getMessage(code, new Object[] {"d1", "d2"}, code, Locale.getDefault());
    assertThat(resolved).isNotEqualTo(code);
  }

  @Test
  void formatUnrecognized_keeps_dash_and_quoted_angle_bracket() {
    // 0-arg resolution skips MessageFormat, so the apostrophes stay literal.
    String resolved = messageSource.getMessage(
        "FORMAT_UNRECOGNIZED", new Object[0], Locale.getDefault());
    assertThat(resolved).isEqualTo("could not detect format — expected XML (starts with '<')");
  }

  @Test
  void xsd_passes_the_parser_detail_through() {
    String resolved = messageSource.getMessage(
        "XSD", new Object[] {"cvc-enumeration-valid: Value 'MAYBE'"}, Locale.getDefault());
    assertThat(resolved).isEqualTo("cvc-enumeration-valid: Value 'MAYBE'");
  }

  @Test
  void envelope_templates_substitute_both_args() {
    assertThat(messageSource.getMessage("ENVELOPE_MISSING_CONTENT",
        new Object[] {"ESFSubmission", "submissionContent"}, Locale.getDefault()))
        .isEqualTo("<ESFSubmission> has no <submissionContent> child");
    assertThat(messageSource.getMessage("ENVELOPE_UNRECOGNIZED",
        new Object[] {"{ns1}ESFSubmission", "{ns2}CSPSubmission"}, Locale.getDefault()))
        .isEqualTo("root element must be {ns1}ESFSubmission or {ns2}CSPSubmission");
    assertThat(messageSource.getMessage("ENVELOPE_EXTRACTION_FAILED",
        new Object[] {"CSPSubmission", "boom"}, Locale.getDefault()))
        .isEqualTo("could not extract CSPSubmission from envelope: boom");
  }
}
