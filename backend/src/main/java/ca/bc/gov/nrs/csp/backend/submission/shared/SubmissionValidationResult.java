package ca.bc.gov.nrs.csp.backend.submission.shared;

import java.util.List;

/**
 * Final, merged outcome of validating a submission upload — the type the
 * controller consumes. Shared by both phases:
 *
 * <ul>
 *   <li>{@code valid} — true when the submission is accepted (no blocking
 *       errors, and for the business phase at least one invoice accepted).</li>
 *   <li>{@code errors} — the flat message list (ERROR and WARNING entries; each
 *       carries its own {@link Severity} and locator).</li>
 *   <li>{@code acceptance} — per-invoice accept/reject summary from the business
 *       phase; {@link SubmissionAcceptance#empty()} for structural-only.</li>
 * </ul>
 */
public record SubmissionValidationResult(
    boolean valid,
    List<SubmissionValidationError> errors,
    SubmissionAcceptance acceptance) {

  public static SubmissionValidationResult ok() {
    return new SubmissionValidationResult(true, List.of(), SubmissionAcceptance.empty());
  }

  public static SubmissionValidationResult failed(List<SubmissionValidationError> errors) {
    return new SubmissionValidationResult(false, List.copyOf(errors), SubmissionAcceptance.empty());
  }
}
