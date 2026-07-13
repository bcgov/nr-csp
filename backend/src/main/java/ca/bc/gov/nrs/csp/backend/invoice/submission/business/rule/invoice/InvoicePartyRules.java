package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterInfo;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class InvoicePartyRules implements InvoiceRule {

  /** For keys whose messages.properties template takes no placeholders. */
  private static final Object[] NO_ARGS = new Object[0];

  @Override
  public void validate(InvoiceRuleContext ctx) {
    otherPartyFieldEmpty(ctx);
    invalidBuyerSubmissionCombination(ctx);
    invalidSellerSubmissionCombination(ctx);
    buyerClientLocationExists(ctx);
    sellerClientLocationExists(ctx);
    submissionNumberEqualsSubmitterNumber(ctx);
    submissionLocationEqualsSubmitterLocation(ctx);
    otherPartyClientLocationExists(ctx);
    otherPartyFreeTextRequired(ctx);
    sellerAndBuyerNotSame(ctx);
  }

  /** Other-party free-text fields must be empty when both parties have number+location */
  void otherPartyFieldEmpty(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();

    boolean submitterNumberLocationExists =
        !isBlank(submitter.submitterClientNumber()) && !isBlank(submitter.submitterLocnCode());
    boolean otherPartyNumberLocationExists =
        !isBlank(submitter.otherClientNumber()) && !isBlank(submitter.otherLocnCode());
    boolean anyFreeTextPresent =
        !isBlank(submitter.otherPartyName())
            || !isBlank(submitter.otherPartyCity())
            || !isBlank(submitter.otherPartyProvState());

    if (submitterNumberLocationExists && otherPartyNumberLocationExists && anyFreeTextPresent) {
      ctx.error("invoice.otherparty.error", NO_ARGS);
    }
  }

  /** Invalid buyer-submission combination (ERROR) */
  void invalidBuyerSubmissionCombination(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();

    boolean buyerNumberLocationExists =
        !isBlank(submitter.otherClientNumber()) && !isBlank(submitter.otherLocnCode());
    boolean allFreeTextPresent =
        !isBlank(submitter.otherPartyName())
            && !isBlank(submitter.otherPartyCity())
            && !isBlank(submitter.otherPartyProvState());

    if (submitter.submittedBy() == SubmitterInfo.SubmittedBy.SELLER
        && buyerNumberLocationExists
        && allFreeTextPresent) {
      ctx.error("invoice.otherparty.buyer.submission.error", NO_ARGS);
    }
  }

  /** Invalid seller-submission combination (ERROR) */
  void invalidSellerSubmissionCombination(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();

    boolean sellerNumberLocationExists =
        !isBlank(submitter.otherClientNumber()) && !isBlank(submitter.otherLocnCode());
    boolean allFreeTextPresent =
        !isBlank(submitter.otherPartyName())
            && !isBlank(submitter.otherPartyCity())
            && !isBlank(submitter.otherPartyProvState());

    if (submitter.submittedBy() == SubmitterInfo.SubmittedBy.BUYER
        && sellerNumberLocationExists
        && allFreeTextPresent) {
      ctx.error("invoice.otherparty.seller.submission.error", NO_ARGS);
    }
  }

  /** Buyer number+location must exist (ERROR) */
  void buyerClientLocationExists(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();
    boolean submittedBySeller = submitter.submittedBy() == SubmitterInfo.SubmittedBy.SELLER;

    String buyerClientNumber =
        submittedBySeller ? submitter.otherClientNumber() : submitter.submitterClientNumber();
    String buyerLocnCode =
        submittedBySeller ? submitter.otherLocnCode() : submitter.submitterLocnCode();

    if (isBlank(buyerClientNumber) || isBlank(buyerLocnCode)) {
      return;
    }
    if (!ctx.referenceData().clientLocationExists(buyerClientNumber, buyerLocnCode)) {
      // Template: client number, location code.
      ctx.error("invoice.buyer.client.location.invalid.error",
          new Object[] {buyerClientNumber, buyerLocnCode});
    }
  }

  /** Seller number+location must exist (ERROR) */
  void sellerClientLocationExists(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();
    boolean submittedBySeller = submitter.submittedBy() == SubmitterInfo.SubmittedBy.SELLER;

    String sellerClientNumber =
        submittedBySeller ? submitter.submitterClientNumber() : submitter.otherClientNumber();
    String sellerLocnCode =
        submittedBySeller ? submitter.submitterLocnCode() : submitter.otherLocnCode();

    if (isBlank(sellerClientNumber) || isBlank(sellerLocnCode)) {
      return;
    }
    if (!ctx.referenceData().clientLocationExists(sellerClientNumber, sellerLocnCode)) {
      ctx.error("invoice.seller.client.location.invalid.error",
          new Object[] {sellerClientNumber, sellerLocnCode});
    }
  }

  /**
   * Submission client number must equal the submitting party's client number
   * (ERROR). Legacy enforced this for BOTH seller and buyer submissions — the
   * invoice-level clientNumber it compared was the submission client — while
   * emitting the seller-worded message key in both directions; matched here
   * exactly.
   */
  void submissionNumberEqualsSubmitterNumber(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();
    if (!Objects.equals(submitter.submissionClientNumber(), submitter.submitterClientNumber())) {
      // Template: submission client number, submitter-side client number.
      ctx.error("invoice.submitter.not.equal.seller.client.number.error",
          new Object[] {submitter.submissionClientNumber(), submitter.submitterClientNumber()});
    }
  }

  /** Location counterpart of {@link #submissionNumberEqualsSubmitterNumber} (ERROR). */
  void submissionLocationEqualsSubmitterLocation(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();
    if (!Objects.equals(submitter.submissionLocnCode(), submitter.submitterLocnCode())) {
      ctx.error("invoice.submitter.not.equal.seller.client.location.error",
          new Object[] {submitter.submissionLocnCode(), submitter.submitterLocnCode()});
    }
  }

  /** Other-party number+location, when supplied, must exist (ERROR) */
  void otherPartyClientLocationExists(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();
    if (isBlank(submitter.otherClientNumber())) {
      return;
    }
    if (!ctx.referenceData().clientLocationExists(
        submitter.otherClientNumber(), submitter.otherLocnCode())) {
      ctx.error("invoice.otherparty.client.location.invalid.error",
          new Object[] {submitter.otherClientNumber(), submitter.otherLocnCode()});
    }
  }

  /** Other-party name/city/province required when other-party number is blank (ERROR) */
  void otherPartyFreeTextRequired(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();
    if (!isBlank(submitter.otherClientNumber())) {
      return;
    }
    String otherParty =
        submitter.submittedBy() == SubmitterInfo.SubmittedBy.SELLER ? "buyer" : "seller";

    requireOtherPartyField(ctx, otherParty, submitter.otherPartyName(), "name");
    requireOtherPartyField(ctx, otherParty, submitter.otherPartyCity(), "city");
    requireOtherPartyField(ctx, otherParty, submitter.otherPartyProvState(), "province");
  }

  /** Records the "other-party field required" error for one free-text field (0-arg templates). */
  private static void requireOtherPartyField(
      InvoiceRuleContext ctx, String otherParty, String value, String keySegment) {
    if (isBlank(value)) {
      ctx.error("invoice.otherparty." + otherParty + "." + keySegment + ".required.error", NO_ARGS);
    }
  }

  /** Seller and buyer cannot be the same client number+location (ERROR) */
  void sellerAndBuyerNotSame(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();

    boolean submitterNumberLocationExists =
        !isBlank(submitter.submitterClientNumber()) && !isBlank(submitter.submitterLocnCode());
    boolean otherPartyNumberLocationExists =
        !isBlank(submitter.otherClientNumber()) && !isBlank(submitter.otherLocnCode());
    if (!submitterNumberLocationExists || !otherPartyNumberLocationExists) {
      return;
    }

    if (Objects.equals(submitter.submitterClientNumber(), submitter.otherClientNumber())
        && Objects.equals(submitter.submitterLocnCode(), submitter.otherLocnCode())) {
      // Template: the two (identical) client numbers — same args the manual path passes.
      ctx.error("invoice.submitter.equal.other.client.error",
          new Object[] {submitter.submitterClientNumber(), submitter.otherClientNumber()});
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
