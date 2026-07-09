package ca.bc.gov.nrs.csp.backend.invoice.submission.business.support;

import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Trims and upper-cases the free-text identifiers on a submission so that
 * downstream reference-data lookups compare against the canonical stored form
 * (the legacy app stores these upper-cased — see rule-doc §0B). Run by
 * {@code BusinessValidationService} on each invoice before the rules execute.
 *
 * <p>Normalises the invoice-level identifiers carried on {@link CSPInvoiceType}
 * and the detail-level source-document identifiers (boom numbers, timber marks,
 * weigh slips) — the latter so the I38 boom-number duplicate lookup matches the
 * upper-cased stored references.
 */
@Component
public class IdentifierNormalizer {

  /** Trim + upper-case a single token; null-safe. */
  public String normalize(String raw) {
    return raw == null ? null : raw.trim().toUpperCase(Locale.ENGLISH);
  }

  /** Normalise the identifiers carried directly on an invoice, in place. */
  public void normalizeInvoiceIdentifiers(CSPInvoiceType invoice) {
    invoice.setInvoiceNumber(normalize(invoice.getInvoiceNumber()));
    invoice.setReplacesInvoiceNumbers(normalize(invoice.getReplacesInvoiceNumbers()));
    invoice.setAdjustsInvoiceNumbers(normalize(invoice.getAdjustsInvoiceNumbers()));

    CSPInvoiceDetailsType details = invoice.getCSPInvoiceDetails();
    if (details != null) {
      details.setBoomNumbers(normalize(details.getBoomNumbers()));
      details.setTimberMarks(normalize(details.getTimberMarks()));
      details.setWeighSlipNumbers(normalize(details.getWeighSlipNumbers()));
    }
  }
}
