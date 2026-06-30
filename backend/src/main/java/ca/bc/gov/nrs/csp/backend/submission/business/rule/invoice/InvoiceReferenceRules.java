package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

/**
 * §2.2 Replace / Adjust invoice-reference rules. One method per rule; add a call
 * in {@link #validate} in catalogue order as each is implemented (see
 * {@code docs/submission-validation-business-rules.md}).
 *
 * <ul>
 *   <li>I4 — only one of Replaces / Adjusts may be populated (ERROR)</li>
 *   <li>I5 — each Replaces number identifies another invoice for this client (ERROR)</li>
 *   <li>I6 — an invoice cannot replace itself (ERROR)</li>
 *   <li>I7 — at most 5 Replaces numbers (ERROR)</li>
 *   <li>I8 — each Adjusts number identifies another invoice for this client (ERROR)</li>
 *   <li>I9 — an adjusted invoice cannot be cancelled/deleted (ERROR)</li>
 *   <li>I10 — an invoice cannot adjust itself (ERROR)</li>
 *   <li>I11 — at most 5 Adjusts numbers (ERROR)</li>
 * </ul>
 */
@Component
public class InvoiceReferenceRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    // TODO: I4–I11 — add one call per rule, in catalogue order.
  }
}
