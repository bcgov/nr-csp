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

/**
 * CSP XML submission intake endpoint. Read-only: it parses and validates
 * the uploaded XML (format → ESF envelope → XSD schema) but persists
 * nothing. Returns 200 with {@code valid: true} when the submission
 * passes, 422 with the collected errors when it fails, and 400 when the
 * file part is missing or unreadable.
 */
@RestController
public class CspSubmissionController implements CspSubmissionApi {

    private static final Logger log = LoggerFactory.getLogger(CspSubmissionController.class);

    private final SubmissionValidationService validationService;

    public CspSubmissionController(SubmissionValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public ResponseEntity<SubmissionValidationResponse> validate(MultipartFile file) {
        // Do not log user-controlled values (e.g. the original filename) — they
        // can carry forged/injected log content. Log only safe, derived facts.
        log.info("POST /api/submissions/validate received file part: present={} size={}",
                file != null && !file.isEmpty(),
                file == null ? 0 : file.getSize());

        if (file == null || file.isEmpty()) {
            SubmissionValidationResponse body = new SubmissionValidationResponse(
                    false, "UPLOAD_MISSING", "file part 'file' is missing or empty",
                    List.of(new ValidationMessageResponse(
                            "UPLOAD_MISSING", null, MessageType.ERROR.name(),
                            "file part 'file' is missing or empty")));
            return ResponseEntity.badRequest().body(body);
        }

        byte[] xml;
        try {
            xml = file.getBytes();
        } catch (IOException e) {
            log.warn("Could not read uploaded submission file: {}", e.getMessage());
            SubmissionValidationResponse body = new SubmissionValidationResponse(
                    false, "UPLOAD_UNREADABLE", "uploaded file could not be read",
                    List.of(new ValidationMessageResponse(
                            "UPLOAD_UNREADABLE", null, MessageType.ERROR.name(),
                            "uploaded file could not be read")));
            return ResponseEntity.badRequest().body(body);
        }

        SubmissionValidationResult result = validationService.validate(xml);
        SubmissionValidationResponse body = toResponse(result);
        HttpStatus status = result.valid() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Maps the pipeline's {@link SubmissionValidationResult} onto the
     * app-consistent response envelope. The parser's machine code lands
     * in {@code messageKey}, the source location (when present) in
     * {@code args}, and the parser message in {@code message}.
     */
    private SubmissionValidationResponse toResponse(SubmissionValidationResult result) {
        if (result.valid()) {
            return new SubmissionValidationResponse(true, "OK", "Submission is valid", List.of());
        }
        List<ValidationMessageResponse> errors = result.errors().stream()
                .map(this::toMessageResponse)
                .toList();
        return new SubmissionValidationResponse(
                false, "VALIDATION_ERROR", "Submission failed validation", errors);
    }

    private ValidationMessageResponse toMessageResponse(SubmissionValidationError err) {
        Object[] args = err.path() == null ? null : new Object[]{err.path()};
        return new ValidationMessageResponse(
                err.code(), args, MessageType.ERROR.name(), err.message());
    }
}
