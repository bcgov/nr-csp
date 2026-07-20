package ca.bc.gov.nrs.csp.backend.controller.dto.submission;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationMessageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response for the submission submit (persist) endpoint. On success it carries
 * the new {@code submissionId}; on a validation failure it mirrors the business
 * validation envelope (the submission is not saved unless every invoice is
 * accepted).
 */
@Schema(description = "Result of submitting (persisting) an uploaded submission.")
public record SubmissionSubmitResponse(
        @Schema(description = "True when the submission was saved", example = "true")
        boolean valid,
        @Schema(description = "Outcome code: OK, VALIDATION_ERROR, PARTIALLY_ACCEPTED, or an upload code", example = "OK")
        String code,
        @Schema(description = "Human-readable summary")
        String message,
        @Schema(description = "New csp_submission id when saved; null when not saved", example = "12345")
        Long submissionId,
        @Schema(description = "Invoice numbers accepted by business validation")
        List<String> acceptedInvoices,
        @Schema(description = "Invoice numbers rejected by business validation (blocks the save)")
        List<String> rejectedInvoices,
        @Schema(description = "Validation messages when the submission was not saved")
        List<ValidationMessageResponse> errors
) {}
