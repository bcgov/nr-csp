package ca.bc.gov.nrs.csp.backend.exception;

import ca.bc.gov.nrs.csp.backend.controller.HealthController;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationErrorResponse;
import ca.bc.gov.nrs.csp.backend.service.HealthService;
import ca.bc.gov.nrs.csp.backend.service.mapper.HealthMapper;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalApiExceptionHandlerTest {

    private static Stream<Arguments> validationMessageFallbackCases() {
        return Stream.of(
                Arguments.of("no.such.key", "no.such.key"),
                Arguments.of(null, ""),
                Arguments.of("   ", "")
        );
    }

    @Mock HealthService healthService;
    @Mock HealthMapper healthMapper;

    MockMvc mockMvc;
    GlobalApiExceptionHandler handler;
    MockHttpServletRequest servletRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalApiExceptionHandler(new StaticMessageSource());
        servletRequest = new MockHttpServletRequest();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HealthController(healthService, healthMapper))
                .setControllerAdvice(handler)
                .build();
    }

    // ── MockMvc-based tests (service-thrown exceptions) ───────────────────────

    @Test
    void notFound_returns404_withApiErrorShape() throws Exception {
        given(healthService.getHealth()).willThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    void badRequest_returns400_withApiErrorShape() throws Exception {
        given(healthService.getHealth()).willThrow(new BadRequestException("Invalid input"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid input"));
    }

    @Test
    void unexpectedException_returns500_withApiErrorShape() throws Exception {
        given(healthService.getHealth()).willThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ── Direct handler call tests ─────────────────────────────────────────────

    @Test
    void conflict_returns409() {
        var response = handler.handleConflict(new ConflictException("Duplicate entry"), servletRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CONFLICT", response.getBody().code());
        assertEquals("Duplicate entry", response.getBody().message());
    }

    @Test
    void unprocessableEntity_returns422() {
        var response = handler.handleUnprocessable(
                new UnprocessableEntityException("Cannot process"), servletRequest);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("UNPROCESSABLE_ENTITY", response.getBody().code());
    }

    @Test
    void jwtSigningKey_returns401() {
        var response = handler.handleJwtSigningKey(
                new JwtSigningKeyException("Key error", new RuntimeException()), servletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("INVALID_TOKEN", response.getBody().code());
    }

    @Test
    void databaseProcedure_returns500() {
        var response = handler.handleDatabaseProcedure(
                new DatabaseProcedureException("ORA-001", "FK constraint violated"), servletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("DATABASE_ERROR", response.getBody().code());
    }

    @Test
    void reportGeneration_returns500() {
        var response = handler.handleReportGeneration(
                new ReportGenerationException("Jasper failed", new RuntimeException()), servletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("REPORT_ERROR", response.getBody().code());
    }

    @Test
    void accessDenied_returns403() {
        var response = handler.handleAccessDenied(
                new AuthorizationDeniedException("Denied", new AuthorizationDecision(false)),
                servletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("ACCESS_DENIED", response.getBody().code());
    }

    @Test
    void unauthenticated_returns401() {
        var response = handler.handleUnauthenticated(
                new AuthenticationCredentialsNotFoundException("No credentials"), servletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("UNAUTHENTICATED", response.getBody().code());
    }

    @Test
    void noResource_returns404() {
        var response = handler.handleNoResource(
                new NoResourceFoundException(HttpMethod.GET, "/missing/resource", null), servletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().code());
    }

    @Test
    void asyncRequestNotUsable_isHandledSilently() {
        assertDoesNotThrow(() ->
                handler.handleClientAbort(new AsyncRequestNotUsableException("disconnected",
                        new java.io.IOException("broken pipe"))));
    }

    @Test
    void dataIntegrityViolation_returns400() {
        var response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("FK violation"), servletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("DATA_INTEGRITY_ERROR", response.getBody().code());
    }

    @Test
    void constraintViolation_returns400_withViolationMessages() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must not be null");

        var response = handler.handleConstraintViolation(
                new ConstraintViolationException("Constraint", Set.of(violation)), servletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().code());
        assertTrue(response.getBody().message().contains("must not be null"));
    }

    @Test
    void validationException_returns400_withStructuredErrors() {
        var msg = new ValidationMessage("invoice.amount.required", new Object[]{}, MessageType.ERROR);
        var result = new ValidationResult(List.of(msg));
        var ex = new ValidationException("Validation failed", result);

        var response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ValidationErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("VALIDATION_ERROR", body.code());
        assertEquals(1, body.errors().size());
        assertEquals("invoice.amount.required", body.errors().get(0).messageKey());
    }

    @ParameterizedTest
    @MethodSource("validationMessageFallbackCases")
    void validationException_resolvesMessageText_withFallbacks(String messageKey, String expectedMessage) {
        var msg = new ValidationMessage(messageKey, new Object[]{}, MessageType.ERROR);
        var result = new ValidationResult(List.of(msg));

        var response = handler.handleValidationException(new ValidationException("err", result));

        assertNotNull(response.getBody());
        assertEquals(expectedMessage, response.getBody().errors().getFirst().message());
    }

    @Test
    void validationException_resolvesMessageText_whenKeyExistsInBundle() {
        var messageSource = new StaticMessageSource();
        messageSource.addMessage("invoice.amount.required", Locale.getDefault(), "Amount is required");
        var localHandler = new GlobalApiExceptionHandler(messageSource);

        var msg = new ValidationMessage("invoice.amount.required", new Object[]{}, MessageType.ERROR);
        var result = new ValidationResult(List.of(msg));

        var response = localHandler.handleValidationException(new ValidationException("err", result));

        assertEquals("Amount is required", response.getBody().errors().get(0).message());
    }


    // ── MethodArgumentTypeMismatchException ───────────────────────────────────

    @SuppressWarnings("unused")
    private static void dummyEndpoint(LocalDate dateParam, Integer intParam) {
        // used only to build a MethodParameter for MethodArgumentTypeMismatchException
    }

    private static MethodParameter methodParameter(int index) throws NoSuchMethodException {
        return new MethodParameter(
                GlobalApiExceptionHandlerTest.class.getDeclaredMethod(
                        "dummyEndpoint", LocalDate.class, Integer.class),
                index);
    }

    @Test
    void typeMismatch_returns400_withDateFormatHint_forLocalDateParam() throws Exception {
        var ex = new MethodArgumentTypeMismatchException(
                "not-a-date", LocalDate.class, "startDate", methodParameter(0),
                new IllegalArgumentException("bad date"));

        var response = handler.handleTypeMismatch(ex, servletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().code());
        assertEquals("Invalid value for parameter 'startDate': 'not-a-date'. Expected format: yyyy-MM-dd.",
                response.getBody().message());
    }

    @Test
    void typeMismatch_returns400_withoutDateHint_forNonDateParam() throws Exception {
        var ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "count", methodParameter(1),
                new NumberFormatException("abc"));

        var response = handler.handleTypeMismatch(ex, servletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().code());
        assertEquals("Invalid value for parameter 'count': 'abc'.", response.getBody().message());
    }

    @Test
    void typeMismatch_returns400_withoutDateHint_whenRequiredTypeIsNull() throws Exception {
        var ex = new MethodArgumentTypeMismatchException(
                "abc", null, "filter", methodParameter(1), null);

        var response = handler.handleTypeMismatch(ex, servletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid value for parameter 'filter': 'abc'.", response.getBody().message());
    }

    @Test
    void dataIntegrityViolation_returns400_whenMostSpecificCauseIsNull() {
        var ex = new DataIntegrityViolationException("FK violation") {
            @Override
            public Throwable getMostSpecificCause() {
                return null;
            }
        };

        var response = handler.handleDataIntegrity(ex, servletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("DATA_INTEGRITY_ERROR", response.getBody().code());
    }

    @Test
    void dataIntegrityViolation_returns400_whenRootCausePresent() {
        var ex = new DataIntegrityViolationException(
                "wrapper", new java.sql.SQLException("ORA-02291: integrity constraint violated"));

        var response = handler.handleDataIntegrity(ex, servletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("DATA_INTEGRITY_ERROR", response.getBody().code());
    }
}
