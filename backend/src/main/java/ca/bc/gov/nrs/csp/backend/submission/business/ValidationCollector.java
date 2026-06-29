package ca.bc.gov.nrs.csp.backend.submission.business;

import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates the messages a run of rules produces. Each entry is tagged with
 * the invoice number it belongs to ({@code null} for submission-level messages)
 * so {@code BusinessValidationService} can derive per-invoice acceptance.
 *
 * <p>Rules never touch this directly — they call {@code error(...)} /
 * {@code warning(...)} on their context, which stamps the invoice number,
 * locator, and severity.
 */
public class ValidationCollector {

  /** One collected message plus the invoice it is scoped to (null = submission-level). */
  public record Entry(String invoiceNumber, SubmissionValidationError error) {}

  private final List<Entry> entries = new ArrayList<>();

  public void add(String invoiceNumber, SubmissionValidationError error) {
    entries.add(new Entry(invoiceNumber, error));
  }

  public List<Entry> entries() {
    return List.copyOf(entries);
  }
}
