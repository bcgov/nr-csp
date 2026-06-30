package ca.bc.gov.nrs.csp.backend.submission.business.support;

import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Trims and upper-cases the free-text identifiers on a submission so that
 * downstream reference-data lookups compare against the canonical stored form
 * (the legacy app stores these upper-cased — see rule-doc §0B). Run by
 * {@code BusinessValidationService} on each invoice before the rules execute.
 *
 * <p>Currently normalises the invoice-level identifiers carried on
 * {@link CSPInvoiceType}. Detail-level identifiers (boom numbers, timber marks,
 * weigh slips, FOB) are normalised here too when those rules are implemented.
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
  }
}
