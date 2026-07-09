package ca.bc.gov.nrs.csp.backend.util.validation.invoiceDetails;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.invoice.rules.Finding;
import ca.bc.gov.nrs.csp.backend.invoice.rules.InvoiceTotals;
import ca.bc.gov.nrs.csp.backend.invoice.rules.InvoiceTotalsRuleSet;
import ca.bc.gov.nrs.csp.backend.invoice.rules.Severity;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository.InvoiceMatch;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository.RelatedInvoice;
import ca.bc.gov.nrs.csp.backend.util.constants.ActionType;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class InvoiceValidator {

    private static final Logger log = LoggerFactory.getLogger(InvoiceValidator.class);

    private final InvoiceRepository invoiceRepo;
    private final CommonValidation commonValidation;
    private final List<ValidationMessage> messages = new ArrayList<>();

    private boolean manual;
    private String sellerClientNum;
    private String sellerClientLoc;
    private String buyerClientNum;
    private String buyerClientLoc;

    public InvoiceValidator(InvoiceRepository invoiceRepo, CommonValidation commonValidation) {
        this.invoiceRepo = invoiceRepo;
        this.commonValidation = commonValidation;
    }

    public ValidationResult validate(InvoiceDetails details,
                                     List<LineItem> lineItems,
                                     boolean manual,
                                     ActionType action) {
        messages.clear();
        this.manual = manual;
        if (details == null) {
            addError("invoice.details.missing.error", null);
            return new ValidationResult(messages);
        }

        LocalDate invDate = details.invoiceDate();
        if (invDate == null) {
            addError("invoice.date.required.error", new Object[]{details.invNumber()});
            return new ValidationResult(messages);
        }
        List<LineItem> lines = lineItems == null ? List.of() : lineItems;

        if (ConstantsCode.INVOICE_SUBMITTEDBY_SELLER.equals(details.submittedBy())) {
            sellerClientNum = blankToNull(details.submitterClientNum());
            sellerClientLoc = blankToNull(details.submitterLocation());
            buyerClientNum = blankToNull(details.otherClientNum());
            buyerClientLoc = blankToNull(details.otherClientLocation());
        } else {
            sellerClientNum = blankToNull(details.otherClientNum());
            sellerClientLoc = blankToNull(details.otherClientLocation());
            buyerClientNum = blankToNull(details.submitterClientNum());
            buyerClientLoc = blankToNull(details.submitterLocation());
        }

        checkForInvoiceNumDuplicate(details);
        checkMonthComplete(details);

        if (checkInvoiceType(details.invType(), invDate)) {
            checkInvoiceTypeForSalesOrPurchase(details.invType());
        }

        String replaces = details.replaceInvNum();
        String adjusts = details.adjustInvNum();
        if (!isBlank(replaces) && !isBlank(adjusts)) {
            addError("invoice.both.replace.adjust.invoicenum.error", null);
        }

        replaceAdjustInvoiceCheck(replaces, details.submitterClientNum(), details.submitterLocation(),
                "invoice.replace.invoicenumber.error", false);
        replaceAdjustByItselfCheck(replaces, details.invNumber(), "invoice.replace.with.itself.error");
        checkCsvMaxItems(replaces, ConstantsCode.MAXOFCSVFORREPLACEINVOICENUM,
                "invoice.morethanmax.replace.invoicenum.error");

        replaceAdjustInvoiceCheck(adjusts, details.submitterClientNum(), details.submitterLocation(),
                "invoice.adjust.invoicenumber.error", true);
        replaceAdjustByItselfCheck(adjusts, details.invNumber(), "invoice.adjust.with.itself.error");
        checkCsvMaxItems(adjusts, ConstantsCode.MAXOFCSVFORADJUSTINVOICENUM,
                "invoice.morethanmax.adjust.invoicenum.error");

        checkElectronicOtherPartySubmitter(details);
        checkSenderBuyerForInvoiceType(details);
        checkOtherPartyClient(details);
        // For manual invoices, confirm the submitter's own client number +
        // location actually exists in CSP. Both error paths from checkSubmiterClient
        // surface as page-level banner notifications (not inline field highlights).
        // The ESF path validates the submitter as buyer/seller separately.
        if (manual) {
            checkSubmiterClient(details.submitterClientNum(), details.submitterLocation(),
                    "invoice.submitter.client.location.invalid.error");
        }
        isSameSellerAndBuyer(details);
        isValidMaturity(details.maturity(), invDate);
        isFobProvided(details.fobCode());
        isValidSortCode(details.primarySortCode(), invDate, "invoice.primary.sortcode.invalid.error");
        isSubmitProcessRequiered(action, "invoice.submit.saved.warning");
        isReviewerCommentUpdate(details, action.toString(), "invoice.reviewer.notes.update.warning");

        checkTotals(details, lines);

        checkSourceDocumentRefs(details.boomNumbers(), details.timberMarks(), details.weightSlips());

        checkListMaxSize(details.boomNumbers(), ConstantsCode.MAXOFCSVFORBOOMNUMBERS,
                "invoice.morethan.Max.boomnumbers.error");
        checkBoomNumberForDuplicates(details.invID(), details.boomNumbers());
        checkListMaxSize(details.timberMarks(), ConstantsCode.MAXOFCSVFORTIMBERMARKS,
                "invoice.morethan.Max.timbermarks.error");
        checkListMaxSize(details.weightSlips(), ConstantsCode.MAXOFCSVFORWEIGHSLIPS,
                "invoice.morethan.Max.weighslips.error");

        checkListItemLengths(details.boomNumbers(), ConstantsCode.MAXTOKENLENGTHFORBOOMNUMBERS,
                "invoice.tokennumber.lenght.error", "Boom Numbers");
        checkListItemLengths(details.timberMarks(), ConstantsCode.MAXTOKENLENGTHFORTIMBERMARKS,
                "invoice.tokennumber.lenght.error", "Timber Marks");
        checkListItemLengths(details.weightSlips(), ConstantsCode.MAXTOKENLENGTHFORWEIGHSLIPS,
                "invoice.tokennumber.lenght.error", "Weigh Slips");

        checkInvoiceDateNotFuture(invDate, details.invNumber(), "invoice.date.in.future.error");

        checkInvoiceLines(lines, action, details.invType(), invDate);

        return new ValidationResult(messages);
    }

    public ValidationResult validateForChangeStatus(InvoiceDetails details, String newStatus, String userID) {
        messages.clear();
        if (details == null) {
            addError("invoice.details.missing.error", null);
            return new ValidationResult(messages);
        }
        if (ConstantsCode.INVENTRYSTATUS_APPROVED.equals(newStatus)
                && Objects.equals(details.entryUserID(), userID)) {
            addError("invoice.entry.user.cannot.approve.it.error", null);
        } else if (ConstantsCode.INVENTRYSTATUS_REJECTED.equals(newStatus)
                && isBlank(details.reviewComments())) {
            addError("invoice.reject.need.reviewer.comment.error", null);
        }
        // For REJ/CAN/UNA, parity with the legacy isReviewerCommentUpdate rule:
        // the reviewer comments on the new request must differ from what's currently saved.
        boolean reviewChangeRequired =
                ConstantsCode.INVENTRYSTATUS_REJECTED.equals(newStatus)
                || ConstantsCode.INVENTRYSTATUS_CANCELLED.equals(newStatus)
                || ConstantsCode.INVENTRYSTATUS_UNAPPROVED.equals(newStatus);
        if (reviewChangeRequired && details.invID() != null) {
            String submittedComments = details.reviewComments() == null ? "" : details.reviewComments();
            String savedComments = invoiceRepo.findReviewCommentsById(details.invID());
            String saved = savedComments == null ? "" : savedComments;
            if (submittedComments.equals(saved)) {
                log.debug("InvoiceValidator: reviewer comments unchanged for status {}", newStatus);
                addError("invoice.reviewer.notes.update.warning", null);
            }
        }
        return new ValidationResult(messages);
    }

    private boolean checkForInvoiceNumDuplicate(InvoiceDetails details) {
        if (!manual) return true;
        // Duplicate detection applies to Sale invoices only: warn when another
        // (non-rejected) Sale invoice with the same number exists for the same
        // submitter client number + location.
        if (!ConstantsCode.INVTYPE_SALE.equals(details.invType())) return true;
        String newSubmitterNum = details.submitterClientNum();
        String newSubmitterLoc = details.submitterLocation();
        try {
            List<InvoiceMatch> matches = invoiceRepo.findByClientInvoiceNo(details.invNumber());
            for (InvoiceMatch m : matches) {
                if (Objects.equals(m.coastalLogSaleId(), details.invID())) continue;
                if (ConstantsCode.INVENTRYSTATUS_REJECTED.equals(m.invoiceStatusCode())) continue;

                if (Objects.equals(newSubmitterNum, m.submitterClientNumber())
                        && Objects.equals(newSubmitterLoc, m.submitterClientLocnCode())
                        && Objects.equals(details.invType(), m.invoiceTypeCode())) {
                    log.debug("InvoiceValidator: duplicate seller invoice detected");
                    addWarning("invoice.number.duplicate.same.type.warning",
                            new Object[]{details.invNumber(), m.invoiceTypeCode()});
                    break;
                }
            }
        } catch (Exception e) {
            log.error("InvoiceValidator - checkForInvoiceNumDuplicate failed", e);
        }
        return true;
    }

    private boolean checkMonthComplete(InvoiceDetails details) {
        boolean monthComplete = invoiceRepo.isMonthCompleted(
                details.invoiceDate(),
                details.submitterClientNum(),
                details.submitterLocation(),
                details.invID());
        if (monthComplete) {
            log.debug("InvoiceValidator: month already completed");
            addWarning("invoice.month.completed.warning",
                    new Object[]{details.invNumber(), details.invoiceDate()});
        }
        return true;
    }

    private boolean checkInvoiceType(String invType, LocalDate invDate) {
        if (!commonValidation.isValidInvoiceType(invType, invDate)) {
            log.debug("InvoiceValidator: invalid invoice type {}", invType);
            addError("invoice.type.invalid.error", new Object[]{invType, invDate});
            return false;
        }
        return true;
    }

    private boolean checkInvoiceTypeForSalesOrPurchase(String invType) {
        if (!ConstantsCode.INVTYPE_SALE.equals(invType)
                && !ConstantsCode.INVTYPE_PURCHASE.equals(invType)) {
            log.debug("InvoiceValidator: invoice type not sale or purchase {}", invType);
            addWarning("invoice.type.not.saleorpurchase.warning", new Object[]{invType});
        }
        return true;
    }

    private boolean replaceAdjustInvoiceCheck(String invNums, String clientNum, String clientLoc,
                                              String messageKey, boolean checkForCancelled) {
        if (isBlank(invNums)) return true;
        List<String> errorList = new ArrayList<>();
        for (String raw : invNums.split(",")) {
            String invNo = raw.trim();
            if (invNo.isEmpty()) continue;
            List<RelatedInvoice> related = invoiceRepo.findByInvoiceNoAndClient(invNo, clientNum, clientLoc);
            if (related.isEmpty()) {
                errorList.add(invNo);
            } else if (checkForCancelled) {
                for (RelatedInvoice inv : related) {
                    if (ConstantsCode.INVENTRYSTATUS_CANCELLED.equals(inv.invoiceStatusCode())) {
                        addError("invoice.validation.adjustedInvoiceCancelled", null);
                        return false;
                    }
                }
            }
        }
        if (!errorList.isEmpty()) {
            String joined = String.join(" , ", errorList);
            log.debug("InvoiceValidator: replaceAdjustInvoiceCheck failed for {}", joined);
            addError(messageKey, new Object[]{joined});
            return false;
        }
        return true;
    }

    private boolean replaceAdjustByItselfCheck(String invNums, String thisInvNum, String messageKey) {
        if (isBlank(invNums)) return true;
        for (String raw : invNums.split(",")) {
            if (raw.trim().equals(thisInvNum)) {
                addError(messageKey, null);
                return false;
            }
        }
        return true;
    }

    private boolean checkCsvMaxItems(String csv, int maxValidItems, String messageKey) {
        if (csv == null) return true;
        if (csv.split(",").length > maxValidItems) {
            log.debug("InvoiceValidator: csv max items exceeded");
            addError(messageKey, new Object[]{maxValidItems});
            return false;
        }
        return true;
    }

    private boolean checkListMaxSize(List<String> list, int maxValidItems, String messageKey) {
        if (list == null) return true;
        if (list.size() > maxValidItems) {
            log.debug("InvoiceValidator: list max size exceeded");
            addError(messageKey, new Object[]{maxValidItems});
            return false;
        }
        return true;
    }

    private boolean checkListItemLengths(List<String> list, int maxLengthValid,
                                         String messageKey, String numberType) {
        if (list == null) return true;
        List<String> tooLong = new ArrayList<>();
        for (String value : list) {
            if (value != null && value.trim().length() > maxLengthValid) {
                tooLong.add(value.trim());
            }
        }
        if (!tooLong.isEmpty()) {
            log.debug("InvoiceValidator: list item length exceeded for {}", numberType);
            addError(messageKey, new Object[]{numberType, maxLengthValid, String.join(", ", tooLong)});
            return false;
        }
        return true;
    }

    private boolean checkInvClientNumLocCombo(String clientNumber, String clientLoc) {
        if (isBlank(clientNumber) || isBlank(clientLoc)) return false;
        return commonValidation.isValidClientLocation(clientNumber, clientLoc);
    }

    private boolean checkSubmiterClient(String clientNumber, String clientLoc, String messageKey) {
        if (isBlank(clientNumber)) {
            if (manual) {
                addError("invoice.manual.submitter.name.error", new Object[]{clientNumber, clientLoc});
            } else {
                addError(messageKey, new Object[]{clientNumber, clientLoc});
            }
            return false;
        }
        if (!checkInvClientNumLocCombo(clientNumber, clientLoc)) {
            addError(messageKey, new Object[]{clientNumber, clientLoc});
            return false;
        }
        return true;
    }

    private boolean checkOtherPartyClient(InvoiceDetails details) {
        String clientNumber = details.otherClientNum();
        String clientLoc = details.otherClientLocation();
        String clientName = details.otherClientName();
        String clientCity = details.otherClientCity();
        String clientProvince = details.otherClientProvState();
        String submittedBy = details.submittedBy();

        if (!isBlank(clientNumber)) {
            if (!checkInvClientNumLocCombo(clientNumber, clientLoc)) {
                addError("invoice.otherparty.client.location.invalid.error",
                        new Object[]{clientNumber, clientLoc});
                return false;
            }
            return true;
        }

        if (manual) {
            if (isBlank(clientName) && isBlank(clientCity) && isBlank(clientProvince)) {
                addError("invoice.manual.other.party.name.error", null);
                return false;
            }
            return true;
        }

        boolean ok = true;
        boolean submittedBySeller = ConstantsCode.INVOICE_SUBMITTEDBY_SELLER.equals(submittedBy);
        if (isBlank(clientName)) {
            addError(submittedBySeller
                    ? "invoice.otherparty.buyer.name.required.error"
                    : "invoice.otherparty.seller.name.required.error", null);
            ok = false;
        }
        if (isBlank(clientCity)) {
            addError(submittedBySeller
                    ? "invoice.otherparty.buyer.city.required.error"
                    : "invoice.otherparty.seller.city.required.error", null);
            ok = false;
        }
        if (isBlank(clientProvince)) {
            addError(submittedBySeller
                    ? "invoice.otherparty.buyer.province.required.error"
                    : "invoice.otherparty.seller.province.required.error", null);
            ok = false;
        }
        return ok;
    }

    private boolean isFobProvided(String fobCode) {
        if (isBlank(fobCode)) {
            log.debug("InvoiceValidator: FOB code missing");
            addError("invoice.fob.required.error", null);
            return false;
        }
        return true;
    }

    private boolean isValidMaturity(String maturity, LocalDate invoiceDate) {
        if (!commonValidation.isValidMaturity(maturity, invoiceDate)) {
            log.debug("InvoiceValidator: invalid maturity {}", maturity);
            addError("invoice.maturity.invalid.error", new Object[]{maturity, invoiceDate});
            return false;
        }
        return true;
    }

    private boolean isValidSortCode(String sortCode, LocalDate invDate, String messageKey) {
        if (isBlank(sortCode)) return true;
        if (!commonValidation.isValidSortCode(sortCode, invDate)) {
            log.debug("InvoiceValidator: invalid sort code {}", sortCode);
            addError(messageKey, new Object[]{sortCode, invDate});
            return false;
        }
        return true;
    }

    private boolean isSubmitProcessRequiered(ActionType action, String messageKey) {
        if (manual && action == ActionType.SAVE) {
            log.debug("InvoiceValidator: submit-required reminder");
            addWarning(messageKey, null);
        }
        return true;
    }

    private boolean isReviewerCommentUpdate(InvoiceDetails details, String invStatus, String messageKey) {
        if (!manual || ActionType.OTHER.toString().equals(invStatus)) return true;
        if (details.invID() == null) return true;
        boolean reviewRequired = invStatus.equalsIgnoreCase(ActionType.DELETE.toString())
                || invStatus.equalsIgnoreCase(ConstantsCode.INVENTRYSTATUS_REJECTED)
                || invStatus.equalsIgnoreCase(ConstantsCode.INVENTRYSTATUS_UNAPPROVED)
                || invStatus.equalsIgnoreCase(ConstantsCode.INVENTRYSTATUS_CANCELLED);
        if (!reviewRequired) return true;

        String currentComments = details.reviewComments() == null ? "" : details.reviewComments();
        String savedComments = invoiceRepo.findReviewCommentsById(details.invID());
        savedComments = savedComments == null ? "" : savedComments;
        if (currentComments.equals(savedComments)) {
            log.debug("InvoiceValidator: reviewer comment not updated");
            addError(messageKey, null);
            return false;
        }
        return true;
    }

    /**
     * Totals rules I24–I29, delegated to the shared channel-agnostic
     * {@link InvoiceTotalsRuleSet} (refactor doc §5) — the same core the
     * electronic path runs, so the two channels cannot drift. Each
     * {@link Finding} surfaces as a {@link ValidationMessage} whose key + args
     * resolve from {@code messages.properties} downstream (InvoiceMapper).
     */
    private boolean checkTotals(InvoiceDetails details, List<LineItem> lines) {
        List<InvoiceTotals.Line> coreLines = new ArrayList<>();
        for (LineItem line : lines) {
            coreLines.add(new InvoiceTotals.Line(line.volume(), line.price(),
                    line.numOfPieces() == null ? 0 : line.numOfPieces()));
        }
        InvoiceTotals totals = new InvoiceTotals(details.invType(),
                details.totalAmt(), details.totalVol(), details.totalPieces(), coreLines);

        boolean ok = true;
        for (Finding f : InvoiceTotalsRuleSet.validate(totals)) {
            if (f.severity() == Severity.ERROR) {
                log.debug("InvoiceValidator: totals rule failed: {}", f.code());
                addError(f.code(), f.args());
                ok = false;
            } else {
                addWarning(f.code(), f.args());
            }
        }
        return ok;
    }

    private boolean checkSourceDocumentRefs(List<String> boomNums, List<String> timberMarks, List<String> weighSlips) {
        int provided = 0;
        if (boomNums != null && !boomNums.isEmpty()) provided++;
        if (weighSlips != null && !weighSlips.isEmpty()) provided++;
        if (timberMarks != null && !timberMarks.isEmpty()) provided++;
        if (provided == 0) {
            log.debug("InvoiceValidator: no source document refs provided");
            addError("invoice.oneofthe.boom.timber.wiegh.requiered.error", null);
            return false;
        }
        return true;
    }

    private boolean checkBoomNumberForDuplicates(Long invoiceID, List<String> boomNums) {
        if (boomNums == null || boomNums.isEmpty()) return true;
        List<String> duplicates = new ArrayList<>();
        for (String boomNum : boomNums) {
            if (isBlank(boomNum)) continue;
            int count = invoiceRepo.countBoomNumberDuplicates(invoiceID,
                    ConstantsCode.LOGSOURCECODE_BOOMNUMBER, boomNum.trim());
            if (count > 0) duplicates.add(boomNum.trim());
        }
        if (!duplicates.isEmpty()) {
            log.debug("InvoiceValidator: duplicate boom numbers {}", duplicates);
            addWarning("invoice.boomnumber.duplicate.warning",
                    new Object[]{String.join(" , ", duplicates)});
        }
        return true;
    }

    private boolean checkElectronicOtherPartySubmitter(InvoiceDetails details) {
        if (manual) return true;

        boolean hasSubmitterClient = !isBlank(details.submitterClientNum()) && !isBlank(details.submitterLocation());
        boolean hasOtherClient = !isBlank(details.otherClientNum()) && !isBlank(details.otherClientLocation());
        boolean hasOtherName = !isBlank(details.otherClientName());
        boolean hasOtherCity = !isBlank(details.otherClientCity());
        boolean hasOtherProv = !isBlank(details.otherClientProvState());

        if (hasSubmitterClient && hasOtherClient && (hasOtherName || hasOtherCity || hasOtherProv)) {
            addError("invoice.otherparty.error", null);
            return false;
        }
        if (hasSubmitterClient && hasOtherClient && hasOtherCity) {
            addError("invoice.otherparty.error", null);
            return false;
        }
        if (hasSubmitterClient && hasOtherClient && hasOtherProv) {
            addError("invoice.otherparty.error", null);
            return false;
        }
        if (!isBlank(buyerClientNum) && !isBlank(buyerClientLoc)
                && hasOtherName && hasOtherCity && hasOtherProv
                && ConstantsCode.INVOICE_SUBMITTEDBY_SELLER.equals(details.submittedBy())) {
            addError("invoice.otherparty.buyer.submission.error", null);
            return false;
        }
        if (!isBlank(sellerClientNum) && !isBlank(sellerClientLoc)
                && hasOtherName && hasOtherCity && hasOtherProv
                && ConstantsCode.INVOICE_SUBMITTEDBY_BUYER.equals(details.submittedBy())) {
            addError("invoice.otherparty.seller.submission.error", null);
            return false;
        }

        // Validate BOTH the seller and buyer client number + location exist in CSP
        // (C-03 / C-04). Both are foreign keys on coastal_log_sale, so an invalid
        // value must be caught here with a clear message instead of failing at the
        // database. When the seller submits, the seller IS the submitter, so the
        // submitter's location is effectively validated too — C-02's "no error"
        // therefore holds only for valid data, which is the normal ESF case.
        boolean ok = true;
        if (!isBlank(sellerClientNum) && !isBlank(sellerClientLoc)) {
            if (!checkSubmiterClient(sellerClientNum, sellerClientLoc, "invoice.seller.client.location.invalid.error")) {
                ok = false;
            }
        }
        if (!isBlank(buyerClientNum) && !isBlank(buyerClientLoc)) {
            if (!checkSubmiterClient(buyerClientNum, buyerClientLoc, "invoice.buyer.client.location.invalid.error")) {
                ok = false;
            }
        }
        // C-07 / C-08: only when the seller submits (sellerSubmission = 'Y') must
        // the submitter's (submission's) client number + location match the
        // seller's. The message reads "submission... must match seller...", so the
        // args are {submitter, seller}.
        if (ConstantsCode.INVOICE_SUBMITTEDBY_SELLER.equals(details.submittedBy())) {
            if (!Objects.equals(details.submitterClientNum(), details.clientNumber())) {
                addError("invoice.submitter.not.equal.seller.client.number.error",
                        new Object[]{details.submitterClientNum(), details.clientNumber()});
                ok = false;
            }
            if (!Objects.equals(details.submitterLocation(), details.clientLocation())) {
                addError("invoice.submitter.not.equal.seller.client.location.error",
                        new Object[]{details.submitterLocation(), details.clientLocation()});
                ok = false;
            }
        }
        return ok;
    }

    private boolean checkSenderBuyerForInvoiceType(InvoiceDetails details) {
        if (ConstantsCode.INVOICE_SUBMITTEDBY_SELLER.equals(details.submittedBy())
                && ConstantsCode.INVTYPE_PURCHASE.equals(details.invType())) {
            addError("invoice.type.invalid.submitter",
                    new Object[]{details.submittedBy(), details.invType()});
            return false;
        }
        if (ConstantsCode.INVOICE_SUBMITTEDBY_BUYER.equals(details.submittedBy())
                && ConstantsCode.INVTYPE_SALE.equals(details.invType())) {
            addError("invoice.type.invalid.submitter",
                    new Object[]{details.submittedBy(), details.invType()});
            return false;
        }
        return true;
    }

    private boolean isSameSellerAndBuyer(InvoiceDetails details) {
        if (isBlank(details.submitterClientNum()) || isBlank(details.submitterLocation())
                || isBlank(details.otherClientNum()) || isBlank(details.otherClientLocation())) {
            return true;
        }
        if (Objects.equals(details.submitterClientNum(), details.otherClientNum())
                && Objects.equals(details.submitterLocation(), details.otherClientLocation())) {
            addError("invoice.submitter.equal.other.client.error",
                    new Object[]{details.submitterClientNum(), details.otherClientNum()});
            return false;
        }
        return true;
    }

    private boolean checkInvoiceDateNotFuture(LocalDate invDate, String invoiceNumber, String messageKey) {
        if (invDate == null) return true;
        LocalDate today = LocalDate.now();
        if (invDate.isAfter(today)) {
            log.debug("InvoiceValidator: invoice date in future");
            addError(messageKey, new Object[]{invoiceNumber, today});
            return false;
        }
        return true;
    }

    private boolean checkInvoiceLines(List<LineItem> lines, ActionType action, String invType, LocalDate invDate) {
        if (action == ActionType.SUBMIT && lines.isEmpty()) {
            log.debug("InvoiceValidator: no line items on submit");
            addError("invoice.noline.item.error", null);
            return false;
        }
        InvoiceLineValidator lineValidator = new InvoiceLineValidator(commonValidation);
        boolean ok = true;
        for (LineItem line : lines) {
            ValidationResult lineResult = lineValidator.validate(line, invType, invDate);
            messages.addAll(lineResult.messages());
            if (lineResult.hasErrors()) ok = false;
        }
        return ok;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String blankToNull(String s) {
        return isBlank(s) ? null : s;
    }

    private void addError(String key, Object[] args) {
        messages.add(new ValidationMessage(key, args == null ? null : Arrays.copyOf(args, args.length), MessageType.ERROR));
    }

    private void addWarning(String key, Object[] args) {
        messages.add(new ValidationMessage(key, args == null ? null : Arrays.copyOf(args, args.length), MessageType.WARNING));
    }
}
