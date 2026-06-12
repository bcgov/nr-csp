package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReferenceDataWarmupService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataWarmupService.class);

    private final LookupService lookupService;

    public ReferenceDataWarmupService(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Warming reference-data caches");

        lookupService.getMaturityCodes();
        lookupService.getInvoiceTypes();
        lookupService.getInvoiceStatuses();
        lookupService.getSubmissionStatuses();
        lookupService.getSortCodes();
        List<LookupItem> speciesCodes = lookupService.getSpeciesCodes();
        lookupService.getGradeCodes();
        lookupService.getModellingCodes();
        lookupService.getFobCodes();
        lookupService.getSpeciesGradeCombinations();

        // Preload per-species grade lists so the first filtered dropdown request is also warm.
        speciesCodes.stream()
                .map(LookupItem::code)
                .forEach(lookupService::getGradesBySpecies);

        log.info("Reference-data caches warmed for {} species code(s)", speciesCodes.size());
    }
}
