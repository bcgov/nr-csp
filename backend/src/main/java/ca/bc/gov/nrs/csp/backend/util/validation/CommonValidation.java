package ca.bc.gov.nrs.csp.backend.util.validation;

import ca.bc.gov.nrs.csp.backend.repository.ValidationLookupRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class CommonValidation {

    private final ValidationLookupRepository lookupRepo;

    public CommonValidation(ValidationLookupRepository lookupRepo) {
        this.lookupRepo = lookupRepo;
    }

    public boolean isValidSortCode(String sortCode, LocalDate usedDate) {
        return lookupRepo.existsActiveSortCode(sortCode, usedDate);
    }

    public boolean isValidMaturity(String maturity, LocalDate usedDate) {
        return lookupRepo.existsActiveMaturityCode(maturity, usedDate);
    }

    public boolean isValidSpeciesGradeCombination(String species, String grade) {
        return lookupRepo.existsSpeciesGradeCombination(species, grade);
    }

    public boolean isValidInvoiceType(String invType, LocalDate usedDate) {
        return lookupRepo.existsActiveInvoiceTypeCode(invType, usedDate);
    }

    public boolean isValidClientLocation(String clientNumber, String clientLocnCode) {
        return lookupRepo.existsClientLocation(clientNumber, clientLocnCode);
    }

    public Optional<Long> findSpeciesGradeXrefId(String species, String grade, LocalDate onDate) {
        return lookupRepo.findSpeciesGradeXrefId(species, grade, onDate);
    }

    public boolean isDuplicateFlatPriceConversion(
            String modellingCode, String sortCode, Long speciesGradeXrefId,
            String maturity, LocalDate effectiveDate, Long excludeId) {
        return lookupRepo.existsDuplicateFlatPriceConversion(
                modellingCode, sortCode, speciesGradeXrefId, maturity, effectiveDate, excludeId);
    }

    public boolean isValidDateRange(LocalDate effectiveDate, LocalDate expiryDate) {
        return expiryDate == null || !effectiveDate.isAfter(expiryDate);
    }
}
