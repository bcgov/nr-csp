package ca.bc.gov.nrs.csp.backend.invoice.submission.shared;

import java.util.List;

/**
 * Per-invoice acceptance summary derived by the business-rule phase, supporting
 * the legacy "partial acceptance" behaviour: a submission can be accepted with
 * some invoices accepted and others rejected.
 *
 * <p>{@code accepted} / {@code rejected} hold invoice numbers. Empty for the
 * structural phase (which decides nothing per-invoice).
 */
public record SubmissionAcceptance(List<String> accepted, List<String> rejected) {

  public SubmissionAcceptance {
    accepted = List.copyOf(accepted);
    rejected = List.copyOf(rejected);
  }

  public static SubmissionAcceptance empty() {
    return new SubmissionAcceptance(List.of(), List.of());
  }
}
