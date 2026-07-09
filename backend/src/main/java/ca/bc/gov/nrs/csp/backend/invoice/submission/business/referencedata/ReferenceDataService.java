package ca.bc.gov.nrs.csp.backend.invoice.submission.business.referencedata;

import ca.bc.gov.nrs.csp.backend.repository.FlatPriceConversionRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository;
import ca.bc.gov.nrs.csp.backend.repository.ValidationLookupRepository;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * All reference-data (DB) lookups the business rules need — one concrete class,
 * each rule-shaped lookup delegates to the app's existing repositories
 *
 * <p>The only business logic living at this layer is the small ESF-shaped
 * adaptation some lookups need: the C3 "maturity M → O" substitution and the
 * "ESF submissions are always new" null-invoice exclusions.
 */
@Component
@RequiredArgsConstructor
public class ReferenceDataService {

  private final ValidationLookupRepository validationLookups;
  private final InvoiceRepository invoices;
  private final FlatPriceConversionRepository flatPriceConversions;

  /** Client number + location exists in CLIENT_LOCATION. */
  public boolean clientLocationExists(String clientNumber, String locnCode) {
    return validationLookups.existsClientLocation(clientNumber, locnCode);
  }

  /** Invoice type code recognised and active on the invoice date. */
  public boolean invoiceTypeValidOn(String invoiceTypeCode, LocalDate date) {
    return validationLookups.existsActiveInvoiceTypeCode(invoiceTypeCode, date);
  }

  /** Maturity code recognised and active on the invoice date. */
  public boolean maturityValidOn(String maturityCode, LocalDate date) {
    return validationLookups.existsActiveMaturityCode(maturityCode, date);
  }

  /** Sort code recognised and active on the invoice date. */
  public boolean sortCodeValidOn(String sortCode, LocalDate date) {
    return validationLookups.existsActiveSortCode(sortCode, date);
  }

  /** Species + grade combination exists in CSP_SPECIES_GRADE_XREF. */
  public boolean speciesGradeCombinationExists(String species, String grade) {
    return validationLookups.existsSpeciesGradeCombination(species, grade);
  }

  /** Flat-price conversion factor for the maturity/sort/species/grade on a date. */
  public Optional<Integer> conversionFactor(
      String maturity, String sortCode, String species, String grade, LocalDate date) {
    // Maturity 'M' (Mixed growth) uses the 'O' (Old growth) conversion factor.
    String effectiveMaturity = "M".equals(maturity) ? "O" : maturity;
    return flatPriceConversions.findApplicableFactor(effectiveMaturity, sortCode, species, grade, date);
  }

  /** Whether any conversion factor is defined for a sort code at all. */
  public boolean conversionFactorsExistForSortCode(String sortCode) {
    return flatPriceConversions.existsForSortCode(sortCode);
  }

  /** Invoices for this client matching a number (for replace/adjust + status checks). */
  public List<InvoiceRef> findInvoices(String invoiceNumber, String clientNumber, String locnCode) {
    return invoices.findByInvoiceNoAndClient(invoiceNumber, clientNumber, locnCode).stream()
        .map(related -> new InvoiceRef(invoiceNumber, related.invoiceStatusCode()))
        .toList();
  }

  /** Whether the invoice date falls in a month already flagged Complete for the client. */
  public boolean isMonthComplete(LocalDate invoiceDate, String clientNumber, String locnCode) {
    // ESF submissions are always new, so there is no current invoice to exclude.
    return invoices.isMonthCompleted(invoiceDate, clientNumber, locnCode, null);
  }

  /** Whether a boom number is already recorded as a source document on another invoice. */
  public boolean boomNumberUsedByAnotherInvoice(String boomNumber) {
    // ESF submissions are always new, so there is no current invoice to exclude.
    return invoices.countBoomNumberDuplicates(
        null, ConstantsCode.LOGSOURCECODE_BOOMNUMBER, boomNumber) > 0;
  }
}
