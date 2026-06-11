package ca.bc.gov.nrs.csp.backend.service.model;

public record ClientLocation(
        String clientNumber,
        String clientName,
        String clientLocnCode,
        String clientLocnName,
        String city,
        String province
) {}
