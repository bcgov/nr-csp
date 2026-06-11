package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Invoice line item")
public record LineItemResponse(
        @Schema(description = "Line item id", example = "1")
        Long lineItemID,
        @Schema(description = "Parent invoice id", example = "12345")
        Long invoiceID,
        @Schema(description = "Secondary sort code", example = "SORT01")
        String secondSort,
        @Schema(description = "Client's own secondary sort code", example = "SORT01")
        String clientSecondarySort,
        @Schema(description = "Species code", example = "SP1")
        String species,
        @Schema(description = "Species description resolved from the log_sale_species_code lookup", example = "Cedar")
        String speciesDescription,
        @Schema(description = "Grade code", example = "G1")
        String grade,
        @Schema(description = "Number of pieces", example = "50")
        Integer numOfPieces,
        @Schema(description = "Unit price", example = "25.00")
        BigDecimal price,
        @Schema(description = "Volume", example = "6.25")
        BigDecimal volume,
        @Schema(description = "Converted price", example = "null")
        BigDecimal convertedPrice,
        @Schema(description = "Calculated amount (volume × price)", example = "156.25")
        BigDecimal amount
) {}
