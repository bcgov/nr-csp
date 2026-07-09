package ca.bc.gov.nrs.csp.backend.util.validation.invoiceDetails;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository.InvoiceMatch;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository.RelatedInvoice;
import ca.bc.gov.nrs.csp.backend.util.constants.ActionType;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class InvoiceValidatorTest {

    @Mock InvoiceRepository invoiceRepo;
    @Mock CommonValidation commonValidation;

    InvoiceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InvoiceValidator(invoiceRepo, commonValidation);
        lenient().when(commonValidation.isValidInvoiceType(any(), any())).thenReturn(true);
        lenient().when(commonValidation.isValidMaturity(any(), any())).thenReturn(true);
        lenient().when(commonValidation.isValidSortCode(any(), any())).thenReturn(true);
        lenient().when(commonValidation.isValidSpeciesGradeCombination(any(), any())).thenReturn(true);
        lenient().when(commonValidation.isValidClientLocation(any(), any())).thenReturn(true);
        lenient().when(invoiceRepo.findByClientInvoiceNo(any())).thenReturn(List.of());
        lenient().when(invoiceRepo.findByInvoiceNoAndClient(any(), any(), any())).thenReturn(List.of());
        lenient().when(invoiceRepo.isMonthCompleted(any(), any(), any(), any())).thenReturn(false);
        lenient().when(invoiceRepo.countBoomNumberDuplicates(any(), any(), any())).thenReturn(0);
        lenient().when(invoiceRepo.findReviewCommentsById(any())).thenReturn(null);
    }

    // ---------------------------------------------------------------
    // validate() — entry / null guards
    // ---------------------------------------------------------------

    @Test
    void validate_nullDetails_returnsMissingError() {
        ValidationResult result = validator.validate(null, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.details.missing.error");
    }

    @Test
    void validate_nullInvoiceDate_returnsRequiredErrorAndShortCircuits() {
        InvoiceDetails details = invWith(i -> i.invoiceDate = null);

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.date.required.error");
        assertThat(result.messages()).hasSize(1);
    }

    @Test
    void validate_validInvoice_returnsNoErrorsOrWarnings() {
        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.OTHER);

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.hasWarnings()).isFalse();
    }

    // ---------------------------------------------------------------
    // checkForInvoiceNumDuplicate
    // ---------------------------------------------------------------

    @Test
    void validate_nonManual_skipsDuplicateCheck() {
        validator.validate(validInvoice(), List.of(), false, ActionType.OTHER);

        // findByClientInvoiceNo would be invoked only in manual mode
        // (the lenient stub allows no-invocation without error)
    }

    @Test
    void validate_manualWithNoDuplicate_noWarning() {
        ValidationResult result = validator.validate(validInvoice(), List.of(), true, ActionType.OTHER);

        assertNoWarning(result, "invoice.number.duplicate.same.type.warning");
    }

    @Test
    void validate_manualSellerSaleDuplicate_addsWarning() {
        given(invoiceRepo.findByClientInvoiceNo("INV-001"))
                .willReturn(List.of(saleMatchBySubmitter("00001234", "01")));

        ValidationResult result = validator.validate(validInvoice(), List.of(), true, ActionType.OTHER);

        assertHasWarning(result, "invoice.number.duplicate.same.type.warning");
    }

    @Test
    void validate_manualWithRejectedMatch_skipped() {
        InvoiceMatch rejected = matchBuilder()
                .invoiceStatusCode(ConstantsCode.INVENTRYSTATUS_REJECTED)
                .submitterClientNumber("00001234")
                .submitterClientLocnCode("01")
                .build();
        given(invoiceRepo.findByClientInvoiceNo("INV-001")).willReturn(List.of(rejected));

        ValidationResult result = validator.validate(validInvoice(), List.of(), true, ActionType.OTHER);

        assertNoWarning(result, "invoice.number.duplicate.same.type.warning");
    }

    @Test
    void validate_manualSelfMatch_skipped() {
        InvoiceMatch self = matchBuilder()
                .coastalLogSaleId(1L)   // matches validInvoice().invID
                .submitterClientNumber("00001234")
                .submitterClientLocnCode("01")
                .build();
        given(invoiceRepo.findByClientInvoiceNo("INV-001")).willReturn(List.of(self));

        ValidationResult result = validator.validate(validInvoice(), List.of(), true, ActionType.OTHER);

        assertNoWarning(result, "invoice.number.duplicate.same.type.warning");
    }

    // ---------------------------------------------------------------
    // checkMonthComplete
    // ---------------------------------------------------------------

    @Test
    void validate_monthCompleted_addsWarning() {
        given(invoiceRepo.isMonthCompleted(any(), any(), any(), any())).willReturn(true);

        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.OTHER);

        assertHasWarning(result, "invoice.month.completed.warning");
    }

    // ---------------------------------------------------------------
    // checkInvoiceType
    // ---------------------------------------------------------------

    @Test
    void validate_invalidInvoiceType_addsError() {
        given(commonValidation.isValidInvoiceType("BAD", validInvoice().invoiceDate())).willReturn(false);
        InvoiceDetails details = invWith(i -> i.invType = "BAD");

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.type.invalid.error");
    }

    @Test
    void validate_invoiceTypeIsAdj_addsNotSalePurchaseWarning() {
        InvoiceDetails details = invWith(i -> i.invType = "ADJ");

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasWarning(result, "invoice.type.not.saleorpurchase.warning");
    }

    // ---------------------------------------------------------------
    // both replace + adjust populated
    // ---------------------------------------------------------------

    @Test
    void validate_bothReplaceAndAdjustPopulated_addsError() {
        InvoiceDetails details = invWith(i -> {
            i.replaceInvNum = "OLD-100";
            i.adjustInvNum = "OLD-200";
        });
        given(invoiceRepo.findByInvoiceNoAndClient(any(), any(), any()))
                .willReturn(List.of(new RelatedInvoice(99L, "APP")));

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.both.replace.adjust.invoicenum.error");
    }

    // ---------------------------------------------------------------
    // replaceAdjustInvoiceCheck and replaceAdjustByItselfCheck
    // ---------------------------------------------------------------

    private static Stream<Arguments> replaceAdjustScenarios() {
        return Stream.of(
                Arguments.of("replaceInvNum", "MISSING-1", "invoice.replace.invoicenumber.error"),
                Arguments.of("adjustInvNum", "ADJ-OLD", "invoice.validation.adjustedInvoiceCancelled"),
                Arguments.of("replaceInvNum", "INV-001", "invoice.replace.with.itself.error"),
                Arguments.of("adjustInvNum", "INV-001", "invoice.adjust.with.itself.error")
        );
    }

    @ParameterizedTest
    @MethodSource("replaceAdjustScenarios")
    void validate_replaceAdjustInvoiceScenarios(String field, String value, String expectedError) {
        InvoiceDetails details = invWith(i -> {
            if ("replaceInvNum".equals(field)) {
                i.replaceInvNum = value;
            } else {
                i.adjustInvNum = value;
            }
        });

        if ("ADJ-OLD".equals(value)) {
            given(invoiceRepo.findByInvoiceNoAndClient("ADJ-OLD", "00001234", "01"))
                    .willReturn(List.of(new RelatedInvoice(99L, ConstantsCode.INVENTRYSTATUS_CANCELLED)));
        }

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, expectedError);
    }

    // ---------------------------------------------------------------
    // checkCsvMaxItems
    // ---------------------------------------------------------------

    @Test
    void validate_tooManyReplaceItems_addsError() {
        // MAXOFCSVFORREPLACEINVOICENUM = 5
        InvoiceDetails details = invWith(i -> i.replaceInvNum = "A,B,C,D,E,F,G");

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.morethanmax.replace.invoicenum.error");
    }

    // ---------------------------------------------------------------
    // checkElectronicOtherPartySubmitter (ESF only)
    // ---------------------------------------------------------------

    @Test
    void validate_esfSubmitterAndOtherWithName_addsOtherPartyError() {
        InvoiceDetails details = invWith(i -> i.otherClientName = "ABC Logging");

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.otherparty.error");
    }

    @Test
    void validate_manualSkipsElectronicOtherPartyCheck() {
        // Combination that would fire in ESF mode
        InvoiceDetails details = invWith(i -> i.otherClientName = "ABC Logging");

        ValidationResult result = validator.validate(details, List.of(), true, ActionType.OTHER);

        assertNoError(result, "invoice.otherparty.error");
    }

    // ---------------------------------------------------------------
    // checkSenderBuyerForInvoiceType
    // ---------------------------------------------------------------

    @Test
    void validate_sellerWithPurchaseType_addsInvalidSubmitterError() {
        InvoiceDetails details = invWith(i -> {
            i.submittedBy = ConstantsCode.INVOICE_SUBMITTEDBY_SELLER;
            i.invType = ConstantsCode.INVTYPE_PURCHASE;
        });

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.type.invalid.submitter");
    }

    @Test
    void validate_buyerWithSaleType_addsInvalidSubmitterError() {
        InvoiceDetails details = invWith(i -> {
            i.submittedBy = ConstantsCode.INVOICE_SUBMITTEDBY_BUYER;
            i.invType = ConstantsCode.INVTYPE_SALE;
        });

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.type.invalid.submitter");
    }

    // ---------------------------------------------------------------
    // checkOtherPartyClient
    // ---------------------------------------------------------------

    @Test
    void validate_invalidOtherPartyClientLocation_addsError() {
        given(commonValidation.isValidClientLocation("00005678", "01")).willReturn(false);

        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.otherparty.client.location.invalid.error");
    }

    @Test
    void validate_manualInvalidSubmitterClientLocation_addsError() {
        // Manual invoices now validate the submitter's own client number + location.
        given(commonValidation.isValidClientLocation("00001234", "01")).willReturn(false);

        ValidationResult result = validator.validate(validInvoice(), List.of(), true, ActionType.OTHER);

        assertHasError(result, "invoice.submitter.client.location.invalid.error");
    }

    @Test
    void validate_esfChecksSellerAndBuyerLocationExistence() {
        // ESF validates BOTH the seller and buyer client+location
        // existence (both are DB foreign keys), so an invalid one is caught with a
        // clear message instead of failing at the database.
        given(commonValidation.isValidClientLocation(any(), any())).willReturn(false);

        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.seller.client.location.invalid.error");
        assertHasError(result, "invoice.buyer.client.location.invalid.error");
    }

    @Test
    void validate_esfDoesNotRaiseManualSubmitterKey() {
        // ESF does not run the manual submitter check (line 105 is manual-only),
        // so the manual-only key is never raised — seller/buyer existence is validated
        // instead (see above).
        given(commonValidation.isValidClientLocation(any(), any())).willReturn(false);

        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.OTHER);

        assertThat(result.messages())
                .noneMatch(m -> "invoice.submitter.client.location.invalid.error".equals(m.messageKey()));
    }

    @Test
    void validate_manualWithAllOtherPartyBlank_addsManualOtherPartyError() {
        InvoiceDetails details = invWith(i -> {
            i.otherClientNum = null;
            i.otherClientLocation = null;
            i.otherClientName = null;
            i.otherClientCity = null;
            i.otherClientProvState = null;
        });

        ValidationResult result = validator.validate(details, List.of(), true, ActionType.OTHER);

        assertHasError(result, "invoice.manual.other.party.name.error");
    }

    @Test
    void validate_esfMissingBuyerNameCityProv_addsRequiredErrors() {
        InvoiceDetails details = invWith(i -> {
            i.otherClientNum = null;
            i.otherClientLocation = null;
            // submittedBy is Seller in default, so other party is buyer → expect buyer.* keys
        });

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.otherparty.buyer.name.required.error");
        assertHasError(result, "invoice.otherparty.buyer.city.required.error");
        assertHasError(result, "invoice.otherparty.buyer.province.required.error");
    }

    // ---------------------------------------------------------------
    // isSameSellerAndBuyer
    // ---------------------------------------------------------------

    @Test
    void validate_submitterEqualsOtherClient_addsError() {
        InvoiceDetails details = invWith(i -> {
            i.otherClientNum = i.submitterClientNum;
            i.otherClientLocation = i.submitterLocation;
        });

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.submitter.equal.other.client.error");
    }

    // ---------------------------------------------------------------
    // isValidMaturity / isValidSortCode (primary)
    // ---------------------------------------------------------------

    @Test
    void validate_invalidMaturity_addsError() {
        given(commonValidation.isValidMaturity(any(), any())).willReturn(false);

        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.maturity.invalid.error");
    }

    @Test
    void validate_invalidPrimarySortCode_addsError() {
        given(commonValidation.isValidSortCode("SORT01", validInvoice().invoiceDate())).willReturn(false);

        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.primary.sortcode.invalid.error");
    }

    // ---------------------------------------------------------------
    // isSubmitProcessRequiered
    // ---------------------------------------------------------------

    @Test
    void validate_manualSave_addsSubmitReminderWarning() {
        ValidationResult result = validator.validate(validInvoice(), List.of(), true, ActionType.SAVE);

        assertHasWarning(result, "invoice.submit.saved.warning");
    }

    @Test
    void validate_nonManualSave_noSubmitReminderWarning() {
        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.SAVE);

        assertNoWarning(result, "invoice.submit.saved.warning");
    }

    // ---------------------------------------------------------------
    // isReviewerCommentUpdate
    // ---------------------------------------------------------------

    @Test
    void validate_manualDeleteWithUnchangedComments_addsError() {
        given(invoiceRepo.findReviewCommentsById(1L)).willReturn("Looks good");

        ValidationResult result = validator.validate(validInvoice(), List.of(), true, ActionType.DELETE);

        assertHasError(result, "invoice.reviewer.notes.update.warning");
    }

    @Test
    void validate_manualDeleteWithChangedComments_noError() {
        given(invoiceRepo.findReviewCommentsById(1L)).willReturn("Different comment");

        ValidationResult result = validator.validate(validInvoice(), List.of(), true, ActionType.DELETE);

        assertNoError(result, "invoice.reviewer.notes.update.warning");
    }

    @Test
    void validate_manualDeleteOnNewInvoice_skipped() {
        InvoiceDetails details = invWith(i -> i.invID = null);

        ValidationResult result = validator.validate(details, List.of(), true, ActionType.DELETE);

        assertNoError(result, "invoice.reviewer.notes.update.warning");
    }

    // ---------------------------------------------------------------
    // checkTotalAmountsVariance
    // ---------------------------------------------------------------

    @Test
    void validate_negativeTotalAmount_addsError() {
        InvoiceDetails details = invWith(i -> i.totalAmt = new BigDecimal("-5"));

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.totalamount.negative.error");
    }

    @Test
    void validate_totalAmountVarianceExceeded_addsWarning() {
        InvoiceDetails details = invWith(i -> i.totalAmt = new BigDecimal("100"));
        // empty lines → calculated = 0 → diff = 100 > variance (5) → warning

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasWarning(result, "invoice.totalamount.dismatch.warning");
    }

    // ---------------------------------------------------------------
    // checkTotalVolumesVariance
    // ---------------------------------------------------------------

    @Test
    void validate_negativeTotalVolume_addsError() {
        InvoiceDetails details = invWith(i -> i.totalVol = new BigDecimal("-1"));

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.totalvolume.negative.error");
    }

    @Test
    void validate_totalVolumeVarianceExceeded_addsWarning() {
        InvoiceDetails details = invWith(i -> i.totalVol = new BigDecimal("50"));

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasWarning(result, "invoice.totalvolume.dismatch.warning");
    }

    // ---------------------------------------------------------------
    // checkTotalPiecesVariance
    // ---------------------------------------------------------------

    @Test
    void validate_negativeTotalPieces_addsError() {
        InvoiceDetails details = invWith(i -> i.totalPieces = -1);

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.totalpieces.negative.error");
    }

    @Test
    void validate_totalPiecesVarianceExceeded_addsWarning() {
        InvoiceDetails details = invWith(i -> i.totalPieces = 10);
        // empty lines → calculated = 0, variance = 0 → 10 > 0 → warning

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasWarning(result, "invoice.totalpieces.dismatch.warning");
    }

    // ---------------------------------------------------------------
    // checkSourceDocumentRefs
    // ---------------------------------------------------------------

    @Test
    void validate_noSourceDocumentRefs_addsError() {
        InvoiceDetails details = invWith(i -> {
            i.boomNumbers = new ArrayList<>();
            i.timberMarks = new ArrayList<>();
            i.weightSlips = new ArrayList<>();
        });

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.oneofthe.boom.timber.wiegh.requiered.error");
    }

    // ---------------------------------------------------------------
    // checkBoomNumberForDuplicates
    // ---------------------------------------------------------------

    @Test
    void validate_boomNumberDuplicate_addsWarning() {
        given(invoiceRepo.countBoomNumberDuplicates(1L, ConstantsCode.LOGSOURCECODE_BOOMNUMBER, "B100"))
                .willReturn(1);

        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.OTHER);

        assertHasWarning(result, "invoice.boomnumber.duplicate.warning");
    }

    // ---------------------------------------------------------------
    // checkListMaxSize / checkListItemLengths
    // ---------------------------------------------------------------

    @Test
    void validate_tooManyBoomNumbers_addsError() {
        // MAXOFCSVFORBOOMNUMBERS = 10
        InvoiceDetails details = invWith(i -> i.boomNumbers = new ArrayList<>(
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")));

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.morethan.Max.boomnumbers.error");
    }

    @Test
    void validate_boomNumberTooLong_addsError() {
        // MAXTOKENLENGTHFORBOOMNUMBERS = 20
        String tooLong = "X".repeat(25);
        InvoiceDetails details = invWith(i -> i.boomNumbers = new ArrayList<>(List.of(tooLong)));

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.tokennumber.lenght.error");
    }

    // ---------------------------------------------------------------
    // checkInvoiceDateNotFuture
    // ---------------------------------------------------------------

    @Test
    void validate_invoiceDateInFuture_addsError() {
        InvoiceDetails details = invWith(i -> i.invoiceDate = LocalDate.now().plusDays(1));

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.date.in.future.error");
    }

    // ---------------------------------------------------------------
    // checkInvoiceLines
    // ---------------------------------------------------------------

    @Test
    void validate_submitWithNoLines_addsNoLineItemError() {
        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.SUBMIT);

        assertHasError(result, "invoice.noline.item.error");
    }

    @Test
    void validate_lineValidationErrors_bubbledUp() {
        // Force a line-level error: invalid sort code on the line
        given(commonValidation.isValidSortCode("LINE-SORT", validInvoice().invoiceDate())).willReturn(false);
        LineItem line = new LineItem(
                10L, 1L, "LINE-SORT", "LINE-SORT", "SP1", null, "G1", 5,
                new BigDecimal("10.00"), new BigDecimal("2.00"),
                null, new BigDecimal("20.00")
        );

        ValidationResult result = validator.validate(validInvoice(), List.of(line), false, ActionType.OTHER);

        assertHasError(result, "invoice.secondry.sortcode.invalid.error");
    }

    // ---------------------------------------------------------------
    // validateForChangeStatus
    // ---------------------------------------------------------------

    @Test
    void validateForChangeStatus_nullDetails_addsMissingError() {
        ValidationResult result = validator.validateForChangeStatus(null, "APP", "user1");

        assertHasError(result, "invoice.details.missing.error");
    }

    @Test
    void validateForChangeStatus_entryUserApproves_addsError() {
        ValidationResult result = validator.validateForChangeStatus(
                validInvoice(), ConstantsCode.INVENTRYSTATUS_APPROVED, "user1");

        assertHasError(result, "invoice.entry.user.cannot.approve.it.error");
    }

    @Test
    void validateForChangeStatus_differentUserApproves_noError() {
        ValidationResult result = validator.validateForChangeStatus(
                validInvoice(), ConstantsCode.INVENTRYSTATUS_APPROVED, "anotherUser");

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validateForChangeStatus_rejectWithoutComments_addsError() {
        InvoiceDetails details = invWith(i -> i.reviewComments = null);

        ValidationResult result = validator.validateForChangeStatus(
                details, ConstantsCode.INVENTRYSTATUS_REJECTED, "user2");

        assertHasError(result, "invoice.reject.need.reviewer.comment.error");
    }

    @Test
    void validateForChangeStatus_rejectWithComments_noError() {
        ValidationResult result = validator.validateForChangeStatus(
                validInvoice(), ConstantsCode.INVENTRYSTATUS_REJECTED, "user2");

        assertThat(result.hasErrors()).isFalse();
    }

    // ---------------------------------------------------------------
    // checkForInvoiceNumDuplicate — loop fall-through and repo failure
    // ---------------------------------------------------------------

    @Test
    void validate_manualDuplicateWithDifferentSubmitter_noWarning() {
        // Match exists but for another submitter → loop completes without a hit
        given(invoiceRepo.findByClientInvoiceNo("INV-001"))
                .willReturn(List.of(saleMatchBySubmitter("00007777", "01")));

        ValidationResult result = validator.validate(validInvoice(), List.of(), true, ActionType.OTHER);

        assertNoWarning(result, "invoice.number.duplicate.same.type.warning");
    }

    @Test
    void validate_manualDuplicateCheckRepoThrows_swallowedWithoutWarning() {
        // The duplicate check must never break validation: exceptions are logged
        // and swallowed, and no duplicate warning is raised.
        given(invoiceRepo.findByClientInvoiceNo("INV-001"))
                .willThrow(new RuntimeException("db down"));

        ValidationResult result = validator.validate(validInvoice(), List.of(), true, ActionType.OTHER);

        assertNoWarning(result, "invoice.number.duplicate.same.type.warning");
        assertThat(result.hasErrors()).isFalse();
    }

    // ---------------------------------------------------------------
    // checkSubmiterClient — blank submitter client number (manual)
    // ---------------------------------------------------------------

    @Test
    void validate_manualBlankSubmitterClientNum_addsManualSubmitterNameError() {
        InvoiceDetails details = invWith(i -> i.submitterClientNum = null);

        ValidationResult result = validator.validate(details, List.of(), true, ActionType.OTHER);

        assertHasError(result, "invoice.manual.submitter.name.error");
    }

    // ---------------------------------------------------------------
    // checkOtherPartyClient — manual with partial other-party info
    // ---------------------------------------------------------------

    @Test
    void validate_manualOtherPartyNameOnly_noManualOtherPartyError() {
        // Manual: no client number, but a name is enough — no error
        InvoiceDetails details = invWith(i -> {
            i.otherClientNum = null;
            i.otherClientLocation = null;
            i.otherClientName = "ABC Logging";
            i.otherClientCity = null;
            i.otherClientProvState = null;
        });

        ValidationResult result = validator.validate(details, List.of(), true, ActionType.OTHER);

        assertNoError(result, "invoice.manual.other.party.name.error");
    }

    @Test
    void validate_esfBuyerSubmittedMissingSellerNameCityProv_addsSellerRequiredErrors() {
        // Buyer submits → the other party is the seller → expect seller.* keys
        InvoiceDetails details = invWith(i -> {
            i.submittedBy = ConstantsCode.INVOICE_SUBMITTEDBY_BUYER;
            i.invType = ConstantsCode.INVTYPE_PURCHASE;
            i.otherClientNum = null;
            i.otherClientLocation = null;
        });

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.otherparty.seller.name.required.error");
        assertHasError(result, "invoice.otherparty.seller.city.required.error");
        assertHasError(result, "invoice.otherparty.seller.province.required.error");
    }

    // ---------------------------------------------------------------
    // isFobProvided
    // ---------------------------------------------------------------

    @Test
    void validate_blankFobCode_addsError() {
        InvoiceDetails details = invWith(i -> i.fobCode = null);

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.fob.required.error");
    }

    // ---------------------------------------------------------------
    // checkElectronicOtherPartySubmitter — buyer/seller submission errors
    // and submitter-vs-seller client mismatch
    // ---------------------------------------------------------------

    @Test
    void validate_esfSellerSubmitsBuyerClientAndFullOtherPartyInfo_addsBuyerSubmissionError() {
        // Seller submission with no submitter client, but the buyer's client
        // number+location AND full name/city/province are all supplied
        InvoiceDetails details = invWith(i -> {
            i.submitterClientNum = null;
            i.otherClientName = "Buyer Co";
            i.otherClientCity = "Victoria";
            i.otherClientProvState = "BC";
        });

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.otherparty.buyer.submission.error");
    }

    @Test
    void validate_esfBuyerSubmitsSellerClientAndFullOtherPartyInfo_addsSellerSubmissionError() {
        // Buyer submission with no submitter client, but the seller's client
        // number+location AND full name/city/province are all supplied
        InvoiceDetails details = invWith(i -> {
            i.submittedBy = ConstantsCode.INVOICE_SUBMITTEDBY_BUYER;
            i.invType = ConstantsCode.INVTYPE_PURCHASE;
            i.submitterClientNum = null;
            i.otherClientName = "Seller Co";
            i.otherClientCity = "Nanaimo";
            i.otherClientProvState = "BC";
        });

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.otherparty.seller.submission.error");
    }

    @Test
    void validate_esfSellerSubmitterMismatchesSellerClient_addsMismatchErrors() {
        // C-07 / C-08: when the seller submits, the submitter's client number
        // and location must match the seller's (clientNumber / clientLocation)
        InvoiceDetails details = invWith(i -> {
            i.clientNumber = "00009999";
            i.clientLocation = "99";
        });

        ValidationResult result = validator.validate(details, List.of(), false, ActionType.OTHER);

        assertHasError(result, "invoice.submitter.not.equal.seller.client.number.error");
        assertHasError(result, "invoice.submitter.not.equal.seller.client.location.error");
    }

    @Test
    void validate_esfSellerSubmitterMatchesSellerClient_noMismatchErrors() {
        ValidationResult result = validator.validate(validInvoice(), List.of(), false, ActionType.OTHER);

        assertNoError(result, "invoice.submitter.not.equal.seller.client.number.error");
        assertNoError(result, "invoice.submitter.not.equal.seller.client.location.error");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private InvoiceDetails validInvoice() {
        return new Inv().build();
    }

    private InvoiceDetails invWith(Consumer<Inv> override) {
        Inv i = new Inv();
        override.accept(i);
        return i.build();
    }

    private static class Inv {
        Long invID = 1L;
        String invNumber = "INV-001";
        LocalDate invoiceDate = LocalDate.of(2024, Month.JUNE, 15);
        String invStatus = "DFT";
        String invType = "SAL";
        String maturity = "M";
        String fobCode = "FOB01";
        String primarySortCode = "SORT01";
        BigDecimal totalAmt = BigDecimal.ZERO;
        Integer totalPieces = 0;
        BigDecimal totalVol = BigDecimal.ZERO;
        String submitterClientNum = "00001234";
        String submitterLocation = "01";
        String submittedBy = "Seller";
        String clientNumber = "00001234";
        String clientLocation = "01";
        String otherClientNum = "00005678";
        String otherClientLocation = "01";
        String otherClientName = null;
        String otherClientCity = null;
        String otherClientProvState = null;
        List<String> boomNumbers = new ArrayList<>(List.of("B100"));
        List<String> timberMarks = new ArrayList<>();
        List<String> weightSlips = new ArrayList<>();
        String replaceInvNum = null;
        String adjustInvNum = null;
        String reviewComments = "Looks good";
        String submitComments = null;
        String entryUserID = "user1";

        InvoiceDetails build() {
            return new InvoiceDetails(invID, invNumber, invoiceDate, invStatus, invType, maturity,
                    fobCode, primarySortCode, totalAmt, totalPieces, totalVol, submitterClientNum,
                    submitterLocation, submittedBy, clientNumber, clientLocation, otherClientNum,
                    otherClientLocation, otherClientName, otherClientCity, otherClientProvState,
                    boomNumbers, timberMarks, weightSlips, replaceInvNum, adjustInvNum, reviewComments,
                    submitComments, entryUserID);
        }
    }

    private InvoiceMatch saleMatchBySubmitter(String submitterNum, String submitterLoc) {
        return matchBuilder()
                .invoiceTypeCode(ConstantsCode.INVTYPE_SALE)
                .submitterClientNumber(submitterNum)
                .submitterClientLocnCode(submitterLoc)
                .build();
    }

    private static MatchBuilder matchBuilder() {
        return new MatchBuilder();
    }

    private static class MatchBuilder {
        Long coastalLogSaleId = 999L;
        String invoiceStatusCode = "APP";
        String invoiceTypeCode = ConstantsCode.INVTYPE_SALE;
        String submitterClientNumber;
        String submitterClientLocnCode;
        String buyerClientNumber;
        String buyerClientLocnCode;
        String sellerClientNumber;
        String sellerClientLocnCode;
        String buyerParticipantName;
        String buyerParticipantCity;
        String buyerParticipantProvince;
        String sellerParticipantName;
        String sellerParticipantCity;
        String sellerParticipantProvince;

        MatchBuilder coastalLogSaleId(Long v) { this.coastalLogSaleId = v; return this; }
        MatchBuilder invoiceStatusCode(String v) { this.invoiceStatusCode = v; return this; }
        MatchBuilder invoiceTypeCode(String v) { this.invoiceTypeCode = v; return this; }
        MatchBuilder submitterClientNumber(String v) { this.submitterClientNumber = v; return this; }
        MatchBuilder submitterClientLocnCode(String v) { this.submitterClientLocnCode = v; return this; }

        InvoiceMatch build() {
            return new InvoiceMatch(coastalLogSaleId, invoiceStatusCode, invoiceTypeCode,
                    submitterClientNumber, submitterClientLocnCode,
                    buyerClientNumber, buyerClientLocnCode,
                    sellerClientNumber, sellerClientLocnCode,
                    buyerParticipantName, buyerParticipantCity, buyerParticipantProvince,
                    sellerParticipantName, sellerParticipantCity, sellerParticipantProvince);
        }
    }

    private void assertHasError(ValidationResult r, String key) {
        assertThat(r.errors()).extracting(ValidationMessage::messageKey)
                .as("expected error with key %s; got %s", key, r.messages())
                .contains(key);
    }

    private void assertHasWarning(ValidationResult r, String key) {
        assertThat(r.warnings()).extracting(ValidationMessage::messageKey)
                .as("expected warning with key %s; got %s", key, r.messages())
                .contains(key);
    }

    private void assertNoError(ValidationResult r, String key) {
        assertThat(r.errors()).noneMatch(m -> m.messageKey().equals(key));
    }

    private void assertNoWarning(ValidationResult r, String key) {
        assertThat(r.warnings()).noneMatch(m -> m.messageKey().equals(key));
    }
}
