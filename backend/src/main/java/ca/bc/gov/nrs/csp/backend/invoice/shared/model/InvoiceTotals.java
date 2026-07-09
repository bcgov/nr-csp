package ca.bc.gov.nrs.csp.backend.invoice.shared.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dumb carrier: the only invoice data the totals rules (I24–I29) need. Both the
 * electronic (ESF/XML) and manual (CRUD) paths populate it from their own models.
 * No invoice number — per-invoice scoping is the caller's job.
 */
public record InvoiceTotals(
    String invoiceType,
    BigDecimal submittedAmount,
    BigDecimal submittedVolume,
    Integer submittedPieces,
    List<Line> lines) {

  public record Line(BigDecimal volume, BigDecimal price, int pieces) {}
}
