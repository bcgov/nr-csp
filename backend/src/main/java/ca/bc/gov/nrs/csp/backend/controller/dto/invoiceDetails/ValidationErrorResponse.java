package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Error response body returned when an invoice operation fails business-rule validation. The errors list contains every validation error that fired.")
public record ValidationErrorResponse(
        @Schema(description = "Error code identifying the error type", example = "VALIDATION_ERROR")
        String code,
        @Schema(description = "Human-readable summary", example = "Invoice failed validation")
        String message,
        @Schema(description = "Structured list of validation errors")
        List<ValidationMessageResponse> errors
) {}
