package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.CspSubmissionApi;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationMessageResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionParseResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionSubmitResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionValidationResponse;
import ca.bc.gov.nrs.csp.backend.invoice.submission.SubmissionValidationService;
import ca.bc.gov.nrs.csp.backend.service.CspSubmissionPersistenceService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmitterType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationResult;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.StructuralValidationService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.parser.SubmissionEnvelopeStripper;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 *   <li>{@code POST /api/submissions/parse} — structural validation plus the
 *       parsed submission content used to populate the upload form.</li>
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
    private final SubmissionEnvelopeStripper envelopeStripper;
    private final CspSubmissionPersistenceService persistenceService;
    private final MessageSource messageSource;

    public CspSubmissionController(SubmissionValidationService validationService,
            SubmissionEnvelopeStripper envelopeStripper,
            CspSubmissionPersistenceService persistenceService, MessageSource messageSource) {
        this.validationService = validationService;
        this.envelopeStripper = envelopeStripper;
        this.persistenceService = persistenceService;
        this.messageSource = messageSource;
    }

    @Override
    public ResponseEntity<SubmissionValidationResponse> validateStructural(MultipartFile file) {
        return handle("structural", file, validationService::validateStructural);
    }

    @Override
    public ResponseEntity<SubmissionValidationResponse> validateBusiness(MultipartFile file) {
        return handle("business", file, validationService::validateBusiness);
    }

    @Override
    public ResponseEntity<SubmissionParseResponse> parse(MultipartFile file) {
        log.info("POST /api/submissions/parse received file part: present={} size={}",
                file != null && !file.isEmpty(),
                file == null ? 0 : file.getSize());

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    parseError("UPLOAD_MISSING", "file part 'file' is missing or empty"));
        }

        byte[] xml;
        try {
            xml = file.getBytes();
        } catch (IOException e) {
            log.warn("Could not read uploaded submission file: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    parseError("UPLOAD_UNREADABLE", "uploaded file could not be read"));
        }

        StructuralValidationService.ValidationOutcome outcome = validationService.parse(xml);
        SubmissionValidationResult result = outcome.result();

        // Structural failure (or nothing parsed): return the structural errors and no content.
        if (!result.valid() || outcome.submission() == null) {
            List<ValidationMessageResponse> messages = result.errors().stream()
                    .map(this::toMessageResponse)
                    .toList();
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                    new SubmissionParseResponse(false, "VALIDATION_ERROR",
                            "Submission failed structural validation", messages, null));
        }

        // Email/telephone live only in the ESF envelope (absent for a bare CSP body).
        SubmissionEnvelopeStripper.SubmissionMetadata metadata = envelopeStripper.extractMetadata(xml);
        SubmissionParseResponse.ParsedSubmission content =
                toParsedSubmission((CSPSubmissionType) outcome.submission(), metadata);
        return ResponseEntity.ok(new SubmissionParseResponse(
                true, "OK", "Submission parsed", List.of(), content));
    }

    @Override
    public ResponseEntity<SubmissionSubmitResponse> submit(MultipartFile file) {
        log.info("POST /api/submissions/submit received file part: present={} size={}",
                file != null && !file.isEmpty(),
                file == null ? 0 : file.getSize());

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    submitError("UPLOAD_MISSING", "file part 'file' is missing or empty"));
        }

        byte[] xml;
        try {
            xml = file.getBytes();
        } catch (IOException e) {
            log.warn("Could not read uploaded submission file: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    submitError("UPLOAD_UNREADABLE", "uploaded file could not be read"));
        }

        // Gate on business validation: only a fully accepted submission (no rejected
        // invoices) is persisted. A "partial acceptance" reports result.valid()==true
        // but still carries rejected invoices, so block on either condition.
        SubmissionValidationResult result = validationService.validateBusiness(xml);
        List<String> rejected = result.acceptance().rejected();
        if (!result.valid() || !rejected.isEmpty()) {
            List<ValidationMessageResponse> messages = result.errors().stream()
                    .map(this::toMessageResponse)
                    .toList();
            String code = rejected.isEmpty() ? "VALIDATION_ERROR" : "PARTIALLY_ACCEPTED";
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                    new SubmissionSubmitResponse(false, code,
                            "Submission cannot be saved until all issues are resolved.",
                            null, result.acceptance().accepted(), rejected, messages));
        }

        Long submissionId = persistenceService.persist(xml);
        return ResponseEntity.ok(new SubmissionSubmitResponse(
                true, "OK", "Submission saved.", submissionId,
                result.acceptance().accepted(), List.of(), List.of()));
    }

    /**
     * Maps the parsed JAXB tree onto the flat, form-friendly DTO. Invoices and
     * line items are flattened into parallel lists; each row carries its 1-based
     * positional index so the frontend can attach business-validation errors to
     * the exact invoice/line the locators reference.
     */
    private SubmissionParseResponse.ParsedSubmission toParsedSubmission(
            CSPSubmissionType submission, SubmissionEnvelopeStripper.SubmissionMetadata metadata) {
        CSPSubmitterType submitter = submission.getCSPSubmitter();

        List<SubmissionParseResponse.ParsedInvoice> invoices = new ArrayList<>();
        List<SubmissionParseResponse.ParsedLineItem> lineItems = new ArrayList<>();

        List<CSPInvoiceType> parsedInvoices = submission.getCSPInvoice();
        for (int i = 0; i < parsedInvoices.size(); i++) {
            CSPInvoiceType invoice = parsedInvoices.get(i);
            int invoiceIndex = i + 1;
            CSPInvoiceDetailsType details = invoice.getCSPInvoiceDetails();

            invoices.add(new SubmissionParseResponse.ParsedInvoice(
                    invoiceIndex,
                    invoice.getInvoiceNumber(),
                    formatDate(invoice.getInvoiceDate()),
                    invoice.getInvoiceType(),
                    invoice.getSellerClientNumber(),
                    invoice.getBuyerClientNumber(),
                    details == null ? null : details.getMaturity(),
                    details == null ? null : details.getLocationFOB(),
                    details == null ? null : details.getTotalAmount(),
                    details == null ? null : details.getTotalVolume(),
                    details == null ? null : details.getTotalPieces()));

            List<CSPLineItemType> lines = invoice.getCSPLineItem();
            for (int j = 0; j < lines.size(); j++) {
                CSPLineItemType line = lines.get(j);
                lineItems.add(new SubmissionParseResponse.ParsedLineItem(
                        invoiceIndex,
                        j + 1,
                        invoice.getInvoiceNumber(),
                        line.getSpecies(),
                        line.getGrade(),
                        line.getSecondarySortCode(),
                        line.getClientSecondarySortCode(),
                        line.getNumberOfPieces(),
                        line.getVolume(),
                        line.getPrice()));
            }
        }

        return new SubmissionParseResponse.ParsedSubmission(
                metadata.email(),
                metadata.telephone(),
                submission.getMonthComplete(),
                submitter == null || submitter.getSellerSubmission() == null
                        ? null : submitter.getSellerSubmission().value(),
                submitter == null ? null : submitter.getSubmissionClientNumber(),
                submitter == null ? null : submitter.getSubmissionClientLocnCode(),
                invoices,
                lineItems);
    }

    /** Renders an XSD date as ISO {@code yyyy-MM-dd}, or null when absent. */
    private static String formatDate(XMLGregorianCalendar date) {
        if (date == null) return null;
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonth(), date.getDay());
    }

    private SubmissionParseResponse parseError(String code, String message) {
        return new SubmissionParseResponse(false, code, message,
                List.of(new ValidationMessageResponse(code, null, MessageType.ERROR.name(), message)),
                null);
    }

    private SubmissionSubmitResponse submitError(String code, String message) {
        return new SubmissionSubmitResponse(false, code, message, null, List.of(), List.of(),
                List.of(new ValidationMessageResponse(code, null, MessageType.ERROR.name(), message)));
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
        // Include all messages either way: a valid/partial result still carries the
        // rejected invoices' ERRORs (and any WARNINGs).
        List<ValidationMessageResponse> messages = result.errors().stream()
                .map(this::toMessageResponse)
                .toList();
        List<String> accepted = result.acceptance().accepted();
        List<String> rejected = result.acceptance().rejected();

        if (!result.valid()) {
            // Rejected outright: a submission-level error, or no invoice was accepted.
            return new SubmissionValidationResponse(
                    false, "VALIDATION_ERROR", "Submission failed validation",
                    accepted, rejected, messages);
        }
        if (!rejected.isEmpty()) {
            // Accepted, but some invoices were rejected — must NOT read as a clean accept.
            // valid=false (so callers keying on valid notice), but HTTP stays 200 since the
            // submission was processed and the accepted invoices proceed.
            return new SubmissionValidationResponse(
                    false, "PARTIALLY_ACCEPTED",
                    "Submission partially accepted: " + rejected.size()
                            + " invoice(s) rejected and must be corrected and resubmitted",
                    accepted, rejected, messages);
        }
        // Fully accepted.
        return new SubmissionValidationResponse(
                true, "OK", "Submission is valid", accepted, rejected, messages);
    }

    private ValidationMessageResponse toMessageResponse(SubmissionValidationError err) {
        // Uniform format (refactor doc §3.5 Step C): every message is a key + its
        // template args. Resolve against messages.properties, prefixed with the
        // locator so multi-invoice submissions stay attributable; fall back to the
        // bare key when the bundle has no entry.
        String type = err.severity() == null ? MessageType.ERROR.name() : err.severity().name();
        String text = messageSource.getMessage(err.code(), err.args(), err.code(), Locale.getDefault());
        String message = err.path() == null ? text : err.path() + ": " + text;
        return new ValidationMessageResponse(err.code(), err.args(), type, message);
    }

    private SubmissionValidationResponse error(String code, String message) {
        return new SubmissionValidationResponse(false, code, message,
                List.of(), List.of(),
                List.of(new ValidationMessageResponse(code, null, MessageType.ERROR.name(), message)));
    }
}
