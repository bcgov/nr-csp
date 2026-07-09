package ca.bc.gov.nrs.csp.backend.exception;

import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionClassesTest {

    @Nested
    class BadRequestExceptionTest {

        @Test
        void constructor_setsMessage() {
            var ex = new BadRequestException("bad input");

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("bad input");
            assertThat(ex.getCause()).isNull();
        }
    }

    @Nested
    class ConflictExceptionTest {

        @Test
        void messageConstructor_setsMessage() {
            var ex = new ConflictException("duplicate entry");

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("duplicate entry");
            assertThat(ex.getCause()).isNull();
        }

        @Test
        void messageAndCauseConstructor_setsBoth() {
            var cause = new IllegalStateException("root");
            var ex = new ConflictException("duplicate entry", cause);

            assertThat(ex.getMessage()).isEqualTo("duplicate entry");
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        void isAnnotatedWithConflictResponseStatus() {
            ResponseStatus annotation = ConflictException.class.getAnnotation(ResponseStatus.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    class DatabaseProcedureExceptionTest {

        @Test
        void constructor_setsErrorCodeAndMessage() {
            var ex = new DatabaseProcedureException("ORA-20001", "procedure failed");

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getErrorCode()).isEqualTo("ORA-20001");
            assertThat(ex.getMessage()).isEqualTo("procedure failed");
            assertThat(ex.getCause()).isNull();
        }
    }

    @Nested
    class JwtSigningKeyExceptionTest {

        @Test
        void constructor_setsMessageAndCause() {
            var cause = new RuntimeException("key fetch failed");
            var ex = new JwtSigningKeyException("cannot verify token", cause);

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("cannot verify token");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    class ReportGenerationExceptionTest {

        @Test
        void constructor_setsMessageAndCause() {
            var cause = new Exception("jasper error");
            var ex = new ReportGenerationException("report failed", cause);

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("report failed");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    class ResourceNotFoundExceptionTest {

        @Test
        void constructor_setsMessage() {
            var ex = new ResourceNotFoundException("invoice not found");

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("invoice not found");
            assertThat(ex.getCause()).isNull();
        }

        @Test
        void isAnnotatedWithNotFoundResponseStatus() {
            ResponseStatus annotation = ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class UnprocessableEntityExceptionTest {

        @Test
        void constructor_setsMessage() {
            var ex = new UnprocessableEntityException("cannot process");

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("cannot process");
            assertThat(ex.getCause()).isNull();
        }
    }

    @Nested
    class ValidationExceptionTest {

        @Test
        void constructor_setsMessageAndResult() {
            var message = new ValidationMessage("invoice.amount.required", new Object[]{}, MessageType.ERROR);
            var result = new ValidationResult(List.of(message));
            var ex = new ValidationException("validation failed", result);

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("validation failed");
            assertThat(ex.getResult()).isSameAs(result);
            assertThat(ex.getResult().errors()).containsExactly(message);
        }
    }
}
