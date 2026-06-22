package ca.bc.gov.nrs.csp.backend.controller.dto.inbox;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record InboxRowResponse(
        @Schema(description = "Electronic submission ID. Null/blank for Manual submissions.")
        String submissionId,

        @Schema(description = "Submission entry date (yyyy-MM-dd).")
        LocalDate submissionDate,

        @Schema(description = "Submission status description (not code) from CSP_SUBMISSION_STATUS_CODE.")
        String submissionStatus,

        @Schema(description = "Submission type: Electronic or Manual.")
        String submissionType,

        @Schema(description = "Total invoices from sub.number_invoices_submitted (stored column).")
        Integer invTotal,

        @Schema(description = "Count of invoices with status APP (Approved).")
        Integer invApproved,

        @Schema(description = "Count of invoices with status REJ (Rejected).")
        Integer invRejected,

        @Schema(description = "Count of invoices with status PRO (Processing).")
        Integer invProcessing,

        @Schema(description = "Count of invoices with status CAN (Cancelled).")
        Integer invCancelled
) {}
