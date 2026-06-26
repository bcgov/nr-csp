package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionDetailResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionHistoryRowResponse;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.repository.SubmissionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Business logic for the Submission History screens. The list is unfiltered
 * (the UI shows every submission), so the service simply delegates to
 * {@link SubmissionHistoryRepository}; the detail lookup throws 404 when the
 * submission doesn't exist.
 */
@Service
public class SubmissionHistoryService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionHistoryService.class);

    private final SubmissionHistoryRepository repository;

    public SubmissionHistoryService(SubmissionHistoryRepository repository) {
        this.repository = repository;
    }

    public Page<SubmissionHistoryRowResponse> search(Pageable pageable) {
        Page<SubmissionHistoryRowResponse> results = repository.search(pageable);
        log.debug("Submission history list returned {} of {} result(s)",
                results.getNumberOfElements(), results.getTotalElements());
        return results;
    }

    /** Loads a single submission's detail, or throws 404 when it doesn't exist. */
    public SubmissionDetailResponse getById(Long cspSubmissionId) {
        log.debug("Submission history detail requested for id={}", cspSubmissionId);
        return repository.findDetail(cspSubmissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + cspSubmissionId + " was not found."));
    }
}
