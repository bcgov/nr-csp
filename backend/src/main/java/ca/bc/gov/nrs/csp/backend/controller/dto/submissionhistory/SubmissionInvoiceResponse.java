package ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A row in the submission detail "Invoice Details" table — one invoice
 * ({@code coastal_log_sale}) belonging to the submission.
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
        Integer totalPieces
) {}
