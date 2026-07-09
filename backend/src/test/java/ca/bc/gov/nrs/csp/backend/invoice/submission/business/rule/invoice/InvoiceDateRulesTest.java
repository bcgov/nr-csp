package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.Severity;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Template for testing an invoice rule: call the rule's method directly, build a
 * context, assert on the collector. A fixed {@link Clock} pins "today" so the
 * date check is deterministic; {@link ReferenceDataService} is mocked so the
 * month-complete check is exercised without a database.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceDateRulesTest {

  private static final ZoneId ZONE = ZoneId.of("America/Vancouver");
  private static final LocalDate TODAY = LocalDate.of(2024, Month.JUNE, 15);

  @Mock
  ReferenceDataService referenceData;

  private final InvoiceDateRules rules =
      new InvoiceDateRules(Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE));

  @Test
  void dateNotInFuture_errors_when_in_the_future() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.dateNotInFuture(context(collector, TODAY.plusDays(1)));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.date.in.future.error");
    assertThat(collector.entries().get(0).invoiceIndex()).isZero();
    assertThat(collector.entries().get(0).error().path()).isEqualTo("invoice #1 (INV-1)");
  }

  @Test
  void dateNotInFuture_passes_for_today_and_past() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.dateNotInFuture(context(collector, TODAY));
    rules.dateNotInFuture(context(collector, TODAY.minusDays(5)));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void invoiceMonthCompleted_warns_when_month_is_complete() throws Exception {
    given(referenceData.isMonthComplete(TODAY, "100", "00")).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceMonthCompleted(context(collector, TODAY));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.month.completed.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
    assertThat(collector.entries().get(0).invoiceIndex()).isZero();
  }

  @Test
  void invoiceMonthCompleted_passes_when_month_is_not_complete() throws Exception {
    given(referenceData.isMonthComplete(TODAY, "100", "00")).willReturn(false);
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceMonthCompleted(context(collector, TODAY));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext context(ValidationCollector collector, LocalDate date) throws Exception {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceDate(xmlDate(date));
    return new InvoiceRuleContext(
        new CSPSubmissionType(), invoice, 0, submitterInfo("100", "00"), referenceData, collector);
  }

  private static SubmitterInfo submitterInfo(String clientNumber, String locnCode) {
    return new SubmitterInfo(
        SubmitterInfo.SubmittedBy.SELLER,
        clientNumber, locnCode,
        null, null, null, null, null,
        clientNumber, locnCode);
  }

  private static XMLGregorianCalendar xmlDate(LocalDate d) throws Exception {
    return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
        d.getYear(), d.getMonthValue(), d.getDayOfMonth(), DatatypeConstants.FIELD_UNDEFINED);
  }
}
