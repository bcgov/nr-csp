package ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row of the Submission History expanded "Invoice comments" sub-table — an
 * invoice ({@code coastal_log_sale}) with its status and reviewer comment.
 */
public record SubmissionInvoiceCommentResponse(
        @Schema(description = "Client invoice number (client_invoice_no).")
        String invoiceNumber,

        @Schema(description = "Invoice status description (not code) from LOG_SALE_ENTRY_STATUS_CODE, e.g. Approved.")
        String status,

        @Schema(description = "Reviewer comment for the invoice (reviewer_notes). May be null.")
        String comment
) {}
