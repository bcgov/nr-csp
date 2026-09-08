package ca.bc.gov.nrs.csp.backend.invoice.submission.business.support;

import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Trims and upper-cases the identifiers and coded values on a submission so that
 * downstream reference-data lookups compare against the canonical stored form.
 * Run once on the whole tree as it is parsed ({@code SubmissionValidationService}),
 * and again per invoice by {@code BusinessValidationService} and
 * {@code CspSubmissionPersistenceService} — it is idempotent, and those two also
 * serve callers that did not come through the parse step.
 *
 * <p>Normalises the invoice-level identifiers carried on {@link CSPInvoiceType},
 * the detail-level source-document identifiers (boom numbers, timber marks,
 * weigh slips) — the latter so the boom-number duplicate lookup matches the
 * upper-cased stored references — and the coded fields, whose values have to
 * match a reference code exactly.
 */
@Component
public class IdentifierNormalizer {

  /** Trim + upper-case a single token; null-safe. */
  public String normalize(String raw) {
    return raw == null ? null : raw.trim().toUpperCase(Locale.ENGLISH);
  }

  /**
   * Normalise every invoice in a parsed submission, in place. Takes the
   * {@code Object}-typed tree the structural phase hands back and ignores
   * anything that is not a CSP submission (including null), so callers do not
   * have to cast.
   */
  public void normalizeSubmission(Object parsedSubmission) {
    if (!(parsedSubmission instanceof CSPSubmissionType submission)) {
      return;
    }
    for (CSPInvoiceType invoice : submission.getCSPInvoice()) {
      normalizeInvoice(invoice);
    }
  }

  /** Normalise both the identifiers and the coded values on one invoice, in place. */
  public void normalizeInvoice(CSPInvoiceType invoice) {
    normalizeInvoiceIdentifiers(invoice);
    normalizeInvoiceCodes(invoice);
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

  /**
   * Upper-cases an invoice's coded fields — invoice type, maturity, primary sort
   * code and, on each line, secondary sort code, species and grade. Each is
   * matched against a reference table exactly, so a file that spells a valid
   * code in lower case ("sal", "fi") is accepted instead of being reported as an
   * unknown code.
   *
   * <p>FOB location is normalised for a consistent stored value only: the schema
   * states the client may enter any value there and it is checked against no
   * table.
   */
  public void normalizeInvoiceCodes(CSPInvoiceType invoice) {
    invoice.setInvoiceType(normalize(invoice.getInvoiceType()));

    CSPInvoiceDetailsType details = invoice.getCSPInvoiceDetails();
    if (details != null) {
      details.setMaturity(normalize(details.getMaturity()));
      details.setPrimarySortCode(normalize(details.getPrimarySortCode()));
      details.setLocationFOB(normalize(details.getLocationFOB()));
    }

    for (CSPLineItemType line : invoice.getCSPLineItem()) {
      line.setSecondarySortCode(normalize(line.getSecondarySortCode()));
      line.setSpecies(normalize(line.getSpecies()));
      line.setGrade(normalize(line.getGrade()));
    }
  }
}
