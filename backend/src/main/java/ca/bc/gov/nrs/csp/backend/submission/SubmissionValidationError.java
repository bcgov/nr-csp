package ca.bc.gov.nrs.csp.backend.submission;

/**
 * A single structural validation error. {@code path} is a best-effort
 * location (e.g. {@code "line 12, col 8"}) and may be null; {@code code}
 * is a stable machine code (XSD / JAXB / XML_PARSE / ENVELOPE_* /
 * FORMAT_UNRECOGNIZED). Ported from nr-fspts so the pipeline stays
 * self-contained and easy to lift into a shared artifact; the controller
 * maps these onto the app's {@code ValidationErrorResponse} at the
 * HTTP boundary.
 */
public record SubmissionValidationError(String path, String code, String message) {

  public static SubmissionValidationError of(String code, String message) {
    return new SubmissionValidationError(null, code, message);
  }

  public static SubmissionValidationError of(String path, String code, String message) {
    return new SubmissionValidationError(path, code, message);
  }
}
