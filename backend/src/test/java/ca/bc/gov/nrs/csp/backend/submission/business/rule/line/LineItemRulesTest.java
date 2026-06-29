package ca.bc.gov.nrs.csp.backend.submission.business.rule.line;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.LineItemRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.submission.shared.Severity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Template for testing a line rule: call the rule's method directly and assert
 * severity. {@code gradeZWarning} records a non-blocking WARNING.
 */
class LineItemRulesTest {

  private final LineItemRules rules = new LineItemRules();

  @Test
  void gradeZWarning_warns_on_grade_z() {
    ValidationCollector collector = new ValidationCollector();

    rules.gradeZWarning(context(collector, "Z"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.grade.z.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void gradeZWarning_silent_for_other_grades() {
    ValidationCollector collector = new ValidationCollector();

    rules.gradeZWarning(context(collector, "A"));

    assertThat(collector.entries()).isEmpty();
  }

  private LineItemRuleContext context(ValidationCollector collector, String grade) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    CSPLineItemType line = new CSPLineItemType();
    line.setGrade(grade);
    return new LineItemRuleContext(invoice, line, 1, null, null, collector);
  }
}
