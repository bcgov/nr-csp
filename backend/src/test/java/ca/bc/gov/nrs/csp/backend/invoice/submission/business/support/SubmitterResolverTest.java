package ca.bc.gov.nrs.csp.backend.invoice.submission.business.support;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterInfo.SubmittedBy;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmitterType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.SellerSubmissionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubmitterResolverTest {

  private final SubmitterResolver resolver = new SubmitterResolver();

  @Test
  void seller_submission_makes_submitter_the_seller() {
    SubmitterInfo info = resolver.resolve(submission(SellerSubmissionType.Y), invoice());

    assertThat(info.submittedBy()).isEqualTo(SubmittedBy.SELLER);
    assertThat(info.submitterClientNumber()).isEqualTo("SELL");
    assertThat(info.submitterLocnCode()).isEqualTo("01");
    assertThat(info.otherClientNumber()).isEqualTo("BUY");
    assertThat(info.otherLocnCode()).isEqualTo("02");
  }

  @Test
  void buyer_submission_makes_submitter_the_buyer() {
    SubmitterInfo info = resolver.resolve(submission(SellerSubmissionType.N), invoice());

    assertThat(info.submittedBy()).isEqualTo(SubmittedBy.BUYER);
    assertThat(info.submitterClientNumber()).isEqualTo("BUY");
    assertThat(info.otherClientNumber()).isEqualTo("SELL");
  }

  private static CSPSubmissionType submission(SellerSubmissionType flag) {
    CSPSubmitterType submitter = new CSPSubmitterType();
    submitter.setSellerSubmission(flag);
    submitter.setSubmissionClientNumber("100");
    submitter.setSubmissionClientLocnCode("00");
    CSPSubmissionType submission = new CSPSubmissionType();
    submission.setCSPSubmitter(submitter);
    return submission;
  }

  private static CSPInvoiceType invoice() {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setSellerClientNumber("SELL");
    invoice.setSellerClientLocnCode("01");
    invoice.setBuyerClientNumber("BUY");
    invoice.setBuyerClientLocnCode("02");
    return invoice;
  }
}
