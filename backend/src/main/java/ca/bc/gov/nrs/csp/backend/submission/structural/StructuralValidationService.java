package ca.bc.gov.nrs.csp.backend.submission.structural;

import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationResult;
import ca.bc.gov.nrs.csp.backend.submission.structural.parser.SubmissionEnvelopeException;
import ca.bc.gov.nrs.csp.backend.submission.structural.parser.SubmissionEnvelopeStripper;
import ca.bc.gov.nrs.csp.backend.submission.structural.parser.SubmissionXmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Structural validation: format detection → ESF envelope stripping →
 * schema-bound JAXB parse. This is the reusable core extracted from
 * nr-fspts — no business rules, no DB access, no application-specific
 * logic. Business-rule validation runs separately, after this passes;
 * the {@code SubmissionValidationService} orchestrator wires the two.
 *
 * <p>Returns an empty error list on success and a populated, blocking
 * list on failure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StructuralValidationService {

  private final SubmissionEnvelopeStripper envelopeStripper;
  private final SubmissionXmlParser parser;

  public SubmissionValidationResult validate(byte[] xml) {
    return validateAndParse(xml).result();
  }

  /**
   * Variant that also returns the parsed JAXB tree so callers (e.g. the
   * business-rule phase) don't have to re-parse on success. On failure,
   * {@code submission} may be null.
   */
  public ValidationOutcome validateAndParse(byte[] bytes) {
    List<SubmissionValidationError> errors = new ArrayList<>();

    SubmissionXmlParser.ParseOutcome outcome = switch (detectFormat(bytes)) {
      case XML -> parseXml(bytes, errors);
      case UNKNOWN -> {
        errors.add(SubmissionValidationError.of(
            "FORMAT_UNRECOGNIZED",
            "could not detect format — expected XML (starts with '<')"));
        yield new SubmissionXmlParser.ParseOutcome(null, List.of());
      }
    };
    errors.addAll(outcome.errors());

    SubmissionValidationResult result = errors.isEmpty()
        ? SubmissionValidationResult.ok()
        : SubmissionValidationResult.failed(errors);
    return new ValidationOutcome(result, outcome.submission());
  }

  /**
   * XML path: strip the ESF envelope if present, then JAXB-parse.
   * Envelope failures short-circuit with their dedicated error code.
   */
  private SubmissionXmlParser.ParseOutcome parseXml(
      byte[] bytes, List<SubmissionValidationError> errors) {
    byte[] bodyBytes;
    try {
      bodyBytes = envelopeStripper.toBareSubmission(bytes);
    } catch (SubmissionEnvelopeException e) {
      errors.add(SubmissionValidationError.of(e.getCode(), e.getMessage()));
      return new SubmissionXmlParser.ParseOutcome(null, List.of());
    }
    return parser.parse(bodyBytes);
  }

  /**
   * Cheap leading-byte sniff. Skips whitespace + UTF-8 BOM; anything
   * that isn't an opening '<' falls through to {@link Format#UNKNOWN}.
   */
  static Format detectFormat(byte[] bytes) {
    if (bytes == null) return Format.UNKNOWN;
    int i = 0;
    // Skip UTF-8 BOM.
    if (bytes.length >= 3
        && (bytes[0] & 0xFF) == 0xEF
        && (bytes[1] & 0xFF) == 0xBB
        && (bytes[2] & 0xFF) == 0xBF) {
      i = 3;
    }
    while (i < bytes.length) {
      byte b = bytes[i];
      if (b == ' ' || b == '\t' || b == '\n' || b == '\r') {
        i++;
        continue;
      }
      if (b == '<') return Format.XML;
      return Format.UNKNOWN;
    }
    return Format.UNKNOWN;
  }

  /** Submission upload format. */
  enum Format { XML, UNKNOWN }

  public record ValidationOutcome(SubmissionValidationResult result, Object submission) {}
}
