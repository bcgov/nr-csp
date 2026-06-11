package ca.bc.gov.nrs.csp.backend.controller.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard error response body returned by {@code GlobalApiExceptionHandler} for all API errors.
 *
 * <p>Convention for {@code code}: use SCREAMING_SNAKE_CASE (e.g., {@code RESOURCE_NOT_FOUND},
 * {@code VALIDATION_ERROR}, {@code DATABASE_ERROR}). Codes are free-form strings — callers
 * should treat them as opaque identifiers for client-side error handling.</p>
 */
@Schema(description = "Standard API error response")
public record ApiError(
        @Schema(description = "Error code identifying the error type", example = "RESOURCE_NOT_FOUND")
        String code,
        @Schema(description = "Human-readable description of the error", example = "Record with id 42 was not found")
        String message
) {}
