package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Body for creating a new invoice. Status is always set to DRAFT on create; client-supplied invStatus / invID / entryUserID are ignored.")
public record CreateInvoiceRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9-]+$")
        @Schema(description = "Client-supplied invoice number", example = "INV-2026-001")
        String invNumber,
        @NotNull
        @Schema(description = "Invoice date", example = "2026-05-19")
        LocalDate invoiceDate,
        @NotBlank
        @Pattern(regexp = "^[A-Z]+$")
        @Schema(description = "Invoice type", example = "SAL")
        String invType,
        @Schema(description = "Maturity code", example = "M")
        String maturity,
        @Schema(description = "FOB code", example = "FOB01")
        String fobCode,
        @Schema(description = "Primary sort code", example = "A")
        String primarySortCode,
        @Schema(description = "Submitted total amount", example = "1250.75")
        BigDecimal totalAmt,
        @Schema(description = "Submitted total pieces", example = "100")
        Integer totalPieces,
        @Schema(description = "Submitted total volume", example = "12.5")
        BigDecimal totalVol,
        @NotBlank
        @Pattern(regexp = "^\\d+$")
        @Schema(description = "Submitter client number", example = "00001234")
        String submitterClientNum,
        @NotBlank
        @Pattern(regexp = "^\\d{2}$")
        @Schema(description = "Submitter location code", example = "00")
        String submitterLocation,
        @NotBlank
        @Pattern(regexp = "^(Buyer|Seller)$")
        @Schema(description = "Submitted by (Buyer or Seller)", example = "Seller")
        String submittedBy,
        @Pattern(regexp = "^\\d+$")
        @Schema(description = "Seller client number", example = "00001234")
        String clientNumber,
        @Pattern(regexp = "^\\d{2}$")
        @Schema(description = "Seller client location", example = "00")
        String clientLocation,
        @Pattern(regexp = "^\\d+$")
        @Schema(description = "Other-party client number", example = "00005678")
        String otherClientNum,
        @Pattern(regexp = "^\\d{2}$")
        @Schema(description = "Other-party client location", example = "00")
        String otherClientLocation,
        @Size(max = 40)
        @Schema(description = "Other-party client name (manual / non-registered)", example = "ABC Logging Ltd.")
        String otherClientName,
        @Size(max = 30)
        @Schema(description = "Other-party client city", example = "Nanaimo")
        String otherClientCity,
        @Pattern(regexp = "^[A-Z]{2}$")
        @Schema(description = "Other-party client province/state", example = "BC")
        String otherClientProvState,
        @Schema(description = "Boom numbers")
        List<String> boomNumbers,
        @Schema(description = "Timber marks")
        List<String> timberMarks,
        @Schema(description = "Weight slips")
        List<String> weightSlips,
        @Schema(description = "CSV of replaced invoice numbers", example = "null")
        String replaceInvNum,
        @Schema(description = "CSV of adjusted invoice numbers", example = "null")
        String adjustInvNum,
        @Schema(description = "Reviewer comments", example = "null")
        String reviewComments,
        @Schema(description = "Submitter comments", example = "null")
        String submitComments,
        @Schema(description = "Manual entry flag (true = manual, false = ESF)", example = "true")
        boolean manual,
        @Valid
        @Schema(description = "Line items belonging to this invoice")
        List<LineItemRequest> lineItems
) {}
