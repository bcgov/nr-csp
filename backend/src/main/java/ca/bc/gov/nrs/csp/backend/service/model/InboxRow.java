package ca.bc.gov.nrs.csp.backend.service.model;

import java.time.LocalDate;

public record InboxRow(
        Long cspSubmissionId,
        Long coastalLogSaleId,
        String submissionId,
        LocalDate submissionDate,
        String submissionStatus,
        String submissionType,
        Integer invTotal,
        Integer invApproved,
        Integer invRejected,
        Integer invProcessing,
        Integer invCancelled
) {}
