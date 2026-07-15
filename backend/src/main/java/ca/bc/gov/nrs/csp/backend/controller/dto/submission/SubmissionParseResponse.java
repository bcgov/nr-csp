package ca.bc.gov.nrs.csp.backend.controller.dto.submission;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationMessageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response for the submission parse endpoint. It combines the structural
 * validation outcome with the parsed submission content used to populate the
 * upload form. On a structural failure {@code valid} is false, {@code errors}
 * carries the schema/format/envelope messages, and {@code submission} is null.
 * On success {@code submission} holds the parsed metadata, invoices and line
 * items (business rules are NOT run here — that is a separate endpoint).
 *
 * <p>The invoice and line-item positional indices ({@code index},
 * {@code invoiceIndex}, {@code lineIndex}, all 1-based) mirror the locators the
 * business-validation phase uses, so the frontend can attach each business
 * error to the exact row it came from.
 */
@Schema(description = "Parsed submission content plus structural validation outcome.")
public record SubmissionParseResponse(
        @Schema(description = "True when the submission is structurally valid and was parsed", example = "true")
        boolean valid,
        @Schema(description = "Outcome code: OK when parsed, VALIDATION_ERROR / upload code otherwise", example = "OK")
        String code,
        @Schema(description = "Human-readable summary")
        String message,
        @Schema(description = "Structural errors when parsing failed; empty on success")
        List<ValidationMessageResponse> errors,
        @Schema(description = "Parsed submission content; null when parsing failed")
        ParsedSubmission submission
) {

    /** Parsed submission metadata plus its flattened invoice and line-item rows. */
    @Schema(description = "Parsed submission content extracted from the uploaded XML.")
    public record ParsedSubmission(
            @Schema(description = "Submitter email from the ESF envelope; null for a bare CSP body")
            String email,
            @Schema(description = "Submitter telephone from the ESF envelope; null for a bare CSP body")
            String telephone,
            String monthComplete,
            @Schema(description = "Seller submission flag from the XML (Y/N)")
            String sellerSubmission,
            String submissionClientNumber,
            String submissionClientLocnCode,
            List<ParsedInvoice> invoices,
            List<ParsedLineItem> lineItems
    ) {}

    /** One invoice row for the "Invoice Details" table. */
    public record ParsedInvoice(
            @Schema(description = "1-based invoice position, matching the business-validation locator")
            int index,
            String invoiceNumber,
            @Schema(description = "Invoice date as ISO yyyy-MM-dd")
            String invoiceDate,
            String invoiceType,
            String sellerClientNumber,
            String buyerClientNumber,
            String maturity,
            String locationFOB,
            BigDecimal totalAmount,
            BigDecimal totalVolume,
            Integer totalPieces
    ) {}

    /** One line-item row for the "Invoice Line Items" table. */
    public record ParsedLineItem(
            @Schema(description = "1-based parent invoice position")
            int invoiceIndex,
            @Schema(description = "1-based line position within its invoice")
            int lineIndex,
            String invoiceNumber,
            String species,
            String grade,
            String secondarySortCode,
            String clientSecondarySortCode,
            Integer numberOfPieces,
            BigDecimal volume,
            BigDecimal price
    ) {}
}
