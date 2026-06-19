package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ChangeStatusRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.CreateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceSummaryExportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceSummaryExportRequest.GroupSummaryExportRow;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceSummaryExportRequest.LineItemExportRow;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItemRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.UpdateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ConflictException;
import ca.bc.gov.nrs.csp.backend.exception.ReportGenerationException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository.LoadedInvoice;
import ca.bc.gov.nrs.csp.backend.repository.LineItemRepository;
import ca.bc.gov.nrs.csp.backend.repository.LogSaleParticipantRepository;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import ca.bc.gov.nrs.csp.backend.service.mapper.InvoiceMapper;
import ca.bc.gov.nrs.csp.backend.util.constants.ActionType;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.invoiceDetails.InvoiceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepo;
    private final LineItemRepository lineItemRepo;
    private final CspSubmissionRepository submissionRepo;
    private final LogSaleParticipantRepository participantRepo;
    private final CommonValidation commonValidation;
    private final PriceConversionService priceConversionService;
    private final InvoiceMapper mapper;

    public InvoiceService(InvoiceRepository invoiceRepo,
                          LineItemRepository lineItemRepo,
                          CspSubmissionRepository submissionRepo,
                          LogSaleParticipantRepository participantRepo,
                          CommonValidation commonValidation,
                          PriceConversionService priceConversionService,
                          InvoiceMapper mapper) {
        this.invoiceRepo = invoiceRepo;
        this.lineItemRepo = lineItemRepo;
        this.submissionRepo = submissionRepo;
        this.participantRepo = participantRepo;
        this.commonValidation = commonValidation;
        this.priceConversionService = priceConversionService;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------
    // GET
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse getById(Long id) {
        log.debug("Loading invoice id={}", id);
        LoadedInvoice loaded = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + id + " was not found."));
        List<LineItem> lines = lineItemRepo.findByInvoiceId(id);
        // Re-validate the saved record so the UI can surface warnings/errors
        // that don't (or no longer) block save. ActionType.OTHER runs the
        // full rule set minus the action-gated nudges (`isSubmitProcessRequiered`
        // and `isReviewerCommentUpdate`), which don't apply to a passive read.
        boolean manual = loaded.submissionNumber() == null;
        ValidationResult validation = newValidator()
                .validate(loaded.details(), lines, manual, ActionType.OTHER);

        // Flat-price conversion is only re-run (and its warnings surfaced) when
        // the invoice is PROCESSING. For every other status the stored converted
        // prices are displayed as-is and no conversion warnings are added.
        if (ConstantsCode.INVENTRYSTATUS_PROCESSING.equals(loaded.details().invStatus())) {
            String user = SecurityContextUtils.requireUsername();
            PriceConversionService.Result conversion = priceConversionService
                    .apply(lines, loaded.details().maturity(), loaded.details().invoiceDate());
            for (LineItem line : conversion.lines()) {
                lineItemRepo.updateConvertedPrice(line.lineItemID(), line.convertedPrice(), user);
            }
            lines = conversion.lines();
            List<ValidationMessage> messages = new ArrayList<>(validation.messages());
            messages.addAll(conversion.warnings());
            validation = new ValidationResult(messages);
        }

        return mapper.toResponse(loaded.details(), loaded.submissionId(), loaded.submissionNumber(), lines, validation);
    }

    // ---------------------------------------------------------------
    // CREATE (always DRAFT)
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        String user = SecurityContextUtils.requireUsername();
        InvoiceDetails details = mapper.toDetails(request, user);
        List<LineItem> lines = mapper.toLineItems(request.lineItems(), null);

        ValidationResult result = newValidator().validate(details, lines, request.manual(), ActionType.SAVE);
        throwIfErrors(result, "Invoice failed validation on create.");

        // Create a submission for this new manual invoice.
        String submissionStatus = request.manual()
                ? ConstantsCode.SUBMSTATUS_LOBBY
                : ConstantsCode.SUBMSTATUS_INBOX;
        Long submissionId = submissionRepo.insertSubmission(
                details.submitterClientNum(), details.submitterLocation(), submissionStatus, user);

        // If the other party is a manual entry (no client number, but name/city/province set),
        // insert a log_sale_participant row and link its id via the correct buyer/seller slot.
        Long buyerParticipantId = null;
        Long sellerParticipantId = null;
        if (hasManualOtherParty(details)) {
            Long pid = participantRepo.insert(
                    details.otherClientName(), details.otherClientCity(), details.otherClientProvState(), user);
            if (otherPartyIsInBuyerSlot(details)) {
                buyerParticipantId = pid;
            } else {
                sellerParticipantId = pid;
            }
        }

        Long newInvoiceId = invoiceRepo.insertInvoice(details, submissionId,
                ConstantsCode.INVENTRYSTATUS_DRAFT, buyerParticipantId, sellerParticipantId, user);
        log.info("Created invoice id={} submissionId={}", newInvoiceId, submissionId);

        // Persist line items and log sources.
        for (LineItem line : lines) {
            lineItemRepo.insertLineItem(newInvoiceId, line, user);
        }
        invoiceRepo.replaceLogSources(newInvoiceId, ConstantsCode.LOGSOURCECODE_BOOMNUMBER, details.boomNumbers(), user);
        invoiceRepo.replaceLogSources(newInvoiceId, ConstantsCode.LOGSOURCECODE_TIMERMARK, details.timberMarks(), user);
        invoiceRepo.replaceLogSources(newInvoiceId, ConstantsCode.LOGSOURCECODE_WEIGHSLIP, details.weightSlips(), user);
        invoiceRepo.replaceRelatedInvoices(newInvoiceId, ConstantsCode.INVRELATETYPE_REPLACE,
                details.replaceInvNum(), details.submitterClientNum(), details.submitterLocation(), user);
        invoiceRepo.replaceRelatedInvoices(newInvoiceId, ConstantsCode.INVRELATETYPE_ADJUST,
                details.adjustInvNum(), details.submitterClientNum(), details.submitterLocation(), user);

        InvoiceDetails saved = withId(details, newInvoiceId, ConstantsCode.INVENTRYSTATUS_DRAFT);
        // A freshly-created submission has no business submission number yet
        // (insertSubmission only sets the surrogate csp_submission_id).
        return mapper.toResponse(saved, submissionId, null, lineItemRepo.findByInvoiceId(newInvoiceId), result);
    }

    // ---------------------------------------------------------------
    // UPDATE (preserves status; status transitions happen via /submit or /status)
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse update(Long id, UpdateInvoiceRequest request) {
        String user = SecurityContextUtils.requireUsername();
        LoadedInvoice existing = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + id + " was not found."));
        String existingStatus = existing.details().invStatus();
        // The header detail fields are read-only on approved, rejected, and
        // cancelled invoices (parity with the legacy app); only DFT/PRO/UNA can
        // be saved. Line-item changes use the dedicated line-item endpoints.
        if (ConstantsCode.INVENTRYSTATUS_APPROVED.equals(existingStatus)
                || ConstantsCode.INVENTRYSTATUS_REJECTED.equals(existingStatus)
                || ConstantsCode.INVENTRYSTATUS_CANCELLED.equals(existingStatus)) {
            throw new ConflictException(
                    "Approved, rejected, or cancelled invoices cannot have their details edited.");
        }

        InvoiceDetails details = mapper.toDetails(request, id, ConstantsCode.INVENTRYSTATUS_DRAFT, existing.details().entryUserID());
        List<LineItem> lines = mapper.toLineItems(request.lineItems(), id);

        ValidationResult result = newValidator().validate(details, lines, request.manual(), ActionType.SAVE);
        throwIfErrors(result, "Invoice failed validation on update.");

        // Reconcile manual-other-party participant row(s) against the new request.
        // We compute the plan (inserts/updates + the set of orphan ids to delete) first,
        // then run updateInvoice to clear the FK references, THEN delete the orphans.
        // Deleting them before updateInvoice would violate the CLS_CLSP_FK constraint.
        ParticipantPlan plan = reconcileParticipant(existing, details, user);

        invoiceRepo.updateInvoice(id, details, ConstantsCode.INVENTRYSTATUS_DRAFT,
                plan.newIds().buyerId(), plan.newIds().sellerId(), user);
        plan.orphans().forEach(participantRepo::delete);

        reconcileLineItems(id, lines, user);
        invoiceRepo.replaceLogSources(id, ConstantsCode.LOGSOURCECODE_BOOMNUMBER, details.boomNumbers(), user);
        invoiceRepo.replaceLogSources(id, ConstantsCode.LOGSOURCECODE_TIMERMARK, details.timberMarks(), user);
        invoiceRepo.replaceLogSources(id, ConstantsCode.LOGSOURCECODE_WEIGHSLIP, details.weightSlips(), user);
        invoiceRepo.replaceRelatedInvoices(id, ConstantsCode.INVRELATETYPE_REPLACE,
                details.replaceInvNum(), details.submitterClientNum(), details.submitterLocation(), user);
        invoiceRepo.replaceRelatedInvoices(id, ConstantsCode.INVRELATETYPE_ADJUST,
                details.adjustInvNum(), details.submitterClientNum(), details.submitterLocation(), user);

        // Saving an existing invoice reverts its submission to LOBBY.
        if (existing.submissionId() != null) {
            submissionRepo.updateSubmissionStatus(existing.submissionId(), ConstantsCode.SUBMSTATUS_LOBBY, user);
        }

        // Flat-price conversion: compute and persist each line's converted price on save
        List<LineItem> savedLines = lineItemRepo.findByInvoiceId(id);
        PriceConversionService.Result conversion = priceConversionService.apply(
                savedLines, details.maturity(), details.invoiceDate());
        for (LineItem line : conversion.lines()) {
            lineItemRepo.updateConvertedPrice(line.lineItemID(), line.convertedPrice(), user);
        }

        InvoiceDetails saved = withId(details, id, ConstantsCode.INVENTRYSTATUS_DRAFT);
        return mapper.toResponse(saved, existing.submissionId(), existing.submissionNumber(), conversion.lines(),
                withConversionWarnings(result, conversion));
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    @Transactional
    public void delete(Long id) {
        LoadedInvoice existing = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + id + " was not found."));
        if (ConstantsCode.INVENTRYSTATUS_APPROVED.equals(existing.details().invStatus())) {
            throw new ConflictException("Approved invoices cannot be deleted; unapprove first.");
        }

        lineItemRepo.deleteAllByInvoiceId(id);
        invoiceRepo.deleteAllLogSources(id);
        // Clean up BOTH directions of the related-invoice relationship before we delete
        // the parent row, so the RELATED_COASTAL_LOG_SALE_ID FK can't block us when some
        // other invoice was referencing this one.
        invoiceRepo.deleteAllRelatedInvoiceRefs(id);
        invoiceRepo.deleteAllIncomingRelatedInvoiceRefs(id);
        invoiceRepo.deleteInvoice(id);
        // Delete participant rows AFTER the invoice (the CLS_CLSP_FK constraint requires
        // the referencing coastal_log_sale row to be gone first).
        participantRepo.delete(existing.buyerParticipantId());
        participantRepo.delete(existing.sellerParticipantId());
        log.info("Deleted invoice id={}", id);
    }

    // ---------------------------------------------------------------
    // SUBMIT (DRAFT or UNAPPROVED → PROCESSING)
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse submit(Long id) {
        String user = SecurityContextUtils.requireUsername();
        LoadedInvoice existing = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + id + " was not found."));
        String status = existing.details().invStatus();
        if (!ConstantsCode.INVENTRYSTATUS_DRAFT.equals(status)
                && !ConstantsCode.INVENTRYSTATUS_UNAPPROVED.equals(status)) {
            throw new ConflictException("Only draft or unapproved invoices can be submitted.");
        }

        List<LineItem> lines = lineItemRepo.findByInvoiceId(id);
        ValidationResult result = newValidator().validate(existing.details(), lines, /*manual*/ true, ActionType.SUBMIT);
        throwIfErrors(result, "Invoice failed validation on submit.");

        // Flat-price conversion ("price spread"): compute each line's converted
        // price and persist it.
        PriceConversionService.Result conversion = priceConversionService.apply(
                lines, existing.details().maturity(), existing.details().invoiceDate());
        for (LineItem line : conversion.lines()) {
            lineItemRepo.updateConvertedPrice(line.lineItemID(), line.convertedPrice(), user);
        }
        lines = conversion.lines();

        // Surface the conversion warnings (factor-not-found / out-of-range)
        // alongside the validator's.
        ValidationResult responseResult = withConversionWarnings(result, conversion);

        invoiceRepo.updateStatus(id, ConstantsCode.INVENTRYSTATUS_PROCESSING, user);
        submissionRepo.updateSubmissionStatus(existing.submissionId(), ConstantsCode.SUBMSTATUS_INBOX, user);
        log.info("Submitted invoice id={} submissionId={}", id, existing.submissionId());

        InvoiceDetails saved = withId(existing.details(), id, ConstantsCode.INVENTRYSTATUS_PROCESSING);
        return mapper.toResponse(saved, existing.submissionId(), existing.submissionNumber(), lines, responseResult);
    }

    // ---------------------------------------------------------------
    // DUPLICATE — clone as new DRAFT
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse duplicate(Long id) {
        String user = SecurityContextUtils.requireUsername();
        LoadedInvoice existing = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + id + " was not found."));
        List<LineItem> existingLines = lineItemRepo.findByInvoiceId(id);

        // Reset id/status on the cloned details and persist as a new DRAFT.
        InvoiceDetails cloned = withId(existing.details(), null, ConstantsCode.INVENTRYSTATUS_DRAFT);

        // Reuse the source invoice's submission — a submission can hold multiple
        // invoices — rather than creating a new one. Its status is left unchanged.
        Long submissionId = existing.submissionId();

        // Clone a manual-other-party participant row if the source had one.
        Long buyerParticipantId = null;
        Long sellerParticipantId = null;
        if (hasManualOtherParty(cloned)) {
            Long pid = participantRepo.insert(
                    cloned.otherClientName(), cloned.otherClientCity(), cloned.otherClientProvState(), user);
            if (otherPartyIsInBuyerSlot(cloned)) {
                buyerParticipantId = pid;
            } else {
                sellerParticipantId = pid;
            }
        }

        Long newInvoiceId = invoiceRepo.insertInvoice(cloned, submissionId,
                ConstantsCode.INVENTRYSTATUS_DRAFT, buyerParticipantId, sellerParticipantId, user);
        for (LineItem line : existingLines) {
            LineItem freshLine = new LineItem(
                    null, newInvoiceId, line.secondSort(), line.clientSecondarySort(), line.species(),
                    line.speciesDescription(), line.grade(), line.numOfPieces(), line.price(), line.volume(),
                    line.convertedPrice(), line.amount());
            lineItemRepo.insertLineItem(newInvoiceId, freshLine, user);
        }
        invoiceRepo.replaceLogSources(newInvoiceId, ConstantsCode.LOGSOURCECODE_BOOMNUMBER, cloned.boomNumbers(), user);
        invoiceRepo.replaceLogSources(newInvoiceId, ConstantsCode.LOGSOURCECODE_TIMERMARK, cloned.timberMarks(), user);
        invoiceRepo.replaceLogSources(newInvoiceId, ConstantsCode.LOGSOURCECODE_WEIGHSLIP, cloned.weightSlips(), user);
        invoiceRepo.replaceRelatedInvoices(newInvoiceId, ConstantsCode.INVRELATETYPE_REPLACE,
                cloned.replaceInvNum(), cloned.submitterClientNum(), cloned.submitterLocation(), user);
        invoiceRepo.replaceRelatedInvoices(newInvoiceId, ConstantsCode.INVRELATETYPE_ADJUST,
                cloned.adjustInvNum(), cloned.submitterClientNum(), cloned.submitterLocation(), user);
        log.info("Duplicated invoice id={} newId={}", id, newInvoiceId);

        InvoiceDetails saved = withId(cloned, newInvoiceId, ConstantsCode.INVENTRYSTATUS_DRAFT);
        return mapper.toResponse(saved, submissionId, existing.submissionNumber(), lineItemRepo.findByInvoiceId(newInvoiceId), null);
    }

    // ---------------------------------------------------------------
    // CHANGE STATUS — approve / reject / cancel / unapprove
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse changeStatus(Long id, ChangeStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new BadRequestException("Status is required.");
        }
        String user = SecurityContextUtils.requireUsername();
        LoadedInvoice existing = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + id + " was not found."));

        // Apply the supplied reviewer comments to the details we hand to the validator
        // so its "reviewer comments must change" check sees the new value.
        InvoiceDetails withNewComments = withReviewComments(existing.details(), request.reviewComments());

        ValidationResult result = newValidator().validateForChangeStatus(withNewComments, request.status(), user);
        throwIfErrors(result, "Invoice failed validation on status change.");

        if (request.reviewComments() != null) {
            invoiceRepo.updateReviewerNotes(id, request.reviewComments(), user);
        }
        invoiceRepo.updateStatus(id, request.status(), user);
        log.info("Changed invoice id={} status={}", id, request.status());

        // Cascade the change to the submission status (after the invoice's own
        // status is persisted, so the counts below reflect the new state).
        applySubmissionStatusOnStatusChange(existing.submissionId(), request.status(), user);

        InvoiceDetails saved = withReviewCommentsAndStatus(existing.details(), request.reviewComments(), request.status());
        return mapper.toResponse(saved, existing.submissionId(), existing.submissionNumber(), lineItemRepo.findByInvoiceId(id), result);
    }

    /**
     * Submission-status side effects of an invoice status change:
     * <ul>
     *   <li>UNAPPROVE — never changes the submission.</li>
     *   <li>APPROVE / REJECT / CANCEL — only once no PROCESSING invoices remain in
     *       the submission (i.e. this was the last processing invoice) does the
     *       submission move: COMPLETE if any APPROVED invoice exists in it,
     *       otherwise REJECTED. While other invoices are still processing, the
     *       submission is left unchanged.</li>
     * </ul>
     * Must be called AFTER the invoice's own status has been persisted.
     */
    private void applySubmissionStatusOnStatusChange(Long submissionId, String newInvoiceStatus, String user) {
        if (submissionId == null) return;
        if (ConstantsCode.INVENTRYSTATUS_UNAPPROVED.equals(newInvoiceStatus)) return;

        int processing = invoiceRepo.countByCspSubmissionIdAndStatus(
                submissionId, ConstantsCode.INVENTRYSTATUS_PROCESSING);
        if (processing > 0) return; // not the last processing invoice — leave the submission alone

        boolean anyApproved = invoiceRepo.countByCspSubmissionIdAndStatus(
                submissionId, ConstantsCode.INVENTRYSTATUS_APPROVED) > 0;
        String newSubmissionStatus = anyApproved
                ? ConstantsCode.SUBMSTATUS_COMPLETE
                : ConstantsCode.SUBMSTATUS_REJECTED;
        submissionRepo.updateSubmissionStatus(submissionId, newSubmissionStatus, user);
        log.info("Submission submissionId={} newStatus={} anyApproved={}", submissionId, newSubmissionStatus, anyApproved);
    }

    // ---------------------------------------------------------------
    // LINE ITEMS (sub-resource — only valid on DRAFT invoices)
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse addLineItem(Long invoiceId, LineItemRequest request) {
        String user = SecurityContextUtils.requireUsername();
        LoadedInvoice existing = loadInvoiceOrThrow(invoiceId);
        requireAddLineItemAllowed(existing);
        // Build the candidate set: every existing line plus the new one. Run
        // the full invoice validator against the full set so totals-variance
        // / line-count rules still fire correctly.
        List<LineItem> existingLines = lineItemRepo.findByInvoiceId(invoiceId);
        LineItem newLine = mapper.toLineItem(request, invoiceId);
        List<LineItem> candidate = new ArrayList<>(existingLines);
        candidate.add(newLine);

        boolean manual = existing.submissionNumber() == null;
        ValidationResult saveResult = newValidator().validate(existing.details(), candidate, manual, ActionType.SAVE);
        throwIfErrors(saveResult, "Line item failed validation.");

        lineItemRepo.insertLineItem(invoiceId, newLine, user);
        log.info("Added line item to invoice id={}", invoiceId);
        return revertToDraftAndRespond(existing, user);
    }

    @Transactional
    public InvoiceResponse updateLineItem(Long invoiceId, Long lineId, LineItemRequest request) {
        String user = SecurityContextUtils.requireUsername();
        LoadedInvoice existing = loadInvoiceOrThrow(invoiceId);
        ensureLineBelongsToInvoice(invoiceId, lineId);

        List<LineItem> existingLines = lineItemRepo.findByInvoiceId(invoiceId);
        LineItem mapped = mapper.toLineItem(request, invoiceId);
        LineItem updatedLine = new LineItem(
                lineId, mapped.invoiceID(), mapped.secondSort(), mapped.clientSecondarySort(), mapped.species(),
                mapped.speciesDescription(), mapped.grade(), mapped.numOfPieces(), mapped.price(), mapped.volume(),
                mapped.convertedPrice(), mapped.amount());
        // Build the candidate set with the updated line swapped in by id.
        List<LineItem> candidate = existingLines.stream()
                .map(line -> line.lineItemID() != null && line.lineItemID().equals(lineId) ? updatedLine : line)
                .toList();

        boolean manual = existing.submissionNumber() == null;
        ValidationResult saveResult = newValidator().validate(existing.details(), candidate, manual, ActionType.SAVE);
        throwIfErrors(saveResult, "Line item failed validation.");

        lineItemRepo.updateLineItem(lineId, updatedLine, user);
        log.info("Updated line item id={} invoiceId={}", lineId, invoiceId);
        return revertToDraftAndRespond(existing, user);
    }

    @Transactional
    public InvoiceResponse deleteLineItem(Long invoiceId, Long lineId) {
        String user = SecurityContextUtils.requireUsername();
        LoadedInvoice existing = loadInvoiceOrThrow(invoiceId);
        ensureLineBelongsToInvoice(invoiceId, lineId);

        lineItemRepo.deleteLineItem(lineId);
        log.info("Deleted line item id={} invoiceId={}", lineId, invoiceId);

        return revertToDraftAndRespond(existing, user);
    }

    /**
     * Every line-item change reverts the invoice to DRAFT and its submission to
     * LOBBY (a no-op when already there), then re-validates the now-persisted
     * record with {@link ActionType#OTHER} — the same action the passive GET
     * uses — so the response carries both warnings and any ERROR-type messages
     * that still describe the saved record.
     */
    private InvoiceResponse revertToDraftAndRespond(LoadedInvoice existing, String user) {
        Long invoiceId = existing.details().invID();
        if (!ConstantsCode.INVENTRYSTATUS_DRAFT.equals(existing.details().invStatus())) {
            invoiceRepo.updateStatus(invoiceId, ConstantsCode.INVENTRYSTATUS_DRAFT, user);
            if (existing.submissionId() != null) {
                submissionRepo.updateSubmissionStatus(existing.submissionId(), ConstantsCode.SUBMSTATUS_LOBBY, user);
            }
            log.info("Line-item change reverted invoice id={} to DRAFT submissionId={}", invoiceId, existing.submissionId());
        }
        InvoiceDetails draftDetails = withId(existing.details(), invoiceId, ConstantsCode.INVENTRYSTATUS_DRAFT);
        boolean manual = existing.submissionNumber() == null;
        List<LineItem> currentLines = lineItemRepo.findByInvoiceId(invoiceId);
        ValidationResult result = newValidator().validate(draftDetails, currentLines, manual, ActionType.OTHER);
        return mapper.toResponse(draftDetails, existing.submissionId(), existing.submissionNumber(), currentLines, result);
    }

    private LoadedInvoice loadInvoiceOrThrow(Long invoiceId) {
        return invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + invoiceId + " was not found."));
    }

    // Add: allowed in every status except APPROVED. (A brand-new/unsaved invoice
    // has no id and never reaches here, covering the "NEW" case.)
    private void requireAddLineItemAllowed(LoadedInvoice existing) {
        if (ConstantsCode.INVENTRYSTATUS_APPROVED.equals(existing.details().invStatus())) {
            throw new ConflictException("Line items cannot be added to an approved invoice.");
        }
    }

    // Edit / delete line items: allowed in every status (a line-item change
    // reverts the invoice to DRAFT — see revertToDraftAndRespond), so no
    // status gate is needed here. Add is the only status-gated line operation
    // (blocked on APPROVED — see requireAddLineItemAllowed).

    private void ensureLineBelongsToInvoice(Long invoiceId, Long lineId) {
        if (!lineItemRepo.findIdsByInvoiceId(invoiceId).contains(lineId)) {
            throw new ResourceNotFoundException("Line item " + lineId + " was not found on invoice " + invoiceId + ".");
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    // Package-private (not private) so service unit tests can stub it on a spy
    // and exercise this service's orchestration without the full validator.
    InvoiceValidator newValidator() {
        return new InvoiceValidator(invoiceRepo, commonValidation);
    }

    private void throwIfErrors(ValidationResult result, String message) {
        if (result.hasErrors()) {
            throw new ValidationException(message, result);
        }
    }

    /**
     * Appends the flat-price-conversion warnings (factor-not-found / out-of-range)
     * to a validation result so they ride along on the response. Returns the
     * original result unchanged when there are no conversion warnings.
     */
    private ValidationResult withConversionWarnings(ValidationResult result, PriceConversionService.Result conversion) {
        if (conversion.warnings().isEmpty()) return result;
        List<ValidationMessage> combined = new ArrayList<>(result.messages());
        combined.addAll(conversion.warnings());
        return new ValidationResult(combined);
    }

    /**
     * Plans the manual-other-party participant row changes for an update. Does any required
     * INSERT/UPDATE immediately, but defers DELETE of orphan rows by returning their ids in
     * {@link ParticipantPlan#orphans()} — callers must run the DELETE *after* updateInvoice
     * clears the FK reference on {@code coastal_log_sale}, otherwise the CLS_CLSP_FK
     * constraint will reject the delete.
     *
     * <p>Four logical branches:</p>
     * <ul>
     *   <li>No manual other party in the new state → both slots cleared; existing rows queued as orphans.</li>
     *   <li>Manual + same slot already has a row → UPDATE in place (id retained, no orphan).</li>
     *   <li>Manual + slot is empty → INSERT a new row.</li>
     *   <li>Slot moved between buyer and seller → INSERT in the new slot, queue the old slot's row as an orphan.</li>
     * </ul>
     */
    private ParticipantPlan reconcileParticipant(LoadedInvoice existing, InvoiceDetails details, String user) {
        Long existingBuyerId = existing.buyerParticipantId();
        Long existingSellerId = existing.sellerParticipantId();
        List<Long> orphans = new ArrayList<>();

        if (!hasManualOtherParty(details)) {
            // Other party is a registered client (or absent) — clear both slots.
            if (existingBuyerId != null) orphans.add(existingBuyerId);
            if (existingSellerId != null) orphans.add(existingSellerId);
            return new ParticipantPlan(new ParticipantIds(null, null), orphans);
        }

        boolean intoBuyerSlot = otherPartyIsInBuyerSlot(details);
        String name = details.otherClientName();
        String city = details.otherClientCity();
        String province = details.otherClientProvState();

        Long newBuyerId;
        Long newSellerId;
        if (intoBuyerSlot) {
            if (existingBuyerId != null) {
                participantRepo.update(existingBuyerId, name, city, province, user);
                newBuyerId = existingBuyerId;
            } else {
                newBuyerId = participantRepo.insert(name, city, province, user);
            }
            newSellerId = null;
            if (existingSellerId != null) orphans.add(existingSellerId);
        } else {
            if (existingSellerId != null) {
                participantRepo.update(existingSellerId, name, city, province, user);
                newSellerId = existingSellerId;
            } else {
                newSellerId = participantRepo.insert(name, city, province, user);
            }
            newBuyerId = null;
            if (existingBuyerId != null) orphans.add(existingBuyerId);
        }
        return new ParticipantPlan(new ParticipantIds(newBuyerId, newSellerId), orphans);
    }

    private boolean hasManualOtherParty(InvoiceDetails details) {
        return isBlank(details.otherClientNum())
                && (!isBlank(details.otherClientName())
                    || !isBlank(details.otherClientCity())
                    || !isBlank(details.otherClientProvState()));
    }

    private boolean otherPartyIsInBuyerSlot(InvoiceDetails details) {
        // When the submitter is the seller, the "other party" is the buyer (buyer slot).
        // When the submitter is the buyer, the "other party" is the seller (seller slot).
        return ConstantsCode.INVOICE_SUBMITTEDBY_SELLER.equals(details.submittedBy());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private record ParticipantIds(Long buyerId, Long sellerId) {}

    private record ParticipantPlan(ParticipantIds newIds, List<Long> orphans) {}

    private void reconcileLineItems(Long invoiceId, List<LineItem> incoming, String user) {
        Set<Long> existingIds = new HashSet<>(lineItemRepo.findIdsByInvoiceId(invoiceId));
        Set<Long> retainedIds = new HashSet<>();
        for (LineItem line : incoming) {
            if (line.lineItemID() != null && existingIds.contains(line.lineItemID())) {
                lineItemRepo.updateLineItem(line.lineItemID(), line, user);
                retainedIds.add(line.lineItemID());
            } else {
                lineItemRepo.insertLineItem(invoiceId, line, user);
            }
        }
        for (Long id : existingIds) {
            if (!retainedIds.contains(id)) lineItemRepo.deleteLineItem(id);
        }
    }

    private InvoiceDetails withId(InvoiceDetails details, Long newId, String status) {
        return new InvoiceDetails(
                newId, details.invNumber(), details.invoiceDate(), status, details.invType(),
                details.maturity(), details.fobCode(), details.primarySortCode(),
                details.totalAmt(), details.totalPieces(), details.totalVol(),
                details.submitterClientNum(), details.submitterLocation(), details.submittedBy(),
                details.clientNumber(), details.clientLocation(),
                details.otherClientNum(), details.otherClientLocation(),
                details.otherClientName(), details.otherClientCity(), details.otherClientProvState(),
                details.boomNumbers(), details.timberMarks(), details.weightSlips(),
                details.replaceInvNum(), details.adjustInvNum(),
                details.reviewComments(), details.submitComments(), details.entryUserID()
        );
    }

    private InvoiceDetails withReviewComments(InvoiceDetails details, String reviewComments) {
        return new InvoiceDetails(
                details.invID(), details.invNumber(), details.invoiceDate(), details.invStatus(), details.invType(),
                details.maturity(), details.fobCode(), details.primarySortCode(),
                details.totalAmt(), details.totalPieces(), details.totalVol(),
                details.submitterClientNum(), details.submitterLocation(), details.submittedBy(),
                details.clientNumber(), details.clientLocation(),
                details.otherClientNum(), details.otherClientLocation(),
                details.otherClientName(), details.otherClientCity(), details.otherClientProvState(),
                details.boomNumbers(), details.timberMarks(), details.weightSlips(),
                details.replaceInvNum(), details.adjustInvNum(),
                reviewComments, details.submitComments(), details.entryUserID()
        );
    }

    private InvoiceDetails withReviewCommentsAndStatus(InvoiceDetails details, String reviewComments, String status) {
        return new InvoiceDetails(
                details.invID(), details.invNumber(), details.invoiceDate(), status, details.invType(),
                details.maturity(), details.fobCode(), details.primarySortCode(),
                details.totalAmt(), details.totalPieces(), details.totalVol(),
                details.submitterClientNum(), details.submitterLocation(), details.submittedBy(),
                details.clientNumber(), details.clientLocation(),
                details.otherClientNum(), details.otherClientLocation(),
                details.otherClientName(), details.otherClientCity(), details.otherClientProvState(),
                details.boomNumbers(), details.timberMarks(), details.weightSlips(),
                details.replaceInvNum(), details.adjustInvNum(),
                reviewComments == null ? details.reviewComments() : reviewComments,
                details.submitComments(), details.entryUserID()
        );
    }

    // ---------------------------------------------------------------
    // GROUP-SUMMARY EXPORT (CSV / PDF)
    // Formats the group rows + their line items exactly as supplied by the
    // client (what's on screen) into a sectioned, readable document.
    // ---------------------------------------------------------------

    private static final String[] LINE_ITEM_HEADERS = {
        "Secondary sort code", "Species", "Client secondary sort code",
        "Number pieces", "Grade", "Volume", "$ Price", "$ Amount"
    };
    private static final float[] LINE_ITEM_WIDTHS = {1.6f, 1f, 1.8f, 1.2f, 0.9f, 1.2f, 1.2f, 1.2f};
    private static final String EXPORT_FILENAME_BASE = "InvoiceGroupSummary";

    public ReportResult exportCsv(InvoiceSummaryExportRequest request) {
        return new ReportResult(generateExportCsv(request), exportFilename(request, "csv"));
    }

    public ReportResult exportPdf(InvoiceSummaryExportRequest request) {
        return new ReportResult(generateExportPdf(request), exportFilename(request, "pdf"));
    }

    private static List<GroupSummaryExportRow> exportRows(InvoiceSummaryExportRequest request) {
        return request != null && request.rows() != null ? request.rows() : List.of();
    }

    private byte[] generateExportPdf(InvoiceSummaryExportRequest request) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font groupFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font totalsFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font emptyFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);

            String title = "Invoice Group Summary"
                    + (isBlankExport(request.invoiceNumber()) ? "" : " — " + request.invoiceNumber());
            Paragraph titlePara = new Paragraph(title, titleFont);
            titlePara.setSpacingAfter(12);
            document.add(titlePara);

            for (GroupSummaryExportRow group : exportRows(request)) {
                Paragraph heading = new Paragraph(groupHeading(group), groupFont);
                heading.setSpacingBefore(6);
                heading.setSpacingAfter(2);
                document.add(heading);

                Paragraph totals = new Paragraph(groupTotals(group), totalsFont);
                totals.setSpacingAfter(4);
                document.add(totals);

                List<LineItemExportRow> items = group.lineItems() != null ? group.lineItems() : List.of();
                if (items.isEmpty()) {
                    document.add(new Paragraph("No line items.", emptyFont));
                    continue;
                }

                PdfPTable table = new PdfPTable(LINE_ITEM_HEADERS.length);
                table.setWidthPercentage(100);
                table.setWidths(LINE_ITEM_WIDTHS);
                table.setSpacingAfter(8);
                for (String header : LINE_ITEM_HEADERS) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setBackgroundColor(new Color(230, 230, 230));
                    cell.setPadding(3);
                    table.addCell(cell);
                }
                for (LineItemExportRow li : items) {
                    addExportCell(table, li.secondarySort(), cellFont);
                    addExportCell(table, li.species(), cellFont);
                    addExportCell(table, li.clientSecondarySort(), cellFont);
                    addExportCell(table, intStrExport(li.numberPieces()), cellFont);
                    addExportCell(table, li.grade(), cellFont);
                    addExportCell(table, decStrExport(li.volume(), 3), cellFont);
                    addExportCell(table, decStrExport(li.price(), 2), cellFont);
                    addExportCell(table, decStrExport(li.amount(), 2), cellFont);
                }
                document.add(table);
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new ReportGenerationException("Failed to generate invoice group summary PDF", e);
        }
    }

    private byte[] generateExportCsv(InvoiceSummaryExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice Group Summary,").append(csvField(nullToEmptyExport(request.invoiceNumber()))).append('\n');

        for (GroupSummaryExportRow group : exportRows(request)) {
            sb.append('\n');
            // Group header line — labelled summary values so the block is self-describing.
            sb.append(csvField("Group " + intStrExport(group.groupNumber()))).append(',')
              .append(csvField("Secondary sort: " + nullToEmptyExport(group.secondarySort()))).append(',')
              .append(csvField("Description: " + nullToEmptyExport(group.description()))).append(',')
              .append(csvField("Species: " + nullToEmptyExport(group.species()))).append(',')
              .append(csvField("Total pieces: " + intStrExport(group.totalPieces()))).append(',')
              .append(csvField("Total volume: " + decStrExport(group.totalVolume(), 3))).append(',')
              .append(csvField("Total $ amount: " + decStrExport(group.totalAmount(), 2))).append(',')
              .append(csvField("Price conversion: " + nullToEmptyExport(group.priceConversion()))).append('\n');

            List<LineItemExportRow> items = group.lineItems() != null ? group.lineItems() : List.of();
            if (items.isEmpty()) {
                sb.append(",No line items.\n");
                continue;
            }
            // Line-item sub-header, indented one column under the group header.
            sb.append(',').append(String.join(",", LINE_ITEM_HEADERS)).append('\n');
            for (LineItemExportRow li : items) {
                sb.append(',')
                  .append(csvField(li.secondarySort())).append(',')
                  .append(csvField(li.species())).append(',')
                  .append(csvField(li.clientSecondarySort())).append(',')
                  .append(csvField(intStrExport(li.numberPieces()))).append(',')
                  .append(csvField(li.grade())).append(',')
                  .append(csvField(decStrExport(li.volume(), 3))).append(',')
                  .append(csvField(decStrExport(li.price(), 2))).append(',')
                  .append(csvField(decStrExport(li.amount(), 2))).append('\n');
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String groupHeading(GroupSummaryExportRow g) {
        StringBuilder sb = new StringBuilder("Group ").append(intStrExport(g.groupNumber()));
        sb.append(":  ").append(nullToEmptyExport(g.secondarySort())).append(" / ").append(nullToEmptyExport(g.species()));
        if (!isBlankExport(g.description())) {
            sb.append("  —  ").append(g.description());
        }
        return sb.toString();
    }

    private static String groupTotals(GroupSummaryExportRow g) {
        return "Total pieces: " + intStrExport(g.totalPieces())
                + "      Total volume: " + decStrExport(g.totalVolume(), 3)
                + "      Total $ amount: " + decStrExport(g.totalAmount(), 2)
                + "      Price conversion: " + nullToEmptyExport(g.priceConversion());
    }

    private void addExportCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", font));
        cell.setPadding(3);
        table.addCell(cell);
    }

    private static String exportFilename(InvoiceSummaryExportRequest request, String ext) {
        String suffix = isBlankExport(request.invoiceNumber())
                ? ""
                : "_" + request.invoiceNumber().trim().replaceAll("[^A-Za-z0-9._-]", "_");
        return EXPORT_FILENAME_BASE + suffix + "." + ext;
    }

    private static String intStrExport(Integer value) {
        return value != null ? value.toString() : "";
    }

    private static String decStrExport(BigDecimal value, int scale) {
        return value != null ? value.setScale(scale, RoundingMode.HALF_UP).toPlainString() : "";
    }

    private static String nullToEmptyExport(String value) {
        return value != null ? value : "";
    }

    private static boolean isBlankExport(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String csvField(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
