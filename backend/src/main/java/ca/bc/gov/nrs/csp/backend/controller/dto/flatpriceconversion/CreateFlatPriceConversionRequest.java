package ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFlatPriceConversionRequest(
        @Schema(description = "Modelling code – either \"P\" (production) or \"M1\"/\"M2\"/\"M3\" (modelling scenarios)", example = "P")
        @NotBlank(message = "Modelling code is required.")
        String modellingCode,

        @Schema(description = "Flat price conversion field values")
        @NotNull(message = "Flat price conversion details are required.")
        @Valid
        FlatPriceConversionDetails details
) {}
