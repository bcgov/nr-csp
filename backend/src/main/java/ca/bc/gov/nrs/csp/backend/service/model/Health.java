package ca.bc.gov.nrs.csp.backend.service.model;

import java.time.Instant;

public record Health(String status, Instant timestamp) {}
