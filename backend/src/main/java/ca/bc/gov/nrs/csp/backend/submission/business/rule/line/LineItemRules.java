package ca.bc.gov.nrs.csp.backend.submission.business.rule.line;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.LineItemRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.LineItemRuleContext;
import org.springframework.stereotype.Component;

/**
 * All line-item-level business rules (catalogue §3), one method per rule.
 *
 * <p><b>How to add a rule:</b> write a package-private
 * {@code void xxx(LineItemRuleContext ctx)} method (header-commented with its
 * catalogue ID), then add a call to it in {@link #validate} in catalogue order.
 * Report via {@code ctx.error(...)} (blocking) or {@code ctx.warning(...)}
 * (non-blocking) using the message key from
 * {@code docs/submission-validation-business-rules.md}; never throw. Unit-test
 * each method directly (see {@code LineItemRulesTest}).
 */
@Component
public class LineItemRules implements LineItemRule {

  @Override
  public void validate(LineItemRuleContext ctx) {
    gradeZWarning(ctx); // L4
  }

  /** L4 — grade {@code "Z"} raises a non-blocking warning. */
  void gradeZWarning(LineItemRuleContext ctx) {
    if ("Z".equals(ctx.line().getGrade())) {
      ctx.warning("invoice.grade.z.warning", "Grade Z is used.");
    }
  }
}
