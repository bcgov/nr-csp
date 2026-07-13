package ca.bc.gov.nrs.csp.backend.invoice.submission.structural.schema;

import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.SubmissionValidationProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises {@link SchemaValidator#validate(byte[])} and its collecting error
 * handler directly. The service path binds the compiled schema to the JAXB
 * unmarshaller and never calls {@code validate()}, so this is the only thing
 * that covers that method + the SAX error/fatalError handling.
 */
class SchemaValidatorTest {

  private static SchemaValidator validator;

  @BeforeAll
  static void compile() throws Exception {
    validator = new SchemaValidator(new SubmissionValidationProperties());
    Method m = SchemaValidator.class.getDeclaredMethod("compileSchema");
    m.setAccessible(true);
    m.invoke(validator);
  }

  @Test
  void schema_is_compiled() {
    assertThat(validator.getSchema()).isNotNull();
  }

  @Test
  void validate_valid_body_has_no_errors() throws IOException {
    List<SubmissionValidationError> errors = validator.validate(read("valid-bare.xml"));
    assertThat(errors).isEmpty();
  }

  @Test
  void validate_schema_invalid_collects_xsd_errors() throws IOException {
    List<SubmissionValidationError> errors = validator.validate(read("schema-invalid.xml"));
    assertThat(errors).isNotEmpty();
    assertThat(errors).extracting(SubmissionValidationError::code).contains("XSD");
  }

  @Test
  void validate_malformed_collects_error() throws IOException {
    List<SubmissionValidationError> errors = validator.validate(read("malformed.xml"));
    assertThat(errors).isNotEmpty();
  }

  /**
   * The {@code ClasspathLsInput} LSInput adapter is internal boilerplate the
   * schema factory only partially calls during compilation. Drive its full
   * surface directly so the adapter is exercised: getters echo the constructor
   * values, setters are intentional no-ops.
   */
  @Test
  void classpathLsInput_adapter_surface() throws Exception {
    Class<?> c = Class.forName(
        "ca.bc.gov.nrs.csp.backend.invoice.submission.structural.schema.SchemaValidator$ClasspathLsInput");
    var ctor = c.getDeclaredConstructor(String.class, String.class, String.class, InputStream.class);
    ctor.setAccessible(true);
    InputStream in = new ByteArrayInputStream(new byte[]{1});
    Object adapter = ctor.newInstance("pub", "sys", "base", in);

    for (var m : c.getDeclaredMethods()) {
      m.setAccessible(true);
      Class<?>[] params = m.getParameterTypes();
      if (params.length == 0) {
        m.invoke(adapter);
      } else if (params.length == 1) {
        m.invoke(adapter, params[0] == boolean.class ? false : null);
      }
    }

    Method getPublicId = c.getDeclaredMethod("getPublicId");
    getPublicId.setAccessible(true);
    assertThat(getPublicId.invoke(adapter)).isEqualTo("pub");
  }

  // ── compileSchema failure path ────────────────────────────────────────

  @Test
  void compileSchema_missing_entry_schema_throws_illegal_state() throws Exception {
    SubmissionValidationProperties props = new SubmissionValidationProperties();
    props.setEntrySchema("schemas/csp/does-not-exist.xsd");
    SchemaValidator broken = new SchemaValidator(props);
    Method m = SchemaValidator.class.getDeclaredMethod("compileSchema");
    m.setAccessible(true);

    InvocationTargetException ex =
        assertThrows(InvocationTargetException.class, () -> m.invoke(broken));

    assertThat(ex.getCause())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to compile CSP submission schema bundle");
  }

  /**
   * Not every {@link SchemaFactory} honours the Apache fullSchemaChecking
   * feature; the base-class {@code setFeature} throws SAXNotRecognizedException,
   * which the validator must swallow and fall back to defaults.
   */
  @Test
  void disableFullSchemaChecking_tolerates_factories_without_the_feature() throws Exception {
    SchemaFactory unsupporting = new SchemaFactory() {
      private ErrorHandler errorHandler;
      private LSResourceResolver resourceResolver;

      @Override public boolean isSchemaLanguageSupported(String schemaLanguage) { return true; }
      @Override public void setErrorHandler(ErrorHandler errorHandler) { this.errorHandler = errorHandler; }
      @Override public ErrorHandler getErrorHandler() { return errorHandler; }
      @Override public void setResourceResolver(LSResourceResolver resourceResolver) { this.resourceResolver = resourceResolver; }
      @Override public LSResourceResolver getResourceResolver() { return resourceResolver; }
      @Override public Schema newSchema(Source[] schemas) { return null; }
      @Override public Schema newSchema() { return null; }
    };
    Method m = SchemaValidator.class
        .getDeclaredMethod("disableFullSchemaChecking", SchemaFactory.class);
    m.setAccessible(true);

    // Must not propagate the SAXNotRecognizedException raised by setFeature.
    assertDoesNotThrow(() -> m.invoke(validator, unsupporting));
  }

  // ── validate() exception mapping ──────────────────────────────────────

  @Test
  void validate_wraps_pre_parse_sax_failure_as_xml_parse() throws Exception {
    // A SAXException raised before the error handler saw anything (errors
    // still empty) must be surfaced as a single XML_PARSE error.
    SchemaValidator v = withSchema(schemaWhoseValidatorThrows(new SAXException("pre-parse failure")));

    List<SubmissionValidationError> errors = v.validate("<x/>".getBytes());

    assertThat(errors).hasSize(1);
    assertThat(errors.getFirst().code()).isEqualTo("XML_PARSE");
    assertThat(errors.getFirst().args()).containsExactly("pre-parse failure");
  }

  @Test
  void validate_wraps_io_failure_as_xml_read() throws Exception {
    SchemaValidator v = withSchema(schemaWhoseValidatorThrows(new IOException("stream broke")));

    List<SubmissionValidationError> errors = v.validate("<x/>".getBytes());

    assertThat(errors).hasSize(1);
    assertThat(errors.getFirst().code()).isEqualTo("XML_READ");
    assertThat(errors.getFirst().args()).containsExactly("stream broke");
  }

  // ── CollectingErrorHandler ────────────────────────────────────────────

  /**
   * error()/fatalError() are exercised via {@code validate()} above; the
   * warning() path never fires on our schema, so drive it directly: warnings
   * are logged only and must not reach the error sink.
   */
  @Test
  void collectingErrorHandler_warning_does_not_collect() throws Exception {
    Class<?> c = Class.forName(
        "ca.bc.gov.nrs.csp.backend.invoice.submission.structural.schema.SchemaValidator$CollectingErrorHandler");
    var ctor = c.getDeclaredConstructor(List.class);
    ctor.setAccessible(true);
    List<SubmissionValidationError> sink = new ArrayList<>();
    Object handler = ctor.newInstance(sink);

    Method warning = c.getDeclaredMethod("warning", SAXParseException.class);
    warning.setAccessible(true);
    warning.invoke(handler, new SAXParseException("just a warning", "pub", "sys", 3, 7));

    assertThat(sink).isEmpty();
  }

  // ── ClasspathLsResolver ───────────────────────────────────────────────
  // Schema compilation only resolves the happy sibling-import path, so the
  // resolver's branch matrix is driven directly (it is a private inner class,
  // mirroring the ClasspathLsInput approach above).

  @Test
  void resolver_returns_null_for_null_systemId() throws Exception {
    assertThat(resolve(newResolver("schemas/csp/"), null, null)).isNull();
  }

  @Test
  void resolver_uses_already_rooted_systemId_as_is() throws Exception {
    assertThat(resolve(newResolver("schemas/csp/"), "schemas/csp/mof-simpleTypes.xsd", null))
        .isNotNull();
  }

  @Test
  void resolver_resolves_sibling_against_schema_root_when_base_is_null() throws Exception {
    assertThat(resolve(newResolver("schemas/csp/"), "mof-simpleTypes.xsd", null)).isNotNull();
  }

  @Test
  void resolver_returns_null_when_resource_missing_on_classpath() throws Exception {
    assertThat(resolve(newResolver("schemas/csp/"), "no-such-file.xsd", null)).isNull();
  }

  @Test
  void resolver_resolves_parent_relative_import_against_base() throws Exception {
    // baseURI points at a nested schema; "../" must collapse back to the root.
    Object in = resolve(
        newResolver("schemas/csp/"),
        "../mof-simpleTypes.xsd",
        "file:/app/classes/schemas/csp/nested/entry.xsd");
    assertThat(in).isNotNull();
  }

  @Test
  void resolver_ignores_leading_parent_hops_past_the_root() throws Exception {
    // More ".." segments than path segments: the extra hops are dropped
    // instead of underflowing.
    Object in = resolve(
        newResolver("schemas/csp/"), "../../../schemas/csp/mof-simpleTypes.xsd", null);
    assertThat(in).isNotNull();
  }

  @Test
  void resolver_ignores_dot_and_empty_segments() throws Exception {
    assertThat(resolve(newResolver("schemas/csp/"), ".//mof-simpleTypes.xsd", null)).isNotNull();
  }

  @Test
  void resolver_falls_back_to_root_when_base_does_not_contain_it() throws Exception {
    Object in = resolve(
        newResolver("schemas/csp/"), "mof-simpleTypes.xsd", "file:/elsewhere/other.xsd");
    assertThat(in).isNotNull();
  }

  @Test
  void resolver_falls_back_to_root_when_base_has_no_directory_part() throws Exception {
    // baseURI equals the root itself (no '/' in the tail), so the directory
    // fallback keeps the configured root.
    Object in = resolve(newResolver("fixtures"), "/submissions/valid-bare.xml", "fixtures");
    assertThat(in).isNotNull();
  }

  // ── helpers ───────────────────────────────────────────────────────────

  /** Builds a SchemaValidator whose compiled schema is replaced by a stub. */
  private static SchemaValidator withSchema(Schema schema) throws Exception {
    SchemaValidator v = new SchemaValidator(new SubmissionValidationProperties());
    Field f = SchemaValidator.class.getDeclaredField("schema");
    f.setAccessible(true);
    f.set(v, schema);
    return v;
  }

  /** A schema whose validator fails with the given exception before any SAX event. */
  private static Schema schemaWhoseValidatorThrows(Exception failure) {
    return new Schema() {
      @Override
      public Validator newValidator() {
        return new Validator() {
          @Override public void reset() {
            // No mutable state in this test validator.
          }
          @Override
          public void validate(Source source, Result result) throws SAXException, IOException {
            if (failure instanceof SAXException sax) {
              throw sax;
            }
            throw (IOException) failure;
          }
          private ErrorHandler errorHandler;
          private LSResourceResolver resourceResolver;

          @Override public void setErrorHandler(ErrorHandler errorHandler) { this.errorHandler = errorHandler; }
          @Override public ErrorHandler getErrorHandler() { return errorHandler; }
          @Override public void setResourceResolver(LSResourceResolver resourceResolver) { this.resourceResolver = resourceResolver; }
          @Override public LSResourceResolver getResourceResolver() { return resourceResolver; }
        };
      }

      @Override
      public ValidatorHandler newValidatorHandler() {
        return null;
      }
    };
  }

  private static Object newResolver(String schemaRoot) throws Exception {
    Class<?> c = Class.forName(
        "ca.bc.gov.nrs.csp.backend.invoice.submission.structural.schema.SchemaValidator$ClasspathLsResolver");
    var ctor = c.getDeclaredConstructor(String.class);
    ctor.setAccessible(true);
    return ctor.newInstance(schemaRoot);
  }

  /** Calls resolveResource(type, namespaceURI, publicId, systemId, baseURI). */
  private static Object resolve(Object resolver, String systemId, String baseURI) throws Exception {
    Method m = resolver.getClass().getDeclaredMethod(
        "resolveResource", String.class, String.class, String.class, String.class, String.class);
    m.setAccessible(true);
    return m.invoke(resolver, null, null, null, systemId, baseURI);
  }

  private static byte[] read(String fixture) throws IOException {
    return new ClassPathResource("fixtures/submissions/" + fixture).getInputStream().readAllBytes();
  }
}
