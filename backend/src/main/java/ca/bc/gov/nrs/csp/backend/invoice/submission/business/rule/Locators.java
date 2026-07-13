package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule;

/**
 * Builds the human-readable locator stamped onto a message's {@code path}. Uses
 * the invoice's 1-based position so blank or duplicate invoice numbers stay
 * unambiguous in the message text (the number is included in parentheses only
 * when present): e.g. {@code "invoice #2 (INV-1)"}, or {@code "invoice #2"} when
 * the number is blank.
 */
final class Locators {

  private Locators() {}

  /** {@code "invoice #<position>"} plus {@code " (<number>)"} when the number is present. */
  static String invoice(int invoiceIndex, String invoiceNumber) {
    String label = "invoice #" + (invoiceIndex + 1);
    return (invoiceNumber == null || invoiceNumber.isBlank())
        ? label
        : label + " (" + invoiceNumber + ")";
  }

  /** The invoice locator plus {@code ", line <n>"}. */
  static String line(int invoiceIndex, String invoiceNumber, int lineNumber) {
    return invoice(invoiceIndex, invoiceNumber) + ", line " + lineNumber;
  }
}
