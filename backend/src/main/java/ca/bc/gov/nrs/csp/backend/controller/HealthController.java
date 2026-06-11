package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.HealthApi;
import ca.bc.gov.nrs.csp.backend.controller.dto.health.HealthResponse;
import ca.bc.gov.nrs.csp.backend.service.HealthService;
import ca.bc.gov.nrs.csp.backend.service.mapper.HealthMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController implements HealthApi {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final HealthService healthService;
    private final HealthMapper healthMapper;

    public HealthController(HealthService healthService, HealthMapper healthMapper) {
        this.healthService = healthService;
        this.healthMapper = healthMapper;
    }

    @Override
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = healthMapper.toResponse(healthService.getHealth());
        log.info("GET    /api/health → 200 OK");
        return ResponseEntity.ok(response);
    }
}
