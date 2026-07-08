package ca.bc.gov.nrs.csp.backend.submission.structural;

import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationResult;
import ca.bc.gov.nrs.csp.backend.submission.structural.parser.SubmissionEnvelopeStripper;
import ca.bc.gov.nrs.csp.backend.submission.structural.parser.SubmissionXmlParser;
import ca.bc.gov.nrs.csp.backend.submission.structural.schema.SchemaValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the envelope stripper + schema validator + JAXB parser chain
 * end-to-end on canned XML fixtures. Dependencies are wired manually
 * (no Spring context) so the test stays fast and exercises the exact
 * structural pipeline the orchestrator uses.
 * {@link SubmissionValidationProperties} defaults are already
 * CSP-specific, so no overrides are needed.
 */
class StructuralValidationServiceTest {

  private static StructuralValidationService service;

  @BeforeAll
  static void wirePipeline() throws Exception {
    SubmissionValidationProperties props = new SubmissionValidationProperties();

    SchemaValidator schemaValidator = new SchemaValidator(props);
    invokePostConstruct(schemaValidator, "compileSchema");

    SubmissionXmlParser parser = new SubmissionXmlParser(props, schemaValidator);
    invokePostConstruct(parser, "initContext");

    service = new StructuralValidationService(new SubmissionEnvelopeStripper(props), parser);
  }

  /**
   * Every structurally valid submission must pass schema validation, whether it
   * is ESF-wrapped or bare. {@code esf-wrapped-business-error.xml} is included
   * deliberately: it only fails downstream business-rule checks (DB lookups,
   * cross-invoice rules), which are out of scope here, so it must pass too —
   * documenting the boundary between this package and business rules.
   */
  @ParameterizedTest
  @ValueSource(strings = {
      "valid-esf-wrapped.xml",
      "valid-bare.xml",
      "esf-wrapped-business-error.xml"
  })
  void accepts_structurally_valid_submission(String fixture) throws IOException {
    SubmissionValidationResult result = service.validate(read(fixture));

    assertThat(result.errors())
        .as("structurally valid submission %s should pass schema validation", fixture)
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
  void rejects_null_bytes() {
    SubmissionValidationResult result = service.validate(null);

    assertThat(result.valid()).isFalse();
    assertThat(result.errors().get(0).code()).isEqualTo("FORMAT_UNRECOGNIZED");
  }

  @Test
  void detects_xml_after_leading_whitespace() {
    // Leading whitespace before the first '<' must be skipped by the sniff and
    // treated as XML (not FORMAT_UNRECOGNIZED).
    byte[] raw = "   \n\t<csp:CSPSubmission xmlns:csp=\"http://www.for.gov.bc.ca/schema/csp\"/>"
        .getBytes();

    SubmissionValidationResult result = service.validate(raw);

    assertThat(result.errors())
        .noneMatch(e -> e.code().equals("FORMAT_UNRECOGNIZED"));
  }

  @Test
  void accepts_bom_prefixed_xml() throws IOException {
    // A UTF-8 BOM must be skipped by the format sniff and tolerated by the parser.
    byte[] body = read("valid-bare.xml");
    byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    byte[] withBom = new byte[bom.length + body.length];
    System.arraycopy(bom, 0, withBom, 0, bom.length);
    System.arraycopy(body, 0, withBom, bom.length, body.length);

    SubmissionValidationResult result = service.validate(withBom);

    assertThat(result.errors()).isEmpty();
    assertThat(result.valid()).isTrue();
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
