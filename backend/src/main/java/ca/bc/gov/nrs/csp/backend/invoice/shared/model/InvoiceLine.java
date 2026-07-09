package ca.bc.gov.nrs.csp.backend.invoice.shared.model;

import java.math.BigDecimal;

/**
 * Dumb carrier: the only line-item data the pure line rules need. Both
 * the electronic (ESF/XML) and manual (CRUD) paths populate it from their own
 * models. {@code lineLabel} is the channel-formatted line reference substituted
 * into the message templates (electronic: {@code "Line 3"}; manual:
 * {@code "Line #7"} / {@code "Line #New"}). Named {@code InvoiceLine} — not
 * {@code LineItem} — to avoid clashing with the manual path's DTO of that name.
 */
public record InvoiceLine(
    String invoiceType,
    String lineLabel,
    String grade,
    Integer numberOfPieces,
    BigDecimal volume,
    BigDecimal price) {}
