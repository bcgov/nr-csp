package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

/**
 * The coded fields on the invoice-details block — the same trio the manual path
 * validates together: maturity and primary sort code against reference data, and
 * FOB location for presence only (the schema notes the client may enter any value
 * there and that it is checked against no table).
 */
@Component
public class InvoiceCodeRules implements InvoiceRule {

  /** For keys whose messages.properties template takes no placeholders. */
  private static final Object[] NO_ARGS = new Object[0];

  @Override
  public void validate(InvoiceRuleContext ctx) {
    maturityValid(ctx);
    locationFobProvided(ctx);
    primarySortCodeValid(ctx);
  }

  /**
   * Maturity is required, and must be recognised and active on the invoice date.
   * The two failures are reported separately: an empty element (which the schema
   * permits — its type only caps the length) says the field is missing rather
   * than blaming a code that was never supplied. Template: code, date.
   */
  void maturityValid(InvoiceRuleContext ctx) {
    String maturity = ctx.maturity();
    if (isBlank(maturity)) {
      ctx.error("invoice.maturity.required.error", new Object[0]);
      return;
    }
    if (!ctx.referenceData().maturityValidOn(maturity, ctx.invoiceDate())) {
      ctx.error("invoice.maturity.invalid.error", new Object[] {maturity, ctx.invoiceDate()});
    }
  }

  /**
   * FOB location is required (0-arg template). The schema requires the element
   * but permits it to be empty — its type only caps the length — so presence has
   * to be checked here or a blank sails through: nothing else looks at this field,
   * since its value is matched against no reference table. Shares the manual
   * path's message key, so both channels report it identically.
   */
  void locationFobProvided(InvoiceRuleContext ctx) {
    if (isBlank(ctx.locationFOB())) {
      ctx.error("invoice.fob.required.error", NO_ARGS);
    }
  }

  /**
   * Primary sort code, when supplied, must be a recognised code active on
   * the invoice date. The field is optional, so a blank value is left untouched.
   * Template: code, date.
   */
  void primarySortCodeValid(InvoiceRuleContext ctx) {
    String sortCode = ctx.primarySortCode();
    if (isBlank(sortCode)) {
      return;
    }
    if (!ctx.referenceData().sortCodeValidOn(sortCode, ctx.invoiceDate())) {
      ctx.error("invoice.primary.sortcode.invalid.error", new Object[] {sortCode, ctx.invoiceDate()});
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
