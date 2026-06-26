package ca.bc.gov.nrs.csp.backend.controller.dto.submission;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationMessageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response for the submission structural-validation endpoint. The
 * per-error entries reuse the app's {@link ValidationMessageResponse}
 * shape (keeping the response contract consistent across the API),
 * while preserving the underlying parser's error formatting: the
 * machine code lands in {@code messageKey}, the source location (if any)
 * in {@code args}, and the parser message in {@code message}.
 */
@Schema(description = "Result of validating an uploaded submission against format, ESF envelope, and XSD schema.")
public record SubmissionValidationResponse(
        @Schema(description = "True when the submission passed all structural validation", example = "false")
        boolean valid,
        @Schema(description = "Summary code: OK on success, VALIDATION_ERROR on failure", example = "VALIDATION_ERROR")
        String code,
        @Schema(description = "Human-readable summary", example = "Submission failed schema validation")
        String message,
        @Schema(description = "Every structural error that fired (empty on success)")
        List<ValidationMessageResponse> errors
) {}
