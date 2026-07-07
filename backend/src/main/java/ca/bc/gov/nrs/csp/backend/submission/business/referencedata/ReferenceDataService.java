package ca.bc.gov.nrs.csp.backend.submission.business.referencedata;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Facade over all reference-data (DB) lookups the business rules need. Rules
 * depend only on this interface, never on repositories directly — so a rule
 * stays a small, easily-unit-tested class (mock this in rule tests), and any
 * lookup is reusable by a rule at any level (submission / invoice / line).
 *
 * <p>Each method maps to a lookup called out in
 * {@code docs/submission-validation-business-rules.md}. The implementation
 * ({@link DefaultReferenceDataService}) is a thin router that delegates to the
 * app's existing repositories, which already encapsulate the SQL and the
 * date-effective ("active on the invoice date") semantics.
 */
public interface ReferenceDataService {

  /** S1 / I15 / I16 / I19 — client number + location exists in CLIENT_LOCATION. */
  boolean clientLocationExists(String clientNumber, String locnCode);

  /** I1 — invoice type code recognised and active on the invoice date. */
  boolean invoiceTypeValidOn(String invoiceTypeCode, LocalDate date);

  /** I22 — maturity code recognised and active on the invoice date. */
  boolean maturityValidOn(String maturityCode, LocalDate date);

  /** I23 / L1 — sort code recognised and active on the invoice date. */
  boolean sortCodeValidOn(String sortCode, LocalDate date);

  /** L2 — species + grade combination exists in CSP_SPECIES_GRADE_XREF. */
  boolean speciesGradeCombinationExists(String species, String grade);

  /** C3 — flat-price conversion factor for the maturity/sort/species/grade on a date (maturity 'M' treated as 'O' by the impl). */
  Optional<Integer> conversionFactor(
      String maturity, String sortCode, String species, String grade, LocalDate date);

  /** C5 — whether any conversion factor is defined for a sort code at all. */
  boolean conversionFactorsExistForSortCode(String sortCode);

  /** I5 / I8 / I9 — invoices for this client matching a number (for replace/adjust + status checks). */
  List<InvoiceRef> findInvoices(String invoiceNumber, String clientNumber, String locnCode);

  /** I40 — whether the invoice date falls in a month already flagged Complete for the client. */
  boolean isMonthComplete(LocalDate invoiceDate, String clientNumber, String locnCode);
}
