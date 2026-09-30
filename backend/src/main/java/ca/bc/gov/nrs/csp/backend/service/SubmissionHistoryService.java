package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionDetailResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionHistoryRowResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionInvoiceCommentResponse;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.repository.SubmissionHistoryRepository;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for the Submission History screens. The list is otherwise
 * unfiltered (the UI shows every submission), so the service simply delegates
 * to {@link SubmissionHistoryRepository} beyond scoping every query to the
 * caller's client number(s) (see {@link SecurityContextUtils#currentClientNumbers()}
 * — empty for an unrestricted caller, i.e. every IDIR user today and any
 * BCeID user FAM hasn't scoped); the detail lookup throws 404 when the
 * submission doesn't exist or is out of the caller's scope.
 */
@Service
public class SubmissionHistoryService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionHistoryService.class);

    private final SubmissionHistoryRepository repository;

    public SubmissionHistoryService(SubmissionHistoryRepository repository) {
        this.repository = repository;
    }

    public Page<SubmissionHistoryRowResponse> search(Pageable pageable) {
        Page<SubmissionHistoryRowResponse> results =
                repository.search(pageable, SecurityContextUtils.currentClientNumbers());
        log.debug("Submission history list returned {} of {} result(s)",
                results.getNumberOfElements(), results.getTotalElements());
        return results;
    }

    /** Loads a single submission's detail, or throws 404 when it doesn't exist or is out of scope. */
    public SubmissionDetailResponse getById(Long cspSubmissionId) {
        log.debug("Submission history detail requested for id={}", cspSubmissionId);
        return repository.findDetail(cspSubmissionId, SecurityContextUtils.currentClientNumbers())
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + cspSubmissionId + " was not found."));
    }

    /** Per-invoice status + reviewer comments for a submission's expanded row. */
    public List<SubmissionInvoiceCommentResponse> getInvoiceComments(Long cspSubmissionId) {
        log.debug("Submission history invoice comments requested for id={}", cspSubmissionId);
        return repository.findInvoiceComments(cspSubmissionId, SecurityContextUtils.currentClientNumbers());
    }
}
