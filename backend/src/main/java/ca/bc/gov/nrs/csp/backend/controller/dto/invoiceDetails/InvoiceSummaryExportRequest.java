package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * The invoice group-summary rows currently displayed in the table, sent from
 * the client so the export contains exactly what's on screen (the grouping,
 * totals, and price-conversion flag are all derived client-side).
 */
@Schema(description = "Currently displayed invoice group-summary rows to export")
public record InvoiceSummaryExportRequest(
        @Schema(description = "Invoice number, used for the report title and filename", example = "INV-001")
        String invoiceNumber,
        @Schema(description = "The group rows currently shown in the table, in display order")
        List<GroupSummaryExportRow> rows
) {
    @Schema(description = "One group-summary row exactly as shown on screen, with its expanded line items")
    public record GroupSummaryExportRow(
            Integer groupNumber,
            String secondarySort,
            String description,
            String species,
            Integer totalPieces,
            BigDecimal totalVolume,
            BigDecimal totalAmount,
            String priceConversion,
            List<LineItemExportRow> lineItems
    ) {}

    @Schema(description = "One line item as shown in the expanded group detail table")
    public record LineItemExportRow(
            String secondarySort,
            String species,
            String clientSecondarySort,
            Integer numberPieces,
            String grade,
            BigDecimal volume,
            BigDecimal price,
            BigDecimal amount
    ) {}
}
