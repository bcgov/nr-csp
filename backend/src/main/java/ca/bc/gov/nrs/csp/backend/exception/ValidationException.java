package ca.bc.gov.nrs.csp.backend.exception;

import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;

/**
 * Thrown when a business-rule validation fails. The wrapped {@link ValidationResult}
 * carries the structured list of errors (and any warnings raised so far) so the
 * exception handler can render them in the response body.
 */
public class ValidationException extends RuntimeException {

    private final ValidationResult result;

    public ValidationException(String message, ValidationResult result) {
        super(message);
        this.result = result;
    }

    public ValidationResult getResult() {
        return result;
    }
}
