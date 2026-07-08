package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterInfo.SubmittedBy;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.shared.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for the §2.3 party rules (I12–I21). Each rule method is called
 * directly with a hand-built {@link SubmitterInfo} so the test stays focused on
 * one rule. {@link ReferenceDataService} is mocked and only stubbed for the
 * client/location existence rules (I15/I16/I19).
 */
@ExtendWith(MockitoExtension.class)
class InvoicePartyRulesTest {

  @Mock
  ReferenceDataService referenceData;

  private final InvoicePartyRules rules = new InvoicePartyRules();

  // ---------------------------------------------------------------- I12

  @Test
  void i12_errors_when_both_parties_identified_and_free_text_present() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .submitter("100", "00")
        .other("200", "01")
        .freeText("Acme", null, null)
        .build();

    rules.otherPartyFieldEmpty(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.otherparty.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void i12_passes_when_both_parties_identified_and_no_free_text() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().submitter("100", "00").other("200", "01").build();

    rules.otherPartyFieldEmpty(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i12_passes_when_other_party_not_identified() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().submitter("100", "00").freeText("Acme", "Van", "BC").build();

    rules.otherPartyFieldEmpty(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I13

  @Test
  void i13_errors_on_seller_submission_with_identified_buyer_and_all_free_text() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.SELLER)
        .other("200", "01")
        .freeText("Acme", "Van", "BC")
        .build();

    rules.invalidBuyerSubmissionCombination(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.otherparty.buyer.submission.error");
  }

  @Test
  void i13_passes_when_not_all_free_text_present() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.SELLER)
        .other("200", "01")
        .freeText("Acme", "Van", null)
        .build();

    rules.invalidBuyerSubmissionCombination(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i13_passes_on_buyer_submission() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.BUYER)
        .other("200", "01")
        .freeText("Acme", "Van", "BC")
        .build();

    rules.invalidBuyerSubmissionCombination(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I14

  @Test
  void i14_errors_on_buyer_submission_with_identified_seller_and_all_free_text() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.BUYER)
        .other("200", "01")
        .freeText("Acme", "Van", "BC")
        .build();

    rules.invalidSellerSubmissionCombination(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.otherparty.seller.submission.error");
  }

  @Test
  void i14_passes_on_seller_submission() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.SELLER)
        .other("200", "01")
        .freeText("Acme", "Van", "BC")
        .build();

    rules.invalidSellerSubmissionCombination(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I15

  @Test
  void i15_errors_when_buyer_combination_not_found_on_seller_submission() {
    given(referenceData.clientLocationExists("200", "01")).willReturn(false);
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().by(SubmittedBy.SELLER).other("200", "01").build();

    rules.buyerClientLocationExists(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.buyer.client.location.invalid.error");
  }

  @Test
  void i15_reads_buyer_from_submitter_on_buyer_submission() {
    given(referenceData.clientLocationExists("300", "02")).willReturn(false);
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().by(SubmittedBy.BUYER).submitter("300", "02").build();

    rules.buyerClientLocationExists(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.buyer.client.location.invalid.error");
  }

  @Test
  void i15_passes_when_buyer_combination_found() {
    given(referenceData.clientLocationExists("200", "01")).willReturn(true);
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().by(SubmittedBy.SELLER).other("200", "01").build();

    rules.buyerClientLocationExists(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i15_skips_lookup_when_buyer_blank() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().by(SubmittedBy.SELLER).build();

    rules.buyerClientLocationExists(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
    verifyNoInteractions(referenceData);
  }

  // ---------------------------------------------------------------- I16

  @Test
  void i16_errors_when_seller_combination_not_found_on_seller_submission() {
    given(referenceData.clientLocationExists("100", "00")).willReturn(false);
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().by(SubmittedBy.SELLER).submitter("100", "00").build();

    rules.sellerClientLocationExists(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.seller.client.location.invalid.error");
  }

  @Test
  void i16_passes_when_seller_combination_found() {
    given(referenceData.clientLocationExists("100", "00")).willReturn(true);
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().by(SubmittedBy.SELLER).submitter("100", "00").build();

    rules.sellerClientLocationExists(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i16_skips_lookup_when_seller_blank() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().by(SubmittedBy.SELLER).build();

    rules.sellerClientLocationExists(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
    verifyNoInteractions(referenceData);
  }

  // ---------------------------------------------------------------- I17

  @Test
  void i17_errors_when_submission_number_differs_from_seller_number() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.SELLER)
        .submitter("999", "00")
        .submission("100", "00")
        .build();

    rules.submissionNumberEqualsSellerNumber(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.submitter.not.equal.seller.client.number.error");
  }

  @Test
  void i17_passes_when_submission_number_matches_seller_number() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.SELLER)
        .submitter("100", "00")
        .submission("100", "00")
        .build();

    rules.submissionNumberEqualsSellerNumber(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i17_skipped_on_buyer_submission() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.BUYER)
        .submitter("999", "00")
        .submission("100", "00")
        .build();

    rules.submissionNumberEqualsSellerNumber(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I18

  @Test
  void i18_errors_when_submission_location_differs_from_seller_location() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.SELLER)
        .submitter("100", "01")
        .submission("100", "00")
        .build();

    rules.submissionLocationEqualsSellerLocation(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.submitter.not.equal.seller.client.location.error");
  }

  @Test
  void i18_passes_when_submission_location_matches_seller_location() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.SELLER)
        .submitter("100", "00")
        .submission("100", "00")
        .build();

    rules.submissionLocationEqualsSellerLocation(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i18_skipped_on_buyer_submission() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.BUYER)
        .submitter("100", "01")
        .submission("100", "00")
        .build();

    rules.submissionLocationEqualsSellerLocation(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I19

  @Test
  void i19_errors_when_other_party_combination_not_found() {
    given(referenceData.clientLocationExists("200", "01")).willReturn(false);
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().other("200", "01").build();

    rules.otherPartyClientLocationExists(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.otherparty.client.location.invalid.error");
  }

  @Test
  void i19_passes_when_other_party_combination_found() {
    given(referenceData.clientLocationExists("200", "01")).willReturn(true);
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().other("200", "01").build();

    rules.otherPartyClientLocationExists(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i19_skips_lookup_when_other_party_number_blank() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().freeText("Acme", "Van", "BC").build();

    rules.otherPartyClientLocationExists(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
    verifyNoInteractions(referenceData);
  }

  // ---------------------------------------------------------------- I20

  @Test
  void i20_errors_for_each_missing_free_text_field_on_seller_submission() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().by(SubmittedBy.SELLER).build(); // other number blank

    rules.otherPartyFreeTextRequired(context(collector, submitter));

    assertThat(collector.entries()).extracting(e -> e.error().code())
        .containsExactlyInAnyOrder(
            "invoice.otherparty.buyer.name.required.error",
            "invoice.otherparty.buyer.city.required.error",
            "invoice.otherparty.buyer.province.required.error");
  }

  @Test
  void i20_uses_seller_message_variant_on_buyer_submission() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party()
        .by(SubmittedBy.BUYER)
        .freeText("Acme", "Van", null) // only province missing
        .build();

    rules.otherPartyFreeTextRequired(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.otherparty.seller.province.required.error");
  }

  @Test
  void i20_passes_when_all_free_text_present() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().freeText("Acme", "Van", "BC").build();

    rules.otherPartyFreeTextRequired(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i20_skipped_when_other_party_number_present() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().other("200", "01").build();

    rules.otherPartyFreeTextRequired(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I21

  @Test
  void i21_errors_when_seller_and_buyer_are_the_same_client_and_location() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().submitter("100", "00").other("100", "00").build();

    rules.sellerAndBuyerNotSame(context(collector, submitter));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.submitter.equal.other.client.error");
  }

  @Test
  void i21_passes_when_client_numbers_differ() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().submitter("100", "00").other("200", "00").build();

    rules.sellerAndBuyerNotSame(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i21_passes_when_locations_differ() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().submitter("100", "00").other("100", "01").build();

    rules.sellerAndBuyerNotSame(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i21_skipped_when_other_party_not_identified() {
    ValidationCollector collector = new ValidationCollector();
    SubmitterInfo submitter = party().submitter("100", "00").build();

    rules.sellerAndBuyerNotSame(context(collector, submitter));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- helpers

  private InvoiceRuleContext context(ValidationCollector collector, SubmitterInfo submitter) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, submitter, referenceData, collector);
  }

  private static Party party() {
    return new Party();
  }

  /** Fluent builder for {@link SubmitterInfo}; every field defaults to null / SELLER. */
  private static final class Party {
    private SubmittedBy by = SubmittedBy.SELLER;
    private String submitterClientNumber;
    private String submitterLocnCode;
    private String otherClientNumber;
    private String otherLocnCode;
    private String otherPartyName;
    private String otherPartyCity;
    private String otherPartyProvState;
    private String submissionClientNumber;
    private String submissionLocnCode;

    Party by(SubmittedBy by) {
      this.by = by;
      return this;
    }

    Party submitter(String number, String locn) {
      this.submitterClientNumber = number;
      this.submitterLocnCode = locn;
      return this;
    }

    Party other(String number, String locn) {
      this.otherClientNumber = number;
      this.otherLocnCode = locn;
      return this;
    }

    Party freeText(String name, String city, String provState) {
      this.otherPartyName = name;
      this.otherPartyCity = city;
      this.otherPartyProvState = provState;
      return this;
    }

    Party submission(String number, String locn) {
      this.submissionClientNumber = number;
      this.submissionLocnCode = locn;
      return this;
    }

    SubmitterInfo build() {
      return new SubmitterInfo(
          by,
          submitterClientNumber,
          submitterLocnCode,
          otherClientNumber,
          otherLocnCode,
          otherPartyName,
          otherPartyCity,
          otherPartyProvState,
          submissionClientNumber,
          submissionLocnCode);
    }
  }
}
