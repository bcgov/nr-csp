package ca.bc.gov.nrs.csp.backend.submission.structural.parser;

import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.submission.structural.SubmissionValidationProperties;
import ca.bc.gov.nrs.csp.backend.submission.structural.schema.SchemaValidator;
import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the application's JAXB unmarshaller. Returns either the parsed
 * submission tree or a list of validation errors collected by the
 * schema-aware event handler. The JAXB context package comes from
 * {@link SubmissionValidationProperties}, so the parser carries no
 * compile-time dependency on the generated types.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionXmlParser {

  private final SubmissionValidationProperties props;
  private final SchemaValidator schemaValidator;

  private JAXBContext jaxbContext;

  @PostConstruct
  void initContext() {
    try {
      this.jaxbContext = JAXBContext.newInstance(props.getJaxbContextPath());
      log.info("JAXB context initialized for {}", props.getJaxbContextPath());
    } catch (JAXBException e) {
      throw new IllegalStateException(
          "Failed to initialize JAXB context for " + props.getJaxbContextPath(), e);
    }
  }

  public ParseOutcome parse(byte[] xml) {
    List<SubmissionValidationError> errors = new ArrayList<>();
    try {
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      unmarshaller.setSchema(schemaValidator.getSchema());
      unmarshaller.setEventHandler(event -> {
        errors.add(
            SubmissionValidationError.of(
                event.getLocator() == null
                    ? null
                    : String.format(
                        "line %d, col %d",
                        event.getLocator().getLineNumber(),
                        event.getLocator().getColumnNumber()),
                "JAXB",
                event.getMessage()));
        return true; // continue collecting
      });

      // XXE hardening — submissions are user-uploaded XML, so disable
      // DTDs and external entities on the StAX reader feeding JAXB.
      XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
      factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xml));
      Object root = unmarshaller.unmarshal(reader);
      return new ParseOutcome(unwrap(root), errors);
    } catch (Exception e) {
      errors.add(SubmissionValidationError.of("XML_PARSE", e.getMessage()));
      return new ParseOutcome(null, errors);
    }
  }

  /** Unwraps a JAXBElement to its value so callers see the body type directly. */
  private Object unwrap(Object root) {
    return root instanceof JAXBElement<?> el ? el.getValue() : root;
  }

  /**
   * Result of an unmarshal attempt. {@code submission} is the parsed body
   * object (the JAXB body type, e.g. {@code CSPSubmissionType}) or null on
   * failure; {@code errors} collects schema/JAXB problems.
   */
  public record ParseOutcome(Object submission, List<SubmissionValidationError> errors) {
    public boolean ok() {
      return submission != null && errors.isEmpty();
    }
  }
}
