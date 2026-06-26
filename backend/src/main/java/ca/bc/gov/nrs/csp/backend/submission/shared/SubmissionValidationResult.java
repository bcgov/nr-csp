package ca.bc.gov.nrs.csp.backend.submission.shared;

import java.util.List;

/**
 * Outcome of validating a submission upload. Empty {@code errors} on
 * success, populated and blocking on failure. Shared by both the
 * structural (format + envelope + schema) and business-rule phases; the
 * orchestrator merges the two into a single result.
 */
public record SubmissionValidationResult(boolean valid, List<SubmissionValidationError> errors) {

  public static SubmissionValidationResult ok() {
    return new SubmissionValidationResult(true, List.of());
  }

  public static SubmissionValidationResult failed(List<SubmissionValidationError> errors) {
    return new SubmissionValidationResult(false, List.copyOf(errors));
  }
}
