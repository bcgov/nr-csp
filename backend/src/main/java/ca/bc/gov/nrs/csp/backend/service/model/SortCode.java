package ca.bc.gov.nrs.csp.backend.service.model;

import java.time.LocalDate;

public record SortCode(
        String sortCode,
        String description,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        LocalDate updateTimestamp
) {}
