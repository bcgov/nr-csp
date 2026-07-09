package ca.bc.gov.nrs.csp.backend.invoice.submission.structural.schema;

import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.SubmissionValidationProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
    Object in = new ByteArrayInputStream(new byte[]{1});
    Object adapter = ctor.newInstance("pub", "sys", "base", in);

    for (var m : c.getDeclaredMethods()) {
      m.setAccessible(true);
      Class<?>[] params = m.getParameterTypes();
      if (params.length == 0) {
        m.invoke(adapter);
      } else if (params.length == 1) {
        m.invoke(adapter, new Object[]{params[0] == boolean.class ? false : null});
      }
    }

    Method getPublicId = c.getDeclaredMethod("getPublicId");
    getPublicId.setAccessible(true);
    assertThat(getPublicId.invoke(adapter)).isEqualTo("pub");
  }

  private static byte[] read(String fixture) throws IOException {
    return new ClassPathResource("fixtures/submissions/" + fixture).getInputStream().readAllBytes();
  }
}
