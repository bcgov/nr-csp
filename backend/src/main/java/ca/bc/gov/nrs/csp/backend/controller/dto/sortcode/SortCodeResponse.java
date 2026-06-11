package ca.bc.gov.nrs.csp.backend.controller.dto.sortcode;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "A production sort code record")
public record SortCodeResponse(
        @Schema(description = "Sort code (single uppercase character)", example = "A")
        String sortCode,

        @Schema(description = "Description", example = "Lumber - Cedar")
        String description,

        @Schema(description = "Effective date", example = "1990-01-01")
        LocalDate effectiveDate,

        @Schema(description = "Expiry date", example = "9999-12-31")
        LocalDate expiryDate,

        @Schema(description = "Last updated")
        LocalDate updateTimestamp
) {}
