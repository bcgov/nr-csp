package ca.bc.gov.nrs.csp.backend.submission.structural.parser;

import ca.bc.gov.nrs.csp.backend.submission.structural.SubmissionValidationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Accepts either a bare application submission document (e.g.
 * {@code <csp:CSPSubmission>}, the direct-upload format) or an
 * {@code <esf:ESFSubmission>} envelope (what the legacy ESF queue
 * delivers) and returns bytes representing just the inner submission
 * body. Downstream schema/JAXB validation sees the same shape either
 * way. The envelope/body element coordinates are supplied via
 * {@link SubmissionValidationProperties}, so nothing here is
 * CSP-specific beyond configuration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionEnvelopeStripper {

  private final SubmissionValidationProperties props;

  /**
   * @return the bytes of the inner submission-body subtree.
   * @throws SubmissionEnvelopeException if the document root is neither
   *     the ESF envelope nor the application body, or the envelope
   *     doesn't contain a body.
   */
  public byte[] toBareSubmission(byte[] raw) throws SubmissionEnvelopeException {
    RootKind kind;
    try {
      kind = detectRoot(raw);
    } catch (XMLStreamException e) {
      throw new SubmissionEnvelopeException(
          "ENVELOPE_PARSE_ERROR",
          "could not read XML to detect root element: " + e.getMessage(),
          new Object[] {e.getMessage()},
          e);
    }

    return switch (kind) {
      case BODY -> raw; // already in the right shape
      case ENVELOPE -> extractInner(raw);
      // The {namespace}root qnames are composed here as args — literal braces
      // would collide with the MessageFormat syntax if embedded in the template.
      case UNKNOWN -> throw new SubmissionEnvelopeException(
          "ENVELOPE_UNRECOGNIZED",
          "root element must be {" + props.getEnvelopeNamespace() + "}" + props.getEnvelopeRoot()
              + " or {" + props.getBodyNamespace() + "}" + props.getBodyRoot(),
          new Object[] {
              "{" + props.getEnvelopeNamespace() + "}" + props.getEnvelopeRoot(),
              "{" + props.getBodyNamespace() + "}" + props.getBodyRoot()});
    };
  }

  // ── XXE-hardened factory helpers ─────────────────────────────────────
  // Submissions are arbitrary user-uploaded XML, so every parser we
  // instantiate against them must explicitly disable DTDs, external
  // entities, XInclude, and external DTD/stylesheet resolution.

  private static XMLInputFactory newSecureXmlInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }

  private static DocumentBuilderFactory newSecureDocumentBuilderFactory()
      throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory;
  }

  private static TransformerFactory newSecureTransformerFactory()
      throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  private RootKind detectRoot(byte[] raw) throws XMLStreamException {
    XMLInputFactory factory = newSecureXmlInputFactory();
    XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(raw));
    try {
      while (reader.hasNext()) {
        if (reader.next() == XMLStreamConstants.START_ELEMENT) {
          String ns = reader.getNamespaceURI() == null ? "" : reader.getNamespaceURI();
          String local = reader.getLocalName();
          if (props.getBodyNamespace().equals(ns) && props.getBodyRoot().equals(local)) {
            return RootKind.BODY;
          }
          if (props.getEnvelopeNamespace().equals(ns) && props.getEnvelopeRoot().equals(local)) {
            return RootKind.ENVELOPE;
          }
          return RootKind.UNKNOWN;
        }
      }
      return RootKind.UNKNOWN;
    } finally {
      reader.close();
    }
  }

  private byte[] extractInner(byte[] raw) throws SubmissionEnvelopeException {
    try {
      DocumentBuilder db = newSecureDocumentBuilderFactory().newDocumentBuilder();
      Document doc = db.parse(new ByteArrayInputStream(raw));

      NodeList contents =
          doc.getElementsByTagNameNS(props.getEnvelopeNamespace(), props.getEnvelopeContentElement());
      if (contents.getLength() == 0) {
        throw new SubmissionEnvelopeException(
            "ENVELOPE_MISSING_CONTENT",
            "<" + props.getEnvelopeRoot() + "> has no <" + props.getEnvelopeContentElement()
                + "> child",
            new Object[] {props.getEnvelopeRoot(), props.getEnvelopeContentElement()});
      }
      NodeList children = contents.item(0).getChildNodes();
      Element body = null;
      for (int i = 0; i < children.getLength(); i++) {
        if (children.item(i) instanceof Element el
            && props.getBodyNamespace().equals(el.getNamespaceURI())
            && props.getBodyRoot().equals(el.getLocalName())) {
          body = el;
          break;
        }
      }
      if (body == null) {
        throw new SubmissionEnvelopeException(
            "ENVELOPE_NO_BODY",
            "<" + props.getEnvelopeContentElement() + "> does not wrap a <" + props.getBodyRoot()
                + "> element",
            new Object[] {props.getEnvelopeContentElement(), props.getBodyRoot()});
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Transformer transformer = newSecureTransformerFactory().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.transform(new DOMSource(body), new StreamResult(out));
      return out.toByteArray();

    } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
      throw new SubmissionEnvelopeException(
          "ENVELOPE_EXTRACTION_FAILED",
          "could not extract " + props.getBodyRoot() + " from envelope: " + e.getMessage(),
          new Object[] {props.getBodyRoot(), e.getMessage()},
          e);
    }
  }

  private enum RootKind { ENVELOPE, BODY, UNKNOWN }
}
