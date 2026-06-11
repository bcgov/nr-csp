package ca.bc.gov.nrs.csp.backend.util.validation;

import java.util.List;

public record ValidationResult(List<ValidationMessage> messages) {

    public ValidationResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public boolean isValid() {
        return messages.stream().noneMatch(m -> m.type() == MessageType.ERROR);
    }

    public boolean hasErrors() {
        return !isValid();
    }

    public boolean hasWarnings() {
        return messages.stream().anyMatch(m -> m.type() == MessageType.WARNING);
    }

    public List<ValidationMessage> errors() {
        return messages.stream().filter(m -> m.type() == MessageType.ERROR).toList();
    }

    public List<ValidationMessage> warnings() {
        return messages.stream().filter(m -> m.type() == MessageType.WARNING).toList();
    }
}
