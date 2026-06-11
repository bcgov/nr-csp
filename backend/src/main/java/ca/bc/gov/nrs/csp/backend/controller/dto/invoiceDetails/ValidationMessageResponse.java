package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single validation error or warning")
public record ValidationMessageResponse(
        @Schema(description = "Message key (matches an entry in messages.properties)", example = "invoice.totalamount.dismatch.warning")
        String messageKey,
        @Schema(description = "Positional arguments substituted into the translated message template")
        Object[] args,
        @Schema(description = "Severity", example = "WARNING", allowableValues = {"ERROR", "WARNING"})
        String type,
        @Schema(description = "Resolved human-readable text with args interpolated, ready to render", example = "Submitted total amount 1234.50 does not match the calculated total.")
        String message
) {}
