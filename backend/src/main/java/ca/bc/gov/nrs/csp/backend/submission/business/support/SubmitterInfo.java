package ca.bc.gov.nrs.csp.backend.submission.business.support;

/**
 * Resolved party view of an invoice, derived once per invoice by
 * {@link SubmitterResolver} from the {@code sellerSubmission} flag. Rules read
 * "submitter" vs "other party" from here instead of re-deriving the
 * seller/buyer mapping (mirrors the legacy XMLParser assignment).
 */
public record SubmitterInfo(
    SubmittedBy submittedBy,
    String submitterClientNumber,
    String submitterLocnCode,
    String otherClientNumber,
    String otherLocnCode,
    String otherPartyName,
    String otherPartyCity,
    String otherPartyProvState,
    String submissionClientNumber,
    String submissionLocnCode) {

  public enum SubmittedBy { SELLER, BUYER }
}
