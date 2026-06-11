package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Body for changing an invoice's status (approve, reject, cancel, unapprove). Reviewer comments are required for reject/cancel/unapprove.")
public record ChangeStatusRequest(
        @NotBlank
        @Pattern(regexp = "^(APP|REJ|CAN|UNA)$")
        @Schema(description = "Target status code", example = "APP")
        String status,
        @Schema(description = "Reviewer comments (required when rejecting, cancelling, or unapproving)")
        String reviewComments
) {}
