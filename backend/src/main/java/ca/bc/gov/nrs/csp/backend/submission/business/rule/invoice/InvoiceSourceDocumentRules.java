package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

/**
 * §2.6 Source-document reference rules (Boom Numbers / Timber Marks / Weigh
 * Slips). Most are pure CSV checks; I38 uses {@code ctx.referenceData()}. One
 * method per rule; add a call in {@link #validate} in catalogue order as each is
 * implemented (see {@code docs/submission-validation-business-rules.md}).
 *
 * <ul>
 *   <li>I30 — at least one of boom / timber / weigh must be present (ERROR)</li>
 *   <li>I31 — duplicate values within a CSV are de-duplicated (transform, not a finding)</li>
 *   <li>I32 — at most 10 boom numbers (ERROR)</li>
 *   <li>I33 — at most 10 timber marks (ERROR)</li>
 *   <li>I34 — at most 10 weigh slips (ERROR)</li>
 *   <li>I35 — each boom-number token ≤ 20 chars (ERROR)</li>
 *   <li>I36 — each timber-mark token ≤ 6 chars (ERROR)</li>
 *   <li>I37 — each weigh-slip token ≤ 100 chars (ERROR)</li>
 *   <li>I38 — boom number already used by another invoice (WARNING)</li>
 * </ul>
 */
@Component
public class InvoiceSourceDocumentRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    // TODO: I30–I38 — add one call per rule, in catalogue order.
  }
}
