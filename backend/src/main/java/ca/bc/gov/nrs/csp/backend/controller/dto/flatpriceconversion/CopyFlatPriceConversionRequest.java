package ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CopyFlatPriceConversionRequest(
        @Schema(description = "Modelling code to copy from", example = "M1")
        @NotBlank(message = "Source modelling code is required.")
        String sourceModellingCode,

        @Schema(description = "Modelling code to copy into", example = "M2")
        @NotBlank(message = "Target modelling code is required.")
        String targetModellingCode
) {}
