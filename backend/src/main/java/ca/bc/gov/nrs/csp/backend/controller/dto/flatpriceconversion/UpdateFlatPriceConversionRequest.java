package ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateFlatPriceConversionRequest(
        @Schema(description = "Revision count used for optimistic locking. Must match the current value on the record.", example = "1")
        @NotNull(message = "Revision count is required.")
        Integer revisionCount,

        @Schema(description = "Flat price conversion field values")
        @NotNull(message = "Flat price conversion details are required.")
        @Valid
        FlatPriceConversionDetails details
) {}
