package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.repository.ClientLocationRepository;
import ca.bc.gov.nrs.csp.backend.repository.SearchRepository;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.model.SearchCriteria;
import ca.bc.gov.nrs.csp.backend.service.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final SearchRepository searchRepository;
    private final ClientLocationRepository clientLocationRepository;

    public SearchService(SearchRepository searchRepository, ClientLocationRepository clientLocationRepository) {
        this.searchRepository = searchRepository;
        this.clientLocationRepository = clientLocationRepository;
    }

    @Transactional(readOnly = true)
    public Page<SearchResult> search(SearchCriteria criteria, Pageable pageable) {
        log.debug("Invoice search requested");
        validateDateRange(criteria.startDate(), criteria.endDate());

        String normalizedSubmitterClientNum = normalizeClientNumber(criteria.submitterClientNum(), "Submitter client number");
        String normalizedSellerBuyerClientNum = normalizeClientNumber(criteria.sellerBuyerClientNum(), "Seller/buyer client number");

        SearchCriteria normalized = new SearchCriteria(
                criteria.invDate(),
                criteria.startDate(),
                criteria.endDate(),
                normalizedSubmitterClientNum,
                normalizedSellerBuyerClientNum,
                criteria.sellerBuyerLocNum(),
                criteria.sellerSubmitter(),
                criteria.invNumber(),
                criteria.invStatus(),
                criteria.invType(),
                criteria.maturity(),
                criteria.keyword()
        );

        Page<SearchResult> results = searchRepository.search(normalized, pageable);
        log.debug("Invoice search returned {} of {} result(s)", results.getNumberOfElements(), results.getTotalElements());
        return results;
    }

    @Transactional(readOnly = true)
    public List<ClientLocation> findClientsByName(String name) {
        if (name == null) {
            throw new BadRequestException("Client name search term must not be blank.");
        }
        String trimmedName = name.trim();
        if (trimmedName.isBlank()) {
            throw new BadRequestException("Client name search term must not be blank.");
        }
        log.debug("Client autocomplete by name requested");
        List<ClientLocation> results = clientLocationRepository.findByName(trimmedName);
        log.debug("Client autocomplete by name returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public List<ClientLocation> findClientsByNumber(String number) {
        if (number == null) {
            throw new BadRequestException("Client number search term must not be blank.");
        }
        String trimmed = number.trim();
        if (trimmed.isBlank()) {
            throw new BadRequestException("Client number search term must not be blank.");
        }
        String padded = normalizeClientNumber(trimmed, "Client number");
        log.debug("Client autocomplete by number requested");
        List<ClientLocation> results = clientLocationRepository.findByNumber(padded);
        log.debug("Client autocomplete by number returned {} result(s)", results.size());
        return results;
    }

    // Replicates CSPClientNumberConverter: validates numeric, zero-pads to 8 digits.
    private String normalizeClientNumber(String clientNumber, String fieldName) {
        if (clientNumber == null || clientNumber.isBlank()) {
            return null;
        }
        String trimmed = clientNumber.trim();
        if (!trimmed.matches("\\d+")) {
            throw new BadRequestException(fieldName + " must be numeric.");
        }
        if (trimmed.length() > 8) {
            throw new BadRequestException(fieldName + " must be 8 digits or fewer.");
        }
        try {
            return String.format("%08d", Long.parseLong(trimmed));
        } catch (NumberFormatException e) {
            throw new BadRequestException(fieldName + " must be numeric.");
        }
    }

    // Replicates SearchCriteriaValidator: startDate must not be after endDate.
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must not be after end date.");
        }
    }
}
