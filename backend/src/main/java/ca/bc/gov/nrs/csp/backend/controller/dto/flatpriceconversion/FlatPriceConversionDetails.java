package ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FlatPriceConversionDetails(
        @Schema(description = "Maturity code (LOG_SALE_TYPE_CODE)", example = "S")
        @NotBlank(message = "Maturity is required.")
        String maturity,

        @Schema(description = "Species code (LOG_SALE_SPECIES_CODE)", example = "FD")
        @NotBlank(message = "Species is required.")
        String species,

        @Schema(description = "Grade code (LOG_SALE_GRADE_CODE)", example = "U")
        @NotBlank(message = "Grade is required.")
        String grade,

        @Schema(description = "Sort code (LOG_SALE_SORT_CODE)", example = "A")
        @NotBlank(message = "Sort code is required.")
        String sortCode,

        @Schema(description = "Flat price conversion value (1–999)", example = "100")
        @NotNull(message = "Flat price conversion is required.")
        @Min(value = 1, message = "Flat price conversion must be at least 1.")
        @Max(value = 999, message = "Flat price conversion must be at most 999.")
        Integer flatPriceConversion,

        @Schema(description = "Effective date", example = "1990-01-01")
        @NotNull(message = "Effective date is required.")
        LocalDate effectiveDate,

        @Schema(description = "Expiry date (nullable)", example = "9999-12-31", nullable = true)
        LocalDate expiryDate
) {}
