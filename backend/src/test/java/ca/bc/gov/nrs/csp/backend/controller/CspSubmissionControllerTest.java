package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionValidationResponse;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.invoice.submission.SubmissionValidationService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmitterType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.SellerSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionAcceptance;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationResult;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.StructuralValidationService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.SubmissionValidationProperties;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.parser.SubmissionEnvelopeStripper;
import ca.bc.gov.nrs.csp.backend.service.CspSubmissionPersistenceService;
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

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CspSubmissionControllerTest {

    @Mock SubmissionValidationService validationService;

    // Real (in-memory) bundle so args-carrying messages resolve like production.
    StaticMessageSource messageSource = new StaticMessageSource();

    // Real stripper (default CSP/ESF config) so envelope metadata extraction runs as in production.
    SubmissionEnvelopeStripper envelopeStripper = new SubmissionEnvelopeStripper(new SubmissionValidationProperties());

    @Mock CspSubmissionPersistenceService persistenceService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CspSubmissionController(
                        validationService, envelopeStripper, persistenceService, messageSource))
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
                new CspSubmissionController(validationService, envelopeStripper, persistenceService, messageSource)
                        .validateStructural(file);

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
                        new Object[]{"INV-1", LocalDate.of(2026, Month.JANUARY, 1)})));
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
                        new Object[]{"INV-BAD", LocalDate.of(2026, Month.JANUARY, 1)})),
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

    // ---------- parse endpoint ----------

    /** A minimal but complete parsed tree: one submitter, one invoice, one line item. */
    private static CSPSubmissionType sampleSubmission() throws Exception {
        CSPSubmissionType submission = new CSPSubmissionType();
        submission.setMonthComplete("Y");

        CSPSubmitterType submitter = new CSPSubmitterType();
        submitter.setSellerSubmission(SellerSubmissionType.Y);
        submitter.setSubmissionClientNumber("00012345");
        submitter.setSubmissionClientLocnCode("00");
        submission.setCSPSubmitter(submitter);

        CSPInvoiceType invoice = new CSPInvoiceType();
        invoice.setInvoiceNumber("INV-1");
        invoice.setInvoiceDate(DatatypeFactory.newInstance()
                .newXMLGregorianCalendarDate(2026, 1, 15, DatatypeConstants.FIELD_UNDEFINED));
        invoice.setInvoiceType("SA");
        invoice.setSellerClientNumber("00012345");
        invoice.setBuyerClientNumber("00067890");

        CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
        details.setMaturity("M");
        details.setLocationFOB("FOB1");
        details.setTotalAmount(new BigDecimal("100.00"));
        details.setTotalVolume(new BigDecimal("12.345"));
        details.setTotalPieces(7);
        invoice.setCSPInvoiceDetails(details);

        CSPLineItemType line = new CSPLineItemType();
        line.setSpecies("FIR");
        line.setGrade("1");
        line.setSecondarySortCode("SC1");
        line.setClientSecondarySortCode("CSC1");
        line.setNumberOfPieces(3);
        line.setVolume(new BigDecimal("4.560"));
        line.setPrice(new BigDecimal("50.00"));
        invoice.getCSPLineItem().add(line);

        submission.getCSPInvoice().add(invoice);
        return submission;
    }

    @Test
    void parse_valid_returns200WithFlattenedContent() throws Exception {
        given(validationService.parse(any())).willReturn(
                new StructuralValidationService.ValidationOutcome(
                        SubmissionValidationResult.ok(), sampleSubmission()));

        mockMvc.perform(multipart("/api/submissions/parse")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.submission.monthComplete").value("Y"))
                .andExpect(jsonPath("$.submission.sellerSubmission").value("Y"))
                .andExpect(jsonPath("$.submission.submissionClientNumber").value("00012345"))
                .andExpect(jsonPath("$.submission.submissionClientLocnCode").value("00"))
                .andExpect(jsonPath("$.submission.invoices[0].index").value(1))
                .andExpect(jsonPath("$.submission.invoices[0].invoiceNumber").value("INV-1"))
                .andExpect(jsonPath("$.submission.invoices[0].invoiceDate").value("2026-01-15"))
                .andExpect(jsonPath("$.submission.invoices[0].invoiceType").value("SA"))
                .andExpect(jsonPath("$.submission.invoices[0].locationFOB").value("FOB1"))
                .andExpect(jsonPath("$.submission.invoices[0].totalPieces").value(7))
                .andExpect(jsonPath("$.submission.lineItems[0].invoiceIndex").value(1))
                .andExpect(jsonPath("$.submission.lineItems[0].lineIndex").value(1))
                .andExpect(jsonPath("$.submission.lineItems[0].invoiceNumber").value("INV-1"))
                .andExpect(jsonPath("$.submission.lineItems[0].species").value("FIR"))
                .andExpect(jsonPath("$.submission.lineItems[0].secondarySortCode").value("SC1"));
    }

    @Test
    void parse_esfEnvelope_extractsEmailAndTelephoneFromMetadata() throws Exception {
        given(validationService.parse(any())).willReturn(
                new StructuralValidationService.ValidationOutcome(
                        SubmissionValidationResult.ok(), sampleSubmission()));

        // Email/telephone are carried by the ESF envelope, not the CSP body; the leading
        // "mailto:" is stripped. extractMetadata runs on the real bytes provided here.
        String esf = """
                <esf:ESFSubmission xmlns:esf="http://www.for.gov.bc.ca/schema/esf">
                  <esf:submissionMetadata>
                    <esf:emailAddress>mailto:jane.doe@example.com</esf:emailAddress>
                    <esf:telephoneNumber>2503878363</esf:telephoneNumber>
                  </esf:submissionMetadata>
                </esf:ESFSubmission>""";

        mockMvc.perform(multipart("/api/submissions/parse")
                        .file(file("submission.xml", esf.getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submission.email").value("jane.doe@example.com"))
                .andExpect(jsonPath("$.submission.telephone").value("2503878363"));
    }

    @Test
    void parse_structuralInvalid_returns422WithErrors() throws Exception {
        messageSource.addMessage("FORMAT_UNRECOGNIZED", Locale.getDefault(), "could not detect format");
        SubmissionValidationResult failed = SubmissionValidationResult.failed(List.of(
                SubmissionValidationError.of("FORMAT_UNRECOGNIZED", new Object[0])));
        given(validationService.parse(any())).willReturn(
                new StructuralValidationService.ValidationOutcome(failed, null));

        mockMvc.perform(multipart("/api/submissions/parse")
                        .file(file("x.txt", "{".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].messageKey").value("FORMAT_UNRECOGNIZED"))
                .andExpect(jsonPath("$.errors[0].message").value("could not detect format"));
    }

    @Test
    void parse_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/submissions/parse"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.code").value("UPLOAD_MISSING"));
    }

    // ---------- submit endpoint ----------

    @Test
    void submit_valid_persistsAndReturnsSubmissionId() throws Exception {
        given(validationService.validateBusiness(any())).willReturn(SubmissionValidationResult.ok());
        given(persistenceService.persist(any())).willReturn(98765L);

        mockMvc.perform(multipart("/api/submissions/submit")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.submissionId").value(98765));

        verify(persistenceService).persist(any());
    }

    @Test
    void submit_invalid_returns422AndDoesNotPersist() throws Exception {
        SubmissionValidationResult failed = SubmissionValidationResult.failed(List.of(
                SubmissionValidationError.error("invoice #1 (INV-1)", "invoice.fob.required.error", new Object[0])));
        given(validationService.validateBusiness(any())).willReturn(failed);

        mockMvc.perform(multipart("/api/submissions/submit")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.submissionId").value(nullValue()))
                .andExpect(jsonPath("$.errors[0].messageKey").value("invoice.fob.required.error"));

        verify(persistenceService, never()).persist(any());
    }

    @Test
    void submit_partialAcceptance_returns422NotSaved() throws Exception {
        SubmissionValidationResult partial = new SubmissionValidationResult(
                true,
                List.of(SubmissionValidationError.error("invoice #2 (INV-BAD)",
                        "invoice.fob.required.error", new Object[0])),
                new SubmissionAcceptance(List.of("INV-GOOD"), List.of("INV-BAD")));
        given(validationService.validateBusiness(any())).willReturn(partial);

        mockMvc.perform(multipart("/api/submissions/submit")
                        .file(file("submission.xml", "<csp:CSPSubmission/>".getBytes())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.code").value("PARTIALLY_ACCEPTED"))
                .andExpect(jsonPath("$.rejectedInvoices[0]").value("INV-BAD"));

        verify(persistenceService, never()).persist(any());
    }

    @Test
    void submit_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/submissions/submit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD_MISSING"));

        verify(persistenceService, never()).persist(any());
    }
}
