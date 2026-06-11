package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Invoice details information")
public record InvoiceDetails(
        @Schema(description = "Invoice detail id", example = "12345")
        Long invID,
        @Pattern(regexp = "^[A-Z0-9-]+$")
        @Schema(description = "Invoice number", example = "INV-2026-001")
        String invNumber,
        @Schema(description = "Invoice date", example = "2026-05-19")
        LocalDate invoiceDate,
        @Pattern(regexp = "^[A-Z]{3}$")
        @Schema(description = "Invoice status", example = "DFT")
        String invStatus,
        @Pattern(regexp = "^[A-Z]+$")
        @Schema(description = "Invoice type", example = "SALE")
        String invType,
        @Schema(description = "Maturity code", example = "MATURITY_CODE")
        String maturity,
        @Schema(description = "Fob code", example = "FOB01")
        String fobCode,
        @Schema(description = "Primary sort code", example = "SORT01")
        String primarySortCode,
        @Schema(description = "Total amt", example = "1250.75")
        BigDecimal totalAmt,
        @Schema(description = "Total pieces", example = "100")
        Integer totalPieces,
        @Schema(description = "Total volume", example = "12.5")
        BigDecimal totalVol,
        @Pattern(regexp = "^\\d+$")
        @Schema(description = "Submitter client number", example = "123456")
        String submitterClientNum,
        @Pattern(regexp = "^\\d{2}$")
        @Schema(description = "Submitter location", example = "01")
        String submitterLocation,
        @Pattern(regexp = "^(Buyer|Seller)$")
        @Schema(description = "Submitted by (Buyer or Seller)", example = "Seller")
        String submittedBy,
        @Pattern(regexp = "^\\d+$")
        @Schema(description = "Seller client number", example = "123456")
        String clientNumber,
        @Pattern(regexp = "^\\d{2}$")
        @Schema(description = "Seller client location", example = "01")
        String clientLocation,
        @Pattern(regexp = "^\\d+$")
        @Schema(description = "Other party client number", example = "null")
        String otherClientNum,
        @Pattern(regexp = "^\\d{2}$")
        @Schema(description = "Other party client location", example = "null")
        String otherClientLocation,
        @Schema(description = "Other party client name", example = "ABC Logging Ltd.")
        String otherClientName,
        @Schema(description = "Other party client city", example = "Nanaimo")
        String otherClientCity,
        @Pattern(regexp = "^[A-Z]{2}$")
        @Schema(description = "Other party client province/state", example = "BC")
        String otherClientProvState,
        @Schema(description = "Boom numbers", example = "[\"B123\",\"B124\"]")
        List<String> boomNumbers,
        @Schema(description = "Timber marks", example = "[\"TM1\"]")
        List<String> timberMarks,
        @Schema(description = "Weight slips", example = "[\"WS1\"]")
        List<String> weightSlips,
        @Schema(description = "Replace Invoice number", example = "null")
        String replaceInvNum,
        @Schema(description = "Adjust invoice number", example = "null")
        String adjustInvNum,
        @Schema(description = "Review comments", example = "Please review")
        String reviewComments,
        @Schema(description = "Submit comments", example = "Submitted via UI")
        String submitComments,
        @Schema(description = "User ID that entered this invoice", example = "user123")
        String entryUserID
) {}
