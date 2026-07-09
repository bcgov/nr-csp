package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the §2.1 invoice type rules. The individual rules (I1–I3) are
 * not implemented yet, so {@link InvoiceTypeRules#validate} must run cleanly and
 * record nothing. Extend this class as each rule is implemented.
 */
class InvoiceTypeRulesTest {

  private final InvoiceTypeRules rules = new InvoiceTypeRules();

  @Test
  void validate_records_nothing_while_no_rule_is_implemented() {
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext context(ValidationCollector collector) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector);
  }
}
