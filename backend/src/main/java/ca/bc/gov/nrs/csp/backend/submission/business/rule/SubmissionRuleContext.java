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

  /** Record a blocking error against the submission, with pre-rendered text. */
  public void error(String code, String message) {
    collector.add(null, SubmissionValidationError.error(LOCATOR, code, message));
  }

  /** Record a non-blocking warning against the submission, with pre-rendered text. */
  public void warning(String code, String message) {
    collector.add(null, SubmissionValidationError.warning(LOCATOR, code, message));
  }

  /**
   * Record a blocking error against the submission as a message key + its
   * {@code messages.properties} template args; the text is resolved at the HTTP
   * boundary (refactor doc §3.5).
   */
  public void error(String code, Object[] args) {
    collector.add(null, SubmissionValidationError.error(LOCATOR, code, args));
  }

  /** Warning counterpart of {@link #error(String, Object[])}. */
  public void warning(String code, Object[] args) {
    collector.add(null, SubmissionValidationError.warning(LOCATOR, code, args));
  }
}
