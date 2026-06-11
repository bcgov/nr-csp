package ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "A flat price conversion record")
public record FlatPriceConversionResponse(
        @Schema(description = "Record identifier", example = "42")
        Long id,

        @Schema(description = "Modelling code – either \"P\" (production) or \"M1\"/\"M2\"/\"M3\" (modelling scenarios)", example = "P")
        String modellingCode,

        @Schema(description = "Maturity code (LOG_SALE_TYPE_CODE)", example = "S")
        String maturity,

        @Schema(description = "Species code (LOG_SALE_SPECIES_CODE)", example = "FD")
        String species,

        @Schema(description = "Grade code (LOG_SALE_GRADE_CODE)", example = "U")
        String grade,

        @Schema(description = "Sort code (LOG_SALE_SORT_CODE)", example = "A")
        String sortCode,

        @Schema(description = "Flat price conversion value (1–999)", example = "100")
        Integer flatPriceConversion,

        @Schema(description = "Effective date", example = "1990-01-01")
        LocalDate effectiveDate,

        @Schema(description = "Expiry date (nullable)", example = "9999-12-31", nullable = true)
        LocalDate expiryDate,

        @Schema(description = "Revision count", example = "1")
        Integer revisionCount,

        @Schema(description = "User who created the record (nullable)", example = "JSMITH", nullable = true)
        String entryUserid,

        @Schema(description = "Date the record was created (nullable)", example = "2024-01-15", nullable = true)
        LocalDate entryTimestamp,

        @Schema(description = "User who last updated the record (nullable)", example = "JDOE", nullable = true)
        String updateUserid,

        @Schema(description = "Date the record was last updated (nullable)", example = "2024-06-01", nullable = true)
        LocalDate updateTimestamp
) {}
