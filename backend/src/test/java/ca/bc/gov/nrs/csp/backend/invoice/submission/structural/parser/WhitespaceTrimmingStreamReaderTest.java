package ca.bc.gov.nrs.csp.backend.invoice.submission.structural.parser;

import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the text accessors directly: which one a consumer reaches for is up to
 * the unmarshaller, so all of them have to agree on the trimmed value.
 */
class WhitespaceTrimmingStreamReaderTest {

  private static final String DOC = "<r><a>  B  </a><b>x  y</b><c>\n  wrapped\n</c></r>";

  @Test
  void trims_the_ends_of_a_value_across_every_text_accessor() throws Exception {
    XMLStreamReader reader = readerAt("a");

    assertThat(reader.getText()).isEqualTo("B");
    assertThat(reader.getTextLength()).isEqualTo(1);
    assertThat(reader.getTextStart()).isZero();
    assertThat(reader.getTextCharacters()).containsExactly('B');

    char[] buffer = new char[8];
    assertThat(reader.getTextCharacters(0, buffer, 0, buffer.length)).isEqualTo(1);
    assertThat(buffer[0]).isEqualTo('B');
    // Asking past the end of the value copies nothing.
    assertThat(reader.getTextCharacters(1, buffer, 0, buffer.length)).isZero();
  }

  @Test
  void keeps_whitespace_inside_a_value() throws Exception {
    assertThat(readerAt("b").getText()).isEqualTo("x  y");
  }

  @Test
  void drops_the_indentation_of_a_value_split_across_lines() throws Exception {
    assertThat(readerAt("c").getText()).isEqualTo("wrapped");
  }

  @Test
  void getElementText_is_trimmed_too() throws Exception {
    XMLStreamReader reader = trimmingReader();
    reader.nextTag(); // <r>
    reader.nextTag(); // <a>

    assertThat(reader.getEventType()).isEqualTo(XMLStreamConstants.START_ELEMENT);
    assertThat(reader.getElementText()).isEqualTo("B");
  }

  /** Advances to the character event inside the named element. */
  private static XMLStreamReader readerAt(String element) throws XMLStreamException {
    XMLStreamReader reader = trimmingReader();
    boolean inside = false;
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        inside = element.equals(reader.getLocalName());
      } else if (event == XMLStreamConstants.CHARACTERS && inside) {
        return reader;
      }
    }
    throw new IllegalStateException("no text found in <" + element + ">");
  }

  private static XMLStreamReader trimmingReader() throws XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
    factory.setProperty(XMLInputFactory.IS_COALESCING, true);
    return new WhitespaceTrimmingStreamReader(
        factory.createXMLStreamReader(new ByteArrayInputStream(DOC.getBytes(StandardCharsets.UTF_8))));
  }
}
