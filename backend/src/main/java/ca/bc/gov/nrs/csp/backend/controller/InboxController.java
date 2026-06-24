package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.InboxApi;
import ca.bc.gov.nrs.csp.backend.controller.dto.inbox.InboxRowResponse;
import ca.bc.gov.nrs.csp.backend.service.InboxService;
import ca.bc.gov.nrs.csp.backend.service.mapper.InboxMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class InboxController implements InboxApi {

    private static final Logger log = LoggerFactory.getLogger(InboxController.class);

    private final InboxService inboxService;
    private final InboxMapper inboxMapper;

    public InboxController(InboxService inboxService, InboxMapper inboxMapper) {
        this.inboxService = inboxService;
        this.inboxMapper = inboxMapper;
    }

    @Override
    public ResponseEntity<Page<InboxRowResponse>> searchInbox(
            LocalDate submissionDateFrom,
            LocalDate submissionDateTo,
            String submittedBy,
            String submissionType,
            String submissionStatus,
            String invoiceNum,
            String submitterClientNum,
            String submitterLocNum,
            String keyword,
            Pageable pageable) {

        log.info("GET /api/inbox submissionDateFrom={} submissionDateTo={} submittedBy={} submissionType={} submissionStatus={} invoiceNum={} submitterClientNum={} keyword={} page={} size={} sort={}",
                submissionDateFrom, submissionDateTo, submittedBy, submissionType, submissionStatus,
                invoiceNum, submitterClientNum, keyword,
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return ResponseEntity.ok(
                inboxMapper.toResponsePage(
                        inboxService.search(
                                submissionDateFrom,
                                submissionDateTo,
                                submittedBy,
                                submissionType,
                                submissionStatus,
                                invoiceNum,
                                submitterClientNum,
                                submitterLocNum,
                                keyword,
                                pageable)));
    }
}
