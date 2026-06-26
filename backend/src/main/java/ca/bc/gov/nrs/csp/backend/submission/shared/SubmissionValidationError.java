package ca.bc.gov.nrs.csp.backend.submission.shared;

/**
 * A single validation error. {@code path} is a best-effort location (e.g.
 * {@code "line 12, col 8"} for structural errors, or an invoice/line
 * locator for business errors) and may be null; {@code code} is a stable
 * machine code (XSD / JAXB / XML_PARSE / ENVELOPE_* / FORMAT_UNRECOGNIZED
 * for the structural phase, or a business message key for the business
 * phase). Shared by both validation phases so callers handle them
 * uniformly; the controller maps these onto the app's
 * {@code ValidationMessageResponse} at the HTTP boundary.
 */
public record SubmissionValidationError(String path, String code, String message) {

  public static SubmissionValidationError of(String code, String message) {
    return new SubmissionValidationError(null, code, message);
  }

  public static SubmissionValidationError of(String path, String code, String message) {
    return new SubmissionValidationError(path, code, message);
  }
}
