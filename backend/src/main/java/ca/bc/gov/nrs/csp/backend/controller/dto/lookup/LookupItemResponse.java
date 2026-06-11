package ca.bc.gov.nrs.csp.backend.controller.dto.lookup;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single dropdown option with its code and display label")
public record LookupItemResponse(
        @Schema(description = "Code value used for filtering", example = "SAL")
        String code,
        @Schema(description = "Human-readable description", example = "Sales")
        String description
) {}
