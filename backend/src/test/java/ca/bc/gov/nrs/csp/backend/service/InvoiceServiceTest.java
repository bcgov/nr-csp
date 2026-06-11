package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ChangeStatusRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.CreateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceSummaryExportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceSummaryExportRequest.GroupSummaryExportRow;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceSummaryExportRequest.LineItemExportRow;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItemRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.UpdateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ConflictException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository.LoadedInvoice;
import ca.bc.gov.nrs.csp.backend.repository.LineItemRepository;
import ca.bc.gov.nrs.csp.backend.repository.LogSaleParticipantRepository;
import ca.bc.gov.nrs.csp.backend.service.mapper.InvoiceMapper;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.util.constants.ActionType;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.invoiceDetails.InvoiceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link InvoiceService}. The service is wrapped in a Mockito spy
 * so its package-private {@code newValidator()} factory can be stubbed to return
 * a mocked {@link InvoiceValidator} — this lets every test drive validation
 * pass/fail deterministically and focus on the service's orchestration of the
 * repositories, the price-conversion service, and the mapper.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepo;
    @Mock LineItemRepository lineItemRepo;
    @Mock CspSubmissionRepository submissionRepo;
    @Mock LogSaleParticipantRepository participantRepo;
    @Mock CommonValidation commonValidation;
    @Mock PriceConversionService priceConversionService;
    @Mock InvoiceMapper mapper;

    InvoiceService service;
    InvoiceValidator validator;

    private static final InvoiceResponse SENTINEL = new InvoiceResponse(
            1L, 10L, "INV-001", LocalDate.of(2026, 1, 15), "DFT", "SAL", "M", null, null,
            null, null, null, "1234", "00", "Seller", null, null, null, null, null, null, null,
            List.of(), List.of(), List.of(), null, null, null, null, "system",
            List.of(), List.of(), List.of());

    private static final ValidationResult VALID = new ValidationResult(List.of());
    private static final ValidationResult WITH_ERROR =
            new ValidationResult(List.of(new ValidationMessage("some.error", null, MessageType.ERROR)));
    private static final ValidationMessage A_WARNING =
            new ValidationMessage("a.warning", null, MessageType.WARNING);

    private static final String USER = "system";

    @BeforeEach
    void setUp() {
        service = spy(new InvoiceService(invoiceRepo, lineItemRepo, submissionRepo,
                participantRepo, commonValidation, priceConversionService, mapper));
        validator = mock(InvoiceValidator.class);
        lenient().doReturn(validator).when(service).newValidator();

        lenient().when(validator.validate(any(), any(), anyBoolean(), any())).thenReturn(VALID);
        lenient().when(validator.validateForChangeStatus(any(), any(), any())).thenReturn(VALID);
        lenient().when(priceConversionService.apply(any(), any(), any()))
                .thenReturn(new PriceConversionService.Result(List.of(), List.of()));
        lenient().when(mapper.toResponse(any(), any(), any(), any())).thenReturn(SENTINEL);
        lenient().when(lineItemRepo.findByInvoiceId(anyLong())).thenReturn(List.of());
        lenient().when(lineItemRepo.findIdsByInvoiceId(anyLong())).thenReturn(List.of());
    }

    // ---------------- helpers ----------------

    private InvoiceDetails details(Long id, String status, String otherClientNum,
                                   String otherClientName, String submittedBy) {
        return new InvoiceDetails(
                id, "INV-001", LocalDate.of(2026, 1, 15), status, "SAL", "M", "FOB01", "SORT01",
                new BigDecimal("100.00"), 10, new BigDecimal("5.0"),
                "1234", "00", submittedBy, "1234", "00",
                otherClientNum, "00", otherClientName, "Nanaimo", "BC",
                List.of("B1"), List.of("T1"), List.of("W1"),
                null, null, "comments", null, "entryUser");
    }

    private InvoiceDetails draftDetails(Long id) {
        return details(id, "DFT", "5678", null, "Seller");
    }

    private LoadedInvoice loaded(InvoiceDetails d, Long submissionId, Long buyerPid, Long sellerPid) {
        return new LoadedInvoice(d, submissionId, buyerPid, sellerPid);
    }

    private LineItem line(Long id, BigDecimal converted) {
        return new LineItem(id, 100L, "SORT01", "CS01", "FIR", "Fir", "G1",
                10, new BigDecimal("10.00"), new BigDecimal("5.0"), converted, new BigDecimal("100.00"));
    }

    // ===============================================================
    // getById
    // ===============================================================

    @Test
    void getById_notFound_throws() {
        given(invoiceRepo.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_processing_mergesValidatorAndConversionWarnings() {
        InvoiceDetails d = details(1L, "PRO", "5678", null, "Seller");
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(d, 10L, null, null)));
        given(lineItemRepo.findByInvoiceId(1L)).willReturn(List.of(line(1L, null)));
        given(validator.validate(any(), any(), anyBoolean(), eq(ActionType.OTHER)))
                .willReturn(new ValidationResult(List.of(A_WARNING)));
        given(priceConversionService.apply(any(), any(), any()))
                .willReturn(new PriceConversionService.Result(List.of(),
                        List.of(new ValidationMessage("conv.warning", null, MessageType.WARNING))));

        service.getById(1L);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(mapper).toResponse(any(), eq(10L), any(), captor.capture());
        // Both the validator warning and the conversion warning must appear.
        assertThat(captor.getValue().warnings()).hasSize(2);
    }

    @Test
    void getById_nonProcessing_skipsConversionCompletely() {
        InvoiceDetails d = draftDetails(1L);
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(d, 10L, null, null)));
        given(lineItemRepo.findByInvoiceId(1L)).willReturn(List.of(line(1L, null)));

        service.getById(1L);

        // Conversion service must not be called at all for non-PROCESSING invoices.
        verify(priceConversionService, never()).apply(any(), any(), any());
        verify(lineItemRepo, never()).updateConvertedPrice(any(), any(), any());
    }

    @Test
    void getById_nonProcessing_doesNotSurfaceConversionWarnings() {
        // Conversion warnings (factor-not-found etc.) must only appear when the
        // invoice is PROCESSING; other statuses return only validator warnings.
        InvoiceDetails d = draftDetails(1L);
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(d, 10L, null, null)));
        given(lineItemRepo.findByInvoiceId(1L)).willReturn(List.of());
        given(validator.validate(any(), any(), anyBoolean(), any()))
                .willReturn(new ValidationResult(List.of(A_WARNING)));

        service.getById(1L);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(mapper).toResponse(any(), any(), any(), captor.capture());
        // Only the validator warning — no conversion warnings injected.
        assertThat(captor.getValue().warnings()).containsExactly(A_WARNING);
    }

    @Test
    void getById_processing_recomputesAndPersistsConvertedPrices() {
        InvoiceDetails d = details(1L, "PRO", "5678", null, "Seller");
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(d, 10L, null, null)));
        given(lineItemRepo.findByInvoiceId(1L)).willReturn(List.of(line(1L, null)));
        LineItem converted = line(1L, new BigDecimal("9.99"));
        given(priceConversionService.apply(any(), any(), any()))
                .willReturn(new PriceConversionService.Result(List.of(converted), List.of()));

        service.getById(1L);

        verify(lineItemRepo).updateConvertedPrice(1L, new BigDecimal("9.99"), USER);
    }

    // ===============================================================
    // create
    // ===============================================================

    private CreateInvoiceRequest createRequest(boolean manual) {
        CreateInvoiceRequest req = mock(CreateInvoiceRequest.class);
        lenient().when(req.manual()).thenReturn(manual);
        lenient().when(req.lineItems()).thenReturn(List.of());
        return req;
    }

    @Test
    void create_validationError_throwsAndPersistsNothing() {
        CreateInvoiceRequest req = createRequest(true);
        given(mapper.toDetails(eq(req), anyString())).willReturn(draftDetails(null));
        given(mapper.toLineItems(any(), isNull())).willReturn(List.of());
        given(validator.validate(any(), any(), anyBoolean(), eq(ActionType.SAVE))).willReturn(WITH_ERROR);

        assertThatThrownBy(() -> service.create(req)).isInstanceOf(ValidationException.class);
        verify(invoiceRepo, never()).insertInvoice(any(), any(), any(), any(), any(), any());
        verify(submissionRepo, never()).insertSubmission(any(), any(), any(), any());
    }

    @Test
    void create_manual_insertsLobbySubmissionAndPersistsEverything() {
        CreateInvoiceRequest req = createRequest(true);
        // Registered other party (otherClientNum set) → no participant insert.
        given(mapper.toDetails(eq(req), anyString())).willReturn(details(null, "DFT", "5678", null, "Seller"));
        given(mapper.toLineItems(any(), isNull())).willReturn(List.of(line(null, null)));
        given(submissionRepo.insertSubmission(any(), any(), eq("LOB"), any())).willReturn(77L);
        given(invoiceRepo.insertInvoice(any(), eq(77L), eq("DFT"), isNull(), isNull(), any())).willReturn(500L);

        service.create(req);

        verify(submissionRepo).insertSubmission(eq("1234"), eq("00"), eq("LOB"), eq(USER));
        verify(invoiceRepo).insertInvoice(any(), eq(77L), eq("DFT"), isNull(), isNull(), eq(USER));
        verify(lineItemRepo).insertLineItem(eq(500L), any(), eq(USER));
        verify(invoiceRepo, times(3)).replaceLogSources(eq(500L), any(), any(), eq(USER));
        verify(invoiceRepo, times(2)).replaceRelatedInvoices(eq(500L), any(), any(), any(), any(), eq(USER));
        verify(participantRepo, never()).insert(any(), any(), any(), any());
    }

    @Test
    void create_nonManual_insertsInboxSubmission() {
        CreateInvoiceRequest req = createRequest(false);
        given(mapper.toDetails(eq(req), anyString())).willReturn(details(null, "DFT", "5678", null, "Seller"));
        given(mapper.toLineItems(any(), isNull())).willReturn(List.of());
        given(submissionRepo.insertSubmission(any(), any(), eq("INB"), any())).willReturn(77L);
        given(invoiceRepo.insertInvoice(any(), any(), any(), any(), any(), any())).willReturn(500L);

        service.create(req);

        verify(submissionRepo).insertSubmission(eq("1234"), eq("00"), eq("INB"), eq(USER));
    }

    @Test
    void create_manualOtherPartyAsBuyer_insertsParticipantIntoBuyerSlot() {
        CreateInvoiceRequest req = createRequest(true);
        // No client number + a name + submittedBy Seller → manual other party in the BUYER slot.
        given(mapper.toDetails(eq(req), anyString())).willReturn(details(null, "DFT", null, "ABC Logging", "Seller"));
        given(mapper.toLineItems(any(), isNull())).willReturn(List.of());
        given(submissionRepo.insertSubmission(any(), any(), any(), any())).willReturn(77L);
        given(participantRepo.insert(eq("ABC Logging"), any(), any(), any())).willReturn(900L);
        given(invoiceRepo.insertInvoice(any(), any(), any(), any(), any(), any())).willReturn(500L);

        service.create(req);

        verify(participantRepo).insert(eq("ABC Logging"), eq("Nanaimo"), eq("BC"), eq(USER));
        verify(invoiceRepo).insertInvoice(any(), eq(77L), eq("DFT"), eq(900L), isNull(), eq(USER));
    }

    @Test
    void create_manualOtherPartyAsSeller_insertsParticipantIntoSellerSlot() {
        CreateInvoiceRequest req = createRequest(true);
        // submittedBy Buyer → the other party is the seller (seller slot).
        given(mapper.toDetails(eq(req), anyString())).willReturn(details(null, "DFT", null, "ABC Logging", "Buyer"));
        given(mapper.toLineItems(any(), isNull())).willReturn(List.of());
        given(submissionRepo.insertSubmission(any(), any(), any(), any())).willReturn(77L);
        given(participantRepo.insert(any(), any(), any(), any())).willReturn(900L);
        given(invoiceRepo.insertInvoice(any(), any(), any(), any(), any(), any())).willReturn(500L);

        service.create(req);

        verify(invoiceRepo).insertInvoice(any(), eq(77L), eq("DFT"), isNull(), eq(900L), eq(USER));
    }

    // ===============================================================
    // update
    // ===============================================================

    private UpdateInvoiceRequest updateRequest() {
        UpdateInvoiceRequest req = mock(UpdateInvoiceRequest.class);
        lenient().when(req.manual()).thenReturn(true);
        lenient().when(req.lineItems()).thenReturn(List.of());
        return req;
    }

    @Test
    void update_notFound_throws() {
        given(invoiceRepo.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(99L, updateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_approvedInvoice_throwsConflict() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "APP", "5678", null, "Seller"), 10L, null, null)));
        assertThatThrownBy(() -> service.update(1L, updateRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_rejectedInvoice_throwsConflict() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "REJ", "5678", null, "Seller"), 10L, null, null)));
        assertThatThrownBy(() -> service.update(1L, updateRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_cancelledInvoice_throwsConflict() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "CAN", "5678", null, "Seller"), 10L, null, null)));
        assertThatThrownBy(() -> service.update(1L, updateRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_persistsConvertedPricesAndSurfacesConversionWarnings() {
        UpdateInvoiceRequest req = updateRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(mapper.toDetails(eq(req), eq(1L), any(), any())).willReturn(draftDetails(1L));
        given(mapper.toLineItems(any(), eq(1L))).willReturn(List.of());
        LineItem converted = line(1L, new BigDecimal("12.34"));
        given(priceConversionService.apply(any(), any(), any()))
                .willReturn(new PriceConversionService.Result(List.of(converted), List.of(A_WARNING)));

        service.update(1L, req);

        verify(lineItemRepo).updateConvertedPrice(1L, new BigDecimal("12.34"), USER);
        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(mapper).toResponse(any(), eq(10L), any(), captor.capture());
        assertThat(captor.getValue().warnings()).contains(A_WARNING);
    }

    @Test
    void update_validationError_throws() {
        UpdateInvoiceRequest req = updateRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(mapper.toDetails(eq(req), eq(1L), any(), any())).willReturn(draftDetails(1L));
        given(mapper.toLineItems(any(), eq(1L))).willReturn(List.of());
        given(validator.validate(any(), any(), anyBoolean(), eq(ActionType.SAVE))).willReturn(WITH_ERROR);

        assertThatThrownBy(() -> service.update(1L, req)).isInstanceOf(ValidationException.class);
        verify(invoiceRepo, never()).updateInvoice(any(), any(), any(), any(), any(), any());
    }

    @Test
    void update_success_reconcilesLinesAndRevertsSubmissionToLobby() {
        UpdateInvoiceRequest req = updateRequest();
        // No manual other party, no existing participant rows.
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(mapper.toDetails(eq(req), eq(1L), any(), any())).willReturn(draftDetails(1L));
        // Incoming: one existing line (id 1, updated) + one new line (null id). Existing on db: 1 and 2.
        given(mapper.toLineItems(any(), eq(1L))).willReturn(List.of(line(1L, null), line(null, null)));
        given(lineItemRepo.findIdsByInvoiceId(1L)).willReturn(List.of(1L, 2L));

        service.update(1L, req);

        verify(invoiceRepo).updateInvoice(eq(1L), any(), eq("DFT"), isNull(), isNull(), eq(USER));
        verify(lineItemRepo).updateLineItem(eq(1L), any(), eq(USER)); // existing line updated
        verify(lineItemRepo).insertLineItem(eq(1L), any(), eq(USER)); // new line inserted
        verify(lineItemRepo).deleteLineItem(2L);                      // dropped line deleted
        verify(submissionRepo).updateSubmissionStatus(10L, "LOB", USER);
    }

    @Test
    void update_noSubmission_doesNotTouchSubmissionStatus() {
        UpdateInvoiceRequest req = updateRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), null, null, null)));
        given(mapper.toDetails(eq(req), eq(1L), any(), any())).willReturn(draftDetails(1L));
        given(mapper.toLineItems(any(), eq(1L))).willReturn(List.of());

        service.update(1L, req);

        verify(submissionRepo, never()).updateSubmissionStatus(any(), any(), any());
    }

    @Test
    void update_registeredOtherParty_clearsBothParticipantSlots() {
        UpdateInvoiceRequest req = updateRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, 50L, 60L)));
        // Registered other party (otherClientNum set, no manual name).
        given(mapper.toDetails(eq(req), eq(1L), any(), any())).willReturn(details(1L, "DFT", "5678", null, "Seller"));
        given(mapper.toLineItems(any(), eq(1L))).willReturn(List.of());

        service.update(1L, req);

        verify(invoiceRepo).updateInvoice(eq(1L), any(), eq("DFT"), isNull(), isNull(), eq(USER));
        verify(participantRepo).delete(50L);
        verify(participantRepo).delete(60L);
    }

    @Test
    void update_manualOtherPartyExistingBuyer_updatesInPlace() {
        UpdateInvoiceRequest req = updateRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, 50L, null)));
        given(mapper.toDetails(eq(req), eq(1L), any(), any())).willReturn(details(1L, "DFT", null, "ABC Logging", "Seller"));
        given(mapper.toLineItems(any(), eq(1L))).willReturn(List.of());

        service.update(1L, req);

        verify(participantRepo).update(eq(50L), eq("ABC Logging"), eq("Nanaimo"), eq("BC"), eq(USER));
        verify(participantRepo, never()).insert(any(), any(), any(), any());
        verify(invoiceRepo).updateInvoice(eq(1L), any(), eq("DFT"), eq(50L), isNull(), eq(USER));
    }

    @Test
    void update_manualOtherPartyMovedSlot_insertsNewAndOrphansOld() {
        UpdateInvoiceRequest req = updateRequest();
        // Existing seller participant 60; new state puts the manual party in the buyer slot.
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, 60L)));
        given(mapper.toDetails(eq(req), eq(1L), any(), any())).willReturn(details(1L, "DFT", null, "ABC Logging", "Seller"));
        given(mapper.toLineItems(any(), eq(1L))).willReturn(List.of());
        given(participantRepo.insert(any(), any(), any(), any())).willReturn(99L);

        service.update(1L, req);

        verify(participantRepo).insert(eq("ABC Logging"), any(), any(), eq(USER));
        verify(invoiceRepo).updateInvoice(eq(1L), any(), eq("DFT"), eq(99L), isNull(), eq(USER));
        verify(participantRepo).delete(60L); // old seller row orphaned
    }

    // ===============================================================
    // delete
    // ===============================================================

    @Test
    void delete_notFound_throws() {
        given(invoiceRepo.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_approvedInvoice_throwsConflict() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "APP", "5678", null, "Seller"), 10L, null, null)));
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);
        verify(invoiceRepo, never()).deleteInvoice(anyLong());
    }

    @Test
    void delete_success_removesChildrenInvoiceAndParticipants() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, 50L, 60L)));

        service.delete(1L);

        verify(lineItemRepo).deleteAllByInvoiceId(1L);
        verify(invoiceRepo).deleteAllLogSources(1L);
        verify(invoiceRepo).deleteAllRelatedInvoiceRefs(1L);
        verify(invoiceRepo).deleteAllIncomingRelatedInvoiceRefs(1L);
        verify(invoiceRepo).deleteInvoice(1L);
        verify(participantRepo).delete(50L);
        verify(participantRepo).delete(60L);
    }

    // ===============================================================
    // submit
    // ===============================================================

    @Test
    void submit_notFound_throws() {
        given(invoiceRepo.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.submit(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void submit_notDraftOrUnapproved_throwsConflict() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "PRO", "5678", null, "Seller"), 10L, null, null)));
        assertThatThrownBy(() -> service.submit(1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void submit_validationError_throws() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(validator.validate(any(), any(), anyBoolean(), eq(ActionType.SUBMIT))).willReturn(WITH_ERROR);

        assertThatThrownBy(() -> service.submit(1L)).isInstanceOf(ValidationException.class);
        verify(invoiceRepo, never()).updateStatus(any(), any(), any());
    }

    @Test
    void submit_draft_persistsConvertedPricesStatusAndSurfacesWarnings() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(lineItemRepo.findByInvoiceId(1L)).willReturn(List.of(line(1L, null)));
        LineItem converted = line(1L, new BigDecimal("12.34"));
        given(priceConversionService.apply(any(), any(), any()))
                .willReturn(new PriceConversionService.Result(List.of(converted), List.of(A_WARNING)));

        service.submit(1L);

        verify(lineItemRepo).updateConvertedPrice(1L, new BigDecimal("12.34"), USER);
        verify(invoiceRepo).updateStatus(1L, "PRO", USER);
        verify(submissionRepo).updateSubmissionStatus(10L, "INB", USER);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(mapper).toResponse(any(), eq(10L), any(), captor.capture());
        assertThat(captor.getValue().warnings()).contains(A_WARNING);
    }

    @Test
    void submit_unapprovedInvoice_isAllowed() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "UNA", "5678", null, "Seller"), 10L, null, null)));

        service.submit(1L);

        verify(invoiceRepo).updateStatus(1L, "PRO", USER);
    }

    // ===============================================================
    // duplicate
    // ===============================================================

    @Test
    void duplicate_notFound_throws() {
        given(invoiceRepo.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.duplicate(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void duplicate_clonesUnderSameSubmissionAsNewDraft() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(lineItemRepo.findByInvoiceId(1L)).willReturn(List.of(line(1L, null), line(2L, null)));
        given(invoiceRepo.insertInvoice(any(), eq(10L), eq("DFT"), any(), any(), any())).willReturn(600L);

        service.duplicate(1L);

        // Re-uses the source submission id (10) and inserts as DRAFT.
        verify(invoiceRepo).insertInvoice(any(), eq(10L), eq("DFT"), isNull(), isNull(), eq(USER));
        // Both source lines re-inserted on the new invoice.
        verify(lineItemRepo, times(2)).insertLineItem(eq(600L), any(), eq(USER));
        // Response carries no validation result on a duplicate.
        verify(mapper).toResponse(any(), eq(10L), any(), isNull());
        verify(submissionRepo, never()).insertSubmission(any(), any(), any(), any());
    }

    @Test
    void duplicate_withManualOtherParty_clonesParticipantRow() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "DFT", null, "ABC Logging", "Seller"), 10L, 50L, null)));
        given(lineItemRepo.findByInvoiceId(1L)).willReturn(List.of());
        given(participantRepo.insert(any(), any(), any(), any())).willReturn(900L);
        given(invoiceRepo.insertInvoice(any(), any(), any(), any(), any(), any())).willReturn(600L);

        service.duplicate(1L);

        verify(participantRepo).insert(eq("ABC Logging"), any(), any(), eq(USER));
        verify(invoiceRepo).insertInvoice(any(), eq(10L), eq("DFT"), eq(900L), isNull(), eq(USER));
    }

    // ===============================================================
    // changeStatus
    // ===============================================================

    @Test
    void changeStatus_nullRequest_throwsBadRequest() {
        assertThatThrownBy(() -> service.changeStatus(1L, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeStatus_nullStatus_throwsBadRequest() {
        assertThatThrownBy(() -> service.changeStatus(1L, new ChangeStatusRequest(null, "c")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeStatus_notFound_throws() {
        given(invoiceRepo.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeStatus(99L, new ChangeStatusRequest("APP", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeStatus_validationError_throws() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "PRO", "5678", null, "Seller"), 10L, null, null)));
        given(validator.validateForChangeStatus(any(), any(), any())).willReturn(WITH_ERROR);

        assertThatThrownBy(() -> service.changeStatus(1L, new ChangeStatusRequest("REJ", "bad")))
                .isInstanceOf(ValidationException.class);
        verify(invoiceRepo, never()).updateStatus(any(), any(), any());
    }

    @Test
    void changeStatus_withComments_updatesReviewerNotesAndStatus() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "PRO", "5678", null, "Seller"), 10L, null, null)));
        given(invoiceRepo.countByCspSubmissionIdAndStatus(10L, "PRO")).willReturn(1); // others still processing

        service.changeStatus(1L, new ChangeStatusRequest("REJ", "Rejecting this"));

        verify(invoiceRepo).updateReviewerNotes(1L, "Rejecting this", USER);
        verify(invoiceRepo).updateStatus(1L, "REJ", USER);
        verify(submissionRepo, never()).updateSubmissionStatus(any(), any(), any());
    }

    @Test
    void changeStatus_noComments_skipsReviewerNotesUpdate() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "PRO", "5678", null, "Seller"), 10L, null, null)));
        given(invoiceRepo.countByCspSubmissionIdAndStatus(10L, "PRO")).willReturn(1);

        service.changeStatus(1L, new ChangeStatusRequest("APP", null));

        verify(invoiceRepo, never()).updateReviewerNotes(any(), any(), any());
        verify(invoiceRepo).updateStatus(1L, "APP", USER);
    }

    @Test
    void changeStatus_unapprove_neverChangesSubmission() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "APP", "5678", null, "Seller"), 10L, null, null)));

        service.changeStatus(1L, new ChangeStatusRequest("UNA", "reopen"));

        verify(submissionRepo, never()).updateSubmissionStatus(any(), any(), any());
        // UNA short-circuits before counting processing invoices.
        verify(invoiceRepo, never()).countByCspSubmissionIdAndStatus(any(), eq("PRO"));
    }

    @Test
    void changeStatus_lastProcessingWithApproved_movesSubmissionToComplete() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "PRO", "5678", null, "Seller"), 10L, null, null)));
        given(invoiceRepo.countByCspSubmissionIdAndStatus(10L, "PRO")).willReturn(0);
        given(invoiceRepo.countByCspSubmissionIdAndStatus(10L, "APP")).willReturn(1);

        service.changeStatus(1L, new ChangeStatusRequest("APP", null));

        verify(submissionRepo).updateSubmissionStatus(10L, "COM", USER);
    }

    @Test
    void changeStatus_lastProcessingNoneApproved_movesSubmissionToRejected() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "PRO", "5678", null, "Seller"), 10L, null, null)));
        given(invoiceRepo.countByCspSubmissionIdAndStatus(10L, "PRO")).willReturn(0);
        given(invoiceRepo.countByCspSubmissionIdAndStatus(10L, "APP")).willReturn(0);

        service.changeStatus(1L, new ChangeStatusRequest("REJ", "no good"));

        verify(submissionRepo).updateSubmissionStatus(10L, "REJ", USER);
    }

    @Test
    void changeStatus_nullSubmission_skipsCascade() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "PRO", "5678", null, "Seller"), null, null, null)));

        service.changeStatus(1L, new ChangeStatusRequest("APP", null));

        verify(invoiceRepo, never()).countByCspSubmissionIdAndStatus(any(), any());
        verify(submissionRepo, never()).updateSubmissionStatus(any(), any(), any());
    }

    // ===============================================================
    // addLineItem
    // ===============================================================

    private LineItemRequest lineItemRequest() {
        return mock(LineItemRequest.class);
    }

    @Test
    void addLineItem_notFound_throws() {
        given(invoiceRepo.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.addLineItem(99L, lineItemRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addLineItem_approvedInvoice_throwsConflict() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "APP", "5678", null, "Seller"), 10L, null, null)));
        assertThatThrownBy(() -> service.addLineItem(1L, lineItemRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void addLineItem_validationError_throws() {
        LineItemRequest req = lineItemRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(mapper.toLineItem(eq(req), eq(1L))).willReturn(line(null, null));
        given(validator.validate(any(), any(), anyBoolean(), eq(ActionType.SAVE))).willReturn(WITH_ERROR);

        assertThatThrownBy(() -> service.addLineItem(1L, req)).isInstanceOf(ValidationException.class);
        verify(lineItemRepo, never()).insertLineItem(any(), any(), any());
    }

    @Test
    void addLineItem_onDraft_insertsWithoutStatusChange() {
        LineItemRequest req = lineItemRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(mapper.toLineItem(eq(req), eq(1L))).willReturn(line(null, null));

        service.addLineItem(1L, req);

        verify(lineItemRepo).insertLineItem(eq(1L), any(), eq(USER));
        verify(invoiceRepo, never()).updateStatus(any(), any(), any()); // already DRAFT
    }

    @Test
    void addLineItem_onUnapproved_revertsToDraftAndLobby() {
        LineItemRequest req = lineItemRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "UNA", "5678", null, "Seller"), 10L, null, null)));
        given(mapper.toLineItem(eq(req), eq(1L))).willReturn(line(null, null));

        service.addLineItem(1L, req);

        verify(lineItemRepo).insertLineItem(eq(1L), any(), eq(USER));
        verify(invoiceRepo).updateStatus(1L, "DFT", USER);
        verify(submissionRepo).updateSubmissionStatus(10L, "LOB", USER);
    }

    // ===============================================================
    // updateLineItem
    // ===============================================================

    @Test
    void updateLineItem_invoiceNotFound_throws() {
        given(invoiceRepo.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateLineItem(99L, 5L, lineItemRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateLineItem_onProcessing_succeedsAndRevertsToDraft() {
        // Line items are now editable in every status; a change reverts the
        // invoice to DRAFT (so a PROCESSING invoice no longer throws).
        LineItemRequest req = lineItemRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "PRO", "5678", null, "Seller"), 10L, null, null)));
        given(lineItemRepo.findIdsByInvoiceId(1L)).willReturn(List.of(5L));
        given(lineItemRepo.findByInvoiceId(1L)).willReturn(List.of(line(5L, null)));
        given(mapper.toLineItem(eq(req), eq(1L))).willReturn(line(null, null));

        service.updateLineItem(1L, 5L, req);

        verify(lineItemRepo).updateLineItem(eq(5L), any(), eq(USER));
        verify(invoiceRepo).updateStatus(1L, "DFT", USER);
    }

    @Test
    void deleteLineItem_onCancelled_succeedsAndRevertsToDraft() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(details(1L, "CAN", "5678", null, "Seller"), 10L, null, null)));
        given(lineItemRepo.findIdsByInvoiceId(1L)).willReturn(List.of(5L));

        service.deleteLineItem(1L, 5L);

        verify(lineItemRepo).deleteLineItem(5L);
        verify(invoiceRepo).updateStatus(1L, "DFT", USER);
    }

    @Test
    void updateLineItem_lineNotOnInvoice_throws() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(lineItemRepo.findIdsByInvoiceId(1L)).willReturn(List.of(7L)); // 5 not present
        assertThatThrownBy(() -> service.updateLineItem(1L, 5L, lineItemRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateLineItem_validationError_throws() {
        LineItemRequest req = lineItemRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(lineItemRepo.findIdsByInvoiceId(1L)).willReturn(List.of(5L));
        given(lineItemRepo.findByInvoiceId(1L)).willReturn(List.of(line(5L, null)));
        given(mapper.toLineItem(eq(req), eq(1L))).willReturn(line(5L, null));
        given(validator.validate(any(), any(), anyBoolean(), eq(ActionType.SAVE))).willReturn(WITH_ERROR);

        assertThatThrownBy(() -> service.updateLineItem(1L, 5L, req)).isInstanceOf(ValidationException.class);
        verify(lineItemRepo, never()).updateLineItem(anyLong(), any(), any());
    }

    @Test
    void updateLineItem_success_updatesLineKeyedByPathId() {
        LineItemRequest req = lineItemRequest();
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(lineItemRepo.findIdsByInvoiceId(1L)).willReturn(List.of(5L));
        given(lineItemRepo.findByInvoiceId(1L)).willReturn(List.of(line(5L, null)));
        given(mapper.toLineItem(eq(req), eq(1L))).willReturn(line(null, null));

        service.updateLineItem(1L, 5L, req);

        verify(lineItemRepo).updateLineItem(eq(5L), any(), eq(USER));
    }

    // ===============================================================
    // deleteLineItem
    // ===============================================================

    @Test
    void deleteLineItem_invoiceNotFound_throws() {
        given(invoiceRepo.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteLineItem(99L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteLineItem_lineNotOnInvoice_throws() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(lineItemRepo.findIdsByInvoiceId(1L)).willReturn(List.of(7L));
        assertThatThrownBy(() -> service.deleteLineItem(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteLineItem_success_deletesLine() {
        given(invoiceRepo.findById(1L)).willReturn(Optional.of(loaded(draftDetails(1L), 10L, null, null)));
        given(lineItemRepo.findIdsByInvoiceId(1L)).willReturn(List.of(5L));

        service.deleteLineItem(1L, 5L);

        verify(lineItemRepo).deleteLineItem(5L);
    }

    // ===============================================================
    // export (CSV / PDF) — pure formatting, no collaborators
    // ===============================================================

    private InvoiceSummaryExportRequest exportRequest(String invoiceNumber, List<LineItemExportRow> items) {
        GroupSummaryExportRow group = new GroupSummaryExportRow(
                1, "S01", "Some description", "FIR", 10,
                new BigDecimal("5.0"), new BigDecimal("100.00"), "Y", items);
        return new InvoiceSummaryExportRequest(invoiceNumber, List.of(group));
    }

    @Test
    void exportCsv_buildsSectionedCsvAndSanitizedFilename() {
        LineItemExportRow li = new LineItemExportRow("S01", "FIR", "CS01", 10, "G1",
                new BigDecimal("5.0"), new BigDecimal("10.00"), new BigDecimal("100.00"));
        ReportResult result = service.exportCsv(exportRequest("INV/001", List.of(li)));

        String csv = new String(result.data(), StandardCharsets.UTF_8);
        assertThat(csv).contains("Invoice Group Summary").contains("FIR").contains("Some description");
        // The '/' in the invoice number is sanitized to '_' for the filename.
        assertThat(result.filename()).isEqualTo("InvoiceGroupSummary_INV_001.csv");
    }

    @Test
    void exportCsv_groupWithoutLineItems_emitsNoLineItemsMarker() {
        ReportResult result = service.exportCsv(exportRequest("INV-001", List.of()));
        String csv = new String(result.data(), StandardCharsets.UTF_8);
        assertThat(csv).contains("No line items.");
    }

    @Test
    void exportCsv_nullInvoiceNumber_usesBaseFilename() {
        ReportResult result = service.exportCsv(new InvoiceSummaryExportRequest(null, List.of()));
        assertThat(result.filename()).isEqualTo("InvoiceGroupSummary.csv");
    }

    @Test
    void exportPdf_returnsPdfBytesAndFilename() {
        LineItemExportRow li = new LineItemExportRow("S01", "FIR", "CS01", 10, "G1",
                new BigDecimal("5.0"), new BigDecimal("10.00"), new BigDecimal("100.00"));
        ReportResult result = service.exportPdf(exportRequest("INV-001", List.of(li)));

        assertThat(result.filename()).isEqualTo("InvoiceGroupSummary_INV-001.pdf");
        assertThat(result.data()).isNotEmpty();
        // PDF magic header.
        assertThat(new String(result.data(), 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void exportPdf_groupWithoutLineItems_stillRendersDocument() {
        ReportResult result = service.exportPdf(exportRequest("INV-001", List.of()));
        assertThat(result.data()).isNotEmpty();
    }
}
