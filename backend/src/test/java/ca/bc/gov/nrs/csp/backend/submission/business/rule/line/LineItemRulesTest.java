package ca.bc.gov.nrs.csp.backend.submission.business.rule.line;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.LineItemRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.submission.shared.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Template for testing a line rule: call the rule's method directly and assert
 * severity. Reference-data rules (L1, L2) mock {@link ReferenceDataService} and
 * key off a fixed invoice date; value rules (L5–L9) are relaxed for type ADJ.
 */
@ExtendWith(MockitoExtension.class)
class LineItemRulesTest {

  private static final LocalDate INVOICE_DATE = LocalDate.of(2024, Month.JUNE, 15);

  @Mock
  ReferenceDataService referenceData;

  private final LineItemRules rules = new LineItemRules();

  // --- L1 secondary sort code -------------------------------------------------

  @Test
  void secondarySortCode_errors_when_not_active_on_invoice_date() throws Exception {
    given(referenceData.sortCodeValidOn("SC", INVOICE_DATE)).willReturn(false);
    ValidationCollector collector = new ValidationCollector();

    rules.secondarySortCodeValid(lineContext(collector, line -> line.setSecondarySortCode("SC")));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.secondry.sortcode.invalid.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void secondarySortCode_passes_when_active_on_invoice_date() throws Exception {
    given(referenceData.sortCodeValidOn("SC", INVOICE_DATE)).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.secondarySortCodeValid(lineContext(collector, line -> line.setSecondarySortCode("SC")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void secondarySortCode_errors_when_blank() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.secondarySortCodeValid(lineContext(collector, line -> line.setSecondarySortCode("")));

    assertThat(collector.entries()).hasSize(1);
  }

  // --- L2 species + grade combination -----------------------------------------

  @Test
  void speciesGrade_errors_when_combination_missing() throws Exception {
    given(referenceData.speciesGradeCombinationExists("FIR", "1")).willReturn(false);
    ValidationCollector collector = new ValidationCollector();

    rules.speciesGradeCombinationValid(lineContext(collector, line -> {
      line.setSpecies("FIR");
      line.setGrade("1");
    }));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.species.grade.combination.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void speciesGrade_passes_when_combination_exists() throws Exception {
    given(referenceData.speciesGradeCombinationExists("FIR", "1")).willReturn(true);
    ValidationCollector collector = new ValidationCollector();

    rules.speciesGradeCombinationValid(lineContext(collector, line -> {
      line.setSpecies("FIR");
      line.setGrade("1");
    }));

    assertThat(collector.entries()).isEmpty();
  }

  // --- L3 grade required ------------------------------------------------------

  @Test
  void grade_errors_when_null() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.gradeRequired(lineContext(collector, line -> line.setGrade(null)));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.grade.invalid.required.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void grade_passes_when_present() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.gradeRequired(lineContext(collector, line -> line.setGrade("1")));

    assertThat(collector.entries()).isEmpty();
  }

  // --- L4 grade Z warning -----------------------------------------------------

  @Test
  void gradeZWarning_warns_on_grade_z() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.gradeZWarning(lineContext(collector, line -> line.setGrade("Z")));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.grade.z.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void gradeZWarning_silent_for_other_grades() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.gradeZWarning(lineContext(collector, line -> line.setGrade("A")));

    assertThat(collector.entries()).isEmpty();
  }

  // --- L5 number of pieces ----------------------------------------------------

  @Test
  void numberOfPieces_errors_when_zero_or_negative() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.numberOfPiecesPositive(lineContext(collector, "SAL", line -> line.setNumberOfPieces(0)));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.numberof.pieces.negative.or.zero.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void numberOfPieces_passes_when_positive() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.numberOfPiecesPositive(lineContext(collector, "SAL", line -> line.setNumberOfPieces(3)));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void numberOfPieces_relaxed_for_adjustment() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.numberOfPiecesPositive(lineContext(collector, "ADJ", line -> line.setNumberOfPieces(0)));

    assertThat(collector.entries()).isEmpty();
  }

  // --- L6 / L7 volume ---------------------------------------------------------

  @Test
  void volume_errors_when_negative() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.volumeNotNegative(lineContext(collector, "SAL", line -> line.setVolume(new BigDecimal("-1"))));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.volume.negative.value.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void volume_negative_relaxed_for_adjustment() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.volumeNotNegative(lineContext(collector, "ADJ", line -> line.setVolume(new BigDecimal("-1"))));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void volume_warns_when_zero() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.volumeZeroWarning(lineContext(collector, "SAL", line -> line.setVolume(BigDecimal.ZERO)));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.volume.zero.value.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void volume_zero_relaxed_for_adjustment() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.volumeZeroWarning(lineContext(collector, "ADJ", line -> line.setVolume(BigDecimal.ZERO)));

    assertThat(collector.entries()).isEmpty();
  }

  // --- L8 / L9 price ----------------------------------------------------------

  @Test
  void price_errors_when_negative() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.priceNotNegative(lineContext(collector, "SAL", line -> line.setPrice(new BigDecimal("-1"))));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.price.negative.value.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void price_negative_relaxed_for_adjustment() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.priceNotNegative(lineContext(collector, "ADJ", line -> line.setPrice(new BigDecimal("-1"))));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void price_warns_when_zero() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.priceZeroWarning(lineContext(collector, "SAL", line -> line.setPrice(BigDecimal.ZERO)));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.price.zero.value.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void price_zero_relaxed_for_adjustment() throws Exception {
    ValidationCollector collector = new ValidationCollector();

    rules.priceZeroWarning(lineContext(collector, "ADJ", line -> line.setPrice(BigDecimal.ZERO)));

    assertThat(collector.entries()).isEmpty();
  }

  // --- helpers ----------------------------------------------------------------

  private LineItemRuleContext lineContext(ValidationCollector collector, LineCustomizer customizer)
      throws Exception {
    return lineContext(collector, "SAL", customizer);
  }

  private LineItemRuleContext lineContext(
      ValidationCollector collector, String invoiceType, LineCustomizer customizer) throws Exception {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType(invoiceType);
    invoice.setInvoiceDate(xmlDate(INVOICE_DATE));

    CSPLineItemType line = new CSPLineItemType();
    customizer.apply(line);

    return new LineItemRuleContext(invoice, line, 0, 1, null, referenceData, collector);
  }

  private static XMLGregorianCalendar xmlDate(LocalDate d) throws Exception {
    return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
        d.getYear(), d.getMonthValue(), d.getDayOfMonth(), DatatypeConstants.FIELD_UNDEFINED);
  }

  @FunctionalInterface
  private interface LineCustomizer {
    void apply(CSPLineItemType line);
  }
}
