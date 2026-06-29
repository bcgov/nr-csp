package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.CspSubmissionApi;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationMessageResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionValidationResponse;
import ca.bc.gov.nrs.csp.backend.submission.SubmissionValidationService;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * CSP XML submission intake endpoints. Read-only: they parse/validate the
 * uploaded XML but persist nothing. The two validation phases are exposed
 * separately:
 *
 * <ul>
 *   <li>{@code POST /api/submissions/validate/structural} — format / ESF
 *       envelope / XSD schema.</li>
 *   <li>{@code POST /api/submissions/validate/business} — CSP business rules.</li>
 * </ul>
 *
 * <p>Each returns 200 with {@code valid: true} on success, 422 with the
 * collected messages on failure, and 400 when the file part is missing or
 * unreadable.
 */
@RestController
public class CspSubmissionController implements CspSubmissionApi {

    private static final Logger log = LoggerFactory.getLogger(CspSubmissionController.class);

    private final SubmissionValidationService validationService;

    public CspSubmissionController(SubmissionValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public ResponseEntity<SubmissionValidationResponse> validateStructural(MultipartFile file) {
        return handle("structural", file, validationService::validateStructural);
    }

    @Override
    public ResponseEntity<SubmissionValidationResponse> validateBusiness(MultipartFile file) {
        return handle("business", file, validationService::validateBusiness);
    }

    /**
     * Shared intake: validate the file part, read its bytes, run the given
     * validation phase, and map the result onto the response envelope.
     */
    private ResponseEntity<SubmissionValidationResponse> handle(
            String phase, MultipartFile file, Function<byte[], SubmissionValidationResult> validator) {
        // Do not log user-controlled values (e.g. the original filename) — they
        // can carry forged/injected log content. Log only safe, derived facts.
        log.info("POST /api/submissions/validate/{} received file part: present={} size={}",
                phase,
                file != null && !file.isEmpty(),
                file == null ? 0 : file.getSize());

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    error("UPLOAD_MISSING", "file part 'file' is missing or empty"));
        }

        byte[] xml;
        try {
            xml = file.getBytes();
        } catch (IOException e) {
            log.warn("Could not read uploaded submission file: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    error("UPLOAD_UNREADABLE", "uploaded file could not be read"));
        }

        SubmissionValidationResult result = validator.apply(xml);
        HttpStatus status = result.valid() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(toResponse(result));
    }

    /**
     * Maps a {@link SubmissionValidationResult} onto the app-consistent response
     * envelope. Each message's machine code lands in {@code messageKey}, its
     * locator (when present) in {@code args}, its severity in {@code type}, and
     * its text in {@code message}.
     */
    private SubmissionValidationResponse toResponse(SubmissionValidationResult result) {
        // Include all messages either way: a valid result may still carry
        // non-blocking warnings from the business phase.
        List<ValidationMessageResponse> messages = result.errors().stream()
                .map(this::toMessageResponse)
                .toList();
        if (result.valid()) {
            return new SubmissionValidationResponse(true, "OK", "Submission is valid", messages);
        }
        return new SubmissionValidationResponse(
                false, "VALIDATION_ERROR", "Submission failed validation", messages);
    }

    private ValidationMessageResponse toMessageResponse(SubmissionValidationError err) {
        Object[] args = err.path() == null ? null : new Object[]{err.path()};
        // Carry the message's own severity (ERROR / WARNING) through to the response.
        String type = err.severity() == null ? MessageType.ERROR.name() : err.severity().name();
        return new ValidationMessageResponse(err.code(), args, type, err.message());
    }

    private SubmissionValidationResponse error(String code, String message) {
        return new SubmissionValidationResponse(false, code, message,
                List.of(new ValidationMessageResponse(code, null, MessageType.ERROR.name(), message)));
    }
}
