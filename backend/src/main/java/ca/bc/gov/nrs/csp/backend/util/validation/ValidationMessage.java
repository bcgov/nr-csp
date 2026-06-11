package ca.bc.gov.nrs.csp.backend.util.validation;

public record ValidationMessage(String messageKey, Object[] args, MessageType type) {
}
