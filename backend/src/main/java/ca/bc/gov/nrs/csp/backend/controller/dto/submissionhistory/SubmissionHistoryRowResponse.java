package ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * One row of the Submission History list. Carries {@code cspSubmissionId} so the
 * UI can navigate (via the Actions "view" button) to the submission detail page.
 */
public record SubmissionHistoryRowResponse(
        @Schema(description = "CSP submission id — used to navigate to the submission detail page.")
        Long cspSubmissionId,

        @Schema(description = "Submission entry date (yyyy-MM-dd).")
        LocalDate submissionDate,

        @Schema(description = "Submitter — the user id that entered the submission (csp_submission.entry_userid).")
        String submittedBy,

        @Schema(description = "Submitting client number.")
        String clientNumber,

        @Schema(description = "Submitting client name (from forest_client).")
        String clientName,

        @Schema(description = "Submission status description (not code) from CSP_SUBMISSION_STATUS_CODE.")
        String submissionStatus,

        @Schema(description = "Admin/reviewer comment for the submission (first non-null reviewer_notes across its invoices). May be null.")
        String comment
) {}
