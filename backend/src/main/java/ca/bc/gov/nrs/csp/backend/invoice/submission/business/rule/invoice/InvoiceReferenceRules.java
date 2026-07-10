package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.referencedata.InvoiceRef;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InvoiceReferenceRules implements InvoiceRule {

  /** For keys whose messages.properties template takes no placeholders. */
  private static final Object[] NO_ARGS = new Object[0];

  /** List separator the templates render — matches the manual path's join. */
  private static final String LIST_SEPARATOR = " , ";

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
      ctx.error("invoice.both.replace.adjust.invoicenum.error", NO_ARGS);
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
      // Template: the joined list of unknown invoice numbers.
      ctx.error("invoice.replace.invoicenumber.error",
          new Object[] {String.join(LIST_SEPARATOR, missing)});
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
        ctx.error("invoice.replace.with.itself.error", NO_ARGS);
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
    if (replaces.split(",").length > ConstantsCode.MAXOFCSVFORREPLACEINVOICENUM) {
      // Template: the maximum permitted count.
      ctx.error("invoice.morethanmax.replace.invoicenum.error",
          new Object[] {ConstantsCode.MAXOFCSVFORREPLACEINVOICENUM});
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
      ctx.error("invoice.adjust.invoicenumber.error",
          new Object[] {String.join(LIST_SEPARATOR, missing)});
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
        if (ConstantsCode.INVENTRYSTATUS_CANCELLED.equals(ref.statusCode())) {
          ctx.error("invoice.validation.adjustedInvoiceCancelled", NO_ARGS);
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
        ctx.error("invoice.adjust.with.itself.error", NO_ARGS);
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
    if (adjusts.split(",").length > ConstantsCode.MAXOFCSVFORADJUSTINVOICENUM) {
      ctx.error("invoice.morethanmax.adjust.invoicenum.error",
          new Object[] {ConstantsCode.MAXOFCSVFORADJUSTINVOICENUM});
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
