package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.config.CacheConfig;
import ca.bc.gov.nrs.csp.backend.repository.LookupRepository;
import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LookupService {

    private static final Logger log = LoggerFactory.getLogger(LookupService.class);

    private final LookupRepository lookupRepository;

    public LookupService(LookupRepository lookupRepository) {
        this.lookupRepository = lookupRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.MATURITY_CODES)
    public List<LookupItem> getMaturityCodes() {
        log.debug("Fetching maturity codes");
        List<LookupItem> results = lookupRepository.findMaturityCodes();
        log.debug("Maturity codes returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.INVOICE_TYPES)
    public List<LookupItem> getInvoiceTypes() {
        log.debug("Fetching invoice type codes");
        List<LookupItem> results = lookupRepository.findInvoiceTypes();
        log.debug("Invoice type codes returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.INVOICE_STATUSES)
    public List<LookupItem> getInvoiceStatuses() {
        log.debug("Fetching invoice status codes");
        List<LookupItem> results = lookupRepository.findInvoiceStatuses();
        log.debug("Invoice status codes returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.SUBMISSION_STATUSES)
    public List<LookupItem> getSubmissionStatuses() {
        log.debug("Fetching submission status codes");
        List<LookupItem> results = lookupRepository.findSubmissionStatuses();
        log.debug("Submission status codes returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.SORT_CODES)
    public List<LookupItem> getSortCodes() {
        log.debug("Fetching sort codes");
        List<LookupItem> results = lookupRepository.findSortCodes();
        log.debug("Sort codes returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.SPECIES_CODES)
    public List<LookupItem> getSpeciesCodes() {
        log.debug("Fetching species codes");
        List<LookupItem> results = lookupRepository.findSpeciesCodes();
        log.debug("Species codes returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.GRADE_CODES)
    public List<LookupItem> getGradeCodes() {
        log.debug("Fetching grade codes");
        List<LookupItem> results = lookupRepository.findGradeCodes();
        log.debug("Grade codes returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.GRADES_BY_SPECIES, key = "#species")
    public List<LookupItem> getGradesBySpecies(String species) {
        log.debug("Fetching grades for species '{}'", species);
        List<LookupItem> results = lookupRepository.findGradesBySpecies(species);
        log.debug("Grades for species '{}' returned {} result(s)", species, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.MODELLING_CODES)
    public List<LookupItem> getModellingCodes() {
        log.debug("Fetching modelling codes");
        List<LookupItem> results = lookupRepository.findModellingCodes();
        log.debug("Modelling codes returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.FOB_CODES)
    public List<LookupItem> getFobCodes() {
        log.debug("Fetching FOB location codes");
        List<LookupItem> results = lookupRepository.findFobCodes();
        log.debug("FOB location codes returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.SPECIES_GRADE_COMBINATIONS)
    public List<LookupRepository.SpeciesGradeCombo> getSpeciesGradeCombinations() {
        log.debug("Fetching species/grade combinations");
        List<LookupRepository.SpeciesGradeCombo> results = lookupRepository.findSpeciesGradeCombinations();
        log.debug("Species/grade combinations returned {} pair(s)", results.size());
        return results;
    }
}
