package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.SearchApi;
import ca.bc.gov.nrs.csp.backend.controller.dto.search.ClientLocationResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.search.SearchResultResponse;
import ca.bc.gov.nrs.csp.backend.service.SearchService;
import ca.bc.gov.nrs.csp.backend.service.mapper.SearchMapper;
import ca.bc.gov.nrs.csp.backend.service.model.SearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class SearchController implements SearchApi {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;
    private final SearchMapper searchMapper;

    public SearchController(SearchService searchService, SearchMapper searchMapper) {
        this.searchService = searchService;
        this.searchMapper = searchMapper;
    }

    @Override
    public ResponseEntity<Page<SearchResultResponse>> search(
            LocalDate invDate, LocalDate startDate, LocalDate endDate,
            String submitterClientNum, String sellerBuyerClientNum, String sellerBuyerLocNum,
            Boolean sellerSubmitter, String invNumber, String invStatus, String invType, String maturity,
            String keyword, Pageable pageable) {

        log.info("GET /api/search invDate={} startDate={} endDate={} invNumber={} invStatus={} invType={} maturity={} submitterClientNum={} sellerSubmitter={} keyword={} page={} size={} sort={}",
                invDate, startDate, endDate, invNumber, invStatus, invType, maturity, submitterClientNum, sellerSubmitter, keyword,
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        SearchCriteria criteria = new SearchCriteria(
                invDate, startDate, endDate,
                submitterClientNum, sellerBuyerClientNum, sellerBuyerLocNum,
                sellerSubmitter, invNumber, invStatus, invType, maturity, keyword
        );
        return ResponseEntity.ok(searchMapper.toResponsePage(searchService.search(criteria, pageable)));
    }

    @Override
    public ResponseEntity<List<ClientLocationResponse>> findClients(String name, String number) {
        log.info("GET /api/clients name={} number={}", name, number);
        if (number != null && !number.isBlank()) {
            return ResponseEntity.ok(searchMapper.toClientLocationResponseList(searchService.findClientsByNumber(number)));
        }
        if (name == null || name.isBlank()) {
            throw new ca.bc.gov.nrs.csp.backend.exception.BadRequestException("Either 'name' or 'number' search parameter is required.");
        }
        return ResponseEntity.ok(searchMapper.toClientLocationResponseList(searchService.findClientsByName(name)));
    }
}
