package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.Severity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the submission rule context: both sinks record at the
 * submission level (null invoice index, {@code "submission"} locator).
 */
class SubmissionRuleContextTest {

  @Test
  void warning_records_a_submission_level_warning() {
    ValidationCollector collector = new ValidationCollector();
    SubmissionRuleContext ctx =
        new SubmissionRuleContext(new CSPSubmissionType(), null, collector);

    ctx.warning("some.warning.code", new Object[] {"informational only"});

    assertThat(collector.entries()).hasSize(1);
    ValidationCollector.Entry entry = collector.entries().get(0);
    assertThat(entry.invoiceIndex()).isNull();
    assertThat(entry.error().path()).isEqualTo("submission");
    assertThat(entry.error().code()).isEqualTo("some.warning.code");
    assertThat(entry.error().args()).containsExactly("informational only");
    assertThat(entry.error().severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void error_records_a_submission_level_error() {
    ValidationCollector collector = new ValidationCollector();
    SubmissionRuleContext ctx =
        new SubmissionRuleContext(new CSPSubmissionType(), null, collector);

    ctx.error("some.error.code", new Object[0]);

    assertThat(collector.entries()).hasSize(1);
    ValidationCollector.Entry entry = collector.entries().get(0);
    assertThat(entry.invoiceIndex()).isNull();
    assertThat(entry.error().path()).isEqualTo("submission");
    assertThat(entry.error().code()).isEqualTo("some.error.code");
    assertThat(entry.error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void submission_returns_the_parsed_submission() {
    CSPSubmissionType submission = new CSPSubmissionType();
    SubmissionRuleContext ctx =
        new SubmissionRuleContext(submission, null, new ValidationCollector());

    assertThat(ctx.submission()).isSameAs(submission);
  }
}
