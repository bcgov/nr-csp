package ca.bc.gov.nrs.csp.backend.submission.business.rule;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.support.Dates;
import ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;

import java.time.LocalDate;

/**
 * What an {@link InvoiceRule} receives: the invoice (and its parent submission),
 * the resolved {@link SubmitterInfo}, convenience accessors, reference-data
 * access, and {@code error(...)} / {@code warning(...)} sinks pre-scoped to this
 * invoice. Any ERROR recorded here rejects this invoice only (other invoices in
 * the submission are unaffected — partial acceptance).
 */
public final class InvoiceRuleContext {

  private final CSPSubmissionType submission;
  private final CSPInvoiceType invoice;
  private final SubmitterInfo submitter;
  private final ReferenceDataService referenceData;
  private final ValidationCollector collector;
  private final String invoiceNumber;
  private final String locator;

  public InvoiceRuleContext(
      CSPSubmissionType submission,
      CSPInvoiceType invoice,
      SubmitterInfo submitter,
      ReferenceDataService referenceData,
      ValidationCollector collector) {
    this.submission = submission;
    this.invoice = invoice;
    this.submitter = submitter;
    this.referenceData = referenceData;
    this.collector = collector;
    this.invoiceNumber = invoice.getInvoiceNumber();
    this.locator = "invoice " + invoiceNumber;
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

  /** The invoice date as a {@link LocalDate} (null if absent). */
  public LocalDate invoiceDate() {
    return Dates.toLocalDate(invoice.getInvoiceDate());
  }

  /** Record a blocking error against this invoice. */
  public void error(String code, String message) {
    collector.add(invoiceNumber, SubmissionValidationError.error(locator, code, message));
  }

  /** Record a non-blocking warning against this invoice. */
  public void warning(String code, String message) {
    collector.add(invoiceNumber, SubmissionValidationError.warning(locator, code, message));
  }
}
