package ca.bc.gov.nrs.csp.backend.invoice.shared.model;

/**
 * One rule violation: message key, severity, and the {@code {0}}/{@code {1}}…
 * substitution args its {@code messages.properties} template expects. The core
 * never renders user-facing text — each channel resolves {@code code} + {@code args}
 * at its own edge (the manual path via {@code InvoiceMapper} + {@code MessageSource};
 * see the refactor doc §3.5).
 */
public record Finding(String code, Severity severity, Object[] args) {}
