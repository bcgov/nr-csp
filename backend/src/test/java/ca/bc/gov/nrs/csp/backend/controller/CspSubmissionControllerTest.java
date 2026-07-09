package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionValidationResponse;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.invoice.submission.SubmissionValidationService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionAcceptance;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CspSubmissionControllerTest {

    @Mock SubmissionValidationService validationService;

    // Real (in-memory) bundle so args-carrying messages resolve like production.
    StaticMessageSource messageSource = new StaticMessageSource();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CspSubmissionController(validationService, messageSource))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .build();
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "text/xml", content);
    }

    // ---------- structural endpoint ----------

    @Test
    void structural_valid_returns200() throws Exception {
        given(validationService.validateStructural(any())).willReturn(SubmissionValidationResult.ok());

        mockMvc.perform(multipart("/api/submissions/validate/structural")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void structural_invalid_returns422WithErrors() throws Exception {
        // Structural errors follow the same (code, args) + bundle format as business
        // ones (refactor doc §3.5 Step B): the parser detail rides in args and the
        // resolved message is locator-prefixed.
        messageSource.addMessage("XSD", Locale.getDefault(), "{0}");
        SubmissionValidationResult failed = SubmissionValidationResult.failed(List.of(
                SubmissionValidationError.of("line 5, col 30", "XSD",
                        new Object[]{"cvc-enumeration-valid: Value 'MAYBE' is not facet-valid"})));
        given(validationService.validateStructural(any())).willReturn(failed);

        mockMvc.perform(multipart("/api/submissions/validate/structural")
                        .file(file("bad.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].messageKey").value("XSD"))
                .andExpect(jsonPath("$.errors[0].type").value("ERROR"))
                .andExpect(jsonPath("$.errors[0].args[0]")
                        .value("cvc-enumeration-valid: Value 'MAYBE' is not facet-valid"))
                .andExpect(jsonPath("$.errors[0].message").value(
                        "line 5, col 30: cvc-enumeration-valid: Value 'MAYBE' is not facet-valid"));
    }

    @Test
    void structural_errorWithNullPath_mapsToMessageWithoutLocationArgs() throws Exception {
        messageSource.addMessage("FORMAT_UNRECOGNIZED", Locale.getDefault(),
                "could not detect format");
        SubmissionValidationResult failed = SubmissionValidationResult.failed(List.of(
                SubmissionValidationError.of("FORMAT_UNRECOGNIZED", new Object[0])));
        given(validationService.validateStructural(any())).willReturn(failed);

        mockMvc.perform(multipart("/api/submissions/validate/structural")
                        .file(file("x.txt", "{".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].messageKey").value("FORMAT_UNRECOGNIZED"))
                .andExpect(jsonPath("$.errors[0].type").value("ERROR"))
                .andExpect(jsonPath("$.errors[0].message").value("could not detect format"));
    }

    @Test
    void structural_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/submissions/validate/structural"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.code").value("UPLOAD_MISSING"));
    }

    @Test
    void structural_unreadableFile_returns400Unreadable() throws Exception {
        // A present-but-unreadable upload: getBytes() throws. Built directly so
        // we can force the IOException the controller's catch block handles.
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getBytes()).willThrow(new IOException("boom"));

        ResponseEntity<SubmissionValidationResponse> resp =
                new CspSubmissionController(validationService, messageSource).validateStructural(file);

        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().valid()).isFalse();
        assertThat(resp.getBody().code()).isEqualTo("UPLOAD_UNREADABLE");
    }

    // ---------- business endpoint ----------

    @Test
    void business_valid_returns200() throws Exception {
        given(validationService.validateBusiness(any())).willReturn(SubmissionValidationResult.ok());

        mockMvc.perform(multipart("/api/submissions/validate/business")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void business_invalid_returns422WithErrors() throws Exception {
        SubmissionValidationResult failed = SubmissionValidationResult.failed(List.of(
                SubmissionValidationError.error("invoice INV-1", "invoice.date.in.future.error",
                        new Object[]{"INV-1", LocalDate.of(2026, 1, 1)})));
        given(validationService.validateBusiness(any())).willReturn(failed);

        mockMvc.perform(multipart("/api/submissions/validate/business")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].messageKey").value("invoice.date.in.future.error"))
                .andExpect(jsonPath("$.errors[0].type").value("ERROR"))
                .andExpect(jsonPath("$.errors[0].args[0]").value("INV-1"));
    }

    @Test
    void business_argsCarryingMessage_resolvesTextFromBundleAndKeepsTemplateArgs() throws Exception {
        // A rule on the messages.properties strategy (refactor doc §3.5) emits
        // code + template args; the controller resolves the text and prefixes the
        // invoice locator so multi-invoice submissions stay attributable.
        messageSource.addMessage("invoice.totalamount.dismatch.warning", Locale.getDefault(),
                "The Total Amount of {0} does not match with the calculated total amount.");
        SubmissionValidationResult failed = SubmissionValidationResult.failed(List.of(
                SubmissionValidationError.warning("invoice INV-1",
                        "invoice.totalamount.dismatch.warning", new Object[]{new BigDecimal("90.00")})));
        given(validationService.validateBusiness(any())).willReturn(failed);

        mockMvc.perform(multipart("/api/submissions/validate/business")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].messageKey").value("invoice.totalamount.dismatch.warning"))
                .andExpect(jsonPath("$.errors[0].type").value("WARNING"))
                .andExpect(jsonPath("$.errors[0].args[0]").value(90.00))
                .andExpect(jsonPath("$.errors[0].message").value(
                        "invoice INV-1: The Total Amount of 90 does not match with the calculated total amount."));
    }

    @Test
    void business_argsCarryingMessage_fallsBackToTheKeyWhenNoBundleEntry() throws Exception {
        // No template registered for the key → the resolved text falls back to the
        // bare key (locator-prefixed) instead of throwing.
        SubmissionValidationResult failed = SubmissionValidationResult.failed(List.of(
                SubmissionValidationError.error("invoice INV-1",
                        "invoice.totalamount.negative.error", new Object[0])));
        given(validationService.validateBusiness(any())).willReturn(failed);

        mockMvc.perform(multipart("/api/submissions/validate/business")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].messageKey").value("invoice.totalamount.negative.error"))
                .andExpect(jsonPath("$.errors[0].message").value(
                        "invoice INV-1: invoice.totalamount.negative.error"));
    }

    @Test
    void business_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/submissions/validate/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD_MISSING"));
    }

    @Test
    void business_partialAcceptance_is200ButNotValidAndSurfacesRejected() throws Exception {
        // One invoice accepted, one rejected for an ERROR. Must NOT read as a clean
        // accept: valid=false, code=PARTIALLY_ACCEPTED, with both invoice lists exposed.
        SubmissionValidationResult partial = new SubmissionValidationResult(
                true,
                List.of(SubmissionValidationError.error("invoice INV-BAD",
                        "invoice.date.in.future.error",
                        new Object[]{"INV-BAD", LocalDate.of(2026, 1, 1)})),
                new SubmissionAcceptance(List.of("INV-GOOD"), List.of("INV-BAD")));
        given(validationService.validateBusiness(any())).willReturn(partial);

        mockMvc.perform(multipart("/api/submissions/validate/business")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.code").value("PARTIALLY_ACCEPTED"))
                .andExpect(jsonPath("$.acceptedInvoices[0]").value("INV-GOOD"))
                .andExpect(jsonPath("$.rejectedInvoices[0]").value("INV-BAD"))
                .andExpect(jsonPath("$.errors[0].messageKey").value("invoice.date.in.future.error"));
    }
}
