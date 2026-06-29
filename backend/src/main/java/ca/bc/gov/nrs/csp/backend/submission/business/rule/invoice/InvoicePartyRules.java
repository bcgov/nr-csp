package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

/**
 * §2.3 Party rules (submitter / other party / seller / buyer). Read the resolved
 * parties from {@code ctx.submitter()}. One method per rule; add a call in
 * {@link #validate} in catalogue order as each is implemented (see
 * {@code docs/submission-validation-business-rules.md}).
 *
 * <ul>
 *   <li>I12 — other-party free-text fields must be empty when both parties have number+location (ERROR)</li>
 *   <li>I13 — invalid buyer-submission combination (ERROR)</li>
 *   <li>I14 — invalid seller-submission combination (ERROR)</li>
 *   <li>I15 — buyer number+location must exist (ERROR)</li>
 *   <li>I16 — seller number+location must exist (ERROR)</li>
 *   <li>I17 — submission client number must equal seller client number (ERROR)</li>
 *   <li>I18 — submission client location must equal seller client location (ERROR)</li>
 *   <li>I19 — other-party number+location, when supplied, must exist (ERROR)</li>
 *   <li>I20 — other-party name/city/province required when other-party number is blank (ERROR)</li>
 *   <li>I21 — seller and buyer cannot be the same client number+location (ERROR)</li>
 * </ul>
 */
@Component
public class InvoicePartyRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    // TODO: I12–I21 — add one call per rule, in catalogue order.
  }
}
