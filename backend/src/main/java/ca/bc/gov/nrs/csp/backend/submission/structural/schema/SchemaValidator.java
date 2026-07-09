package ca.bc.gov.nrs.csp.backend.submission.structural.schema;

import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.submission.structural.SubmissionValidationProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Compiles the application's XSD bundle once at startup and exposes the
 * compiled {@link Schema} for binding to the JAXB unmarshaller, plus a
 * standalone {@link #validate(byte[])} pass. The entry schema and its
 * classpath root come from {@link SubmissionValidationProperties}, so
 * this class is application-agnostic — the only nr-fspts change needed
 * to generalise it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaValidator {

  private final SubmissionValidationProperties props;

  private Schema schema;

  /**
   * Apache feature toggle for the XSD 1.0 "fullSchemaChecking" pass.
   * The runtime validation we actually want (does an instance document
   * satisfy the content model) runs independently of this flag, so
   * disabling it keeps legacy vendored bundles compiling without
   * dropping any real checks.
   */
  private static final String FEATURE_SCHEMA_FULL_CHECKING =
      "http://apache.org/xml/features/validation/schema-full-checking";

  @PostConstruct
  void compileSchema() {
    try {
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      disableFullSchemaChecking(factory);
      factory.setResourceResolver(new ClasspathLsResolver(props.getSchemaClasspathRoot()));
      this.schema = loadSchema(factory);
      log.info("CSP submission schema compiled from {}", props.getEntrySchema());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compile CSP submission schema bundle", e);
    }
  }

  /**
   * Best-effort disable of the XSD 1.0 fullSchemaChecking pass. Not every
   * {@link SchemaFactory} honours the feature; falling back to defaults is fine.
   */
  private void disableFullSchemaChecking(SchemaFactory factory) {
    try {
      factory.setFeature(FEATURE_SCHEMA_FULL_CHECKING, false);
    } catch (Exception featureFail) {
      log.debug("SchemaFactory does not honour {}; will rely on default settings",
          FEATURE_SCHEMA_FULL_CHECKING, featureFail);
    }
  }

  /** Reads the entry schema off the classpath and compiles the bundle. */
  private Schema loadSchema(SchemaFactory factory) throws IOException, SAXException {
    try (InputStream in = openClasspath(props.getEntrySchema())) {
      return factory.newSchema(new StreamSource(in, props.getEntrySchema()));
    }
  }

  public Schema getSchema() {
    return schema;
  }

  /** Validate raw XML bytes against the schema. Returns collected errors. */
  public List<SubmissionValidationError> validate(byte[] xml) {
    List<SubmissionValidationError> errors = new ArrayList<>();
    Validator validator = schema.newValidator();
    validator.setErrorHandler(new CollectingErrorHandler(errors));
    try {
      validator.validate(new StreamSource(new ByteArrayInputStream(xml)));
    } catch (SAXException e) {
      // Already captured by the error handler unless it was a fatal
      // pre-parse problem (e.g. malformed XML before the first element).
      if (errors.isEmpty()) {
        errors.add(SubmissionValidationError.of("XML_PARSE", new Object[] {e.getMessage()}));
      }
    } catch (IOException e) {
      errors.add(SubmissionValidationError.of("XML_READ", new Object[] {e.getMessage()}));
    }
    return errors;
  }

  private static InputStream openClasspath(String path) throws IOException {
    return new ClassPathResource(path).getInputStream();
  }

  /**
   * Resolves {@code <xs:import schemaLocation="mof-simpleTypes.xsd"/>}
   * and friends against sibling files inside the vendored bundle.
   */
  private static final class ClasspathLsResolver implements LSResourceResolver {

    private final String schemaRoot;

    ClasspathLsResolver(String schemaRoot) {
      this.schemaRoot = schemaRoot;
    }

    @Override
    public LSInput resolveResource(
        String type, String namespaceURI, String publicId, String systemId, String baseURI) {
      if (systemId == null) {
        return null;
      }
      String resolved = resolveAgainstBase(systemId, baseURI);
      try {
        InputStream stream = openClasspath(resolved);
        return new ClasspathLsInput(publicId, systemId, baseURI, stream);
      } catch (IOException e) {
        log.warn("Schema resource not found on classpath: {} (base={})", resolved, baseURI);
        return null;
      }
    }

    private String resolveAgainstBase(String systemId, String baseURI) {
      if (systemId.startsWith(schemaRoot)) {
        return systemId;
      }
      String basePath = schemaRoot;
      if (baseURI != null) {
        int idx = baseURI.indexOf(schemaRoot);
        if (idx >= 0) {
          String tail = baseURI.substring(idx);
          int slash = tail.lastIndexOf('/');
          basePath = slash > 0 ? tail.substring(0, slash + 1) : schemaRoot;
        }
      }
      return normalize(basePath + systemId);
    }

    /** Collapses "foo/../bar" segments produced by relative imports. */
    private String normalize(String path) {
      String[] parts = path.split("/");
      java.util.Deque<String> stack = new java.util.ArrayDeque<>();
      for (String p : parts) {
        if ("..".equals(p)) {
          if (!stack.isEmpty()) {
            stack.removeLast();
          }
        } else if (!p.isEmpty() && !".".equals(p)) {
          stack.addLast(p);
        }
        // empty ("") and current-dir (".") segments are intentionally ignored
      }
      return String.join("/", stack);
    }
  }

  private static final class ClasspathLsInput implements LSInput {
    private final String publicId;
    private final String systemId;
    private final String baseURI;
    private InputStream byteStream;

    ClasspathLsInput(String publicId, String systemId, String baseURI, InputStream byteStream) {
      this.publicId = publicId;
      this.systemId = systemId;
      this.baseURI = baseURI;
      this.byteStream = byteStream;
    }

    // This is a read-only adapter over a classpath resource: it exposes the
    // resource's byte stream only. All other LSInput properties are fixed at
    // construction (or unused), so their setters are intentional no-ops.
    @Override public InputStream getByteStream() { return byteStream; }
    @Override public void setByteStream(InputStream byteStream) { this.byteStream = byteStream; }
    @Override public String getPublicId() { return publicId; }
    @Override public void setPublicId(String publicId) { /* immutable: set via constructor */ }
    @Override public String getSystemId() { return systemId; }
    @Override public void setSystemId(String systemId) { /* immutable: set via constructor */ }
    @Override public String getBaseURI() { return baseURI; }
    @Override public void setBaseURI(String baseURI) { /* immutable: set via constructor */ }
    @Override public java.io.Reader getCharacterStream() { return null; }
    @Override public void setCharacterStream(java.io.Reader characterStream) { /* unused: classpath inputs are byte streams only */ }
    @Override public String getStringData() { return null; }
    @Override public void setStringData(String stringData) { /* unused: classpath inputs are byte streams only */ }
    @Override public String getEncoding() { return null; }
    @Override public void setEncoding(String encoding) { /* unused: classpath inputs are byte streams only */ }
    @Override public boolean getCertifiedText() { return false; }
    @Override public void setCertifiedText(boolean certifiedText) { /* unused: not applicable to classpath inputs */ }
  }

  private static final class CollectingErrorHandler implements ErrorHandler {
    private final List<SubmissionValidationError> sink;

    CollectingErrorHandler(List<SubmissionValidationError> sink) {
      this.sink = sink;
    }

    @Override
    public void warning(SAXParseException e) {
      // Warnings don't fail submission; log only.
      log.debug("schema warning at {}:{} — {}", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
    }

    @Override
    public void error(SAXParseException e) {
      sink.add(toError(e));
    }

    @Override
    public void fatalError(SAXParseException e) {
      sink.add(toError(e));
    }

    private SubmissionValidationError toError(SAXParseException e) {
      String location = String.format("line %d, col %d", e.getLineNumber(), e.getColumnNumber());
      return SubmissionValidationError.of(location, "XSD", new Object[] {e.getMessage()});
    }
  }
}
