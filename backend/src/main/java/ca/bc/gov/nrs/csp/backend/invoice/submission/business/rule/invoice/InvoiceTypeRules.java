package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import org.springframework.stereotype.Component;

@Component
public class InvoiceTypeRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    if (invoiceTypeValidOn(ctx)) { // I1
      saleOrPurchase(ctx); // I2
    }
    submitterVsType(ctx); // I3
  }

  /**
   * Invoice type must be recognised and active on the invoice date.
   *
   * @return true if the type is valid, matching the
   *     legacy {@code checkInvoiceType} return value used for that gate.
   */
  boolean invoiceTypeValidOn(InvoiceRuleContext ctx) {
    String type = ctx.invoice().getInvoiceType();
    if (isBlank(type) || !ctx.referenceData().invoiceTypeValidOn(type, ctx.invoiceDate())) {
      ctx.error("invoice.type.invalid.error", new Object[] {type, ctx.invoiceDate()});
      return false;
    }
    return true;
  }

  /** Invoice type should be Sale or Purchase; anything else is a warning. Template: type. */
  void saleOrPurchase(InvoiceRuleContext ctx) {
    String type = ctx.invoice().getInvoiceType();
    if (!ConstantsCode.INVTYPE_SALE.equals(type) && !ConstantsCode.INVTYPE_PURCHASE.equals(type)) {
      ctx.warning("invoice.type.not.saleorpurchase.warning", new Object[] {type});
    }
  }

  /**
   * A Seller submission cannot be type Purchase; a Buyer submission cannot
   * be type Sale. Template: submitted-by label, type — rendered with the same
   * {@code ConstantsCode} strings the manual path passes, so both channels read
   * identically.
   */
  void submitterVsType(InvoiceRuleContext ctx) {
    String type = ctx.invoice().getInvoiceType();
    SubmitterInfo.SubmittedBy submittedBy = ctx.submitter().submittedBy();
    boolean invalid = (submittedBy == SubmitterInfo.SubmittedBy.SELLER
            && ConstantsCode.INVTYPE_PURCHASE.equals(type))
        || (submittedBy == SubmitterInfo.SubmittedBy.BUYER && ConstantsCode.INVTYPE_SALE.equals(type));
    if (invalid) {
      String submittedByLabel = submittedBy == SubmitterInfo.SubmittedBy.SELLER
          ? ConstantsCode.INVOICE_SUBMITTEDBY_SELLER
          : ConstantsCode.INVOICE_SUBMITTEDBY_BUYER;
      ctx.error("invoice.type.invalid.submitter", new Object[] {submittedByLabel, type});
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
