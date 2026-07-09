package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for the §2.6 source-document reference rules (boom / timber /
 * weigh). Each rule method is called directly and assertions are made on the
 * collector, matching the sibling reference-rule tests.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceSourceDocumentRulesTest {

  @Mock
  ReferenceDataService referenceData;

  private final InvoiceSourceDocumentRules rules = new InvoiceSourceDocumentRules();

  // ---------------------------------------------------------------- validate (orchestration)

  @Test
  void validate_runs_every_rule_and_reports_nothing_for_a_valid_invoice() {
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, "B1, B1", "T1", "W1"));

    // I31 de-dupes "B1, B1" -> "B1" in place; all other checks pass.
    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void validate_reports_a_finding_from_a_downstream_rule() {
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, null, null, null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.oneofthe.boom.timber.wiegh.requiered.error");
  }

  // ---------------------------------------------------------------- I30

  @Test
  void i30_errors_when_no_source_document_is_present() {
    ValidationCollector collector = new ValidationCollector();

    rules.atLeastOneSourceDocument(context(collector, null, null, null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.oneofthe.boom.timber.wiegh.requiered.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void i30_errors_when_the_details_block_is_absent() {
    ValidationCollector collector = new ValidationCollector();

    rules.atLeastOneSourceDocument(contextWithoutDetails(collector));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.oneofthe.boom.timber.wiegh.requiered.error");
  }

  @ParameterizedTest(name = "passes when boom=[{0}] timber=[{1}] weigh=[{2}]")
  @CsvSource(value = {
      "B1  , null, null",  // only boom present
      "null, T1  , null",  // only timber present
      "null, null, W1  ",  // only weigh present
      "B1  , T1  , W1  ",  // all present
  }, nullValues = "null")
  void i30_passes_when_at_least_one_is_present(String boom, String timber, String weigh) {
    ValidationCollector collector = new ValidationCollector();

    rules.atLeastOneSourceDocument(context(collector, boom, timber, weigh));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I31

  @Test
  void i31_removes_duplicate_tokens_in_place_across_all_three_fields() {
    CSPInvoiceDetailsType details = details("B1, B2 , B1", "T1,T1", "W1 ,W1, W2");
    InvoiceRuleContext ctx = context(new ValidationCollector(), details);

    rules.deduplicateSourceDocuments(ctx);

    assertThat(details.getBoomNumbers()).isEqualTo("B1,B2");
    assertThat(details.getTimberMarks()).isEqualTo("T1");
    assertThat(details.getWeighSlipNumbers()).isEqualTo("W1,W2");
  }

  @Test
  void i31_leaves_blank_values_untouched_and_is_null_safe() {
    CSPInvoiceDetailsType details = details(null, "  ", "W1");
    InvoiceRuleContext ctx = context(new ValidationCollector(), details);

    rules.deduplicateSourceDocuments(ctx);

    assertThat(details.getBoomNumbers()).isNull();
    assertThat(details.getTimberMarks()).isEqualTo("  ");
    assertThat(details.getWeighSlipNumbers()).isEqualTo("W1");
  }

  @Test
  void i31_drops_empty_tokens_between_consecutive_commas() {
    CSPInvoiceDetailsType details = details("B1,,B2", null, null);
    InvoiceRuleContext ctx = context(new ValidationCollector(), details);

    rules.deduplicateSourceDocuments(ctx);

    assertThat(details.getBoomNumbers()).isEqualTo("B1,B2");
  }

  @Test
  void i31_is_a_no_op_when_the_details_block_is_absent() {
    ValidationCollector collector = new ValidationCollector();

    rules.deduplicateSourceDocuments(contextWithoutDetails(collector));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I32/I33/I34

  @Test
  void i32_errors_when_more_than_ten_boom_numbers() {
    ValidationCollector collector = new ValidationCollector();

    rules.boomNumbersWithinMax(context(collector, elevenTokens(), null, null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.morethan.Max.boomnumbers.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void i33_errors_when_more_than_ten_timber_marks() {
    ValidationCollector collector = new ValidationCollector();

    rules.timberMarksWithinMax(context(collector, null, elevenTokens(), null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.morethan.Max.timbermarks.error");
  }

  @Test
  void i34_errors_when_more_than_ten_weigh_slips() {
    ValidationCollector collector = new ValidationCollector();

    rules.weighSlipsWithinMax(context(collector, null, null, elevenTokens()));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.morethan.Max.weighslips.error");
  }

  @Test
  void max_passes_at_exactly_ten_and_when_blank() {
    ValidationCollector collector = new ValidationCollector();

    rules.boomNumbersWithinMax(context(collector, tenTokens(), null, null));
    rules.boomNumbersWithinMax(context(collector, null, null, null));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I35/I36/I37

  @Test
  void i35_errors_and_lists_boom_tokens_longer_than_twenty_chars() {
    ValidationCollector collector = new ValidationCollector();
    String tooLong = "X".repeat(21);

    rules.boomTokensWithinMaxLength(context(collector, "OK," + tooLong, null, null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.tokennumber.lenght.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
    // Template args: field label, max length, then only the offending token(s).
    assertThat(collector.entries().get(0).error().args()).containsExactly("Boom Numbers", 20, tooLong);
  }

  @Test
  void i36_errors_when_a_timber_mark_exceeds_six_chars() {
    ValidationCollector collector = new ValidationCollector();

    rules.timberTokensWithinMaxLength(context(collector, null, "TOOLONG", null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.tokennumber.lenght.error");
  }

  @Test
  void i37_passes_when_weigh_slips_are_within_one_hundred_chars() {
    ValidationCollector collector = new ValidationCollector();

    rules.weighTokensWithinMaxLength(context(collector, null, null, "X".repeat(100)));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void token_length_check_is_skipped_when_the_field_is_blank() {
    ValidationCollector collector = new ValidationCollector();

    rules.boomTokensWithinMaxLength(context(collector, null, null, null));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I38

  @Test
  void i38_warns_listing_boom_numbers_already_used_by_another_invoice() {
    given(referenceData.boomNumberUsedByAnotherInvoice("B1")).willReturn(true);
    given(referenceData.boomNumberUsedByAnotherInvoice("B2")).willReturn(false);
    ValidationCollector collector = new ValidationCollector();

    rules.boomNumbersNotUsedByAnotherInvoice(context(collector, "B1,B2", null, null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.boomnumber.duplicate.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
    assertThat(collector.entries().get(0).error().args()).containsExactly("B1");
  }

  @Test
  void i38_checks_every_token_including_the_last() {
    given(referenceData.boomNumberUsedByAnotherInvoice("B1")).willReturn(false);
    given(referenceData.boomNumberUsedByAnotherInvoice("B2")).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.boomNumbersNotUsedByAnotherInvoice(context(collector, "B1,B2", null, null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().args()).containsExactly("B2");
  }

  @Test
  void i38_skips_empty_tokens_between_consecutive_commas() {
    given(referenceData.boomNumberUsedByAnotherInvoice("B1")).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.boomNumbersNotUsedByAnotherInvoice(context(collector, "B1, ,", null, null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().args()).containsExactly("B1");
  }

  @Test
  void i38_passes_and_skips_lookup_when_no_boom_numbers() {
    ValidationCollector collector = new ValidationCollector();

    rules.boomNumbersNotUsedByAnotherInvoice(context(collector, null, null, null));

    assertThat(collector.entries()).isEmpty();
    verifyNoInteractions(referenceData);
  }

  // ---------------------------------------------------------------- helpers

  private static String tenTokens() {
    return String.join(",", java.util.Collections.nCopies(10, "T"));
  }

  private static String elevenTokens() {
    return String.join(",", java.util.Collections.nCopies(11, "T"));
  }

  private static CSPInvoiceDetailsType details(String boom, String timber, String weigh) {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setBoomNumbers(boom);
    details.setTimberMarks(timber);
    details.setWeighSlipNumbers(weigh);
    return details;
  }

  private InvoiceRuleContext context(
      ValidationCollector collector, String boom, String timber, String weigh) {
    return context(collector, details(boom, timber, weigh));
  }

  private InvoiceRuleContext context(ValidationCollector collector, CSPInvoiceDetailsType details) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setCSPInvoiceDetails(details);
    return new InvoiceRuleContext(
        new CSPSubmissionType(), invoice, 0, null, referenceData, collector);
  }

  private InvoiceRuleContext contextWithoutDetails(ValidationCollector collector) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    return new InvoiceRuleContext(
        new CSPSubmissionType(), invoice, 0, null, referenceData, collector);
  }
}
