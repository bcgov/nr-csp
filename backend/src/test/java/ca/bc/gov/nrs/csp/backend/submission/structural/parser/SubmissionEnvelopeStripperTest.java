package ca.bc.gov.nrs.csp.backend.submission.structural.parser;

import ca.bc.gov.nrs.csp.backend.submission.structural.SubmissionValidationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Drives every branch of the envelope stripper directly: bare body pass-through,
 * ESF extraction, and each failure code (unrecognized root, missing content,
 * no body, extraction failure, parse error) — which also exercises both
 * {@link SubmissionEnvelopeException} constructors and {@code getCode()}.
 */
class SubmissionEnvelopeStripperTest {

  private SubmissionEnvelopeStripper stripper;

  @BeforeEach
  void setUp() {
    stripper = new SubmissionEnvelopeStripper(new SubmissionValidationProperties());
  }

  private static byte[] xml(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  private static Stream<Arguments> unrecognizedRootInputs() {
    return Stream.of(
        Arguments.of("<CSPSubmission/>"),
        Arguments.of("<csp:Other xmlns:csp=\"http://www.for.gov.bc.ca/schema/csp\"/>"),
        Arguments.of("<esf:Other xmlns:esf=\"http://www.for.gov.bc.ca/schema/esf\"/>")
    );
  }

  @Test
  void bare_body_passes_through_unchanged() throws Exception {
    byte[] raw = xml("<csp:CSPSubmission xmlns:csp=\"http://www.for.gov.bc.ca/schema/csp\"/>");
    assertThat(stripper.toBareSubmission(raw)).isEqualTo(raw);
  }

  @Test
  void esf_envelope_extracts_inner_body() throws Exception {
    byte[] raw = xml(
        "<esf:ESFSubmission xmlns:esf=\"http://www.for.gov.bc.ca/schema/esf\""
            + " xmlns:csp=\"http://www.for.gov.bc.ca/schema/csp\">"
            + "<esf:submissionContent>"
            + "<csp:CSPSubmission><csp:monthComplete>N</csp:monthComplete></csp:CSPSubmission>"
            + "</esf:submissionContent></esf:ESFSubmission>");
    String out = new String(stripper.toBareSubmission(raw), StandardCharsets.UTF_8);
    assertThat(out).contains("CSPSubmission").doesNotContain("ESFSubmission");
  }

  @Test
  void unknown_root_is_unrecognized() {
    SubmissionEnvelopeException ex = assertThrows(SubmissionEnvelopeException.class,
        () -> stripper.toBareSubmission(xml("<foo xmlns=\"urn:other\"/>")));
    assertThat(ex.getCode()).isEqualTo("ENVELOPE_UNRECOGNIZED");
  }

  @Test
  void envelope_without_content_is_missing_content() {
    byte[] raw = xml("<esf:ESFSubmission xmlns:esf=\"http://www.for.gov.bc.ca/schema/esf\"/>");
    SubmissionEnvelopeException ex = assertThrows(SubmissionEnvelopeException.class,
        () -> stripper.toBareSubmission(raw));
    assertThat(ex.getCode()).isEqualTo("ENVELOPE_MISSING_CONTENT");
  }

  @Test
  void envelope_content_without_body_is_no_body() {
    byte[] raw = xml(
        "<esf:ESFSubmission xmlns:esf=\"http://www.for.gov.bc.ca/schema/esf\">"
            + "<esf:submissionContent><other/></esf:submissionContent></esf:ESFSubmission>");
    SubmissionEnvelopeException ex = assertThrows(SubmissionEnvelopeException.class,
        () -> stripper.toBareSubmission(raw));
    assertThat(ex.getCode()).isEqualTo("ENVELOPE_NO_BODY");
  }

  @Test
  void malformed_envelope_body_is_extraction_failed() {
    // Root element is recognised as the ESF envelope, but the document is not
    // well-formed past it — DOM parse fails inside extractInner.
    byte[] raw = xml(
        "<esf:ESFSubmission xmlns:esf=\"http://www.for.gov.bc.ca/schema/esf\">"
            + "<esf:submissionContent><unclosed></esf:submissionContent></esf:ESFSubmission>");
    SubmissionEnvelopeException ex = assertThrows(SubmissionEnvelopeException.class,
        () -> stripper.toBareSubmission(raw));
    assertThat(ex.getCode()).isEqualTo("ENVELOPE_EXTRACTION_FAILED");
  }

  @Test
  void non_xml_content_is_parse_error() {
    // No element start at all — the StAX read in detectRoot throws.
    SubmissionEnvelopeException ex = assertThrows(SubmissionEnvelopeException.class,
        () -> stripper.toBareSubmission(xml("%%% not xml at all %%%")));
    assertThat(ex.getCode()).isEqualTo("ENVELOPE_PARSE_ERROR");
  }

  @ParameterizedTest
  @MethodSource("unrecognizedRootInputs")
  void unrecognized_roots_are_rejected(String payload) {
    SubmissionEnvelopeException ex = assertThrows(SubmissionEnvelopeException.class,
        () -> stripper.toBareSubmission(xml(payload)));
    assertThat(ex.getCode()).isEqualTo("ENVELOPE_UNRECOGNIZED");
  }

  @Test
  void envelope_content_with_wrong_body_local_name_is_no_body() {
    // The content child is in the body namespace but has the wrong local
    // name, so the body scan must reject it.
    byte[] raw = xml(
        "<esf:ESFSubmission xmlns:esf=\"http://www.for.gov.bc.ca/schema/esf\""
            + " xmlns:csp=\"http://www.for.gov.bc.ca/schema/csp\">"
            + "<esf:submissionContent><csp:NotTheBody/></esf:submissionContent>"
            + "</esf:ESFSubmission>");
    SubmissionEnvelopeException ex = assertThrows(SubmissionEnvelopeException.class,
        () -> stripper.toBareSubmission(raw));
    assertThat(ex.getCode()).isEqualTo("ENVELOPE_NO_BODY");
  }
}
