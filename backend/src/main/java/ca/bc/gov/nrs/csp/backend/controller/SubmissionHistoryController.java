package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.SubmissionHistoryApi;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionDetailResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionHistoryRowResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionInvoiceCommentResponse;
import ca.bc.gov.nrs.csp.backend.service.SubmissionHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubmissionHistoryController implements SubmissionHistoryApi {

    private static final Logger log = LoggerFactory.getLogger(SubmissionHistoryController.class);

    private final SubmissionHistoryService submissionHistoryService;

    public SubmissionHistoryController(SubmissionHistoryService submissionHistoryService) {
        this.submissionHistoryService = submissionHistoryService;
    }

    @Override
    public ResponseEntity<Page<SubmissionHistoryRowResponse>> listSubmissionHistory(Pageable pageable) {
        log.info("GET /api/submission-history page={} size={} sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        return ResponseEntity.ok(submissionHistoryService.search(pageable));
    }

    @Override
    public ResponseEntity<SubmissionDetailResponse> getSubmissionDetail(Long id) {
        log.info("GET /api/submission-history/{}", id);
        return ResponseEntity.ok(submissionHistoryService.getById(id));
    }

    @Override
    public ResponseEntity<java.util.List<SubmissionInvoiceCommentResponse>> getSubmissionInvoiceComments(Long id) {
        log.info("GET /api/submission-history/{}/invoices", id);
        return ResponseEntity.ok(submissionHistoryService.getInvoiceComments(id));
    }
}
