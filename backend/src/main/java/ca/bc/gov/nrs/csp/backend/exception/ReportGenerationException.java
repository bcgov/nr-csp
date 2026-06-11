package ca.bc.gov.nrs.csp.backend.exception;

/**
 * Custom exception for errors that occur during the generation of a report.
 * This is used to wrap checked exceptions from the service layer into an
 * unchecked exception that can be handled by the GlobalExceptionHandler.
 */
public class ReportGenerationException extends RuntimeException {
    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
