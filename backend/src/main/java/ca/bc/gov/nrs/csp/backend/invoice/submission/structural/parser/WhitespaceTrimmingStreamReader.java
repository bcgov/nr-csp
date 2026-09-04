package ca.bc.gov.nrs.csp.backend.invoice.submission.structural.parser;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

/**
 * Strips leading and trailing whitespace from each element's text as the
 * document is read, so the schema facets and the bound values see the value the
 * submitter meant rather than the way the file happens to be laid out.
 *
 * <p>Without this a length-capped field fails its {@code maxLength} facet on
 * padding alone — {@code <grade>B  </grade>} is three characters against a
 * maxLength of one — and an element split across lines carries its indentation
 * into the value. Those facets are checked while the document is parsed, so the
 * trimming has to happen here: canonicalising the tree afterwards is too late,
 * because the parse has already failed. Numeric and date fields never had the
 * problem — their built-in whitespace facet collapses padding for them.
 *
 * <p>Only the ends are trimmed; whitespace inside a value is left exactly as it
 * was, so multi-line free text such as submitter notes survives. That relies on
 * the reader being coalescing, so each run of character data arrives as a single
 * event and internal whitespace never sits on a chunk boundary — see
 * {@link SubmissionXmlParser}, which configures it.
 */
final class WhitespaceTrimmingStreamReader extends StreamReaderDelegate {

  /** Trimmed text of the current character event; null for any other event. */
  private String trimmed;

  WhitespaceTrimmingStreamReader(XMLStreamReader reader) {
    super(reader);
  }

  @Override
  public int next() throws XMLStreamException {
    int event = super.next();
    trimmed = isCharacterEvent(event) ? super.getText().trim() : null;
    return event;
  }

  @Override
  public int nextTag() throws XMLStreamException {
    int event = super.nextTag();
    // nextTag() skips over character data without passing through next(), so the
    // cached text would otherwise outlive the event it belongs to.
    trimmed = null;
    return event;
  }

  @Override
  public String getElementText() throws XMLStreamException {
    return super.getElementText().trim();
  }

  @Override
  public String getText() {
    return trimmed != null ? trimmed : super.getText();
  }

  @Override
  public int getTextLength() {
    return trimmed != null ? trimmed.length() : super.getTextLength();
  }

  @Override
  public int getTextStart() {
    return trimmed != null ? 0 : super.getTextStart();
  }

  @Override
  public char[] getTextCharacters() {
    return trimmed != null ? trimmed.toCharArray() : super.getTextCharacters();
  }

  @Override
  public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
      throws XMLStreamException {
    if (trimmed == null) {
      return super.getTextCharacters(sourceStart, target, targetStart, length);
    }
    int copied = Math.min(length, trimmed.length() - sourceStart);
    if (copied <= 0) {
      return 0;
    }
    trimmed.getChars(sourceStart, sourceStart + copied, target, targetStart);
    return copied;
  }

  private static boolean isCharacterEvent(int event) {
    return event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA;
  }
}
