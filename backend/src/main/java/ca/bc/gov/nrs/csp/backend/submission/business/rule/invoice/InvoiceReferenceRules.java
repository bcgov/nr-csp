package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.referencedata.InvoiceRef;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InvoiceReferenceRules implements InvoiceRule {

  /** Max numbers permitted in a Replaces / Adjusts CSV. */
  private static final int MAX_REPLACE_ADJUST_INVOICE_NUMBERS = 5;

  /** Entry-status code for a cancelled/deleted invoice. */
  private static final String CANCELLED_STATUS = "CAN";

  @Override
  public void validate(InvoiceRuleContext ctx) {
    onlyOneOfReplaceOrAdjust(ctx);
    replaceInvoiceNumbersExist(ctx);
    invoiceDoesNotReplaceItself(ctx);
    replaceInvoiceNumbersWithinMax(ctx);
    adjustInvoiceNumbersExist(ctx);
    adjustedInvoiceNotCancelled(ctx);
    invoiceDoesNotAdjustItself(ctx);
    adjustInvoiceNumbersWithinMax(ctx);
  }

  /** Only one of Replaces / Adjusts may be populated (ERROR) */
  void onlyOneOfReplaceOrAdjust(InvoiceRuleContext ctx) {
    CSPInvoiceType invoice = ctx.invoice();
    boolean hasReplaces = !isBlank(invoice.getReplacesInvoiceNumbers());
    boolean hasAdjusts = !isBlank(invoice.getAdjustsInvoiceNumbers());

    if (hasReplaces && hasAdjusts) {
      ctx.error(
          "innoice.both.replace.adjust.invoicenum.error",
          invoiceMessage(ctx.invoiceNumber(),
              "cannot populate both replacesInvoiceNumbers and adjustsInvoiceNumbers."));
    }
  }

  /** Each Replaces number identifies another invoice for this client (ERROR) */
  void replaceInvoiceNumbersExist(InvoiceRuleContext ctx) {
    String replaces = ctx.invoice().getReplacesInvoiceNumbers();
    if (isBlank(replaces)) {
      return;
    }
    SubmitterInfo submitter = ctx.submitter();
    List<String> missing = new ArrayList<>();
    for (String token : replaces.split(",")) {
      String invoiceNumber = token.trim();
      if (invoiceNumber.isEmpty()) {
        continue;
      }
      boolean exists = !ctx.referenceData()
          .findInvoices(invoiceNumber, submitter.submitterClientNumber(), submitter.submitterLocnCode())
          .isEmpty();
      if (!exists) {
        missing.add(invoiceNumber);
      }
    }
    if (!missing.isEmpty()) {
      ctx.error(
          "invoice.replace.invoicenumber.error",
          "The following invoice numbers listed in the replacesInvoiceNumbers field do not identify"
              + " another invoice for this Client: " + String.join(", ", missing)
              + " (invoiceNumber " + ctx.invoiceNumber() + ").");
    }
  }

  /** An invoice cannot replace itself (ERROR) */
  void invoiceDoesNotReplaceItself(InvoiceRuleContext ctx) {
    String replaces = ctx.invoice().getReplacesInvoiceNumbers();
    if (isBlank(replaces)) {
      return;
    }
    String thisInvoiceNumber = ctx.invoiceNumber();
    for (String token : replaces.split(",")) {
      if (token.trim().equals(thisInvoiceNumber)) {
        ctx.error(
            "invoice.replace.with.itself.error",
            invoiceMessage(thisInvoiceNumber, "cannot be replaced by itself."));
        return;
      }
    }
  }

  /** At most 5 Replaces numbers (ERROR) */
  void replaceInvoiceNumbersWithinMax(InvoiceRuleContext ctx) {
    String replaces = ctx.invoice().getReplacesInvoiceNumbers();
    if (isBlank(replaces)) {
      return;
    }
    if (replaces.split(",").length > MAX_REPLACE_ADJUST_INVOICE_NUMBERS) {
      ctx.error(
          "invoice.morethanmax.replace.invoicenum.error",
          "The replacesInvoiceNumbers field for invoiceNumber " + ctx.invoiceNumber()
              + " must be up to " + MAX_REPLACE_ADJUST_INVOICE_NUMBERS + " numbers.");
    }
  }

  /** Each Adjusts number identifies another invoice for this client (ERROR) */
  void adjustInvoiceNumbersExist(InvoiceRuleContext ctx) {
    String adjusts = ctx.invoice().getAdjustsInvoiceNumbers();
    if (isBlank(adjusts)) {
      return;
    }
    SubmitterInfo submitter = ctx.submitter();
    List<String> missing = new ArrayList<>();
    for (String token : adjusts.split(",")) {
      String invoiceNumber = token.trim();
      if (invoiceNumber.isEmpty()) {
        continue;
      }
      boolean exists = !ctx.referenceData()
          .findInvoices(invoiceNumber, submitter.submitterClientNumber(), submitter.submitterLocnCode())
          .isEmpty();
      if (!exists) {
        missing.add(invoiceNumber);
      }
    }
    if (!missing.isEmpty()) {
      ctx.error(
          "invoice.adjust.invoicenumber.error",
          "The following invoice numbers listed in the adjustsInvoiceNumbers field do not identify"
              + " another invoice for this Client: " + String.join(", ", missing)
              + " (invoiceNumber " + ctx.invoiceNumber() + ").");
    }
  }

  /** An adjusted invoice cannot be cancelled/deleted (ERROR) */
  void adjustedInvoiceNotCancelled(InvoiceRuleContext ctx) {
    String adjusts = ctx.invoice().getAdjustsInvoiceNumbers();
    if (isBlank(adjusts)) {
      return;
    }
    SubmitterInfo submitter = ctx.submitter();
    for (String token : adjusts.split(",")) {
      String invoiceNumber = token.trim();
      if (invoiceNumber.isEmpty()) {
        continue;
      }
      for (InvoiceRef ref : ctx.referenceData()
          .findInvoices(invoiceNumber, submitter.submitterClientNumber(), submitter.submitterLocnCode())) {
        if (CANCELLED_STATUS.equals(ref.statusCode())) {
          ctx.error(
              "invoice.validation.adjustedInvoiceCancelled",
              invoiceMessage(ctx.invoiceNumber(),
                  "cannot adjust cancelled or deleted invoice " + invoiceNumber + "."));
          return;
        }
      }
    }
  }

  /** An invoice cannot adjust itself (ERROR) */
  void invoiceDoesNotAdjustItself(InvoiceRuleContext ctx) {
    String adjusts = ctx.invoice().getAdjustsInvoiceNumbers();
    if (isBlank(adjusts)) {
      return;
    }
    String thisInvoiceNumber = ctx.invoiceNumber();
    for (String token : adjusts.split(",")) {
      if (token.trim().equals(thisInvoiceNumber)) {
        ctx.error(
            "invoice.adjust.with.itself.error",
            invoiceMessage(thisInvoiceNumber, "cannot be adjusted by itself."));
        return;
      }
    }
  }

  /** At most 5 Adjusts numbers (ERROR) */
  void adjustInvoiceNumbersWithinMax(InvoiceRuleContext ctx) {
    String adjusts = ctx.invoice().getAdjustsInvoiceNumbers();
    if (isBlank(adjusts)) {
      return;
    }
    if (adjusts.split(",").length > MAX_REPLACE_ADJUST_INVOICE_NUMBERS) {
      ctx.error(
          "invoice.morethanmax.adjust.invoicenum.error",
          "The adjustsInvoiceNumbers field for invoiceNumber " + ctx.invoiceNumber()
              + " must be up to " + MAX_REPLACE_ADJUST_INVOICE_NUMBERS + " numbers.");
    }
  }

  /**
   * Builds an error message scoped to a specific invoice, e.g.
   * {@code "invoiceNumber INV-1 cannot be replaced by itself."}. {@code detail}
   * is the clause following the invoice number (no leading space).
   */
  private static String invoiceMessage(String invoiceNumber, String detail) {
    return "invoiceNumber " + invoiceNumber + " " + detail;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
