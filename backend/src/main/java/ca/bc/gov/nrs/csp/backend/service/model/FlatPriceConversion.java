package ca.bc.gov.nrs.csp.backend.service.model;

import java.time.LocalDate;

public record FlatPriceConversion(
    Long id,
    String modellingCode,
    String maturity,        // log_sale_type_code
    String species,         // log_sale_species_code (resolved from csp_species_grade_xref join)
    String grade,           // log_sale_grade_code (resolved from csp_species_grade_xref join)
    String sortCode,        // log_sale_sort_code
    Integer flatPriceConversion,
    LocalDate effectiveDate,
    LocalDate expiryDate,   // nullable
    Integer revisionCount,
    String entryUserid,
    LocalDate entryTimestamp,
    String updateUserid,
    LocalDate updateTimestamp
) {}
