package ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * Full detail for a single submission, backing the View Submission page:
 * the submission header, a contact/metadata block, the list of invoices it
 * contains, and the flattened list of all their line items.
 */
public record SubmissionDetailResponse(
        @Schema(description = "CSP submission id.")
        Long cspSubmissionId,

        @Schema(description = "Electronic submission id. Null/blank for Manual submissions.")
        String submissionId,

        @Schema(description = "Submission entry date (yyyy-MM-dd).")
        LocalDate submissionDate,

        @Schema(description = "Submitter user id (csp_submission.entry_userid).")
        String submittedBy,

        @Schema(description = "Submission status description from CSP_SUBMISSION_STATUS_CODE.")
        String submissionStatus,

        @Schema(description = "Submitting client number.")
        String clientNumber,

        @Schema(description = "Submitting client name (from forest_client).")
        String clientName,

        @Schema(description = "Submitting client location code (client_locn_code).")
        String clientLocnCode,

        @Schema(description = "Contact email address. Not stored in the submission schema — always null at present.")
        String email,

        @Schema(description = "Contact telephone number. Not stored in the submission schema — always null at present.")
        String telephone,

        @Schema(description = "Month-complete indicator (Y/N) from csp_submission.month_complete_ind.")
        String monthComplete,

        @Schema(description = "Seller-submission indicator (Y/N): Y when the submitting client matches the seller on its invoices.")
        String sellerSubmission,

        @Schema(description = "Admin/reviewer comment (first non-null reviewer_notes across the submission's invoices). May be null.")
        String adminComment,

        @Schema(description = "Invoices belonging to this submission (Invoice Details table).")
        List<SubmissionInvoiceResponse> invoices,

        @Schema(description = "All line items across the submission's invoices (Invoice Line Items table).")
        List<SubmissionLineItemResponse> lineItems
) {}
