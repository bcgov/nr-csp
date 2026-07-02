package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

/**
 * §2.8 Line-item presence rule. Pure check against
 * {@code ctx.invoice().getCSPLineItem()}. Add the call in {@link #validate} when
 * implemented (see {@code docs/submission-validation-business-rules.md}).
 *
 * <ul>
 *   <li>I41 — an invoice must have at least one line item (ERROR)</li>
 * </ul>
 */
@Component
public class InvoiceLineItemPresenceRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    // TODO: I41 — at least one line item.
  }
}
