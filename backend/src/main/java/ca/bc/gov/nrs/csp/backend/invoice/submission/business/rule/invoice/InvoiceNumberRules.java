package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Invoice-number rules. The number identifies the invoice in every message and
 * is what other invoices reference, so its shape is checked here rather than
 * left to whatever first trips over it.
 */
@Component
public class InvoiceNumberRules implements InvoiceRule {

  /** For keys whose messages.properties template takes no placeholders. */
  private static final Object[] NO_ARGS = new Object[0];

  /**
   * Uppercase letters, digits and hyphens only — the same constraint the manual
   * path puts on its request DTOs ({@code InvoiceDetails.invNumber}), so a number
   * one channel accepts the other accepts too.
   *
   * <p>Case is not a concern by the time this runs: identifiers are trimmed and
   * upper-cased as the submission is parsed, so a lower-case number in the file
   * is normalised rather than rejected, and surrounding whitespace (which
   * pretty-printed XML introduces on its own) is dropped before the check. The
   * schema caps the length at 15, so there is no length check here.
   */
  private static final Pattern INVOICE_NUMBER = Pattern.compile("^[A-Z0-9-]+$");

  @Override
  public void validate(InvoiceRuleContext ctx) {
    invoiceNumberValid(ctx);
  }

  /**
   * Invoice number is required and may carry only permitted characters. The two
   * failures are reported separately: an empty element (which the schema allows —
   * its type only caps the length) says the field is missing rather than blaming
   * contents that were never supplied. Neither message needs the value as an
   * argument, since the locator prefixed to every message already carries it.
   */
  void invoiceNumberValid(InvoiceRuleContext ctx) {
    String invoiceNumber = ctx.invoiceNumber();
    if (isBlank(invoiceNumber)) {
      ctx.error("invoice.number.required.error", NO_ARGS);
      return;
    }
    if (!INVOICE_NUMBER.matcher(invoiceNumber).matches()) {
      ctx.error("invoice.number.pattern.error", NO_ARGS);
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
