package ca.bc.gov.nrs.csp.backend.controller.dto.sortcode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateSortCodeRequest(
        @Schema(description = "Sort code (single uppercase character)", example = "A")
        @NotBlank(message = "Sort code is required.")
        @Size(max = 1, message = "Sort code must be at most 1 character.")
        String sortCode,

        @Schema(description = "Description", example = "Lumber - Cedar")
        @NotBlank(message = "Description is required.")
        @Size(max = 120, message = "Description must be at most 120 characters.")
        String description,

        @Schema(description = "Effective date", example = "1990-01-01")
        @NotNull(message = "Effective date is required.")
        LocalDate effectiveDate,

        @Schema(description = "Expiry date", example = "9999-12-31")
        @NotNull(message = "Expiry date is required.")
        LocalDate expiryDate
) {}
