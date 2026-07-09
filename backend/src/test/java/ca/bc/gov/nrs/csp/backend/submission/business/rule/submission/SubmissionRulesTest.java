package ca.bc.gov.nrs.csp.backend.submission.business.rule.submission;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.SubmissionRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmitterType;
import ca.bc.gov.nrs.csp.backend.submission.shared.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Template for testing a submission rule: call the rule's method directly (so
 * the test stays focused on one rule even as more are added to the class), mock
 * {@link ReferenceDataService}, and assert on the collector.
 */
@ExtendWith(MockitoExtension.class)
class SubmissionRulesTest {

  @Mock
  ReferenceDataService referenceData;

  private final SubmissionRules rules = new SubmissionRules();

  @Test
  void clientLocation_errors_when_combination_not_found() {
    given(referenceData.clientLocationExists("100", "00")).willReturn(false);
    ValidationCollector collector = new ValidationCollector();

    rules.clientLocationExists(context(collector, "100", "00"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.submitter.client.location.invalid.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void clientLocation_passes_when_combination_found() {
    given(referenceData.clientLocationExists("100", "00")).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.clientLocationExists(context(collector, "100", "00"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void clientLocation_errors_on_blank_without_hitting_the_db() {
    ValidationCollector collector = new ValidationCollector();

    rules.clientLocationExists(context(collector, "", "00"));

    assertThat(collector.entries()).hasSize(1);
    verifyNoInteractions(referenceData);
  }

  @Test
  void clientLocation_errors_when_locn_code_null_without_hitting_the_db() {
    ValidationCollector collector = new ValidationCollector();

    rules.clientLocationExists(context(collector, "100", null));

    assertThat(collector.entries()).hasSize(1);
    verifyNoInteractions(referenceData);
  }

  private SubmissionRuleContext context(ValidationCollector collector, String number, String locn) {
    CSPSubmitterType submitter = new CSPSubmitterType();
    submitter.setSubmissionClientNumber(number);
    submitter.setSubmissionClientLocnCode(locn);
    CSPSubmissionType submission = new CSPSubmissionType();
    submission.setCSPSubmitter(submitter);
    return new SubmissionRuleContext(submission, referenceData, collector);
  }
}
