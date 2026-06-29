package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

/**
 * §2.5 Totals &amp; variance rules. Calculated totals are summed from the line
 * items ({@code ctx.invoice().getCSPLineItem()}); no DB needed. One method per
 * rule; add a call in {@link #validate} in catalogue order as each is implemented
 * (see {@code docs/submission-validation-business-rules.md}).
 *
 * <ul>
 *   <li>I24 — total amount cannot be negative (except ADJ) (ERROR)</li>
 *   <li>I25 — submitted total amount within ±$5.00 of calculated (WARNING)</li>
 *   <li>I26 — total volume cannot be negative (except ADJ) (ERROR)</li>
 *   <li>I27 — submitted total volume within ±5.00 of calculated (WARNING)</li>
 *   <li>I28 — total pieces cannot be negative (except ADJ) (ERROR)</li>
 *   <li>I29 — submitted total pieces exactly matches calculated (WARNING)</li>
 * </ul>
 */
@Component
public class InvoiceTotalsRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    // TODO: I24–I29 — add one call per rule, in catalogue order.
  }
}
