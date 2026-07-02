package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

/**
 * §2.1 Invoice type rules. One method per rule; add a call in {@link #validate}
 * in catalogue order as each is implemented (see
 * {@code docs/submission-validation-business-rules.md}).
 *
 * <ul>
 *   <li>I1 — invoice type recognised and active on the invoice date (ERROR)</li>
 *   <li>I2 — type should be SAL or PUR, else warn (WARNING)</li>
 *   <li>I3 — submitter vs type: Seller cannot be PUR, Buyer cannot be SAL (ERROR)</li>
 * </ul>
 */
@Component
public class InvoiceTypeRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    // TODO: I1, I2, I3 — add one call per rule, in catalogue order.
  }
}
