package ca.bc.gov.nrs.csp.backend.service.model;

import java.time.LocalDate;

public record SearchResult(
        Long coastalLogSaleId,
        Long cspSubmissionId,
        Long submissionId,
        String invoiceStatus,
        String invoiceNumber,
        LocalDate invoiceDate,
        String type,
        String clientNumber,
        String clientName,
        String maturity,
        String submissionType
) {}
