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
import ca.bc.gov.nrs.csp.backend.util.constants.PermissionConstants;
import ca.bc.gov.nrs.csp.backend.util.constants.Roles;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.invoice.manual.InvoiceValidator;
import ca.bc.gov.nrs.csp.backend.invoice.manual.ManualInvoiceTotals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    /**
     * Statuses in which an invoice sits in an active review queue, and therefore the
     * ones whose cancellation has to cascade to the parent submission. Mirrors the
     * statuses the UI offers Approve / Reject / Cancel from.
     */
    private static final Set<String> REVIEW_QUEUE_STATUSES = Set.of(
            ConstantsCode.INVENTRYSTATUS_PROCESSING, ConstantsCode.INVENTRYSTATUS_UNAPPROVED);

    /** Statuses an invoice can be submitted for review from. */
    private static final Set<String> SUBMITTABLE_STATUSES = Set.of(
            ConstantsCode.INVENTRYSTATUS_DRAFT, ConstantsCode.INVENTRYSTATUS_UNAPPROVED);

    /** Statuses in which Data Entry may change a manual invoice and its line items. */
    private static final Set<String> DATA_ENTRY_EDITABLE_STATUSES = Set.of(
            ConstantsCode.INVENTRYSTATUS_DRAFT, ConstantsCode.INVENTRYSTATUS_UNAPPROVED);

    // Same as the UI's DELETABLE_STATUSES, so the API refuses what the page never offers.
    private static final Set<String> DELETABLE_STATUSES = Set.of(
            ConstantsCode.INVENTRYSTATUS_DRAFT, ConstantsCode.INVENTRYSTATUS_PROCESSING);

    public static final String STALE_INVOICE_MESSAGE =
            "This invoice was changed by another user after you opened it. Refresh the page to see their changes, then try again.";

    private final InvoiceRepository invoiceRepo;
    private final LineItemRepository lineItemRepo;
    private final CspSubmissionRepository submissionRepo;
    private final LogSaleParticipantRepository participantRepo;
    private final CommonValidation commonValidation;
    private final PriceConversionService priceConversionService;
    private final InvoiceMapper mapper;
    private final Clock clock;

    public InvoiceService(InvoiceRepository invoiceRepo,
                          LineItemRepository lineItemRepo,
                          CspSubmissionRepository submissionRepo,
                          LogSaleParticipantRepository participantRepo,
                          CommonValidation commonValidation,
                          PriceConversionService priceConversionService,
                          InvoiceMapper mapper,
                          Clock clock) {
        this.invoiceRepo = invoiceRepo;
        this.lineItemRepo = lineItemRepo;
        this.submissionRepo = submissionRepo;
        this.participantRepo = participantRepo;
        this.commonValidation = commonValidation;
        this.priceConversionService = priceConversionService;
        this.mapper = mapper;
        this.clock = clock;
    }

    // ---------------------------------------------------------------
    // GET
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
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

        // Processing invoices show a conversion preview without writing it, like legacy.
        // Other statuses show the stored prices. Save, submit and approve persist the conversion.
        if (ConstantsCode.INVENTRYSTATUS_PROCESSING.equals(loaded.details().invStatus())) {
            PriceConversionService.Result conversion = priceConversionService
                    .apply(lines, loaded.details().maturity(), loaded.details().invoiceDate());
            lines = conversion.lines();
            validation = withConversionWarnings(validation, conversion);
        }

        return mapper.toResponse(loaded.details(), loaded.submissionId(), loaded.submissionNumber(), lines, validation,
                loaded.version());
    }

    // ---------------------------------------------------------------
    // CREATE (always DRAFT)
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        boolean dataEntryOnly = isDataEntryOnly();
        if (dataEntryOnly) {
            requireManualEntry(request.manual());
        }
        String user = SecurityContextUtils.requireUsername();
        List<LineItem> lines = mapper.toLineItems(request.lineItems(), null, request.invType());
        InvoiceDetails details = withChannelTotals(mapper.toDetails(request, user), lines, request.manual());
        if (dataEntryOnly) {
            details = withReviewComments(details, null);
        }

        ValidationResult result = newValidator().validate(details, lines, request.manual(), ActionType.SAVE);
        throwIfErrors(result, "Invoice failed validation on create.");

        // Create a submission for this new manual invoice.
        String submissionStatus = request.manual()
                ? ConstantsCode.SUBMSTATUS_LOBBY
                : ConstantsCode.SUBMSTATUS_INBOX;
        Long submissionId = submissionRepo.insertSubmission(
                details.submitterClientNum(), details.submitterLocation(), submissionStatus, user);

        // If the other party is a manual entry (no client number, but name/city/province set),
        // link a log_sale_participant row via the correct buyer/seller slot, reusing a matching one like legacy.
        Long buyerParticipantId = null;
        Long sellerParticipantId = null;
        if (hasManualOtherParty(details)) {
            Long pid = participantRepo.findOrInsert(
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
        List<Long> replacedIds = invoiceRepo.replaceRelatedInvoices(newInvoiceId, ConstantsCode.INVRELATETYPE_REPLACE,
                details.replaceInvNum(), details.submitterClientNum(), details.submitterLocation(), user);
        invoiceRepo.replaceRelatedInvoices(newInvoiceId, ConstantsCode.INVRELATETYPE_ADJUST,
                details.adjustInvNum(), details.submitterClientNum(), details.submitterLocation(), user);
        cancelReplacedInvoices(newInvoiceId, replacedIds, user);

        InvoiceDetails saved = withId(details, newInvoiceId, ConstantsCode.INVENTRYSTATUS_DRAFT);
        // A freshly-created submission has no business submission number yet
        // (insertSubmission only sets the surrogate csp_submission_id).
        return mapper.toResponse(saved, submissionId, null, lineItemRepo.findByInvoiceId(newInvoiceId), result,
                invoiceRepo.findVersion(newInvoiceId));
    }

    // ---------------------------------------------------------------
    // UPDATE (preserves status; status transitions happen via /submit or /status)
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse update(Long id, String expectedVersion, UpdateInvoiceRequest request) {
        String user = SecurityContextUtils.requireUsername();
        // Read before the version check, so these are the lines the caller saw. Legacy line adds don't
        // lock the header, so one can commit after the check; it must be kept, not deleted as dropped.
        Set<Long> existingLineIds = new HashSet<>(lineItemRepo.findIdsByInvoiceId(id));
        requireCurrentVersion(id, expectedVersion);
        LoadedInvoice existing = loadInvoiceForEntryAction(id);
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

        InvoiceForm form = toInvoiceForm(id, existing, request);
        InvoiceDetails details = form.details();

        ValidationResult result = newValidator().validate(details, form.lines(), request.manual(), ActionType.SAVE);
        throwIfErrors(result, "Invoice failed validation on update.");

        writeInvoiceForm(id, existing, form, existingLineIds, user);

        // Saving an existing invoice reverts its submission to LOBBY.
        if (existing.submissionId() != null) {
            submissionRepo.updateSubmissionStatus(existing.submissionId(), ConstantsCode.SUBMSTATUS_LOBBY, user);
        }

        // Flat-price conversion: compute and persist each line's converted price on save.
        // Converted-price writes leave the version alone, so the snapshot can come first.
        ResponseSnapshot snapshot = readResponseSnapshot(id);
        PriceConversionService.Result conversion = priceConversionService.apply(
                snapshot.lines(), details.maturity(), details.invoiceDate());
        for (LineItem line : conversion.lines()) {
            lineItemRepo.updateConvertedPrice(line.lineItemID(), line.convertedPrice(), user);
        }

        InvoiceDetails saved = withId(details, id, ConstantsCode.INVENTRYSTATUS_DRAFT);
        return mapper.toResponse(saved, existing.submissionId(), existing.submissionNumber(), conversion.lines(),
                withConversionWarnings(result, conversion), snapshot.version());
    }

    private record InvoiceForm(InvoiceDetails details, List<LineItem> lines) {}

    // The header and lines as the staff form sent them, ready to validate and write.
    private InvoiceForm toInvoiceForm(Long id, LoadedInvoice existing, UpdateInvoiceRequest request) {
        boolean dataEntryOnly = isDataEntryOnly();
        if (dataEntryOnly) {
            requireManualEntry(request.manual());
        }
        List<LineItem> lines = mapper.toLineItems(request.lineItems(), id, request.invType());
        InvoiceDetails details =
                mapper.toDetails(request, id, ConstantsCode.INVENTRYSTATUS_DRAFT, existing.details().entryUserID());
        // The stored channel decides, not the request: an electronic invoice keeps the totals industry declared.
        // Legacy overwrote them with the line sums on every staff save; modern keeps them, as it does at intake.
        details = existing.submissionNumber() == null
                ? ManualInvoiceTotals.withCalculatedTotals(details, lines)
                : withTotalsOf(details, existing.details());
        if (dataEntryOnly) {
            details = withReviewComments(details, existing.details().reviewComments());
        }
        return new InvoiceForm(details, lines);
    }

    // Writes a validated form as a Draft. Called after the version check, inside the caller's transaction.
    private void writeInvoiceForm(Long id, LoadedInvoice existing, InvoiceForm form, Set<Long> existingLineIds,
                                  String user) {
        InvoiceDetails details = form.details();
        // Pick the participant first, then point the invoice at it, then release the old one.
        // Releasing before updateInvoice would violate the CLS_CLSP_FK constraint.
        ParticipantPlan plan = reconcileParticipant(existing, details, user);

        invoiceRepo.updateInvoice(id, details, ConstantsCode.INVENTRYSTATUS_DRAFT,
                plan.newIds().buyerId(), plan.newIds().sellerId(), user);
        plan.released().forEach(participantRepo::deleteIfUnreferenced);

        reconcileLineItems(id, form.lines(), existingLineIds, user);
        invoiceRepo.replaceLogSources(id, ConstantsCode.LOGSOURCECODE_BOOMNUMBER, details.boomNumbers(), user);
        invoiceRepo.replaceLogSources(id, ConstantsCode.LOGSOURCECODE_TIMERMARK, details.timberMarks(), user);
        invoiceRepo.replaceLogSources(id, ConstantsCode.LOGSOURCECODE_WEIGHSLIP, details.weightSlips(), user);
        List<Long> replacedIds = invoiceRepo.replaceRelatedInvoices(id, ConstantsCode.INVRELATETYPE_REPLACE,
                details.replaceInvNum(), details.submitterClientNum(), details.submitterLocation(), user);
        invoiceRepo.replaceRelatedInvoices(id, ConstantsCode.INVRELATETYPE_ADJUST,
                details.adjustInvNum(), details.submitterClientNum(), details.submitterLocation(), user);
        cancelReplacedInvoices(id, replacedIds, user);
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    @Transactional
    public void delete(Long id, String expectedVersion) {
        String user = SecurityContextUtils.requireUsername();
        requireCurrentVersion(id, expectedVersion);
        LoadedInvoice existing = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + id + " was not found."));
        if (existing.submissionNumber() != null) {
            throw new ConflictException("Cannot delete an electronic invoice.");
        }
        if (!DELETABLE_STATUSES.contains(existing.details().invStatus())) {
            throw new ConflictException("Only draft or processing invoices can be deleted.");
        }

        // Legacy parity: snapshot the header and every line as 'D' audit rows before anything is removed.
        invoiceRepo.insertDeleteAudit(id, user);
        lineItemRepo.auditAndDeleteAllByInvoiceId(id, user);
        invoiceRepo.deleteAllLogSources(id);
        // Clean up BOTH directions of the related-invoice relationship before we delete
        // the parent row, so the RELATED_COASTAL_LOG_SALE_ID FK can't block us when some
        // other invoice was referencing this one.
        invoiceRepo.deleteAllRelatedInvoiceRefs(id);
        invoiceRepo.deleteAllIncomingRelatedInvoiceRefs(id);
        invoiceRepo.deleteInvoice(id);
        // Release participant rows AFTER the invoice (the CLS_CLSP_FK constraint requires
        // the referencing coastal_log_sale row to be gone first). Another invoice may still share them.
        participantRepo.deleteIfUnreferenced(existing.buyerParticipantId());
        participantRepo.deleteIfUnreferenced(existing.sellerParticipantId());
        deleteSubmissionIfNowEmptyAndManual(existing);
        log.info("Deleted invoice id={}", id);
    }

    /**
     * Remove the parent submission when deleting this invoice has left it empty — but only for a
     * manually-entered one.
     *
     * <p>{@link #create} inserts a {@code csp_submission} row for every manual invoice, purely as
     * a parent to hang it on. Deleting the invoice used to leave that row behind with nothing
     * referencing it, so each created-then-deleted invoice leaked one orphan. Nothing surfaced it:
     * the Inbox inner-joins invoices, so an empty submission never appears anywhere.
     *
     * <p>Two guards, both deliberate:
     * <ul>
     *   <li><b>Only when empty.</b> An ESF submission carries many invoices, and a manual one can
     *       have gained siblings through Duplicate — removing the parent while any remain would
     *       violate the {@code CLS_CSPS_FK} foreign key and orphan those invoices instead.</li>
     *   <li><b>Only when manual</b> ({@code submissionNumber} is null). An ESF submission is a
     *       record of a real transmission — it carries the business submission number and the
     *       submitter's contact details — so it is kept even once its last invoice is gone. This
     *       undoes exactly what {@code create} did and nothing more.</li>
     * </ul>
     *
     * <p>Safe to run inside the delete transaction: {@code coastal_log_sale} holds the only foreign
     * key into {@code csp_submission}, and those rows are gone by this point.
     */
    private void deleteSubmissionIfNowEmptyAndManual(LoadedInvoice existing) {
        Long submissionId = existing.submissionId();
        if (submissionId == null || existing.submissionNumber() != null) {
            return;
        }
        int remaining = invoiceRepo.countByCspSubmissionId(submissionId);
        if (remaining > 0) {
            return;
        }
        submissionRepo.delete(submissionId);
        log.info("Deleted now-empty manual submission submissionId={}", submissionId);
    }

    // ---------------------------------------------------------------
    // SUBMIT (DRAFT or UNAPPROVED → PROCESSING)
    // ---------------------------------------------------------------

    // Legacy Submit saves the form on screen and submits it in one step (InvoiceBean.saveSubmitInvoiceAction),
    // so the form is validated once for Submit and written before the status change.
    @Transactional
    public InvoiceResponse submit(Long id, String expectedVersion, UpdateInvoiceRequest request) {
        String user = SecurityContextUtils.requireUsername();
        // Read before the version check, as in update.
        Set<Long> existingLineIds = new HashSet<>(lineItemRepo.findIdsByInvoiceId(id));
        requireCurrentVersion(id, expectedVersion);
        LoadedInvoice existing = loadInvoiceForEntryAction(id);
        if (!SUBMITTABLE_STATUSES.contains(existing.details().invStatus())) {
            throw new ConflictException("Only draft or unapproved invoices can be submitted.");
        }

        InvoiceForm form = toInvoiceForm(id, existing, request);
        InvoiceDetails details = form.details();
        ValidationResult result = newValidator().validate(details, form.lines(), /*manual*/ true, ActionType.SUBMIT);
        throwIfErrors(result, "Invoice failed validation on submit.");

        writeInvoiceForm(id, existing, form, existingLineIds, user);
        invoiceRepo.updateStatus(id, ConstantsCode.INVENTRYSTATUS_PROCESSING, user);
        submissionRepo.updateSubmissionStatus(existing.submissionId(), ConstantsCode.SUBMSTATUS_INBOX, user);
        log.info("Submitted invoice id={} submissionId={}", id, existing.submissionId());

        // Flat-price conversion ("price spread"): compute each line's converted
        // price and persist it. Converted-price writes leave the version alone.
        ResponseSnapshot snapshot = readResponseSnapshot(id);
        PriceConversionService.Result conversion = priceConversionService.apply(
                snapshot.lines(), details.maturity(), details.invoiceDate());
        for (LineItem line : conversion.lines()) {
            lineItemRepo.updateConvertedPrice(line.lineItemID(), line.convertedPrice(), user);
        }

        // Surface the conversion warnings (factor-not-found / out-of-range)
        // alongside the validator's.
        ValidationResult responseResult = withConversionWarnings(result, conversion);

        InvoiceDetails saved = withId(details, id, ConstantsCode.INVENTRYSTATUS_PROCESSING);
        return mapper.toResponse(saved, existing.submissionId(), existing.submissionNumber(), conversion.lines(),
                responseResult, snapshot.version());
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

        // Reset id/status on the cloned details and persist as a new DRAFT. A
        // manual invoice's totals come from the lines being cloned rather than
        // from the source's stored totals, so a clone can't inherit a stale one.
        InvoiceDetails cloned = withChannelTotals(
                withId(existing.details(), null, ConstantsCode.INVENTRYSTATUS_DRAFT),
                existingLines, existing.submissionNumber() == null);

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
        return mapper.toResponse(saved, submissionId, existing.submissionNumber(), lineItemRepo.findByInvoiceId(newInvoiceId), null,
                invoiceRepo.findVersion(newInvoiceId));
    }

    // ---------------------------------------------------------------
    // CHANGE STATUS — approve / reject / cancel / unapprove
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse changeStatus(Long id, String expectedVersion, ChangeStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new BadRequestException("Status is required.");
        }
        String user = SecurityContextUtils.requireUsername();
        requireCurrentVersion(id, expectedVersion);
        LoadedInvoice existing = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + id + " was not found."));

        // Apply the supplied reviewer comments to the details we hand to the validator
        // so its "reviewer comments must change" check sees the new value.
        // A null comment in the request means "leave the note as it is" — the write below is
        // skipped for it. So validate against the STORED value in that case, not against null:
        // otherwise the must-have-changed rule could be bypassed by simply omitting the field,
        // since null reads as "" and "" differs from any non-blank stored note.
        String effectiveComments = request.reviewComments() != null
                ? request.reviewComments()
                : existing.details().reviewComments();
        InvoiceDetails withNewComments = withReviewComments(existing.details(), effectiveComments);

        ValidationResult result = newValidator().validateForChangeStatus(withNewComments, request.status(), user);
        throwIfErrors(result, "Invoice failed validation on status change.");

        if (ConstantsCode.INVENTRYSTATUS_APPROVED.equals(request.status())) {
            persistConversionForApproval(id, existing.details(), user);
        }

        if (request.reviewComments() != null) {
            invoiceRepo.updateReviewerNotes(id, request.reviewComments(), user);
        }
        invoiceRepo.updateStatus(id, request.status(), user);
        log.info("Changed invoice id={} status={}", id, request.status());

        // Cascade the change to the submission status (after the invoice's own
        // status is persisted, so the counts below reflect the new state).
        applySubmissionStatusOnStatusChange(existing.submissionId(), request.status(), user);

        InvoiceDetails saved = withReviewCommentsAndStatus(existing.details(), request.reviewComments(), request.status());
        ResponseSnapshot snapshot = readResponseSnapshot(id);
        return mapper.toResponse(saved, existing.submissionId(), existing.submissionNumber(), snapshot.lines(), result,
                snapshot.version());
    }

    // Legacy approve re-runs the conversion with ERROR severity, so a factor-not-found or
    // out-of-range message refuses approval before any write. Otherwise the prices are saved.
    private void persistConversionForApproval(Long id, InvoiceDetails details, String user) {
        PriceConversionService.Result conversion = priceConversionService.apply(
                lineItemRepo.findByInvoiceId(id), details.maturity(), details.invoiceDate());
        if (!conversion.warnings().isEmpty()) {
            List<ValidationMessage> errors = conversion.warnings().stream()
                    .map(message -> new ValidationMessage(message.messageKey(), message.args(), MessageType.ERROR))
                    .toList();
            throw new ValidationException("Invoice failed price conversion on approval.", new ValidationResult(errors));
        }
        for (LineItem line : conversion.lines()) {
            lineItemRepo.updateConvertedPrice(line.lineItemID(), line.convertedPrice(), user);
        }
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

    /**
     * A replacement invoice supersedes every invoice named in its "Replaces invoice
     * number(s)" field, so saving it cancels those originals. Called from create and
     * update with the ids {@link InvoiceRepository#replaceRelatedInvoices} resolved for
     * the REP relationship (never ADJ — an adjusting invoice leaves its target alone).
     *
     * <p>Skipped: an already-CANCELLED original (nothing to change) and a self-reference
     * (the validator rejects "replaces itself", so this is only a defensive guard).</p>
     *
     * <p>The submission-status cascade only runs when the original was in a
     * {@link #REVIEW_QUEUE_STATUSES review-queue status}, which is what the cascade was
     * written for; it clears a replaced invoice out of the inbox. Cancelling a DRAFT
     * original deliberately leaves its submission in the lobby:
     * the cascade would otherwise push a lobby submission (possibly holding other drafts)
     * to REJECTED.</p>
     */
    private void cancelReplacedInvoices(Long replacementId, List<Long> replacedIds, String user) {
        if (replacedIds == null) return;
        for (Long replacedId : replacedIds) {
            cancelReplacedInvoice(replacementId, replacedId, user);
        }
    }

    /**
     * Cancels a single replaced invoice. Skips a null id, a self-reference, an invoice
     * that no longer exists, and one that is already CANCELLED.
     */
    private void cancelReplacedInvoice(Long replacementId, Long replacedId, String user) {
        if (replacedId == null || replacedId.equals(replacementId)) return;
        // Lock it before reading its status, so a concurrent change to it can't land in between.
        if (invoiceRepo.lockVersion(replacedId).isEmpty()) return;
        LoadedInvoice replaced = loadInvoiceOrThrow(replacedId);
        String previousStatus = replaced.details().invStatus();
        if (ConstantsCode.INVENTRYSTATUS_CANCELLED.equals(previousStatus)) return;
        // Cancelling an Unapproved or Processing invoice is a review decision, so Data Entry replaces drafts only.
        if (isDataEntryOnly() && (replaced.submissionNumber() != null
                || !ConstantsCode.INVENTRYSTATUS_DRAFT.equals(previousStatus))) {
            throw new ConflictException("Ask an approver to save the replacement for invoice "
                    + replaced.details().invNumber()
                    + ". Data Entry can replace manual invoices in Draft status.");
        }

        invoiceRepo.updateStatus(replacedId, ConstantsCode.INVENTRYSTATUS_CANCELLED, user);
        log.info("Cancelled invoice id={} (was {}) as it is replaced by invoice id={}",
                replacedId, previousStatus, replacementId);
        if (REVIEW_QUEUE_STATUSES.contains(previousStatus)) {
            applySubmissionStatusOnStatusChange(
                    replaced.submissionId(), ConstantsCode.INVENTRYSTATUS_CANCELLED, user);
        }
    }

    // ---------------------------------------------------------------
    // LINE ITEMS (sub-resource — only valid on DRAFT invoices)
    // ---------------------------------------------------------------

    @Transactional
    public InvoiceResponse addLineItem(Long invoiceId, String expectedVersion, LineItemRequest request) {
        String user = SecurityContextUtils.requireUsername();
        requireCurrentVersion(invoiceId, expectedVersion);
        LoadedInvoice existing = loadInvoiceForEntryAction(invoiceId);
        requireAddLineItemAllowed(existing);
        // Build the candidate set: every existing line plus the new one. Run
        // the full invoice validator against the full set so totals-variance
        // / line-count rules still fire correctly.
        List<LineItem> existingLines = lineItemRepo.findByInvoiceId(invoiceId);
        LineItem newLine = mapper.toLineItem(request, invoiceId, existing.details().invType());
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
    public InvoiceResponse updateLineItem(Long invoiceId, Long lineId, String expectedVersion, LineItemRequest request) {
        String user = SecurityContextUtils.requireUsername();
        requireCurrentVersion(invoiceId, expectedVersion);
        LoadedInvoice existing = loadInvoiceForEntryAction(invoiceId);
        ensureLineBelongsToInvoice(invoiceId, lineId);

        List<LineItem> existingLines = lineItemRepo.findByInvoiceId(invoiceId);
        LineItem mapped = mapper.toLineItem(request, invoiceId, existing.details().invType());
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
    public InvoiceResponse deleteLineItem(Long invoiceId, Long lineId, String expectedVersion) {
        String user = SecurityContextUtils.requireUsername();
        requireCurrentVersion(invoiceId, expectedVersion);
        LoadedInvoice existing = loadInvoiceForEntryAction(invoiceId);
        ensureLineBelongsToInvoice(invoiceId, lineId);

        lineItemRepo.auditAndDeleteLineItem(lineId, user);
        log.info("Deleted line item id={} invoiceId={}", lineId, invoiceId);

        return revertToDraftAndRespond(existing, user);
    }

    /**
     * Every line-item change reverts the invoice to DRAFT and its submission to
     * LOBBY (a no-op when already there), re-derives the header totals from the
     * now-persisted lines, then re-validates the record with
     * {@link ActionType#OTHER} — the same action the passive GET uses — so the
     * response carries both warnings and any ERROR-type messages that still
     * describe the saved record. It also bumps the invoice's revision count, so
     * a line-only change still stops anyone else's stale save.
     */
    private InvoiceResponse revertToDraftAndRespond(LoadedInvoice existing, String user) {
        Long invoiceId = existing.details().invID();
        invoiceRepo.incrementRevisionCount(invoiceId, user);
        if (!ConstantsCode.INVENTRYSTATUS_DRAFT.equals(existing.details().invStatus())) {
            invoiceRepo.updateStatus(invoiceId, ConstantsCode.INVENTRYSTATUS_DRAFT, user);
            if (existing.submissionId() != null) {
                submissionRepo.updateSubmissionStatus(existing.submissionId(), ConstantsCode.SUBMSTATUS_LOBBY, user);
            }
            log.info("Line-item change reverted invoice id={} to DRAFT submissionId={}", invoiceId, existing.submissionId());
        }
        List<LineItem> currentLines = lineItemRepo.findByInvoiceId(invoiceId);
        boolean manual = existing.submissionNumber() == null;
        InvoiceDetails draftDetails = withId(existing.details(), invoiceId, ConstantsCode.INVENTRYSTATUS_DRAFT);
        if (manual) {
            draftDetails = persistCalculatedTotals(draftDetails, currentLines, user);
        }
        ResponseSnapshot snapshot = readResponseSnapshot(invoiceId);
        ValidationResult result = newValidator().validate(draftDetails, snapshot.lines(), manual, ActionType.OTHER);
        return mapper.toResponse(draftDetails, existing.submissionId(), existing.submissionNumber(), snapshot.lines(), result,
                snapshot.version());
    }

    /**
     * The totals to store for this channel: derived from {@code lines} for a
     * manually-entered invoice, left exactly as submitted for an ESF one.
     */
    private static InvoiceDetails withChannelTotals(InvoiceDetails details, List<LineItem> lines, boolean manual) {
        return manual ? ManualInvoiceTotals.withCalculatedTotals(details, lines) : details;
    }

    /**
     * Write the totals calculated from {@code lines} onto the invoice header and
     * return the details carrying them. A no-op write when they already agree, so
     * a line-item change that doesn't move the totals doesn't bump the row's
     * revision count.
     */
    private InvoiceDetails persistCalculatedTotals(InvoiceDetails details, List<LineItem> lines, String user) {
        InvoiceDetails recalculated = ManualInvoiceTotals.withCalculatedTotals(details, lines);
        if (sameTotals(details, recalculated)) {
            return details;
        }
        invoiceRepo.updateTotals(details.invID(), recalculated.totalPieces(),
                recalculated.totalVol(), recalculated.totalAmt(), user);
        log.debug("Recalculated totals for invoice id={}: pieces={} volume={} amount={}",
                details.invID(), recalculated.totalPieces(), recalculated.totalVol(), recalculated.totalAmt());
        return recalculated;
    }

    /** Compares the three totals by value — {@code 0} and {@code 0.00} are the same total. */
    private static boolean sameTotals(InvoiceDetails a, InvoiceDetails b) {
        return Objects.equals(a.totalPieces(), b.totalPieces())
                && sameAmount(a.totalVol(), b.totalVol())
                && sameAmount(a.totalAmt(), b.totalAmt());
    }

    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    /**
     * Legacy parity: refuses a change made from an out-of-date copy of the invoice. The row lock
     * holds until the transaction ends, so two concurrent changes can't both pass the check.
     */
    private void requireCurrentVersion(Long invoiceId, String expectedVersion) {
        String currentVersion = invoiceRepo.lockVersion(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + invoiceId + " was not found."));
        if (!Objects.equals(currentVersion, expectedVersion)) {
            log.info("Refused stale change to invoice id={} expectedVersion={} currentVersion={}",
                    invoiceId, expectedVersion, currentVersion);
            throw new ConflictException(STALE_INVOICE_MESSAGE);
        }
    }

    private LoadedInvoice loadInvoiceOrThrow(Long invoiceId) {
        return invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + invoiceId + " was not found."));
    }

    private static boolean isDataEntryOnly() {
        List<String> roles = SecurityContextUtils.currentRoles();
        return roles.contains(Roles.ENTRY) && roles.stream()
                .noneMatch(role -> PermissionService.roleGrants(role, PermissionConstants.INVOICE_DETAILS_APPROVE));
    }

    private static void requireManualEntry(boolean manual) {
        if (!manual) {
            throw new AuthorizationDeniedException("Data Entry may only write manually entered invoices.");
        }
    }

    // Called after the invoice revision is checked and locked.
    private LoadedInvoice loadInvoiceForEntryAction(Long invoiceId) {
        LoadedInvoice invoice = loadInvoiceOrThrow(invoiceId);
        if (isDataEntryOnly()) {
            requireManualEntry(invoice.submissionNumber() == null);
            if (!DATA_ENTRY_EDITABLE_STATUSES.contains(invoice.details().invStatus())) {
                throw new ConflictException("Data Entry may only change draft or unapproved invoices.");
            }
        }
        return invoice;
    }

    private record ResponseSnapshot(String version, List<LineItem> lines) {}

    // Read after the last version-changing write. Version first: a legacy line add that commits
    // before the lines read can only make the version stale, never count a line the response omits.
    private ResponseSnapshot readResponseSnapshot(Long invoiceId) {
        String version = invoiceRepo.findVersion(invoiceId);
        return new ResponseSnapshot(version, lineItemRepo.findByInvoiceId(invoiceId));
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
        return new InvoiceValidator(invoiceRepo, commonValidation, clock);
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

    // Legacy shares a participant across invoices with the same name, city and province
    // (SubmissionTransferRules.getAndSaveOtherPartiFromDTO), so an edit relinks this invoice instead of
    // changing the shared row. The ids it stops using are returned to release once nothing refers to them.
    private ParticipantPlan reconcileParticipant(LoadedInvoice existing, InvoiceDetails details, String user) {
        Long participantId = hasManualOtherParty(details) ? participantFor(existing, details, user) : null;
        boolean intoBuyerSlot = otherPartyIsInBuyerSlot(details);
        ParticipantIds newIds = new ParticipantIds(
                intoBuyerSlot ? participantId : null, intoBuyerSlot ? null : participantId);
        List<Long> released = new ArrayList<>();
        for (Long oldId : new Long[] {existing.buyerParticipantId(), existing.sellerParticipantId()}) {
            if (oldId != null && !oldId.equals(participantId)) released.add(oldId);
        }
        return new ParticipantPlan(newIds, released);
    }

    // Keeps the linked row while its details are unchanged, so an unrelated save doesn't relink it.
    private Long participantFor(LoadedInvoice existing, InvoiceDetails details, String user) {
        Long current = otherPartyIsInBuyerSlot(existing.details())
                ? existing.buyerParticipantId()
                : existing.sellerParticipantId();
        boolean unchanged = Objects.equals(existing.details().otherClientName(), details.otherClientName())
                && Objects.equals(existing.details().otherClientCity(), details.otherClientCity())
                && Objects.equals(existing.details().otherClientProvState(), details.otherClientProvState());
        if (current != null && unchanged) return current;
        return participantRepo.findOrInsert(
                details.otherClientName(), details.otherClientCity(), details.otherClientProvState(), user);
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

    private record ParticipantPlan(ParticipantIds newIds, List<Long> released) {}

    private void reconcileLineItems(Long invoiceId, List<LineItem> incoming, Set<Long> existingIds, String user) {
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
            if (!retainedIds.contains(id)) lineItemRepo.auditAndDeleteLineItem(id, user);
        }
    }

    private InvoiceDetails withId(InvoiceDetails details, Long newId, String status) {
        return new InvoiceDetails(
                newId, details.invNumber(), details.invoiceDate(), status, details.invType(),
                details.maturity(), details.fobCode(), details.primarySortCode(), details.clientPrimarySortCode(),
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

    private static InvoiceDetails withTotalsOf(InvoiceDetails details, InvoiceDetails totalsSource) {
        return new InvoiceDetails(
                details.invID(), details.invNumber(), details.invoiceDate(), details.invStatus(), details.invType(),
                details.maturity(), details.fobCode(), details.primarySortCode(), details.clientPrimarySortCode(),
                totalsSource.totalAmt(), totalsSource.totalPieces(), totalsSource.totalVol(),
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
                details.maturity(), details.fobCode(), details.primarySortCode(), details.clientPrimarySortCode(),
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
                details.maturity(), details.fobCode(), details.primarySortCode(), details.clientPrimarySortCode(),
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
