package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.SortCodeResponse;
import ca.bc.gov.nrs.csp.backend.service.model.SortCode;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface SortCodeMapper {
    SortCodeResponse toResponse(SortCode sortCode);
    List<SortCodeResponse> toResponseList(List<SortCode> sortCodes);

    default Page<SortCodeResponse> toResponsePage(Page<SortCode> page) {
        return page.map(this::toResponse);
    }
}
