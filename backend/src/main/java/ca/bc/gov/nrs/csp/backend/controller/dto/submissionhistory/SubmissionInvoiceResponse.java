package ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A row in the submission detail "Invoices" table — one invoice
 * ({@code coastal_log_sale}) belonging to the submission. The top-level fields
 * back the table row; the remaining fields back the expandable per-invoice
 * "Invoice details" panel (replaces/adjusts numbers, the other party, sort
 * codes, log-source references, and the submitter/staff comments).
 */
public record SubmissionInvoiceResponse(
        @Schema(description = "Invoice id (coastal_log_sale_id).")
        Long coastalLogSaleId,

        @Schema(description = "Client invoice number (client_invoice_no).")
        String invoiceNumber,

        @Schema(description = "Client invoice date (yyyy-MM-dd).")
        LocalDate invoiceDate,

        @Schema(description = "Invoice type code (csp_invoice_type_code), e.g. SAL.")
        String type,

        @Schema(description = "Invoice decision/status description (not code) from LOG_SALE_ENTRY_STATUS_CODE, e.g. Pending.")
        String status,

        @Schema(description = "Seller client number/location, e.g. 00126920/00.")
        String sellerClient,

        @Schema(description = "Buyer client number/location, e.g. 00123946/00.")
        String buyerClient,

        @Schema(description = "Maturity code (log_sale_type_code).")
        String maturity,

        @Schema(description = "FOB location (log_sale_fob_location).")
        String fobLocation,

        @Schema(description = "Total invoice amount (client_total_invoice_amt).")
        BigDecimal totalAmount,

        @Schema(description = "Total invoice volume (client_total_invoice_volume).")
        BigDecimal totalVolume,

        @Schema(description = "Total invoice pieces (client_total_invoice_pieces).")
        Integer totalPieces,

        @Schema(description = "Comma-separated invoice numbers this invoice replaces (REP relations).")
        String replacesInvoiceNumbers,

        @Schema(description = "Comma-separated invoice numbers this invoice adjusts (ADJ relations).")
        String adjustsInvoiceNumbers,

        @Schema(description = "Seller client location code (seller_client_locn_code).")
        String sellerClientLocnCode,

        @Schema(description = "Buyer client location code (buyer_client_locn_code).")
        String buyerClientLocnCode,

        @Schema(description = "Other party (non-submitter) participant name.")
        String otherPartyName,

        @Schema(description = "Other party (non-submitter) participant city.")
        String otherPartyCity,

        @Schema(description = "Other party (non-submitter) participant province/state.")
        String otherPartyProvState,

        @Schema(description = "Primary sort code (log_sale_sort_code).")
        String primarySortCode,

        @Schema(description = "Client primary sort code (client_primary_sort_code).")
        String clientPrimarySortCode,

        @Schema(description = "Comma-separated boom numbers (coastal_log_sale_log_source, BOOM).")
        String boomNumbers,

        @Schema(description = "Comma-separated timber marks (coastal_log_sale_log_source, MARK).")
        String timberMarks,

        @Schema(description = "Comma-separated weigh slip numbers (coastal_log_sale_log_source, WEIGH).")
        String weighSlips,

        @Schema(description = "Submitter notes for the invoice (submitter_notes). May be null.")
        String submitterNotes,

        @Schema(description = "Staff/reviewer comment for the invoice (reviewer_notes). May be null.")
        String staffComment
) {}
