package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.search.ClientLocationResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.search.SearchResultResponse;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.model.SearchResult;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface SearchMapper {
    SearchResultResponse toResponse(SearchResult result);
    List<SearchResultResponse> toResponseList(List<SearchResult> results);
    ClientLocationResponse toClientLocationResponse(ClientLocation clientLocation);
    List<ClientLocationResponse> toClientLocationResponseList(List<ClientLocation> clientLocations);

    default Page<SearchResultResponse> toResponsePage(Page<SearchResult> page) {
        return page.map(this::toResponse);
    }
}
