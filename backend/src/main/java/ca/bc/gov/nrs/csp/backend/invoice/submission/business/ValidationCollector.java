package ca.bc.gov.nrs.csp.backend.invoice.submission.business;

import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates the messages a run of rules produces. Each entry is tagged with
 * the <b>index</b> of the invoice it belongs to ({@code null} for
 * submission-level messages) so {@code BusinessValidationService} derives
 * accept/reject per invoice <i>identity</i> — never per the user-supplied
 * invoice number, which may be blank or duplicated across invoices.
 *
 * <p>Rules never touch this directly — they call {@code error(...)} /
 * {@code warning(...)} on their context, which stamps the invoice index,
 * locator, and severity.
 */
public class ValidationCollector {

  /** One collected message plus the index of the invoice it is scoped to (null = submission-level). */
  public record Entry(Integer invoiceIndex, SubmissionValidationError error) {}

  private final List<Entry> entries = new ArrayList<>();

  public void add(Integer invoiceIndex, SubmissionValidationError error) {
    entries.add(new Entry(invoiceIndex, error));
  }

  public List<Entry> entries() {
    return List.copyOf(entries);
  }
}
