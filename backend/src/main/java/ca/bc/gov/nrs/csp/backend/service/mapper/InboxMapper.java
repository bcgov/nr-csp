package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.inbox.InboxRowResponse;
import ca.bc.gov.nrs.csp.backend.service.model.InboxRow;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface InboxMapper {

    InboxRowResponse toResponse(InboxRow row);

    List<InboxRowResponse> toResponseList(List<InboxRow> rows);

    default Page<InboxRowResponse> toResponsePage(Page<InboxRow> page) {
        return page.map(this::toResponse);
    }
}
