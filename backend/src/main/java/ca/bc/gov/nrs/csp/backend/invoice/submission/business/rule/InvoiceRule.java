package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule;

/**
 * Invoice-level business rules. Implemented by one
 * {@code @Component} sub-section under {@code rule.invoice}
 * ({@code InvoiceTypeRules}, {@code InvoiceReferenceRules}, {@code InvoicePartyRules},
 * {@code InvoiceCodeRules}, {@code InvoiceTotalsRules}, {@code InvoiceSourceDocumentRules},
 * {@code InvoiceDateRules}, {@code InvoiceLineItemPresenceRules}). Each is a
 * cohesive bean, auto-discovered and run once per invoice. Emit findings via
 * {@code ctx.error(...)} / {@code ctx.warning(...)}; never throw for a
 * validation failure.
 *
 * <p>See {@code rule.invoice.InvoiceDateRules} for the template.
 */
public interface InvoiceRule {

  void validate(InvoiceRuleContext ctx);
}
