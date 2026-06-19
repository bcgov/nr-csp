package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.CopyFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.CreateFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.FlatPriceConversionDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.FlatPriceConversionResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.UpdateFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ConflictException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.UnprocessableEntityException;
import ca.bc.gov.nrs.csp.backend.repository.FlatPriceConversionRepository;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.model.FlatPriceConversion;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FlatPriceConversionService {

    private static final Logger log = LoggerFactory.getLogger(FlatPriceConversionService.class);

    private final FlatPriceConversionRepository repository;
    private final CommonValidation commonValidation;

    public FlatPriceConversionService(FlatPriceConversionRepository repository,
                                      CommonValidation commonValidation) {
        this.repository = repository;
        this.commonValidation = commonValidation;
    }

    /**
     * Search for flat price conversion records matching the given filters.
     * modellingCode is required; all other parameters are optional filters.
     */
    @Transactional(readOnly = true)
    public List<FlatPriceConversionResponse> search(String modellingCode,
                                                    String maturity,
                                                    String sortCode,
                                                    String species,
                                                    String grade) {
        log.debug("Searching flat price conversions for modellingCode={}", modellingCode);

        if (modellingCode == null || modellingCode.isBlank()) {
            throw new BadRequestException("Modelling code is required.");
        }

        List<FlatPriceConversionResponse> results = repository.search(modellingCode, maturity, sortCode, species, grade)
                .stream()
                .map(this::toResponse)
                .toList();

        log.debug("Search returned {} result(s)", results.size());
        return results;
    }

    /**
     * Create a new flat price conversion record.
     */
    public FlatPriceConversionResponse create(CreateFlatPriceConversionRequest request) {
        log.debug("Creating flat price conversion for modellingCode={}", request.modellingCode());

        String modellingCode = request.modellingCode();
        FlatPriceConversionDetails details = request.details();

        // Validate date range
        if (!commonValidation.isValidDateRange(details.effectiveDate(), details.expiryDate())) {
            throw new BadRequestException("Effective date must not be after expiry date.");
        }

        // Validate species/grade combination
        if (!commonValidation.isValidSpeciesGradeCombination(details.species(), details.grade())) {
            throw new UnprocessableEntityException("Invalid species/grade combination.");
        }

        // Resolve speciesGradeXrefId
        Long speciesGradeXrefId = commonValidation.findSpeciesGradeXrefId(details.species(), details.grade(), details.effectiveDate())
                .orElseThrow(() -> new UnprocessableEntityException("Species/grade combination not found."));

        // Validate sort code active on effective date
        if (!commonValidation.isValidSortCode(details.sortCode(), details.effectiveDate())) {
            throw new UnprocessableEntityException("Sort code is not active on the effective date.");
        }

        // Validate maturity active on effective date
        if (!commonValidation.isValidMaturity(details.maturity(), details.effectiveDate())) {
            throw new UnprocessableEntityException("Maturity code is not active on the effective date.");
        }

        // Check for duplicate
        if (commonValidation.isDuplicateFlatPriceConversion(
                modellingCode,
                details.sortCode(),
                speciesGradeXrefId,
                details.maturity(),
                details.effectiveDate(),
                null)) {
            throw new ConflictException(
                    "A flat price conversion already exists for modellingCode=" + modellingCode
                    + ", sortCode=" + details.sortCode()
                    + ", species=" + details.species()
                    + ", grade=" + details.grade()
                    + ", maturity=" + details.maturity()
                    + ", effectiveDate=" + details.effectiveDate()
            );
        }

        FlatPriceConversion record = new FlatPriceConversion(
                null,
                modellingCode,
                details.maturity(),
                details.species(),
                details.grade(),
                details.sortCode(),
                details.flatPriceConversion(),
                details.effectiveDate(),
                details.expiryDate(),
                0,
                null,
                null,
                null,
                null
        );

        log.debug("Inserting flat price conversion for modellingCode={}", modellingCode);
        repository.insert(record, speciesGradeXrefId, SecurityContextUtils.requireUsername());

        // The duplicate check above guarantees uniqueness on (modellingCode, sortCode, xrefId, maturity, effectiveDate).
        // The search + effectiveDate filter will find exactly the row just inserted.
        FlatPriceConversionResponse response = repository.search(modellingCode, details.maturity(), details.sortCode(), details.species(), details.grade())
                .stream()
                .filter(r -> r.effectiveDate().equals(details.effectiveDate()))
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Inserted record could not be retrieved."));

        log.debug("Flat price conversion created successfully with id={}", response.id());
        return response;
    }

    /**
     * Update an existing flat price conversion record.
     */
    public FlatPriceConversionResponse update(Long id, UpdateFlatPriceConversionRequest request) {
        log.debug("Updating flat price conversion id={}", id);

        if (id == null || id <= 0) {
            throw new BadRequestException("Id is required.");
        }

        FlatPriceConversion existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found: " + id));

        if (!request.revisionCount().equals(existing.revisionCount())) {
            throw new ConflictException("Record has been modified by another user. Please reload and try again.");
        }

        FlatPriceConversionDetails details = request.details();

        // Validate date range
        if (!commonValidation.isValidDateRange(details.effectiveDate(), details.expiryDate())) {
            throw new BadRequestException("Effective date must not be after expiry date.");
        }

        // Validate species/grade combination
        if (!commonValidation.isValidSpeciesGradeCombination(details.species(), details.grade())) {
            throw new UnprocessableEntityException("Invalid species/grade combination.");
        }

        // Resolve speciesGradeXrefId
        Long speciesGradeXrefId = commonValidation.findSpeciesGradeXrefId(details.species(), details.grade(), details.effectiveDate())
                .orElseThrow(() -> new UnprocessableEntityException("Species/grade combination not found."));

        // Validate sort code active on effective date
        if (!commonValidation.isValidSortCode(details.sortCode(), details.effectiveDate())) {
            throw new UnprocessableEntityException("Sort code is not active on the effective date.");
        }

        // Validate maturity active on effective date
        if (!commonValidation.isValidMaturity(details.maturity(), details.effectiveDate())) {
            throw new UnprocessableEntityException("Maturity code is not active on the effective date.");
        }

        // Check for duplicate (excluding current record)
        if (commonValidation.isDuplicateFlatPriceConversion(
                existing.modellingCode(),
                details.sortCode(),
                speciesGradeXrefId,
                details.maturity(),
                details.effectiveDate(),
                id)) {
            throw new ConflictException(
                    "A flat price conversion already exists for modellingCode=" + existing.modellingCode()
                    + ", sortCode=" + details.sortCode()
                    + ", species=" + details.species()
                    + ", grade=" + details.grade()
                    + ", maturity=" + details.maturity()
                    + ", effectiveDate=" + details.effectiveDate()
            );
        }

        FlatPriceConversion record = new FlatPriceConversion(
                id,
                existing.modellingCode(),
                details.maturity(),
                details.species(),
                details.grade(),
                details.sortCode(),
                details.flatPriceConversion(),
                details.effectiveDate(),
                details.expiryDate(),
                request.revisionCount(),
                existing.entryUserid(),
                existing.entryTimestamp(),
                null,
                null
        );

        repository.update(id, record, speciesGradeXrefId, SecurityContextUtils.requireUsername());

        FlatPriceConversionResponse response = repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Updated record could not be retrieved: " + id));

        log.debug("Flat price conversion updated successfully id={}", id);
        return response;
    }

    /**
     * Soft-delete (audit) then physically remove a flat price conversion record.
     */
    public void delete(Long id) {
        log.debug("Deleting flat price conversion id={}", id);

        if (id == null || id <= 0) {
            throw new BadRequestException("Id is required.");
        }

        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found: " + id));

        repository.auditDelete(id, SecurityContextUtils.requireUsername());
        repository.deleteById(id);

        log.debug("Flat price conversion deleted successfully id={}", id);
    }

    /**
     * Copy all flat price conversion records from one modelling code to another.
     */
    @Transactional
    public void copy(CopyFlatPriceConversionRequest request) {
        String source = request.sourceModellingCode();
        String target = request.targetModellingCode();

        log.debug("Copying flat price conversions from {} to {}", source, target);

        if (source == null || source.isBlank()) {
            throw new BadRequestException("Source modelling code is required.");
        }
        if (target == null || target.isBlank()) {
            throw new BadRequestException("Target modelling code is required.");
        }
        if (source.equals(target)) {
            throw new BadRequestException("Source and target modelling codes must differ.");
        }

        if (!repository.existsByModellingCode(source)) {
            throw new ResourceNotFoundException("No flat price conversions found for source modelling code: " + source);
        }
        if (repository.existsByModellingCode(target)) {
            throw new ConflictException("Target modelling code already has flat price conversions: " + target);
        }

        repository.copy(source, target, SecurityContextUtils.requireUsername());

        log.debug("Flat price conversions copied successfully from {} to {}", source, target);
    }

    /**
     * Remove all flat price conversion records for a given modelling code.
     */
    public void clearAll(String modellingCode) {
        log.debug("Clearing all flat price conversions for modellingCode={}", modellingCode);

        if (modellingCode == null || modellingCode.isBlank()) {
            throw new BadRequestException("Modelling code is required.");
        }

        repository.clearAll(modellingCode);

        log.debug("All flat price conversions cleared successfully for modellingCode={}", modellingCode);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private FlatPriceConversionResponse toResponse(FlatPriceConversion domain) {
        return new FlatPriceConversionResponse(
                domain.id(),
                domain.modellingCode(),
                domain.maturity(),
                domain.species(),
                domain.grade(),
                domain.sortCode(),
                domain.flatPriceConversion(),
                domain.effectiveDate(),
                domain.expiryDate(),
                domain.revisionCount(),
                domain.entryUserid(),
                domain.entryTimestamp(),
                domain.updateUserid(),
                domain.updateTimestamp()
        );
    }
}
