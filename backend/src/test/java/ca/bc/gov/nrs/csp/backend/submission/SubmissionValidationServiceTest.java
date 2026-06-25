package ca.bc.gov.nrs.csp.backend.submission;

import ca.bc.gov.nrs.csp.backend.submission.parser.SubmissionEnvelopeStripper;
import ca.bc.gov.nrs.csp.backend.submission.parser.SubmissionXmlParser;
import ca.bc.gov.nrs.csp.backend.submission.validator.SchemaValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the envelope stripper + schema validator + JAXB parser chain
 * end-to-end on canned XML fixtures. Dependencies are wired manually
 * (no Spring context) so the test stays fast and exercises the exact
 * pipeline the controller uses. {@link SubmissionValidationProperties}
 * defaults are already CSP-specific, so no overrides are needed.
 */
class SubmissionValidationServiceTest {

  private static SubmissionValidationService service;

  @BeforeAll
  static void wirePipeline() throws Exception {
    SubmissionValidationProperties props = new SubmissionValidationProperties();

    SchemaValidator schemaValidator = new SchemaValidator(props);
    invokePostConstruct(schemaValidator, "compileSchema");

    SubmissionXmlParser parser = new SubmissionXmlParser(props, schemaValidator);
    invokePostConstruct(parser, "initContext");

    service = new SubmissionValidationService(new SubmissionEnvelopeStripper(props), parser);
  }

  @Test
  void accepts_esf_wrapped_submission() throws IOException {
    SubmissionValidationResult result = service.validate(read("valid-esf-wrapped.xml"));

    assertThat(result.errors())
        .as("real ESF-wrapped CSP submission should pass schema validation")
        .isEmpty();
    assertThat(result.valid()).isTrue();
  }

  @Test
  void accepts_bare_submission() throws IOException {
    SubmissionValidationResult result = service.validate(read("valid-bare.xml"));

    assertThat(result.errors())
        .as("bare <csp:CSPSubmission> should be validated identically to the wrapped form")
        .isEmpty();
    assertThat(result.valid()).isTrue();
  }

  @Test
  void business_error_file_is_structurally_valid() throws IOException {
    // This sample fails downstream business-rule checks (DB lookups,
    // cross-invoice rules), not structural validation. This pipeline only
    // covers format/envelope/schema, so it must pass here — documenting
    // the boundary between this package and business rules.
    SubmissionValidationResult result = service.validate(read("esf-wrapped-business-error.xml"));

    assertThat(result.errors())
        .as("business-rule failures are out of scope for structural validation")
        .isEmpty();
    assertThat(result.valid()).isTrue();
  }

  @Test
  void rejects_schema_invalid_submission() throws IOException {
    SubmissionValidationResult result = service.validate(read("schema-invalid.xml"));

    assertThat(result.valid()).isFalse();
    assertThat(result.errors()).isNotEmpty();
    assertThat(result.errors())
        .extracting(SubmissionValidationError::code)
        .anyMatch(c -> c.equals("XSD") || c.equals("JAXB"));
  }

  @Test
  void rejects_malformed_xml() throws IOException {
    SubmissionValidationResult result = service.validate(read("malformed.xml"));

    assertThat(result.valid()).isFalse();
    assertThat(result.errors()).isNotEmpty();
  }

  @Test
  void rejects_empty_byte_array() {
    SubmissionValidationResult result = service.validate(new byte[0]);

    assertThat(result.valid()).isFalse();
    assertThat(result.errors()).isNotEmpty();
    assertThat(result.errors().get(0).code()).isEqualTo("FORMAT_UNRECOGNIZED");
  }

  @Test
  void rejects_non_xml_payload() {
    SubmissionValidationResult result = service.validate("{\"not\":\"xml\"}".getBytes());

    assertThat(result.valid()).isFalse();
    assertThat(result.errors()).hasSize(1);
    assertThat(result.errors().get(0).code()).isEqualTo("FORMAT_UNRECOGNIZED");
  }

  @Test
  void rejects_unknown_root_element() {
    byte[] junk = ("<?xml version=\"1.0\"?>"
        + "<somethingElse xmlns=\"http://example.com/other\"/>")
        .getBytes();
    SubmissionValidationResult result = service.validate(junk);

    assertThat(result.valid()).isFalse();
    assertThat(result.errors()).hasSize(1);
    assertThat(result.errors().get(0).code()).isEqualTo("ENVELOPE_UNRECOGNIZED");
  }

  // -------- helpers --------

  private static byte[] read(String fixture) throws IOException {
    return new ClassPathResource("fixtures/submissions/" + fixture)
        .getInputStream()
        .readAllBytes();
  }

  /** Invoke a package-private @PostConstruct method to mirror Spring's wiring. */
  private static void invokePostConstruct(Object bean, String methodName) throws Exception {
    Method m = bean.getClass().getDeclaredMethod(methodName);
    m.setAccessible(true);
    m.invoke(bean);
  }
}
