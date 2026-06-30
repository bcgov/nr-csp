package ca.bc.gov.nrs.csp.backend.controller.dto.submission;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationMessageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response for the submission validation endpoints. Beyond the overall
 * {@code valid} flag and summary {@code code}, it surfaces the per-invoice
 * acceptance breakdown so a partial outcome (some invoices accepted, others
 * rejected) is never mistaken for a full accept: {@code acceptedInvoices}
 * proceed, while {@code rejectedInvoices} must be corrected and resubmitted
 * (their reasons are in {@code errors}). For structural validation the two
 * invoice lists are empty.
 */
@Schema(description = "Result of validating an uploaded submission (structural or business).")
public record SubmissionValidationResponse(
        @Schema(description = "True ONLY when the submission is fully accepted (no rejected invoices)", example = "false")
        boolean valid,
        @Schema(description = "Outcome code: OK (fully accepted), PARTIALLY_ACCEPTED, or VALIDATION_ERROR", example = "PARTIALLY_ACCEPTED")
        String code,
        @Schema(description = "Human-readable summary")
        String message,
        @Schema(description = "Invoice numbers that passed all rules and are accepted (empty for structural validation)")
        List<String> acceptedInvoices,
        @Schema(description = "Invoice numbers rejected for at least one ERROR — correct and resubmit these")
        List<String> rejectedInvoices,
        @Schema(description = "Every message that fired (ERROR and WARNING), each with locator and severity")
        List<ValidationMessageResponse> errors
) {}
