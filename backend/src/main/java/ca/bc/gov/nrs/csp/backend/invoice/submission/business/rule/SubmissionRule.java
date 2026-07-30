package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule;

/**
 * Submission-level business rules. Implemented by a
 * {@code @Component} under {@code rule.submission} — normally the single
 * {@code SubmissionRules} class, with one method per rule. Auto-discovered and
 * run once per submission. Emit findings via {@code ctx.error(...)} /
 * {@code ctx.warning(...)}; never throw for a validation failure.
 *
 * <p>See {@code rule.submission.SubmissionRules} for the template.
 */
public interface SubmissionRule {

  void validate(SubmissionRuleContext ctx);
}
