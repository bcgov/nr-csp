package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.LookupApi;
import ca.bc.gov.nrs.csp.backend.controller.dto.lookup.LookupItemResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.lookup.SpeciesGradeCombinationResponse;
import ca.bc.gov.nrs.csp.backend.service.LookupService;
import ca.bc.gov.nrs.csp.backend.service.mapper.LookupMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LookupController implements LookupApi {

    private static final Logger log = LoggerFactory.getLogger(LookupController.class);

    private final LookupService lookupService;
    private final LookupMapper lookupMapper;

    public LookupController(LookupService lookupService, LookupMapper lookupMapper) {
        this.lookupService = lookupService;
        this.lookupMapper = lookupMapper;
    }

    @Override
    public ResponseEntity<List<LookupItemResponse>> getMaturityCodes() {
        log.info("GET /api/lookup/maturity");
        return ResponseEntity.ok(lookupMapper.toResponseList(lookupService.getMaturityCodes()));
    }

    @Override
    public ResponseEntity<List<LookupItemResponse>> getInvoiceTypes() {
        log.info("GET /api/lookup/type");
        return ResponseEntity.ok(lookupMapper.toResponseList(lookupService.getInvoiceTypes()));
    }

    @Override
    public ResponseEntity<List<LookupItemResponse>> getInvoiceStatuses() {
        log.info("GET /api/lookup/status");
        return ResponseEntity.ok(lookupMapper.toResponseList(lookupService.getInvoiceStatuses()));
    }

    @Override
    public ResponseEntity<List<LookupItemResponse>> getSubmissionStatuses() {
        log.info("GET /api/lookup/submission-status");
        return ResponseEntity.ok(lookupMapper.toResponseList(lookupService.getSubmissionStatuses()));
    }

    @Override
    public ResponseEntity<List<LookupItemResponse>> getSortCodes() {
        log.info("GET /api/lookup/sort-code");
        return ResponseEntity.ok(lookupMapper.toResponseList(lookupService.getSortCodes()));
    }

    @Override
    public ResponseEntity<List<LookupItemResponse>> getSpeciesCodes() {
        log.info("GET /api/lookup/species");
        return ResponseEntity.ok(lookupMapper.toResponseList(lookupService.getSpeciesCodes()));
    }

    @Override
    public ResponseEntity<List<LookupItemResponse>> getGradeCodes() {
        log.info("GET /api/lookup/grade");
        return ResponseEntity.ok(lookupMapper.toResponseList(lookupService.getGradeCodes()));
    }

    @Override
    public ResponseEntity<List<LookupItemResponse>> getGradesBySpecies(String species) {
        log.info("GET /api/lookup/grade-by-species/{}", species);
        return ResponseEntity.ok(lookupMapper.toResponseList(lookupService.getGradesBySpecies(species)));
    }

    @Override
    public ResponseEntity<List<LookupItemResponse>> getModellingCodes() {
        log.info("GET /api/lookup/modelling-code");
        return ResponseEntity.ok(lookupMapper.toResponseList(lookupService.getModellingCodes()));
    }

    @Override
    public ResponseEntity<List<LookupItemResponse>> getFobCodes() {
        log.info("GET /api/lookup/fob");
        return ResponseEntity.ok(lookupMapper.toResponseList(lookupService.getFobCodes()));
    }

    @Override
    public ResponseEntity<List<SpeciesGradeCombinationResponse>> getSpeciesGradeCombinations() {
        log.info("GET /api/lookup/species-grade-combinations");
        List<SpeciesGradeCombinationResponse> body = lookupService.getSpeciesGradeCombinations().stream()
                .map(c -> new SpeciesGradeCombinationResponse(c.species(), c.grade()))
                .toList();
        return ResponseEntity.ok(body);
    }
}
