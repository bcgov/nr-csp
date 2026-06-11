package ca.bc.gov.nrs.csp.backend.controller.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Client and location entry for autocomplete")
public record ClientLocationResponse(
        @Schema(description = "Client number", example = "00014963")
        String clientNumber,
        @Schema(description = "Client name", example = "ACME LOGGING LTD")
        String clientName,
        @Schema(description = "Client location code", example = "00")
        String clientLocnCode,
        @Schema(description = "Location name", example = "HEAD OFFICE")
        String clientLocnName,
        @Schema(description = "City", example = "VICTORIA")
        String city,
        @Schema(description = "Province", example = "BC")
        String province
) {}
