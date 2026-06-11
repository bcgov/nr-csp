package ca.bc.gov.nrs.csp.backend.controller.dto.health;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Health check response")
public record HealthResponse(
        @Schema(description = "Application status", example = "UP")
        String status,
        @Schema(description = "Server timestamp at time of request")
        Instant timestamp
) {}
