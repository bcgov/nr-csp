package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

/**
 * §2.4 Reference-data code rules. Both use {@code ctx.referenceData()}. One
 * method per rule; add a call in {@link #validate} in catalogue order as each is
 * implemented (see {@code docs/submission-validation-business-rules.md}).
 *
 * <ul>
 *   <li>I22 — maturity code recognised and active on the invoice date (ERROR)</li>
 *   <li>I23 — primary sort code recognised and active on the invoice date (ERROR)</li>
 * </ul>
 */
@Component
public class InvoiceCodeRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    // TODO: I22, I23 — add one call per rule, in catalogue order.
  }
}
