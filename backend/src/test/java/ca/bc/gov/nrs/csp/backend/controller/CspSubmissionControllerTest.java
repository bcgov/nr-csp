package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionValidationResponse;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.submission.SubmissionValidationService;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationResult;
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
import java.util.List;

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

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CspSubmissionController(validationService))
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
        SubmissionValidationResult failed = SubmissionValidationResult.failed(List.of(
                SubmissionValidationError.of("line 5, col 30", "XSD",
                        "cvc-enumeration-valid: Value 'MAYBE' is not facet-valid")));
        given(validationService.validateStructural(any())).willReturn(failed);

        mockMvc.perform(multipart("/api/submissions/validate/structural")
                        .file(file("bad.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].messageKey").value("XSD"))
                .andExpect(jsonPath("$.errors[0].type").value("ERROR"))
                .andExpect(jsonPath("$.errors[0].args[0]").value("line 5, col 30"));
    }

    @Test
    void structural_errorWithNullPath_mapsToMessageWithoutLocationArgs() throws Exception {
        SubmissionValidationResult failed = SubmissionValidationResult.failed(List.of(
                SubmissionValidationError.of("FORMAT_UNRECOGNIZED", "could not detect format")));
        given(validationService.validateStructural(any())).willReturn(failed);

        mockMvc.perform(multipart("/api/submissions/validate/structural")
                        .file(file("x.txt", "{".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].messageKey").value("FORMAT_UNRECOGNIZED"))
                .andExpect(jsonPath("$.errors[0].type").value("ERROR"));
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
                new CspSubmissionController(validationService).validateStructural(file);

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
                        "invoiceDate for invoiceNumber INV-1 cannot be in the future.")));
        given(validationService.validateBusiness(any())).willReturn(failed);

        mockMvc.perform(multipart("/api/submissions/validate/business")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].messageKey").value("invoice.date.in.future.error"))
                .andExpect(jsonPath("$.errors[0].type").value("ERROR"))
                .andExpect(jsonPath("$.errors[0].args[0]").value("invoice INV-1"));
    }

    @Test
    void business_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/submissions/validate/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD_MISSING"));
    }
}
