package ca.bc.gov.nrs.csp.backend.controller.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "A single invoice search result row")
public record SearchResultResponse(
        @Schema(description = "Coastal log sale ID — true PK of the result row", example = "200456")
        Long coastalLogSaleId,
        @Schema(description = "CSP submission ID (navigation key, not displayed)", example = "100123")
        Long cspSubmissionId,
        @Schema(description = "Invoice status code", example = "APP")
        String invoiceStatus,
        @Schema(description = "Invoice number", example = "WFP521046")
        String invoiceNumber,
        @Schema(description = "Invoice date", example = "2024-01-31")
        LocalDate invoiceDate,
        @Schema(description = "Invoice type code", example = "SAL")
        String type,
        @Schema(description = "Submitter client number", example = "01496328")
        String clientNumber,
        @Schema(description = "Submitter client name (joined from forest_client)", example = "ACME LOGGING LTD")
        String clientName,
        @Schema(description = "Log sale maturity code", example = "O")
        String maturity,
        @Schema(description = "Submission type: 'ESF' if electronically submitted, 'Manual' otherwise", example = "ESF")
        String submissionType
) {}
