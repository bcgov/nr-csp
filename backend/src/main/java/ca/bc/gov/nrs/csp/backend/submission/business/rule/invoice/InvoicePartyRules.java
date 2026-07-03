package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterInfo;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class InvoicePartyRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    otherPartyFieldEmpty(ctx);
    invalidBuyerSubmissionCombination(ctx);
    invalidSellerSubmissionCombination(ctx);
    buyerClientLocationExists(ctx);
    sellerClientLocationExists(ctx);
    submissionNumberEqualsSellerNumber(ctx);
    submissionLocationEqualsSellerLocation(ctx);
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
      ctx.error(
          "invoice.otherparty.error",
          "other-party name/city/province must be empty when both parties have number+location for invoiceNumber "
              + ctx.invoiceNumber());
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
      ctx.error(
          "invoice.otherparty.buyer.submission.error",
          "sellerSubmission must be N when a buyer client number+location and other-party "
              + "name/city/province are all supplied for invoiceNumber "
              + ctx.invoiceNumber());
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
      ctx.error(
          "invoice.otherparty.seller.submission.error",
          "sellerSubmission must be Y when a seller client number+location and other-party "
              + "name/city/province are all supplied for invoiceNumber "
              + ctx.invoiceNumber());
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
      ctx.error(
          "invoice.buyer.client.location.invalid.error",
          "The combination of the buyer client number " + buyerClientNumber
              + " and location " + buyerLocnCode + " cannot be found in CSP for invoiceNumber "
              + ctx.invoiceNumber() + ".");
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
      ctx.error(
          "invoice.seller.client.location.invalid.error",
          "The combination of the seller client number " + sellerClientNumber
              + " and location " + sellerLocnCode + " cannot be found in CSP for invoiceNumber "
              + ctx.invoiceNumber() + ".");
    }
  }

  /** Submission client number must equal seller client number (ERROR) */
  void submissionNumberEqualsSellerNumber(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();
    if (submitter.submittedBy() != SubmitterInfo.SubmittedBy.SELLER) {
      return;
    }
    if (!Objects.equals(submitter.submissionClientNumber(), submitter.submitterClientNumber())) {
      ctx.error(
          "invoice.submitter.not.equal.seller.client.number.error",
          "submissionClientNumber " + submitter.submissionClientNumber()
              + " must match sellerClientNumber " + submitter.submitterClientNumber()
              + " when sellerSubmission equals 'Y' for invoiceNumber " + ctx.invoiceNumber() + ".");
    }
  }

  /** Submission client location must equal seller client location (ERROR) */
  void submissionLocationEqualsSellerLocation(InvoiceRuleContext ctx) {
    SubmitterInfo submitter = ctx.submitter();
    if (submitter.submittedBy() != SubmitterInfo.SubmittedBy.SELLER) {
      return;
    }
    if (!Objects.equals(submitter.submissionLocnCode(), submitter.submitterLocnCode())) {
      ctx.error(
          "invoice.submitter.not.equal.seller.client.location.error",
          "submissionClientLocnCode " + submitter.submissionLocnCode()
              + " must match sellerClientLocnCode " + submitter.submitterLocnCode()
              + " when sellerSubmission equals 'Y' for invoiceNumber " + ctx.invoiceNumber() + ".");
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
      ctx.error(
          "invoice.otherparty.client.location.invalid.error",
          "The combination of the other-party client number " + submitter.otherClientNumber()
              + " and location " + submitter.otherLocnCode()
              + " cannot be found in CSP for invoiceNumber " + ctx.invoiceNumber() + ".");
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

    if (isBlank(submitter.otherPartyName())) {
      ctx.error(
          "invoice.otherparty." + otherParty + ".name.required.error",
          "otherPartyName is required when there is no " + otherParty
              + " client number+location for invoiceNumber " + ctx.invoiceNumber() + ".");
    }
    if (isBlank(submitter.otherPartyCity())) {
      ctx.error(
          "invoice.otherparty." + otherParty + ".city.required.error",
          "otherPartyCity is required when there is no " + otherParty
              + " client number+location for invoiceNumber " + ctx.invoiceNumber() + ".");
    }
    if (isBlank(submitter.otherPartyProvState())) {
      ctx.error(
          "invoice.otherparty." + otherParty + ".province.required.error",
          "otherPartyProvinceState is required when there is no " + otherParty
              + " client number+location for invoiceNumber " + ctx.invoiceNumber() + ".");
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
      ctx.error(
          "invoice.submitter.equal.other.client.error",
          "The seller and buyer cannot have the same client number+location ("
              + submitter.submitterClientNumber() + "/" + submitter.submitterLocnCode()
              + ") for invoiceNumber " + ctx.invoiceNumber() + ".");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
