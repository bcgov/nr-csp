package ca.bc.gov.nrs.csp.backend.submission.business.rule;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;

/**
 * What a {@link SubmissionRule} receives: the parsed submission, reference-data
 * access, and {@code error(...)} / {@code warning(...)} sinks pre-scoped to the
 * submission level (no invoice locator). A submission-level ERROR rejects the
 * whole submission.
 */
public final class SubmissionRuleContext {

  private static final String LOCATOR = "submission";

  private final CSPSubmissionType submission;
  private final ReferenceDataService referenceData;
  private final ValidationCollector collector;

  public SubmissionRuleContext(
      CSPSubmissionType submission,
      ReferenceDataService referenceData,
      ValidationCollector collector) {
    this.submission = submission;
    this.referenceData = referenceData;
    this.collector = collector;
  }

  public CSPSubmissionType submission() {
    return submission;
  }

  public ReferenceDataService referenceData() {
    return referenceData;
  }

  /** Record a blocking error against the submission. */
  public void error(String code, String message) {
    collector.add(null, SubmissionValidationError.error(LOCATOR, code, message));
  }

  /** Record a non-blocking warning against the submission. */
  public void warning(String code, String message) {
    collector.add(null, SubmissionValidationError.warning(LOCATOR, code, message));
  }
}
