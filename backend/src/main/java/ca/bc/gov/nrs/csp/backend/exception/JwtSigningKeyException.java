package ca.bc.gov.nrs.csp.backend.exception;

public class JwtSigningKeyException extends RuntimeException {
    public JwtSigningKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
