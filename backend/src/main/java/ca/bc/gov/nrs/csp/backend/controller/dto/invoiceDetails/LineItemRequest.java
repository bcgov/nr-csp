package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

@Schema(description = "Line item supplied on invoice create/update. Omit lineItemID for new lines; include it to update an existing line.")
public record LineItemRequest(
        @Schema(description = "Existing line item id (null for new lines)", example = "1")
        Long lineItemID,
        @Pattern(regexp = "^[A-Z0-9]+$")
        @Schema(description = "Secondary sort code", example = "SORT01")
        String secondSort,
        @Schema(description = "Client's own secondary sort code (free text)", example = "SORT01")
        String clientSecondarySort,
        @Pattern(regexp = "^[A-Z0-9]+$")
        @Schema(description = "Species code", example = "SP1")
        String species,
        @Pattern(regexp = "^[A-Z0-9]+$")
        @Schema(description = "Grade code", example = "G1")
        String grade,
        @Schema(description = "Number of pieces", example = "50")
        Integer numOfPieces,
        @Schema(description = "Unit price", example = "25.00")
        BigDecimal price,
        @Schema(description = "Volume", example = "6.25")
        BigDecimal volume,
        @Schema(description = "Converted price (optional — usually computed server-side)", example = "null")
        BigDecimal convertedPrice
) {}
