package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

@Component
public class InvoiceCodeRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    maturityValid(ctx); // I22
    primarySortCodeValid(ctx); // I23
  }

  /** Maturity code must be recognised and active on the invoice date. */
  void maturityValid(InvoiceRuleContext ctx) {
    String maturity = ctx.maturity();
    if (isBlank(maturity) || !ctx.referenceData().maturityValidOn(maturity, ctx.invoiceDate())) {
      ctx.error(
          "invoice.maturity.invalid.error",
          "maturity " + maturity + " for invoiceNumber " + ctx.invoiceNumber()
              + " is not a recognized code active on the invoice date.");
    }
  }

  /**
   * Primary sort code, when supplied, must be a recognised code active on
   * the invoice date. The field is optional, so a blank value is left untouched.
   */
  void primarySortCodeValid(InvoiceRuleContext ctx) {
    String sortCode = ctx.primarySortCode();
    if (isBlank(sortCode)) {
      return;
    }
    if (!ctx.referenceData().sortCodeValidOn(sortCode, ctx.invoiceDate())) {
      ctx.error(
          "invoice.primary.sortcode.invalid.error",
          "primarySortCode " + sortCode + " for invoiceNumber " + ctx.invoiceNumber()
              + " is not a recognized code active on the invoice date.");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
