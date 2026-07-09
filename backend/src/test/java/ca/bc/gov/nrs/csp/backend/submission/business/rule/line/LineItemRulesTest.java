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

  // --- L3–L9 delegation smoke tests (refactor doc §8) --------------------------
  // The exhaustive value-rule matrix lives in the core InvoiceLineRuleSetTest.
  // Here we only prove the adapter: the JAXB line maps onto InvoiceLine, findings
  // surface through LineItemRuleContext, and the core severity maps onto the
  // collector's ERROR / WARNING sinks with the message key + template args.

  @Test
  void valueRule_error_surfaces_through_the_collector_with_code_and_args() throws Exception {
    stubReferenceDataToPass(null); // L2 sees the null grade — stub it to pass too
    ValidationCollector collector = new ValidationCollector();

    // Grade missing → L3 ERROR; every other value is rule-clean.
    rules.validate(lineContext(collector, "SAL", line -> {
      cleanLine(line);
      line.setGrade(null);
    }));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.grade.invalid.required.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
    assertThat(collector.entries().get(0).error().args()).containsExactly("Line 1");
    assertThat(collector.entries().get(0).error().message()).isNull();
  }

  @Test
  void valueRule_warning_surfaces_with_the_line_label_as_template_arg() throws Exception {
    stubReferenceDataToPass("1");
    ValidationCollector collector = new ValidationCollector();

    // Zero price → L9 WARNING.
    rules.validate(lineContext(collector, "SAL", line -> {
      cleanLine(line);
      line.setPrice(BigDecimal.ZERO);
    }));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code())
        .isEqualTo("invoice.price.zero.value.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
    assertThat(collector.entries().get(0).error().args()).containsExactly("Line 1");
  }

  @Test
  void adjustment_relaxation_passes_through_the_adapter() throws Exception {
    stubReferenceDataToPass("1");
    ValidationCollector collector = new ValidationCollector();

    // ADJ relaxes the value rules: negative volume/price and zero pieces stay quiet.
    rules.validate(lineContext(collector, "ADJ", line -> {
      cleanLine(line);
      line.setNumberOfPieces(0);
      line.setVolume(new BigDecimal("-1"));
      line.setPrice(new BigDecimal("-1"));
    }));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void clean_line_produces_no_findings() throws Exception {
    stubReferenceDataToPass("1");
    ValidationCollector collector = new ValidationCollector();

    rules.validate(lineContext(collector, "SAL", LineItemRulesTest::cleanLine));

    assertThat(collector.entries()).isEmpty();
  }

  /** L1/L2 pass (for the given grade) so the smoke tests isolate the L3–L9 delegation. */
  private void stubReferenceDataToPass(String grade) {
    given(referenceData.sortCodeValidOn("SC", INVOICE_DATE)).willReturn(true);
    given(referenceData.speciesGradeCombinationExists("FIR", grade)).willReturn(true);
  }

  /** A line that passes every rule: perturb one field per test. */
  private static void cleanLine(CSPLineItemType line) {
    line.setSecondarySortCode("SC");
    line.setSpecies("FIR");
    line.setGrade("1");
    line.setNumberOfPieces(3);
    line.setVolume(new BigDecimal("10"));
    line.setPrice(new BigDecimal("5"));
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
