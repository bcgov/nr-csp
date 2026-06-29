package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Template for testing an invoice rule: call the rule's method directly, build a
 * context, assert on the collector. {@code dateNotInFuture} (I39) needs no mocks.
 */
class InvoiceDateRulesTest {

  private final InvoiceDateRules rules = new InvoiceDateRules();

  @Test
  void dateNotInFuture_errors_when_in_the_future() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.dateNotInFuture(context(collector, LocalDate.now().plusDays(1)));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.date.in.future.error");
    assertThat(collector.entries().get(0).invoiceNumber()).isEqualTo("INV-1");
  }

  @Test
  void dateNotInFuture_passes_for_today_and_past() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.dateNotInFuture(context(collector, LocalDate.now()));
    rules.dateNotInFuture(context(collector, LocalDate.now().minusDays(5)));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext context(ValidationCollector collector, LocalDate date) throws Exception {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceDate(xmlDate(date));
    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, null, null, collector);
  }

  private static XMLGregorianCalendar xmlDate(LocalDate d) throws Exception {
    return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
        d.getYear(), d.getMonthValue(), d.getDayOfMonth(), DatatypeConstants.FIELD_UNDEFINED);
  }
}
