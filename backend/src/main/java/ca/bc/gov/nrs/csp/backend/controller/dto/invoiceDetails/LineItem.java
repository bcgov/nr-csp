package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

@Schema(description = "Invoice line item information")
public record LineItem(
        @Schema(description = "Line item id", example = "1")
        Long lineItemID,
        @Schema(description = "Invoice id", example = "12345")
        Long invoiceID,
        @Pattern(regexp = "^[A-Z0-9]+$")
        @Schema(description = "Second sort", example = "SORT01")
        String secondSort,
        @Schema(description = "Client's own secondary sort code", example = "SORT01")
        String clientSecondarySort,
        @Pattern(regexp = "^[A-Z0-9]+$")
        @Schema(description = "Species", example = "SPC1")
        String species,
        @Schema(description = "Species description (resolved from log_sale_species_code lookup)", example = "Cedar")
        String speciesDescription,
        @Pattern(regexp = "^[A-Z0-9]+$")
        @Schema(description = "Grade", example = "G1")
        String grade,
        @Schema(description = "Number of pieces", example = "50")
        Integer numOfPieces,
        @Schema(description = "Price", example = "25.0")
        BigDecimal price,
        @Schema(description = "Volume", example = "6.25")
        BigDecimal volume,
        @Schema(description = "Converted price", example = "null")
        BigDecimal convertedPrice,
        @Schema(description = "Amount", example = "156.25")
        BigDecimal amount
) {}
