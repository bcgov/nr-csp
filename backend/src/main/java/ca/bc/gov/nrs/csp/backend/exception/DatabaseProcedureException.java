package ca.bc.gov.nrs.csp.backend.exception;

import lombok.Getter;

@Getter
public class DatabaseProcedureException extends RuntimeException {

    private final String errorCode;

    public DatabaseProcedureException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
    }
}