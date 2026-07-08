package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterInfo;
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
 * Template for testing invoice type rules: call the rule's method directly,
 * mock {@link ReferenceDataService} where needed, and assert on the collector.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceTypeRulesTest {

  private static final LocalDate INVOICE_DATE = LocalDate.of(2024, Month.JUNE, 15);

  @Mock
  ReferenceDataService referenceData;

  private final InvoiceTypeRules rules = new InvoiceTypeRules();

  @Test
  void invoiceTypeValidOn_errors_when_not_active_on_invoice_date() throws Exception {
    given(referenceData.invoiceTypeValidOn("SAL", INVOICE_DATE)).willReturn(false);
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceTypeValidOn(context(collector, "SAL", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.type.invalid.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void invoiceTypeValidOn_passes_when_active_on_invoice_date() throws Exception {
    given(referenceData.invoiceTypeValidOn("SAL", INVOICE_DATE)).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceTypeValidOn(context(collector, "SAL", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void invoiceTypeValidOn_errors_on_blank_without_hitting_the_db() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceTypeValidOn(context(collector, "", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).hasSize(1);
    verifyNoInteractions(referenceData);
  }

  @Test
  void saleOrPurchase_warns_when_type_is_neither_sale_nor_purchase() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.saleOrPurchase(context(collector, "ADJ", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.type.not.saleorpurchase.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void saleOrPurchase_passes_for_sale_or_purchase() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.saleOrPurchase(context(collector, "SAL", SubmitterInfo.SubmittedBy.SELLER));
    rules.saleOrPurchase(context(collector, "PUR", SubmitterInfo.SubmittedBy.BUYER));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void validate_skips_saleOrPurchase_warning_when_type_is_invalid() throws Exception {
    // Legacy InvoiceValidator.isValid only calls checkInvoiceTypeForSalesOrPurchase
    // when checkInvoiceType succeeds — an unrecognized type must report only I1.
    given(referenceData.invoiceTypeValidOn("XYZ", INVOICE_DATE)).willReturn(false);
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, "XYZ", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.type.invalid.error");
  }

  @Test
  void validate_runs_saleOrPurchase_warning_when_type_is_valid_but_not_sale_or_purchase() throws Exception {
    given(referenceData.invoiceTypeValidOn("ADJ", INVOICE_DATE)).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, "ADJ", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.type.not.saleorpurchase.warning");
  }

  @Test
  void submitterVsType_errors_when_seller_submission_is_purchase() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.submitterVsType(context(collector, "PUR", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.type.invalid.submitter");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void submitterVsType_errors_when_buyer_submission_is_sale() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.submitterVsType(context(collector, "SAL", SubmitterInfo.SubmittedBy.BUYER));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.type.invalid.submitter");
  }

  @Test
  void submitterVsType_passes_for_matching_combinations() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.submitterVsType(context(collector, "SAL", SubmitterInfo.SubmittedBy.SELLER));
    rules.submitterVsType(context(collector, "PUR", SubmitterInfo.SubmittedBy.BUYER));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext context(
      ValidationCollector collector, String invoiceType, SubmitterInfo.SubmittedBy submittedBy)
      throws Exception {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType(invoiceType);
    invoice.setInvoiceDate(xmlDate(INVOICE_DATE));

    SubmitterInfo submitter = new SubmitterInfo(
        submittedBy, "100", "00", null, null, null, null, null, "100", "00");

    return new InvoiceRuleContext(
        new CSPSubmissionType(), invoice, 0, submitter, referenceData, collector);
  }

  private static XMLGregorianCalendar xmlDate(LocalDate d) throws Exception {
    return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
        d.getYear(), d.getMonthValue(), d.getDayOfMonth(), DatatypeConstants.FIELD_UNDEFINED);
  }
}
