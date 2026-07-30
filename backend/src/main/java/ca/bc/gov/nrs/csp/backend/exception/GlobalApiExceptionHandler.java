package ca.bc.gov.nrs.csp.backend.exception;

import ca.bc.gov.nrs.csp.backend.controller.dto.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationErrorResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationMessageResponse;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Locale;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalApiExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(ValidationException ex) {
        List<ValidationMessageResponse> errors = ex.getResult().errors().stream()
                .map(this::toMessageResponse)
                .toList();
        log.warn("Business-rule validation failed with {} error(s)", errors.size());
        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse("VALIDATION_ERROR", ex.getMessage(), errors));
    }

    private ValidationMessageResponse toMessageResponse(ValidationMessage m) {
        return new ValidationMessageResponse(
                m.messageKey(),
                m.args(),
                m.type().name(),
                resolveMessageText(m.messageKey(), m.args())
        );
    }

    /**
     * Resolve a validation key against the application's `messages.properties`
     * bundle so the API response carries human-readable text alongside the
     * machine-readable key. Falls back to the key itself when the bundle has
     * no matching template — keeps missing translations visible without
     * blowing up the response.
     */
    private String resolveMessageText(String key, Object[] args) {
        if (key == null || key.isBlank()) return "";
        try {
            return messageSource.getMessage(key, args, Locale.getDefault());
        } catch (NoSuchMessageException e) {
            return key;
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ApiError> handleUnprocessable(UnprocessableEntityException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("Unprocessable entity: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError("UNPROCESSABLE_ENTITY", ex.getMessage()));
    }

    @ExceptionHandler(JwtSigningKeyException.class)
    public ResponseEntity<ApiError> handleJwtSigningKey(JwtSigningKeyException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("JWT signing key error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_TOKEN", "Token signature could not be verified."));
    }

    @ExceptionHandler(DatabaseProcedureException.class)
    public ResponseEntity<ApiError> handleDatabaseProcedure(DatabaseProcedureException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.error("Database procedure error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("DATABASE_ERROR", "A database error occurred while processing the request."));
    }

    @ExceptionHandler(ReportGenerationException.class)
    public ResponseEntity<ApiError> handleReportGeneration(ReportGenerationException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.error("Report generation failed.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("REPORT_ERROR", "An error occurred while generating the report."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        String message = "Invalid value for parameter '" + ex.getName() + "': '" + ex.getValue() + "'.";
        if (ex.getRequiredType() != null
                && java.time.LocalDate.class.isAssignableFrom(ex.getRequiredType())) {
            message += " Expected format: yyyy-MM-dd.";
        }
        log.warn("Type mismatch: {}", message);
        return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("Missing required parameter: {}", ex.getParameterName());
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Required parameter '" + ex.getParameterName() + "' is missing."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", message);
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        // Surface DB constraint violations (e.g. an Oracle FK such as a client
        // number/location that doesn't exist) as a clean 400 instead of a raw 500
        // stack trace. The specific cause is logged for diagnostics only.
        log.warn("Data integrity violation: {}",
                ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(new ApiError("DATA_INTEGRITY_ERROR",
                "The request references data that does not exist or violates a database constraint. "
                        + "Please verify the client numbers, locations, and codes."));
    }

    @ExceptionHandler({AuthorizationDeniedException.class})
    public ResponseEntity<ApiError> handleAccessDenied(Exception ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("ACCESS_DENIED", "You do not have permission to perform this action."));
    }

    @ExceptionHandler({AuthenticationCredentialsNotFoundException.class})
    public ResponseEntity<ApiError> handleUnauthenticated(AuthenticationCredentialsNotFoundException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("Unauthenticated access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("UNAUTHENTICATED", "Authentication is required."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.debug("Static resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientAbort(AsyncRequestNotUsableException ex) {
        log.debug("Client disconnected before response was fully sent.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.error("Unhandled exception.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred."));
    }

    // Prevents HttpMediaTypeNotAcceptableException when error handlers return JSON after
    // a report endpoint has pre-set PRODUCIBLE_MEDIA_TYPES to application/pdf or text/csv.
    private void clearProducibleMediaTypes(HttpServletRequest request) {
        request.removeAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
    }
}
