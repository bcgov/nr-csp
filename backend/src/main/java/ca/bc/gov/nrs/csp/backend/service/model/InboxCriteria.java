package ca.bc.gov.nrs.csp.backend.service.model;

import java.time.LocalDate;

public record InboxCriteria(
        LocalDate submissionDateFrom,
        LocalDate submissionDateTo,
        String submittedBy,
        String submissionType,
        String submissionStatus,
        String invoiceNum,
        String submitterClientNum,
        String submitterLocNum
) {}
