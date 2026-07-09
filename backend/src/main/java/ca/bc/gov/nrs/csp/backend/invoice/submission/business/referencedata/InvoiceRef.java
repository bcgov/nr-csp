package ca.bc.gov.nrs.csp.backend.invoice.submission.business.referencedata;

/**
 * Lightweight projection of an existing invoice returned by
 * {@link ReferenceDataService#findInvoices}. Carries only what the
 * replace/adjust rules need: the invoice number and its entry-status code
 * (e.g. {@code "CAN"} for cancelled — see rule I9).
 */
public record InvoiceRef(String invoiceNumber, String statusCode) {}
