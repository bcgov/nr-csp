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
 * Unit tests for the §2.1 invoice type rules (I1–I3). {@link ReferenceDataService}
 * is mocked so the type-active-on-date check is exercised without a database.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceTypeRulesTest {

  private static final LocalDate DATE = LocalDate.of(2024, Month.JUNE, 15);

  @Mock
  ReferenceDataService referenceData;

  private final InvoiceTypeRules rules = new InvoiceTypeRules();

  @Test
  void validate_recognisedSaleBySeller_recordsNothing() throws Exception {
    given(referenceData.invoiceTypeValidOn("SAL", DATE)).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, "SAL", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void validate_unrecognisedType_reportsOnlyI1_notAlsoI2() throws Exception {
    given(referenceData.invoiceTypeValidOn("XXX", DATE)).willReturn(false);
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, "XXX", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.type.invalid.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
    assertThat(collector.entries().get(0).invoiceIndex()).isZero();
  }

  @Test
  void invoiceTypeValidOn_blankType_errors_withoutConsultingReferenceData() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    boolean valid = rules.invoiceTypeValidOn(context(collector, " ", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(valid).isFalse();
    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.type.invalid.error");
    verifyNoInteractions(referenceData);
  }

  @Test
  void saleOrPurchase_warns_for_other_recognised_type() throws Exception {
    given(referenceData.invoiceTypeValidOn("ADJ", DATE)).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, "ADJ", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.type.not.saleorpurchase.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void submitterVsType_errors_when_seller_submits_purchase() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.submitterVsType(context(collector, "PUR", SubmitterInfo.SubmittedBy.SELLER));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.type.invalid.submitter");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void submitterVsType_errors_when_buyer_submits_sale() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.submitterVsType(context(collector, "SAL", SubmitterInfo.SubmittedBy.BUYER));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.type.invalid.submitter");
  }

  @Test
  void submitterVsType_passes_matching_submitter_and_type() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.submitterVsType(context(collector, "SAL", SubmitterInfo.SubmittedBy.SELLER));
    rules.submitterVsType(context(collector, "PUR", SubmitterInfo.SubmittedBy.BUYER));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext context(
      ValidationCollector collector, String type, SubmitterInfo.SubmittedBy submittedBy)
      throws Exception {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType(type);
    invoice.setInvoiceDate(xmlDate(DATE));
    return new InvoiceRuleContext(
        new CSPSubmissionType(), invoice, 0, submitterInfo(submittedBy), referenceData, collector);
  }

  private static SubmitterInfo submitterInfo(SubmitterInfo.SubmittedBy submittedBy) {
    return new SubmitterInfo(
        submittedBy,
        "100", "00",
        null, null, null, null, null,
        "100", "00");
  }

  private static XMLGregorianCalendar xmlDate(LocalDate d) throws Exception {
    return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
        d.getYear(), d.getMonthValue(), d.getDayOfMonth(), DatatypeConstants.FIELD_UNDEFINED);
  }
}
