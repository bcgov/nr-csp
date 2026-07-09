package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

@Component
public class InvoiceLineItemPresenceRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    lineItemExists(ctx);
  }

  /** An invoice must have at least one line item (0-arg template). */
  void lineItemExists(InvoiceRuleContext ctx) {
    boolean hasLineItems = ctx.invoice().getCSPLineItem() != null && !ctx.invoice().getCSPLineItem().isEmpty();

    if (!hasLineItems) {
      ctx.error("invoice.noline.item.error", new Object[0]);
    }
  }
}
