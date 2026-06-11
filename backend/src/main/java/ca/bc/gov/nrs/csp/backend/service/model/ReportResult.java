package ca.bc.gov.nrs.csp.backend.service.model;

/**
 * Holds the result of a report generation: the raw bytes and the suggested download filename.
 */
public record ReportResult(byte[] data, String filename) {}
