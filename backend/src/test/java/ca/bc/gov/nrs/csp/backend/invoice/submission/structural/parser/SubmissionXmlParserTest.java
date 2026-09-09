package ca.bc.gov.nrs.csp.backend.invoice.submission.structural.parser;

import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.SubmissionValidationProperties;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.schema.SchemaValidator;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

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
  void trims_padded_values_so_they_are_measured_without_their_padding() throws IOException {
    // Every value here is valid once trimmed, and several (grade, species,
    // secondary sort code, the line-wrapped invoice number) would breach their
    // maxLength facet if the padding counted. The facets are checked during the
    // parse, so this has to hold at this level.
    SubmissionXmlParser.ParseOutcome out = parser.parse(read("whitespace-padded.xml"));

    assertThat(out.errors()).isEmpty();
    assertThat(out.ok()).isTrue();

    CSPSubmissionType submission = (CSPSubmissionType) out.submission();
    CSPInvoiceType invoice = submission.getCSPInvoice().get(0);
    assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-TEST-001");
    assertThat(invoice.getInvoiceType()).isEqualTo("SAL");
    assertThat(invoice.getCSPInvoiceDetails().getMaturity()).isEqualTo("O");
    assertThat(invoice.getCSPInvoiceDetails().getLocationFOB()).isEqualTo("TEST-FOB");

    CSPLineItemType line = invoice.getCSPLineItem().get(0);
    assertThat(line.getGrade()).isEqualTo("B");
    assertThat(line.getSpecies()).isEqualTo("XX");
    assertThat(line.getSecondarySortCode()).isEqualTo("S1");
  }

  @Test
  void keeps_whitespace_inside_a_value() throws IOException {
    // Only the ends are trimmed, so multi-line free text is not reflowed.
    SubmissionXmlParser.ParseOutcome out = parser.parse(read("whitespace-padded.xml"));

    CSPSubmissionType submission = (CSPSubmissionType) out.submission();
    assertThat(submission.getCSPInvoice().get(0).getCSPInvoiceDetails().getSubmitterNotes())
        .isEqualTo("first line\n        second line");
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

  @Test
  void initContext_with_bad_package_throws_illegal_state() throws Exception {
    SubmissionValidationProperties props = new SubmissionValidationProperties();
    props.setJaxbContextPath("ca.bc.gov.nrs.csp.backend.no.such.pkg");
    SubmissionXmlParser broken = new SubmissionXmlParser(props, new SchemaValidator(props));
    Method m = SubmissionXmlParser.class.getDeclaredMethod("initContext");
    m.setAccessible(true);

    InvocationTargetException ex =
        assertThrows(InvocationTargetException.class, () -> m.invoke(broken));

    assertThat(ex.getCause())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to initialize JAXB context")
        .hasCauseInstanceOf(JAXBException.class);
  }

  /**
   * JAXB events are allowed to arrive without a locator; the collected error
   * must then carry a null path instead of a formatted line/col. A real parse
   * always supplies a locator, so the unmarshaller is stubbed to emit one.
   */
  @Test
  void parse_event_without_locator_reports_null_path() throws Exception {
    Unmarshaller unmarshaller = mock(Unmarshaller.class);
    JAXBContext ctx = mock(JAXBContext.class);
    given(ctx.createUnmarshaller()).willReturn(unmarshaller);

    AtomicReference<ValidationEventHandler> handler = new AtomicReference<>();
    willAnswer(inv -> {
      handler.set(inv.getArgument(0));
      return null;
    }).given(unmarshaller).setEventHandler(any());

    ValidationEvent event = mock(ValidationEvent.class);
    given(event.getLocator()).willReturn(null);
    given(event.getMessage()).willReturn("no locator available");
    given(unmarshaller.unmarshal(any(XMLStreamReader.class))).willAnswer(inv -> {
      handler.get().handleEvent(event);
      return new Object(); // not a JAXBElement → unwrap() passes it through
    });

    SubmissionXmlParser.ParseOutcome out = parserWith(ctx).parse("<x/>".getBytes());

    assertThat(out.errors()).hasSize(1);
    assertThat(out.errors().get(0).path()).isNull();
    assertThat(out.errors().get(0).code()).isEqualTo("JAXB");
    assertThat(out.errors().get(0).args()).containsExactly("no locator available");
    assertThat(out.submission()).isNotNull();
  }

  @Test
  void parse_unwraps_jaxbElement_root_to_its_value() throws Exception {
    Unmarshaller unmarshaller = mock(Unmarshaller.class);
    JAXBContext ctx = mock(JAXBContext.class);
    given(ctx.createUnmarshaller()).willReturn(unmarshaller);
    given(unmarshaller.unmarshal(any(XMLStreamReader.class)))
        .willReturn(new JAXBElement<>(new QName("x"), String.class, "body"));

    SubmissionXmlParser.ParseOutcome out = parserWith(ctx).parse("<x/>".getBytes());

    assertThat(out.errors()).isEmpty();
    assertThat(out.submission()).isEqualTo("body");
    assertThat(out.ok()).isTrue();
  }

  private static byte[] read(String fixture) throws IOException {
    return new ClassPathResource("fixtures/submissions/" + fixture).getInputStream().readAllBytes();
  }

  private static void invoke(Object bean, String method) throws Exception {
    Method m = bean.getClass().getDeclaredMethod(method);
    m.setAccessible(true);
    m.invoke(bean);
  }

  /** Builds a parser whose JAXB context is replaced by the given stub. */
  private static SubmissionXmlParser parserWith(JAXBContext ctx) throws Exception {
    SubmissionValidationProperties props = new SubmissionValidationProperties();
    SubmissionXmlParser p = new SubmissionXmlParser(props, new SchemaValidator(props));
    Field f = SubmissionXmlParser.class.getDeclaredField("jaxbContext");
    f.setAccessible(true);
    f.set(p, ctx);
    return p;
  }
}
