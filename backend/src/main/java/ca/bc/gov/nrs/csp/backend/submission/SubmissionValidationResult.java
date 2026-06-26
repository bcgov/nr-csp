package ca.bc.gov.nrs.csp.backend.submission;

import java.util.List;

/**
 * Outcome of structural (format + envelope + schema) validation of a
 * submission upload. Empty {@code errors} on success, populated and
 * blocking on failure. Business-rule validation (DB lookups, cross-field
 * rules) runs separately, after this passes.
 */
public record SubmissionValidationResult(boolean valid, List<SubmissionValidationError> errors) {

  public static SubmissionValidationResult ok() {
    return new SubmissionValidationResult(true, List.of());
  }

  public static SubmissionValidationResult failed(List<SubmissionValidationError> errors) {
    return new SubmissionValidationResult(false, List.copyOf(errors));
  }
}
