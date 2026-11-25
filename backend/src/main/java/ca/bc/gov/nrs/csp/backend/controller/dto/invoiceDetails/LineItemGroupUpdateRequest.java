package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@Schema(description = "Moves every line in a group to a new secondary sort and species, like legacy's group edit.")
public record LineItemGroupUpdateRequest(
        @NotEmpty
        @Schema(description = "Ids of the line items in the group", example = "[1, 2]")
        List<Long> lineItemIds,
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9]+$")
        @Schema(description = "New secondary sort code", example = "SORT01")
        String secondSort,
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9]+$")
        @Schema(description = "New species code", example = "SP1")
        String species
) {}
