package ca.bc.gov.nrs.csp.backend.submission.business.rule;

/**
 * Line-item-level business rules (catalogue §3, L1–L9). Implemented by a
 * {@code @Component} under {@code rule.line} — normally the single
 * {@code LineItemRules} class, with one method per rule. Auto-discovered and run
 * once per line item. Emit findings via {@code ctx.error(...)} /
 * {@code ctx.warning(...)}; never throw for a validation failure.
 *
 * <p>See {@code rule.line.LineItemRules} for the template.
 */
public interface LineItemRule {

  void validate(LineItemRuleContext ctx);
}
