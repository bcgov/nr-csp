package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Full invoice response returned by every invoice endpoint (GET, POST, PUT, submit, duplicate, status change, and line-item operations). The `warnings` and `errors` lists both reflect a re-validation of the saved record after the operation completes — `errors` contains anything that would block save/submit (returned here for visibility; the request itself is NOT blocked), `warnings` contains soft nudges.")
public record InvoiceResponse(
        @Schema(description = "Invoice id", example = "12345")
        Long invID,
        @Schema(description = "Surrogate parent submission key (csp_submission_id) — the internal join key, not shown to users", example = "67890")
        Long submissionId,
        @Schema(description = "Business submission number (csp_submission.submission_id) shown to users as \"Submission ID\". Null for manually-entered invoices.", example = "67890")
        Long submissionNumber,
        @Schema(description = "Invoice number", example = "INV-2026-001")
        String invNumber,
        @Schema(description = "Invoice date", example = "2026-05-19")
        LocalDate invoiceDate,
        @Schema(description = "Invoice status", example = "DFT")
        String invStatus,
        @Schema(description = "Invoice type", example = "SAL")
        String invType,
        @Schema(description = "Maturity code", example = "M")
        String maturity,
        @Schema(description = "FOB code", example = "FOB01")
        String fobCode,
        @Schema(description = "Primary sort code", example = "SORT01")
        String primarySortCode,
        @Schema(description = "Submitted total amount", example = "1250.75")
        BigDecimal totalAmt,
        @Schema(description = "Submitted total pieces", example = "100")
        Integer totalPieces,
        @Schema(description = "Submitted total volume", example = "12.5")
        BigDecimal totalVol,
        @Schema(description = "Submitter client number", example = "00001234")
        String submitterClientNum,
        @Schema(description = "Submitter location code", example = "01")
        String submitterLocation,
        @Schema(description = "Submitted by", example = "Seller")
        String submittedBy,
        @Schema(description = "Seller client number", example = "00001234")
        String clientNumber,
        @Schema(description = "Seller client location", example = "01")
        String clientLocation,
        @Schema(description = "Other-party client number", example = "00005678")
        String otherClientNum,
        @Schema(description = "Other-party client location", example = "01")
        String otherClientLocation,
        @Schema(description = "Other-party client name")
        String otherClientName,
        @Schema(description = "Other-party client city")
        String otherClientCity,
        @Schema(description = "Other-party client province/state", example = "BC")
        String otherClientProvState,
        @Schema(description = "Boom numbers")
        List<String> boomNumbers,
        @Schema(description = "Timber marks")
        List<String> timberMarks,
        @Schema(description = "Weight slips")
        List<String> weightSlips,
        @Schema(description = "CSV of replaced invoice numbers")
        String replaceInvNum,
        @Schema(description = "CSV of adjusted invoice numbers")
        String adjustInvNum,
        @Schema(description = "Reviewer comments")
        String reviewComments,
        @Schema(description = "Submitter comments")
        String submitComments,
        @Schema(description = "User id that entered the invoice")
        String entryUserID,
        @Schema(description = "Line items belonging to the invoice")
        List<LineItemResponse> lineItems,
        @ArraySchema(
                arraySchema = @Schema(description = "Non-blocking validation warnings raised by re-validating the saved record after the operation completes. Returned on every invoice endpoint. Empty list when none."),
                schema = @Schema(implementation = ValidationMessageResponse.class)
        )
        List<ValidationMessageResponse> warnings,
        @ArraySchema(
                arraySchema = @Schema(description = "Validation errors raised by re-validating the saved record after the operation completes. Errors that would block save/submit are surfaced here for visibility; the calling request itself is NOT blocked. Returned on every invoice endpoint. Empty list when none."),
                schema = @Schema(implementation = ValidationMessageResponse.class)
        )
        List<ValidationMessageResponse> errors
) {}
