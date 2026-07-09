package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.shared.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Template for testing an invoice totals rule (§2.5): call the rule's method
 * directly and assert on the collector. These rules read only the invoice's own
 * fields, so no reference-data mock is needed.
 */
class InvoiceTotalsRulesTest {

  private final InvoiceTotalsRules rules = new InvoiceTotalsRules();

  // --- validate: runs every totals rule ---

  @Test
  void validate_runs_all_rules_and_passes_for_consistent_totals() {
    ValidationCollector collector = new ValidationCollector();
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setTotalAmount(new BigDecimal("100.00"));
    details.setTotalVolume(new BigDecimal("10.000"));
    details.setTotalPieces(3);

    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType("SAL");
    invoice.setCSPInvoiceDetails(details);
    // 10.000 × 10 = 100.00 amount, 10.000 volume and 3 pieces all match the submitted totals.
    CSPLineItemType line = line("10.000", "10");
    line.setNumberOfPieces(3);
    invoice.getCSPLineItem().add(line);

    rules.validate(new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_errors_when_negative() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalAmountNotNegative(context(collector, "SAL", new BigDecimal("-0.01")));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.totalamount.negative.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void totalAmount_passes_when_positive() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalAmountNotNegative(context(collector, "SAL", new BigDecimal("100.00")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_passes_when_zero() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalAmountNotNegative(context(collector, "SAL", BigDecimal.ZERO));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_is_exempt_for_ADJ_when_negative() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalAmountNotNegative(context(collector, "ADJ", new BigDecimal("-100.00")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_is_skipped_when_absent() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalAmountNotNegative(context(collector, "SAL", null));

    assertThat(collector.entries()).isEmpty();
  }

  // --- I25: submitted total amount within ±$5.00 of calculated ---

  @Test
  void totalAmount_variance_passes_when_matching_calculated() {
    ValidationCollector collector = new ValidationCollector();
    // Σ(10 × 10) = 100.00 calculated, submitted matches exactly.
    rules.totalAmountWithinVariance(
        context(collector, "SAL", new BigDecimal("100.00"), line("10", "10")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_variance_passes_within_five_dollars() {
    ValidationCollector collector = new ValidationCollector();
    // calculated 100.00, submitted 104.99 → diff 4.99 ≤ 5.00.
    rules.totalAmountWithinVariance(
        context(collector, "SAL", new BigDecimal("104.99"), line("10", "10")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_variance_passes_at_exactly_five_dollars() {
    ValidationCollector collector = new ValidationCollector();
    // calculated 100.00, submitted 95.00 → diff exactly 5.00 → still within tolerance.
    rules.totalAmountWithinVariance(
        context(collector, "SAL", new BigDecimal("95.00"), line("10", "10")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_variance_warns_when_outside_five_dollars() {
    ValidationCollector collector = new ValidationCollector();
    // calculated 100.00, submitted 90.00 → diff 10.00 > 5.00.
    rules.totalAmountWithinVariance(
        context(collector, "SAL", new BigDecimal("90.00"), line("10", "10")));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.totalamount.dismatch.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void totalAmount_variance_sums_multiple_line_items() {
    ValidationCollector collector = new ValidationCollector();
    // (10 × 10) + (2 × 5) = 110.00 calculated, submitted matches.
    rules.totalAmountWithinVariance(
        context(collector, "SAL", new BigDecimal("110.00"), line("10", "10"), line("2", "5")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_variance_uses_sign_preserving_calc_for_ADJ() {
    ValidationCollector collector = new ValidationCollector();
    // ADJ: (-10 × -10) is kept negative → calculated -100.00, submitted matches.
    rules.totalAmountWithinVariance(
        context(collector, "ADJ", new BigDecimal("-100.00"), line("-10", "-10")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_variance_warns_for_ADJ_when_sign_ignored() {
    ValidationCollector collector = new ValidationCollector();
    // ADJ calculated is -100.00; a submitted +100.00 is 200 off → warns.
    rules.totalAmountWithinVariance(
        context(collector, "ADJ", new BigDecimal("100.00"), line("-10", "-10")));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.totalamount.dismatch.warning");
  }

  @Test
  void totalAmount_variance_keeps_negative_volume_positive_price_negative_for_ADJ() {
    ValidationCollector collector = new ValidationCollector();
    // ADJ: only a both-negative product needs the sign fix; -10 × 10 = -100.00 already negative.
    rules.totalAmountWithinVariance(
        context(collector, "ADJ", new BigDecimal("-100.00"), line("-10", "10")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_variance_leaves_positive_lines_untouched_for_ADJ() {
    ValidationCollector collector = new ValidationCollector();
    // ADJ with a non-negative volume: no sign adjustment, 10 × 10 = 100.00.
    rules.totalAmountWithinVariance(
        context(collector, "ADJ", new BigDecimal("100.00"), line("10", "10")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_variance_ignores_lines_missing_volume_or_price() {
    ValidationCollector collector = new ValidationCollector();
    CSPLineItemType volumeOnly = new CSPLineItemType();
    volumeOnly.setVolume(new BigDecimal("999"));
    CSPLineItemType priceOnly = new CSPLineItemType();
    priceOnly.setPrice(new BigDecimal("999"));
    // Only the complete line contributes: 10 × 10 = 100.00 calculated.
    rules.totalAmountWithinVariance(
        context(collector, "SAL", new BigDecimal("100.00"), line("10", "10"), volumeOnly, priceOnly));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalAmount_variance_is_skipped_when_submitted_absent() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalAmountWithinVariance(context(collector, "SAL", null, line("10", "10")));

    assertThat(collector.entries()).isEmpty();
  }

  // --- I26: total volume cannot be negative (except ADJ) ---

  @Test
  void totalVolume_errors_when_negative() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalVolumeNotNegative(volumeContext(collector, "SAL", new BigDecimal("-0.001")));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.totalvolume.negative.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void totalVolume_passes_when_positive() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalVolumeNotNegative(volumeContext(collector, "SAL", new BigDecimal("50.000")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalVolume_passes_when_zero() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalVolumeNotNegative(volumeContext(collector, "SAL", BigDecimal.ZERO));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalVolume_is_exempt_for_ADJ_when_negative() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalVolumeNotNegative(volumeContext(collector, "ADJ", new BigDecimal("-50.000")));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalVolume_is_skipped_when_absent() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalVolumeNotNegative(volumeContext(collector, "SAL", null));

    assertThat(collector.entries()).isEmpty();
  }

  // --- I27: submitted total volume within ±5.00 of calculated ---

  // Calculated volume = Σ(10.000, 15.000) = 25.000; each submitted value is within ±5.00.
  @ParameterizedTest(name = "passes when submitted={0} ({1})")
  @CsvSource({
      "25.000, exact match",
      "29.999, within +5",
      "20.000, exactly -5 boundary",
  })
  void totalVolume_variance_passes_within_tolerance(String submitted, String scenario) {
    ValidationCollector collector = new ValidationCollector();

    rules.totalVolumeWithinVariance(
        volumeVarianceContext(collector, "SAL", new BigDecimal(submitted), "10.000", "15.000"));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalVolume_variance_warns_when_outside_five() {
    ValidationCollector collector = new ValidationCollector();
    // calculated 25.000, submitted 15.000 → diff 10.00 > 5.00.
    rules.totalVolumeWithinVariance(
        volumeVarianceContext(collector, "SAL", new BigDecimal("15.000"), "10.000", "15.000"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.totalvolume.dismatch.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void totalVolume_variance_ignores_lines_missing_volume() {
    ValidationCollector collector = new ValidationCollector();
    // The line without a volume contributes nothing: calculated stays 10.000.
    InvoiceRuleContext ctx =
        volumeVarianceContext(collector, "SAL", new BigDecimal("10.000"), "10.000");
    ctx.invoice().getCSPLineItem().add(new CSPLineItemType());

    rules.totalVolumeWithinVariance(ctx);

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalVolume_variance_is_skipped_when_submitted_absent() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalVolumeWithinVariance(volumeVarianceContext(collector, "SAL", null, "10.000"));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext volumeVarianceContext(ValidationCollector collector, String invoiceType,
      BigDecimal totalVolume, String... lineVolumes) {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setTotalVolume(totalVolume);

    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType(invoiceType);
    invoice.setCSPInvoiceDetails(details);
    for (String volume : lineVolumes) {
      CSPLineItemType line = new CSPLineItemType();
      line.setVolume(new BigDecimal(volume));
      invoice.getCSPLineItem().add(line);
    }

    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector);
  }

  private InvoiceRuleContext volumeContext(ValidationCollector collector, String invoiceType, BigDecimal totalVolume) {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setTotalVolume(totalVolume);

    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType(invoiceType);
    invoice.setCSPInvoiceDetails(details);

    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector);
  }

  // --- I28: total pieces cannot be negative (except ADJ) ---

  @Test
  void totalPieces_errors_when_negative() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalPiecesNotNegative(piecesContext(collector, "SAL", -1));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.totalpieces.negative.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void totalPieces_passes_when_positive() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalPiecesNotNegative(piecesContext(collector, "SAL", 42));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalPieces_passes_when_zero() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalPiecesNotNegative(piecesContext(collector, "SAL", 0));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalPieces_is_exempt_for_ADJ_when_negative() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalPiecesNotNegative(piecesContext(collector, "ADJ", -5));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalPieces_is_skipped_when_absent() {
    ValidationCollector collector = new ValidationCollector();

    rules.totalPiecesNotNegative(piecesContext(collector, "SAL", null));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext piecesContext(ValidationCollector collector, String invoiceType, Integer totalPieces) {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setTotalPieces(totalPieces);

    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType(invoiceType);
    invoice.setCSPInvoiceDetails(details);

    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector);
  }

  // --- I29: submitted total pieces must exactly match calculated ---

  @Test
  void totalPieces_variance_passes_when_matching_calculated() {
    ValidationCollector collector = new ValidationCollector();
    // Σ pieces = 3 + 4 = 7 calculated, submitted matches exactly.
    rules.totalPiecesMatchesCalculated(piecesVarianceContext(collector, "SAL", 7, 3, 4));

    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void totalPieces_variance_warns_when_off_by_one() {
    ValidationCollector collector = new ValidationCollector();
    // calculated 7, submitted 6 → exact-match required, so warns.
    rules.totalPiecesMatchesCalculated(piecesVarianceContext(collector, "SAL", 6, 3, 4));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.totalpieces.dismatch.warning");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void totalPieces_variance_applies_to_ADJ() {
    ValidationCollector collector = new ValidationCollector();
    // Exact-match check runs for all invoice types, including ADJ.
    rules.totalPiecesMatchesCalculated(piecesVarianceContext(collector, "ADJ", 6, 3, 4));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.totalpieces.dismatch.warning");
  }

  @Test
  void totalPieces_variance_defaults_absent_submitted_to_zero() {
    ValidationCollector collector = new ValidationCollector();
    // Absent submitted total defaults to 0; calculated 7 ≠ 0 → warns.
    rules.totalPiecesMatchesCalculated(piecesVarianceContext(collector, "SAL", null, 3, 4));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.totalpieces.dismatch.warning");
  }

  @Test
  void totalPieces_variance_passes_when_absent_submitted_and_no_pieces() {
    ValidationCollector collector = new ValidationCollector();
    // Absent submitted defaults to 0; calculated 0 → matches, no warning.
    rules.totalPiecesMatchesCalculated(piecesVarianceContext(collector, "SAL", null, 0));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext piecesVarianceContext(ValidationCollector collector, String invoiceType,
      Integer totalPieces, int... linePieces) {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setTotalPieces(totalPieces);

    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType(invoiceType);
    invoice.setCSPInvoiceDetails(details);
    for (int pieces : linePieces) {
      CSPLineItemType line = new CSPLineItemType();
      line.setNumberOfPieces(pieces);
      invoice.getCSPLineItem().add(line);
    }

    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector);
  }

  private InvoiceRuleContext context(ValidationCollector collector, String invoiceType, BigDecimal totalAmount,
      CSPLineItemType... lines) {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setTotalAmount(totalAmount);

    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType(invoiceType);
    invoice.setCSPInvoiceDetails(details);
    for (CSPLineItemType line : lines) {
      invoice.getCSPLineItem().add(line);
    }

    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector);
  }

  private static CSPLineItemType line(String volume, String price) {
    CSPLineItemType line = new CSPLineItemType();
    line.setVolume(new BigDecimal(volume));
    line.setPrice(new BigDecimal(price));
    return line;
  }
}
