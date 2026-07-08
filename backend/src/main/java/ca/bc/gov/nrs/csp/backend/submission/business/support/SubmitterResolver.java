package ca.bc.gov.nrs.csp.backend.submission.business.support;

import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmitterType;
import ca.bc.gov.nrs.csp.backend.submission.generated.SellerSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterInfo.SubmittedBy;
import org.springframework.stereotype.Component;

/**
 * Resolves which party submitted an invoice and which is the "other party",
 * from the submission-level {@code sellerSubmission} flag. Mirrors the legacy
 * XMLParser assignment: a Seller submission makes the submitter the seller and
 * the other party the buyer; a Buyer submission is the reverse.
 *
 * <p>Pure logic, no DB. Run once per invoice by {@code BusinessValidationService}
 * and handed to every invoice/line rule via the context.
 */
@Component
public class SubmitterResolver {

  public SubmitterInfo resolve(CSPSubmissionType submission, CSPInvoiceType invoice) {
    CSPSubmitterType submitter = submission.getCSPSubmitter();
    boolean submittedBySeller = submitter.getSellerSubmission() == SellerSubmissionType.Y;
    String submissionClientNumber = submitter.getSubmissionClientNumber();
    String submissionLocnCode = submitter.getSubmissionClientLocnCode();

    if (submittedBySeller) {
      return new SubmitterInfo(
          SubmittedBy.SELLER,
          invoice.getSellerClientNumber(), invoice.getSellerClientLocnCode(),
          invoice.getBuyerClientNumber(), invoice.getBuyerClientLocnCode(),
          invoice.getOtherPartyName(), invoice.getOtherPartyCity(), invoice.getOtherPartyProvState(),
          submissionClientNumber, submissionLocnCode);
    }
    return new SubmitterInfo(
        SubmittedBy.BUYER,
        invoice.getBuyerClientNumber(), invoice.getBuyerClientLocnCode(),
        invoice.getSellerClientNumber(), invoice.getSellerClientLocnCode(),
        invoice.getOtherPartyName(), invoice.getOtherPartyCity(), invoice.getOtherPartyProvState(),
        submissionClientNumber, submissionLocnCode);
  }
}
