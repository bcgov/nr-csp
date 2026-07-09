package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.shared.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Template for testing an invoice reference-data code rule: call the rule's
 * method directly, mock {@link ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService},
 * and assert on the collector. The lookups are date-effective, so a fixed
 * invoice date is used as the lookup key.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceCodeRulesTest {

  private static final LocalDate INVOICE_DATE = LocalDate.of(2024, Month.JUNE, 15);

  @Mock
  ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService referenceData;

  private final InvoiceCodeRules rules = new InvoiceCodeRules();

  @Test
  void validate_runs_all_rules_and_passes_for_valid_codes() throws Exception {
    given(referenceData.maturityValidOn("O", INVOICE_DATE)).willReturn(true);
    given(referenceData.sortCodeValidOn("X1", INVOICE_DATE)).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, "O", "X1"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void maturity_errors_when_not_active_on_invoice_date() throws Exception {
    given(referenceData.maturityValidOn("O", INVOICE_DATE)).willReturn(false);
    ValidationCollector collector = new ValidationCollector();

    rules.maturityValid(context(collector, "O", null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.maturity.invalid.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void maturity_passes_when_active_on_invoice_date() throws Exception {
    given(referenceData.maturityValidOn("O", INVOICE_DATE)).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.maturityValid(context(collector, "O", null));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void maturity_errors_on_blank_without_hitting_the_db() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.maturityValid(context(collector, "", null));

    assertThat(collector.entries()).hasSize(1);
    verifyNoInteractions(referenceData);
  }

  @Test
  void maturity_errors_on_null_without_hitting_the_db() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.maturityValid(context(collector, null, null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.maturity.invalid.error");
    verifyNoInteractions(referenceData);
  }

  @Test
  void primarySortCode_errors_when_supplied_and_not_active_on_invoice_date() throws Exception {
    given(referenceData.sortCodeValidOn("X1", INVOICE_DATE)).willReturn(false);
    ValidationCollector collector = new ValidationCollector();

    rules.primarySortCodeValid(context(collector, "O", "X1"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.primary.sortcode.invalid.error");
  }

  @Test
  void primarySortCode_passes_when_supplied_and_active_on_invoice_date() throws Exception {
    given(referenceData.sortCodeValidOn("X1", INVOICE_DATE)).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.primarySortCodeValid(context(collector, "O", "X1"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void primarySortCode_is_skipped_when_blank() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.primarySortCodeValid(context(collector, "O", ""));

    assertThat(collector.entries()).isEmpty();
    verifyNoInteractions(referenceData);
  }

  private InvoiceRuleContext context(ValidationCollector collector, String maturity, String primarySortCode)
      throws Exception {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setMaturity(maturity);
    details.setPrimarySortCode(primarySortCode);

    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceDate(xmlDate(INVOICE_DATE));
    invoice.setCSPInvoiceDetails(details);

    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, referenceData, collector);
  }

  private static XMLGregorianCalendar xmlDate(LocalDate d) throws Exception {
    return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
        d.getYear(), d.getMonthValue(), d.getDayOfMonth(), DatatypeConstants.FIELD_UNDEFINED);
  }
}
