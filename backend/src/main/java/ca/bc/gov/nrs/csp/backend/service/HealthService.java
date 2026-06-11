package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.service.model.Health;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class HealthService {

    public Health getHealth() {
        return new Health("UP", Instant.now());
    }
}
