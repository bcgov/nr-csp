package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.health.HealthResponse;
import ca.bc.gov.nrs.csp.backend.service.model.Health;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HealthMapper {
    HealthResponse toResponse(Health health);
}
