package ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * A row in the submission detail "Invoice Line Items" table — one
 * {@code coastal_log_sale_detail} line, with its parent invoice number.
 */
public record SubmissionLineItemResponse(
        @Schema(description = "Parent invoice id (coastal_log_sale_id) this line belongs to.")
        Long coastalLogSaleId,

        @Schema(description = "Parent invoice number (client_invoice_no) this line belongs to.")
        String invoiceNumber,

        @Schema(description = "Species code (log_sale_species_code).")
        String species,

        @Schema(description = "Grade code (log_sale_grade_code).")
        String grade,

        @Schema(description = "Sort code (log_sale_sort_code).")
        String sortCode,

        @Schema(description = "Client secondary sort code (client_secondary_sort_code).")
        String clientSortCode,

        @Schema(description = "Number of pieces.")
        Integer pieces,

        @Schema(description = "Volume.")
        BigDecimal volume,

        @Schema(description = "Price.")
        BigDecimal price
) {}
