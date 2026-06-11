package ca.bc.gov.nrs.csp.backend.controller.api;

import ca.bc.gov.nrs.csp.backend.controller.dto.error.ApiError;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ChangeStatusRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.CreateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceSummaryExportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItemRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.UpdateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Invoice", description = "Invoice details — get, save, submit, duplicate, change status, delete")
@RequestMapping("/api/invoices")
public interface InvoiceApi {

    @Operation(summary = "Get an invoice by id",
            description = "Returns the invoice header, line items, and source-document references. Returns 404 if the invoice does not exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    ResponseEntity<InvoiceResponse> getById(
            @Parameter(description = "Invoice id", example = "12345") @PathVariable Long id);

    @Operation(summary = "Create a new invoice",
            description = "Always persists the new invoice with status DRAFT and creates a parent submission. The response body's warnings list contains any non-blocking validation warnings raised during the save.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping
    ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request);

    @Operation(summary = "Update an existing invoice",
            description = "Updates the header and reconciles line items: any line with an existing id is updated, lines without an id are inserted, and existing lines absent from the request are deleted. Status remains DRAFT — use /submit or /status to transition.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invoice is in a status that does not allow edits (e.g., APPROVED)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    ResponseEntity<InvoiceResponse> update(
            @Parameter(description = "Invoice id", example = "12345") @PathVariable Long id,
            @Valid @RequestBody UpdateInvoiceRequest request);

    @Operation(summary = "Delete an invoice")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invoice is in a status that prevents deletion (e.g., APPROVED)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Invoice id", example = "12345") @PathVariable Long id);

    @Operation(summary = "Submit a DRAFT invoice",
            description = "Transitions a DRAFT invoice to PROCESSING. Runs a stricter validation (at least one line item required) and updates the parent submission status to INBOX.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submitted",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invoice is not in DRAFT status",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/submit")
    ResponseEntity<InvoiceResponse> submit(
            @Parameter(description = "Invoice id", example = "12345") @PathVariable Long id);

    @Operation(summary = "Duplicate an invoice",
            description = "Clones an existing invoice (header + lines + source-document references) as a brand-new DRAFT invoice under a new submission. The returned response describes the new invoice.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Duplicated",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Source invoice not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/duplicate")
    ResponseEntity<InvoiceResponse> duplicate(
            @Parameter(description = "Source invoice id", example = "12345") @PathVariable Long id);

    @Operation(summary = "Change invoice status",
            description = "Approves, rejects, cancels, or unapproves an invoice. Reviewer comments are required by the business rules for reject/cancel/unapprove.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/status")
    ResponseEntity<InvoiceResponse> changeStatus(
            @Parameter(description = "Invoice id", example = "12345") @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request);

    // -------------------------------------------------------------------
    // Line-item sub-resource endpoints (only valid on DFT invoices)
    // -------------------------------------------------------------------

    @Operation(summary = "Add a line item to a DRAFT invoice",
            description = "Persists the line item against the parent invoice and re-runs full-invoice validation. Returns the refreshed InvoiceResponse with updated totals + warnings.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Line item added",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invoice is not in DRAFT status",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/line-items")
    ResponseEntity<InvoiceResponse> addLineItem(
            @Parameter(description = "Invoice id", example = "12345") @PathVariable Long id,
            @Valid @RequestBody LineItemRequest request);

    @Operation(summary = "Update a line item on a DRAFT invoice",
            description = "The line id in the URL path takes precedence over any lineItemID in the request body. Returns the refreshed InvoiceResponse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Line item updated",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invoice or line item not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invoice is not in DRAFT status",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/line-items/{lineId}")
    ResponseEntity<InvoiceResponse> updateLineItem(
            @Parameter(description = "Invoice id", example = "12345") @PathVariable Long id,
            @Parameter(description = "Line item id", example = "1") @PathVariable Long lineId,
            @Valid @RequestBody LineItemRequest request);

    @Operation(summary = "Delete a line item from a DRAFT invoice",
            description = "Removes the line and returns the refreshed InvoiceResponse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Line item deleted",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invoice or line item not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invoice is not in DRAFT status",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}/line-items/{lineId}")
    ResponseEntity<InvoiceResponse> deleteLineItem(
            @Parameter(description = "Invoice id", example = "12345") @PathVariable Long id,
            @Parameter(description = "Line item id", example = "1") @PathVariable Long lineId);

    @Operation(summary = "Export the displayed group-summary rows as CSV",
            description = "Formats the group rows sent in the body (exactly what's on screen) as a CSV download.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "CSV file"))
    @PostMapping(value = "/export/csv", produces = "text/csv")
    ResponseEntity<Resource> exportGroupSummaryCsv(@RequestBody InvoiceSummaryExportRequest request);

    @Operation(summary = "Export the displayed group-summary rows as PDF",
            description = "Formats the group rows sent in the body (exactly what's on screen) as a PDF download.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "PDF file"))
    @PostMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<Resource> exportGroupSummaryPdf(@RequestBody InvoiceSummaryExportRequest request);
}
