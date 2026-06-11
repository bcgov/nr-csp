package ca.bc.gov.nrs.csp.backend.service.model;

import java.time.LocalDate;

public record SearchCriteria(
        LocalDate invDate,
        LocalDate startDate,
        LocalDate endDate,
        String submitterClientNum,
        String sellerBuyerClientNum,
        String sellerBuyerLocNum,
        Boolean sellerSubmitter,
        String invNumber,
        String invStatus,
        String invType,
        String maturity,
        String keyword
) {}
