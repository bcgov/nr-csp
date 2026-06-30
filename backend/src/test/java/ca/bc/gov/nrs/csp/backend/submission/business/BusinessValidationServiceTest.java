package ca.bc.gov.nrs.csp.backend.submission.business;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice.InvoiceDateRules;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.line.LineItemRules;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.submission.SubmissionRules;
import ca.bc.gov.nrs.csp.backend.submission.business.support.IdentifierNormalizer;
import ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterResolver;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmitterType;
import ca.bc.gov.nrs.csp.backend.submission.generated.SellerSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.shared.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    line.setGrade(grade);
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
