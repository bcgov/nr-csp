package ca.bc.gov.nrs.csp.backend.submission.business.rule;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.submission.business.support.Dates;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What an {@link InvoiceRule} receives: the invoice (and its parent submission),
 * the resolved {@link SubmitterInfo}, convenience accessors, reference-data
 * access, and {@code error(...)} / {@code warning(...)} sinks pre-scoped to this
 * invoice. Any ERROR recorded here rejects this invoice only — keyed on the
 * invoice's <b>index</b> (its identity within the submission), never its
 * user-supplied number, which can be blank or duplicated. Other invoices are
 * unaffected (partial acceptance).
 */
public final class InvoiceRuleContext {

  private final CSPSubmissionType submission;
  private final CSPInvoiceType invoice;
  private final int invoiceIndex;
  private final SubmitterInfo submitter;
  private final ReferenceDataService referenceData;
  private final ValidationCollector collector;
  private final String invoiceNumber;
  private final String locator;

  public InvoiceRuleContext(
      CSPSubmissionType submission,
      CSPInvoiceType invoice,
      int invoiceIndex,
      SubmitterInfo submitter,
      ReferenceDataService referenceData,
      ValidationCollector collector) {
    this.submission = submission;
    this.invoice = invoice;
    this.invoiceIndex = invoiceIndex;
    this.submitter = submitter;
    this.referenceData = referenceData;
    this.collector = collector;
    this.invoiceNumber = invoice.getInvoiceNumber();
    this.locator = Locators.invoice(invoiceIndex, invoiceNumber);
  }

  public CSPSubmissionType submission() {
    return submission;
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

  public String invoiceNumber() {
    return invoiceNumber;
  }

  /** Invoice type code (e.g. {@code SAL}, {@code PUR}, {@code ADJ}); null if absent. */
  public String invoiceType() {
    return invoice.getInvoiceType();
  }

  /** Submitted total amount from the invoice details (null if details/amount absent). */
  public BigDecimal totalAmount() {
    CSPInvoiceDetailsType details = invoice.getCSPInvoiceDetails();
    return details == null ? null : details.getTotalAmount();
  }

  /** Submitted total volume from the invoice details (null if details/volume absent). */
  public BigDecimal totalVolume() {
    CSPInvoiceDetailsType details = invoice.getCSPInvoiceDetails();
    return details == null ? null : details.getTotalVolume();
  }

  /** Submitted total pieces from the invoice details (null if details/pieces absent; the field is optional). */
  public Integer totalPieces() {
    CSPInvoiceDetailsType details = invoice.getCSPInvoiceDetails();
    return details == null ? null : details.getTotalPieces();
  }

  /** The invoice date as a {@link LocalDate} (null if absent). */
  public LocalDate invoiceDate() {
    return Dates.toLocalDate(invoice.getInvoiceDate());
  }

  /** Maturity code from the invoice details (null if details/maturity absent). */
  public String maturity() {
    CSPInvoiceDetailsType details = invoice.getCSPInvoiceDetails();
    return details == null ? null : details.getMaturity();
  }

  /** Primary sort code from the invoice details (null if details/code absent). */
  public String primarySortCode() {
    CSPInvoiceDetailsType details = invoice.getCSPInvoiceDetails();
    return details == null ? null : details.getPrimarySortCode();
  }

  /** Record a blocking error against this invoice. */
  public void error(String code, String message) {
    collector.add(invoiceIndex, SubmissionValidationError.error(locator, code, message));
  }

  /** Record a non-blocking warning against this invoice. */
  public void warning(String code, String message) {
    collector.add(invoiceIndex, SubmissionValidationError.warning(locator, code, message));
  }
}
