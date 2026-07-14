package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.referencedata.InvoiceRef;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterInfo.SubmittedBy;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for the §2.2 replace/adjust reference rules. Each rule method is
 * called directly and assertions are made on the collector.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceReferenceRulesTest {

  @Mock
  ReferenceDataService referenceData;

  private final InvoiceReferenceRules rules = new InvoiceReferenceRules();

  // ---------------------------------------------------------------- validate

  @Test
  void validate_runs_all_rules_and_passes_when_no_references_are_populated() {
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, null, null));

    assertThat(collector.entries()).isEmpty();
    verifyNoInteractions(referenceData);
  }

  // ---------------------------------------------------------------- I4

  @Test
  void i4_errors_when_both_replaces_and_adjusts_are_populated() {
    ValidationCollector collector = new ValidationCollector();

    rules.onlyOneOfReplaceOrAdjust(context(collector, "INV-2", "INV-3"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.both.replace.adjust.invoicenum.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @ParameterizedTest(name = "passes when replaces=[{0}] adjusts=[{1}]")
  @CsvSource(value = {
      "INV-2 , null",   // only replaces populated
      "null  , INV-3",  // only adjusts populated
      "null  , null",   // neither populated
      "'  '  , INV-3",  // blank replaces treated as not populated
  }, nullValues = "null")
  void i4_passes_unless_both_replaces_and_adjusts_are_populated(String replaces, String adjusts) {
    ValidationCollector collector = new ValidationCollector();

    rules.onlyOneOfReplaceOrAdjust(context(collector, replaces, adjusts));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I5

  @Test
  void i5_errors_listing_replaces_numbers_that_do_not_exist_for_the_client() {
    given(referenceData.findInvoices("INV-2", "100", "00")).willReturn(List.of());
    given(referenceData.findInvoices("INV-3", "100", "00"))
        .willReturn(List.of(new InvoiceRef("INV-3", "APP")));
    ValidationCollector collector = new ValidationCollector();

    rules.replaceInvoiceNumbersExist(replaceContext(collector, "INV-2,INV-3", "100", "00"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.replace.invoicenumber.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
    // Template arg: only the missing number, not the one that resolved.
    assertThat(collector.entries().get(0).error().args()).containsExactly("INV-2");
  }

  @Test
  void i5_passes_when_every_replaces_number_identifies_an_invoice_for_the_client() {
    given(referenceData.findInvoices("INV-2", "100", "00"))
        .willReturn(List.of(new InvoiceRef("INV-2", "APP")));
    ValidationCollector collector = new ValidationCollector();

    rules.replaceInvoiceNumbersExist(replaceContext(collector, "INV-2", "100", "00"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i5_trims_tokens_and_skips_blank_entries() {
    given(referenceData.findInvoices("INV-2", "100", "00"))
        .willReturn(List.of(new InvoiceRef("INV-2", "APP")));
    ValidationCollector collector = new ValidationCollector();

    rules.replaceInvoiceNumbersExist(replaceContext(collector, " INV-2 , ", "100", "00"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i5_skips_lookup_when_replaces_is_blank() {
    ValidationCollector collector = new ValidationCollector();

    rules.replaceInvoiceNumbersExist(replaceContext(collector, null, "100", "00"));

    assertThat(collector.entries()).isEmpty();
    verifyNoInteractions(referenceData);
  }

  // ---------------------------------------------------------------- I6

  @Test
  void i6_errors_when_replaces_contains_its_own_invoice_number() {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceDoesNotReplaceItself(context(collector, "INV-1", null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.replace.with.itself.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void i6_errors_when_own_number_is_one_of_several_replaces_numbers() {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceDoesNotReplaceItself(context(collector, "INV-2, INV-1", null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.replace.with.itself.error");
  }

  @Test
  void i6_passes_when_replaces_does_not_contain_its_own_number() {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceDoesNotReplaceItself(context(collector, "INV-2,INV-3", null));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i6_passes_when_replaces_is_blank() {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceDoesNotReplaceItself(context(collector, null, null));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I7

  @Test
  void i7_errors_when_more_than_five_replaces_numbers() {
    ValidationCollector collector = new ValidationCollector();

    rules.replaceInvoiceNumbersWithinMax(context(collector, "A,B,C,D,E,F", null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.morethanmax.replace.invoicenum.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @ParameterizedTest(name = "passes when replaces=[{0}]")
  @CsvSource(value = {
      "'A,B,C,D,E'",  // exactly the max of five
      "A",            // a single number
      "null",         // blank / not populated
  }, nullValues = "null")
  void i7_passes_when_replaces_within_max(String replaces) {
    ValidationCollector collector = new ValidationCollector();

    rules.replaceInvoiceNumbersWithinMax(context(collector, replaces, null));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I8

  @Test
  void i8_errors_listing_adjusts_numbers_that_do_not_exist_for_the_client() {
    given(referenceData.findInvoices("INV-2", "100", "00")).willReturn(List.of());
    given(referenceData.findInvoices("INV-3", "100", "00"))
        .willReturn(List.of(new InvoiceRef("INV-3", "APP")));
    ValidationCollector collector = new ValidationCollector();

    rules.adjustInvoiceNumbersExist(adjustContext(collector, "INV-2,INV-3", "100", "00"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.adjust.invoicenumber.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
    // Template arg: only the missing number, not the one that resolved.
    assertThat(collector.entries().get(0).error().args()).containsExactly("INV-2");
  }

  @Test
  void i8_passes_when_every_adjusts_number_identifies_an_invoice_for_the_client() {
    given(referenceData.findInvoices("INV-2", "100", "00"))
        .willReturn(List.of(new InvoiceRef("INV-2", "APP")));
    ValidationCollector collector = new ValidationCollector();

    rules.adjustInvoiceNumbersExist(adjustContext(collector, "INV-2", "100", "00"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i8_trims_tokens_and_skips_blank_entries() {
    given(referenceData.findInvoices("INV-2", "100", "00"))
        .willReturn(List.of(new InvoiceRef("INV-2", "APP")));
    ValidationCollector collector = new ValidationCollector();

    rules.adjustInvoiceNumbersExist(adjustContext(collector, " INV-2 , ", "100", "00"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i8_skips_lookup_when_adjusts_is_blank() {
    ValidationCollector collector = new ValidationCollector();

    rules.adjustInvoiceNumbersExist(adjustContext(collector, null, "100", "00"));

    assertThat(collector.entries()).isEmpty();
    verifyNoInteractions(referenceData);
  }

  // ---------------------------------------------------------------- I9

  @Test
  void i9_errors_when_an_adjusted_invoice_is_cancelled() {
    given(referenceData.findInvoices("INV-2", "100", "00"))
        .willReturn(List.of(new InvoiceRef("INV-2", "CAN")));
    ValidationCollector collector = new ValidationCollector();

    rules.adjustedInvoiceNotCancelled(adjustContext(collector, "INV-2", "100", "00"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.validation.adjustedInvoiceCancelled");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void i9_passes_when_adjusted_invoices_are_not_cancelled() {
    given(referenceData.findInvoices("INV-2", "100", "00"))
        .willReturn(List.of(new InvoiceRef("INV-2", "APP")));
    ValidationCollector collector = new ValidationCollector();

    rules.adjustedInvoiceNotCancelled(adjustContext(collector, "INV-2", "100", "00"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i9_reports_once_and_stops_at_the_first_cancelled_invoice() {
    given(referenceData.findInvoices("INV-2", "100", "00"))
        .willReturn(List.of(new InvoiceRef("INV-2", "CAN")));
    ValidationCollector collector = new ValidationCollector();

    rules.adjustedInvoiceNotCancelled(adjustContext(collector, "INV-2,INV-3", "100", "00"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.validation.adjustedInvoiceCancelled");
  }

  @Test
  void i9_passes_when_adjusted_invoice_is_not_found() {
    given(referenceData.findInvoices("INV-2", "100", "00")).willReturn(List.of());
    ValidationCollector collector = new ValidationCollector();

    rules.adjustedInvoiceNotCancelled(adjustContext(collector, "INV-2", "100", "00"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i9_skips_lookup_when_adjusts_is_blank() {
    ValidationCollector collector = new ValidationCollector();

    rules.adjustedInvoiceNotCancelled(adjustContext(collector, null, "100", "00"));

    assertThat(collector.entries()).isEmpty();
    verifyNoInteractions(referenceData);
  }

  @Test
  void i9_trims_tokens_and_skips_blank_entries() {
    given(referenceData.findInvoices("INV-2", "100", "00"))
        .willReturn(List.of(new InvoiceRef("INV-2", "APP")));
    ValidationCollector collector = new ValidationCollector();

    rules.adjustedInvoiceNotCancelled(adjustContext(collector, " INV-2 , ", "100", "00"));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I10

  @Test
  void i10_errors_when_adjusts_contains_its_own_invoice_number() {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceDoesNotAdjustItself(context(collector, null, "INV-1"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.adjust.with.itself.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void i10_errors_when_own_number_is_one_of_several_adjusts_numbers() {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceDoesNotAdjustItself(context(collector, null, "INV-2, INV-1"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.adjust.with.itself.error");
  }

  @Test
  void i10_passes_when_adjusts_does_not_contain_its_own_number() {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceDoesNotAdjustItself(context(collector, null, "INV-2,INV-3"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void i10_passes_when_adjusts_is_blank() {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceDoesNotAdjustItself(context(collector, null, null));

    assertThat(collector.entries()).isEmpty();
  }

  // ---------------------------------------------------------------- I11

  @Test
  void i11_errors_when_more_than_five_adjusts_numbers() {
    ValidationCollector collector = new ValidationCollector();

    rules.adjustInvoiceNumbersWithinMax(context(collector, null, "A,B,C,D,E,F"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.morethanmax.adjust.invoicenum.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @ParameterizedTest(name = "passes when adjusts=[{0}]")
  @CsvSource(value = {
      "'A,B,C,D,E'",  // exactly the max of five
      "A",            // a single number
      "null",         // blank / not populated
  }, nullValues = "null")
  void i11_passes_when_adjusts_within_max(String adjusts) {
    ValidationCollector collector = new ValidationCollector();

    rules.adjustInvoiceNumbersWithinMax(context(collector, null, adjusts));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext replaceContext(
      ValidationCollector collector, String replaces, String submitterNumber, String submitterLocn) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setReplacesInvoiceNumbers(replaces);
    SubmitterInfo submitter = new SubmitterInfo(
        SubmittedBy.SELLER, submitterNumber, submitterLocn,
        null, null, null, null, null, submitterNumber, submitterLocn);
    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, submitter, referenceData, collector);
  }

  private InvoiceRuleContext adjustContext(
      ValidationCollector collector, String adjusts, String submitterNumber, String submitterLocn) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setAdjustsInvoiceNumbers(adjusts);
    SubmitterInfo submitter = new SubmitterInfo(
        SubmittedBy.SELLER, submitterNumber, submitterLocn,
        null, null, null, null, null, submitterNumber, submitterLocn);
    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, submitter, referenceData, collector);
  }

  private InvoiceRuleContext context(ValidationCollector collector, String replaces, String adjusts) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setReplacesInvoiceNumbers(replaces);
    invoice.setAdjustsInvoiceNumbers(adjusts);
    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, referenceData, collector);
  }
}
