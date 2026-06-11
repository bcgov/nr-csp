package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.lookup.LookupItemResponse;
import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LookupMapper {
    LookupItemResponse toResponse(LookupItem item);
    List<LookupItemResponse> toResponseList(List<LookupItem> items);
}
