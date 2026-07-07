package ca.bc.gov.nrs.csp.backend.submission.business.referencedata;

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
 * The {@link ReferenceDataService} implementation: a thin router that delegates
 * each rule-shaped lookup to the app's existing repositories. The repositories
 * already own the SQL and the date-effective / month-complete / conversion logic,
 * so there is intentionally no per-domain service layer here — that would be an
 * empty pass-through. The only business logic that lives at this layer is the
 * single C3 "maturity M → O" substitution, inlined below.
 */
@Component
@RequiredArgsConstructor
public class DefaultReferenceDataService implements ReferenceDataService {

  private final ValidationLookupRepository validationLookups;
  private final InvoiceRepository invoices;
  private final FlatPriceConversionRepository flatPriceConversions;

  @Override
  public boolean clientLocationExists(String clientNumber, String locnCode) {
    return validationLookups.existsClientLocation(clientNumber, locnCode);
  }

  @Override
  public boolean invoiceTypeValidOn(String invoiceTypeCode, LocalDate date) {
    return validationLookups.existsActiveInvoiceTypeCode(invoiceTypeCode, date);
  }

  @Override
  public boolean maturityValidOn(String maturityCode, LocalDate date) {
    return validationLookups.existsActiveMaturityCode(maturityCode, date);
  }

  @Override
  public boolean sortCodeValidOn(String sortCode, LocalDate date) {
    return validationLookups.existsActiveSortCode(sortCode, date);
  }

  @Override
  public boolean speciesGradeCombinationExists(String species, String grade) {
    return validationLookups.existsSpeciesGradeCombination(species, grade);
  }

  @Override
  public Optional<Integer> conversionFactor(
      String maturity, String sortCode, String species, String grade, LocalDate date) {
    // C3: maturity 'M' (Mixed growth) uses the 'O' (Old growth) conversion factor.
    String effectiveMaturity = "M".equals(maturity) ? "O" : maturity;
    return flatPriceConversions.findApplicableFactor(effectiveMaturity, sortCode, species, grade, date);
  }

  @Override
  public boolean conversionFactorsExistForSortCode(String sortCode) {
    return flatPriceConversions.existsForSortCode(sortCode);
  }

  @Override
  public List<InvoiceRef> findInvoices(String invoiceNumber, String clientNumber, String locnCode) {
    return invoices.findByInvoiceNoAndClient(invoiceNumber, clientNumber, locnCode).stream()
        .map(related -> new InvoiceRef(invoiceNumber, related.invoiceStatusCode()))
        .toList();
  }

  @Override
  public boolean isMonthComplete(LocalDate invoiceDate, String clientNumber, String locnCode) {
    // ESF submissions are always new, so there is no current invoice to exclude.
    return invoices.isMonthCompleted(invoiceDate, clientNumber, locnCode, null);
  }

  @Override
  public boolean boomNumberUsedByAnotherInvoice(String boomNumber) {
    // ESF submissions are always new, so there is no current invoice to exclude.
    return invoices.countBoomNumberDuplicates(
        null, ConstantsCode.LOGSOURCECODE_BOOMNUMBER, boomNumber) > 0;
  }
}
