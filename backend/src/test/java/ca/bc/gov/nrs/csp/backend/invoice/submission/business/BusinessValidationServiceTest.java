package ca.bc.gov.nrs.csp.backend.invoice.submission.business;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice.InvoiceCodeRules;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice.InvoiceDateRules;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.line.LineItemRules;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.submission.SubmissionRules;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.SubmissionRule;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.IdentifierNormalizer;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterResolver;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmitterType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.SellerSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * Exercises the rule-running framework end-to-end with the three example rules
 * wired in (as Spring would), demonstrating accumulate-and-derive: partial
 * acceptance, the valid flag, and severity-aware messages.
 */
@ExtendWith(MockitoExtension.class)
class BusinessValidationServiceTest {

  @Mock
  ReferenceDataService referenceData;

  private static final ZoneId ZONE = ZoneId.of("America/Vancouver");
  private static final LocalDate TODAY = LocalDate.of(2024, Month.JUNE, 15);
  private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);

  private BusinessValidationService service() {
    return new BusinessValidationService(
        List.of(new SubmissionRules()),
        List.of(new InvoiceDateRules(CLOCK)),
        List.of(new LineItemRules()),
        new SubmitterResolver(),
        new IdentifierNormalizer(),
        referenceData);
  }

  @Test
  void partial_acceptance_rejects_only_the_bad_invoice() {
    given(referenceData.clientLocationExists(any(), any())).willReturn(true);
    given(referenceData.sortCodeValidOn(any(), any())).willReturn(true);
    given(referenceData.speciesGradeCombinationExists(any(), any())).willReturn(true);

    CSPSubmissionType submission = submissionWith(
        invoice("INV-GOOD", TODAY.minusDays(1), "Z"),
        invoice("INV-BAD", TODAY.plusDays(1), "A"));

    BusinessValidationOutcome outcome = service().validate(submission);

    assertThat(outcome.acceptance().accepted()).containsExactly("INV-GOOD");
    assertThat(outcome.acceptance().rejected()).containsExactly("INV-BAD");
    // Submission-level OK + at least one invoice accepted ⇒ overall valid.
    assertThat(outcome.valid()).isTrue();
    // One ERROR (future date on INV-BAD) and one WARNING (grade Z on INV-GOOD).
    assertThat(outcome.messages()).anyMatch(m -> m.severity() == Severity.ERROR
        && m.code().equals("invoice.date.in.future.error"));
    assertThat(outcome.messages()).anyMatch(m -> m.severity() == Severity.WARNING
        && m.code().equals("invoice.grade.z.warning"));
  }

  @Test
  void invalid_client_location_rejects_whole_submission() {
    given(referenceData.clientLocationExists(any(), any())).willReturn(false);

    CSPSubmissionType submission = submissionWith(
        invoice("INV-1", TODAY.minusDays(1), "A"));

    BusinessValidationOutcome outcome = service().validate(submission);

    assertThat(outcome.valid()).isFalse();
    assertThat(outcome.messages()).anyMatch(m -> m.severity() == Severity.ERROR
        && m.code().equals("invoice.submitter.client.location.invalid.error"));
  }

  @Test
  void duplicate_invoice_numbers_do_not_cross_contaminate() {
    given(referenceData.clientLocationExists(any(), any())).willReturn(true);
    given(referenceData.sortCodeValidOn(any(), any())).willReturn(true);
    given(referenceData.speciesGradeCombinationExists(any(), any())).willReturn(true);

    // Two invoices share the SAME number; only the second has a future-date ERROR.
    // Keying on index (not the number) must keep the first (clean) invoice accepted.
    CSPSubmissionType submission = submissionWith(
        invoice("DUP", TODAY.minusDays(1), "A"),    // clean
        invoice("DUP", TODAY.plusDays(1), "A"));    // future date -> rejected

    BusinessValidationOutcome outcome = service().validate(submission);

    assertThat(outcome.acceptance().accepted()).containsExactly("DUP");  // the clean invoice (index 0)
    assertThat(outcome.acceptance().rejected()).containsExactly("DUP");  // the bad invoice (index 1)
    assertThat(outcome.valid()).isTrue();                                // >= 1 accepted
  }

  // -------- S2: submission valid iff S1 passes AND >= 1 invoice passes all its rules --------

  @Test
  void s2_submission_invalid_when_s1_ok_but_all_invoices_rejected() {
    given(referenceData.clientLocationExists(any(), any())).willReturn(true); // S1 passes
    given(referenceData.sortCodeValidOn(any(), any())).willReturn(true);
    given(referenceData.speciesGradeCombinationExists(any(), any())).willReturn(true);

    // Every invoice has a future date (ERROR), so none passes -> nothing accepted.
    CSPSubmissionType submission = submissionWith(
        invoice("INV-1", TODAY.plusDays(1), "A"),
        invoice("INV-2", TODAY.plusDays(2), "A"));

    BusinessValidationOutcome outcome = service().validate(submission);

    // S1 ok but zero invoices accepted -> submission invalid (the second S2 clause fails).
    assertThat(outcome.acceptance().accepted()).isEmpty();
    assertThat(outcome.acceptance().rejected()).containsExactly("INV-1", "INV-2");
    assertThat(outcome.valid()).isFalse();
  }

  @Test
  void s2_submission_valid_when_s1_ok_and_all_invoices_pass() {
    given(referenceData.clientLocationExists(any(), any())).willReturn(true);
    given(referenceData.sortCodeValidOn(any(), any())).willReturn(true);
    given(referenceData.speciesGradeCombinationExists(any(), any())).willReturn(true);

    CSPSubmissionType submission = submissionWith(
        invoice("INV-1", TODAY.minusDays(1), "A"),
        invoice("INV-2", TODAY.minusDays(2), "A"));

    BusinessValidationOutcome outcome = service().validate(submission);

    assertThat(outcome.acceptance().accepted()).containsExactly("INV-1", "INV-2");
    assertThat(outcome.acceptance().rejected()).isEmpty();
    assertThat(outcome.valid()).isTrue();
  }

  @Test
  void s2_warning_only_invoice_still_counts_as_passing() {
    given(referenceData.clientLocationExists(any(), any())).willReturn(true);
    given(referenceData.sortCodeValidOn(any(), any())).willReturn(true);
    given(referenceData.speciesGradeCombinationExists(any(), any())).willReturn(true);

    // Grade Z raises a WARNING only; a warning does not reject, so the invoice passes.
    CSPSubmissionType submission = submissionWith(invoice("INV-1", TODAY.minusDays(1), "Z"));

    BusinessValidationOutcome outcome = service().validate(submission);

    assertThat(outcome.acceptance().accepted()).containsExactly("INV-1");
    assertThat(outcome.valid()).isTrue();
    assertThat(outcome.messages()).anyMatch(m -> m.severity() == Severity.WARNING
        && m.code().equals("invoice.grade.z.warning"));
  }

  @Test
  void submission_level_warning_does_not_invalidate_the_submission() {
    given(referenceData.sortCodeValidOn(any(), any())).willReturn(true);
    given(referenceData.speciesGradeCombinationExists(any(), any())).willReturn(true);

    // A submission-level WARNING must not trip the submission-level ERROR check:
    // only an ERROR with a null invoice index rejects the whole submission.
    SubmissionRule warnOnlyRule = ctx -> ctx.warning("submission.test.warning", new Object[0]);
    BusinessValidationService service = new BusinessValidationService(
        List.of(warnOnlyRule),
        List.of(new InvoiceDateRules(CLOCK)),
        List.of(new LineItemRules()),
        new SubmitterResolver(),
        new IdentifierNormalizer(),
        referenceData);

    BusinessValidationOutcome outcome =
        service.validate(submissionWith(invoice("INV-1", TODAY.minusDays(1), "A")));

    assertThat(outcome.acceptance().accepted()).containsExactly("INV-1");
    assertThat(outcome.valid()).isTrue();
    assertThat(outcome.messages()).anyMatch(m -> m.severity() == Severity.WARNING
        && m.code().equals("submission.test.warning"));
  }

  @Test
  void lowercase_codes_are_upper_cased_before_the_reference_lookups() {
    // The reported repro: a file spelling valid codes in lower case must be
    // accepted, not reported as unknown codes. Every stub below recognises only
    // the canonical upper-case form, so an unnormalised value fails the lookup.
    given(referenceData.clientLocationExists(any(), any())).willReturn(true);
    given(referenceData.maturityValidOn(eq("O"), any())).willReturn(true);
    given(referenceData.sortCodeValidOn(eq("G"), any())).willReturn(true);   // primary
    given(referenceData.sortCodeValidOn(eq("A"), any())).willReturn(true);   // secondary
    given(referenceData.speciesGradeCombinationExists("FI", "B")).willReturn(true);

    CSPInvoiceType invoice = invoice("inv-1", TODAY.minusDays(1), "b");
    invoice.setInvoiceType("sal");
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setMaturity("o");
    details.setPrimarySortCode("g");
    details.setLocationFOB("porv");
    invoice.setCSPInvoiceDetails(details);
    CSPLineItemType line = invoice.getCSPLineItem().get(0);
    line.setSecondarySortCode("a");
    line.setSpecies("fi");

    BusinessValidationService service = new BusinessValidationService(
        List.of(new SubmissionRules()),
        List.of(new InvoiceDateRules(CLOCK), new InvoiceCodeRules()),
        List.of(new LineItemRules()),
        new SubmitterResolver(),
        new IdentifierNormalizer(),
        referenceData);

    BusinessValidationOutcome outcome = service.validate(submissionWith(invoice));

    assertThat(outcome.messages()).noneMatch(m -> m.severity() == Severity.ERROR);
    assertThat(outcome.valid()).isTrue();
    assertThat(outcome.acceptance().accepted()).containsExactly("INV-1");

    // The tree itself now carries the canonical codes — this is the same tree the
    // submit path persists, so what was validated is what gets saved.
    assertThat(invoice.getInvoiceType()).isEqualTo("SAL");
    assertThat(details.getMaturity()).isEqualTo("O");
    assertThat(details.getPrimarySortCode()).isEqualTo("G");
    assertThat(details.getLocationFOB()).isEqualTo("PORV");
    assertThat(line.getSecondarySortCode()).isEqualTo("A");
    assertThat(line.getSpecies()).isEqualTo("FI");
    assertThat(line.getGrade()).isEqualTo("B");
  }

  // -------- fixture builders --------

  private static CSPSubmissionType submissionWith(CSPInvoiceType... invoices) {
    CSPSubmitterType submitter = new CSPSubmitterType();
    submitter.setSellerSubmission(SellerSubmissionType.Y);
    submitter.setSubmissionClientNumber("100");
    submitter.setSubmissionClientLocnCode("00");
    CSPSubmissionType submission = new CSPSubmissionType();
    submission.setCSPSubmitter(submitter);
    submission.getCSPInvoice().addAll(List.of(invoices));
    return submission;
  }

  private static CSPInvoiceType invoice(String number, LocalDate date, String grade) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber(number);
    invoice.setInvoiceType("SAL");
    invoice.setInvoiceDate(xmlDate(date));
    CSPLineItemType line = new CSPLineItemType();
    line.setSecondarySortCode("SC");
    line.setSpecies("FIR");
    line.setGrade(grade);
    line.setNumberOfPieces(1);
    line.setVolume(BigDecimal.ONE);
    line.setPrice(BigDecimal.ONE);
    invoice.getCSPLineItem().add(line);
    return invoice;
  }

  private static XMLGregorianCalendar xmlDate(LocalDate d) {
    try {
      return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
          d.getYear(), d.getMonthValue(), d.getDayOfMonth(), DatatypeConstants.FIELD_UNDEFINED);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
