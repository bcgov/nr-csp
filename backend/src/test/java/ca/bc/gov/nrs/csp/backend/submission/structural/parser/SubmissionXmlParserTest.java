package ca.bc.gov.nrs.csp.backend.submission.structural.parser;

import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.submission.structural.SubmissionValidationProperties;
import ca.bc.gov.nrs.csp.backend.submission.structural.schema.SchemaValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the JAXB parser directly: a schema-bound parse of a valid body, the
 * error-collecting path on a schema-invalid body, and the {@code ParseOutcome.ok()}
 * truth table.
 */
class SubmissionXmlParserTest {

  private static SubmissionXmlParser parser;

  @BeforeAll
  static void wire() throws Exception {
    SubmissionValidationProperties props = new SubmissionValidationProperties();
    SchemaValidator sv = new SchemaValidator(props);
    invoke(sv, "compileSchema");
    parser = new SubmissionXmlParser(props, sv);
    invoke(parser, "initContext");
  }

  @Test
  void parses_valid_bare_body() throws IOException {
    SubmissionXmlParser.ParseOutcome out = parser.parse(read("valid-bare.xml"));
    assertThat(out.errors()).isEmpty();
    assertThat(out.submission()).isNotNull();
    assertThat(out.ok()).isTrue();
  }

  @Test
  void collects_errors_on_schema_invalid_body() throws IOException {
    SubmissionXmlParser.ParseOutcome out = parser.parse(read("schema-invalid.xml"));
    assertThat(out.errors()).isNotEmpty();
    assertThat(out.ok()).isFalse();
  }

  @Test
  void parseOutcome_ok_truth_table() {
    assertThat(new SubmissionXmlParser.ParseOutcome(null, List.of()).ok()).isFalse();
    assertThat(new SubmissionXmlParser.ParseOutcome(new Object(), List.of()).ok()).isTrue();
    assertThat(new SubmissionXmlParser.ParseOutcome(
        new Object(), List.of(SubmissionValidationError.of("X", new Object[0]))).ok()).isFalse();
  }

  private static byte[] read(String fixture) throws IOException {
    return new ClassPathResource("fixtures/submissions/" + fixture).getInputStream().readAllBytes();
  }

  private static void invoke(Object bean, String method) throws Exception {
    Method m = bean.getClass().getDeclaredMethod(method);
    m.setAccessible(true);
    m.invoke(bean);
  }
}
