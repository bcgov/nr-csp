package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.Dates;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationError;

import java.time.LocalDate;

/**
 * What a {@link LineItemRule} receives: the line item, its parent invoice and
 * resolved {@link SubmitterInfo}, convenience accessors (parent invoice type and
 * date — several line rules are relaxed for type {@code ADJ}), reference-data
 * access, and {@code error(...)} / {@code warning(...)} sinks scoped to this line.
 * A line ERROR rejects the parent invoice — keyed on the parent invoice's
 * <b>index</b> (its identity), never its number.
 */
public final class LineItemRuleContext {

  private final CSPInvoiceType invoice;
  private final CSPLineItemType line;
  private final int invoiceIndex;
  private final int lineNumber;
  private final SubmitterInfo submitter;
  private final ReferenceDataService referenceData;
  private final ValidationCollector collector;
  private final String locator;

  public LineItemRuleContext(
      CSPInvoiceType invoice,
      CSPLineItemType line,
      int invoiceIndex,
      int lineNumber,
      SubmitterInfo submitter,
      ReferenceDataService referenceData,
      ValidationCollector collector) {
    this.invoice = invoice;
    this.line = line;
    this.invoiceIndex = invoiceIndex;
    this.lineNumber = lineNumber;
    this.submitter = submitter;
    this.referenceData = referenceData;
    this.collector = collector;
    this.locator = Locators.line(invoiceIndex, invoice.getInvoiceNumber(), lineNumber);
  }

  public CSPLineItemType line() {
    return line;
  }

  public CSPInvoiceType invoice() {
    return invoice;
  }

  public SubmitterInfo submitter() {
    return submitter;
  }

  public ReferenceDataService referenceData() {
    return referenceData;
  }

  /** 1-based position of this line within its invoice (for messages/locators). */
  public int lineNumber() {
    return lineNumber;
  }

  /** Parent invoice type code (e.g. {@code SAL} / {@code PUR} / {@code ADJ}). */
  public String invoiceType() {
    return invoice.getInvoiceType();
  }

  /** Parent invoice date as a {@link LocalDate} (null if absent). */
  public LocalDate invoiceDate() {
    return Dates.toLocalDate(invoice.getInvoiceDate());
  }

  /**
   * Record a blocking error against this line (rejects the parent invoice) as a
   * message key + its {@code messages.properties} template args; the text is
   * resolved at the HTTP boundary (refactor doc §3.5 — there is deliberately no
   * rendered-text sink).
   */
  public void error(String code, Object[] args) {
    collector.add(invoiceIndex, SubmissionValidationError.error(locator, code, args));
  }

  /** Warning counterpart of {@link #error(String, Object[])}. */
  public void warning(String code, Object[] args) {
    collector.add(invoiceIndex, SubmissionValidationError.warning(locator, code, args));
  }
}
