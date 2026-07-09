package ca.bc.gov.nrs.csp.backend.invoice.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The single authoritative test of the shared totals rules (catalogue §2.5,
 * I24–I29) — the matrix formerly in the electronic {@code InvoiceTotalsRulesTest}
 * now lives here against the pure core (refactor doc §8). Each channel keeps
 * only a thin glue smoke test.
 *
 * <p>{@code validate} runs all six rules, so assertions target the specific
 * message code under test. Null-submitted variance behaviour follows the legacy
 * system of record (refactor doc §7.1 item 3): the check still runs with the
 * absent total treated as zero.
 */
class InvoiceTotalsRuleSetTest {

  private static final String I24 = "invoice.totalamount.negative.error";
  private static final String I25 = "invoice.totalamount.dismatch.warning";
  private static final String I26 = "invoice.totalvolume.negative.error";
  private static final String I27 = "invoice.totalvolume.dismatch.warning";
  private static final String I28 = "invoice.totalpieces.negative.error";
  private static final String I29 = "invoice.totalpieces.dismatch.warning";

  // --- I24: total amount cannot be negative (except ADJ) ---

  @Test
  void totalAmount_errors_when_negative() {
    Optional<Finding> f = finding(amount("SAL", "-0.01"), I24);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.ERROR);
    assertThat(f.get().args()).isEmpty();
  }

  @Test
  void totalAmount_passes_when_positive() {
    assertThat(codes(amount("SAL", "100.00"))).doesNotContain(I24);
  }

  @Test
  void totalAmount_passes_when_zero() {
    assertThat(codes(amount("SAL", "0"))).doesNotContain(I24);
  }

  @Test
  void totalAmount_is_exempt_for_ADJ_when_negative() {
    assertThat(codes(amount("ADJ", "-100.00"))).doesNotContain(I24);
  }

  @Test
  void totalAmount_negative_check_is_skipped_when_absent() {
    assertThat(codes(amount("SAL", null))).doesNotContain(I24);
  }

  // --- I25: submitted total amount within ±$5.00 of calculated ---

  @ParameterizedTest(name = "passes when submitted={0} ({1})")
  @CsvSource({
      "100.00, exact match",
      "104.99, within +5",
      "95.00,  exactly -5 boundary",
  })
  void totalAmount_variance_passes_within_tolerance(String submitted, String scenario) {
    // Σ(10 × 10) = 100.00 calculated.
    assertThat(codes(amount("SAL", submitted, line("10", "10")))).doesNotContain(I25);
  }

  @Test
  void totalAmount_variance_warns_when_outside_five_dollars() {
    // calculated 100.00, submitted 90.00 → diff 10.00 > 5.00.
    Optional<Finding> f = finding(amount("SAL", "90.00", line("10", "10")), I25);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.WARNING);
    assertThat(f.get().args()).containsExactly(new BigDecimal("90.00"));
  }

  @Test
  void totalAmount_variance_sums_multiple_line_items() {
    // (10 × 10) + (2 × 5) = 110.00 calculated, submitted matches.
    assertThat(codes(amount("SAL", "110.00", line("10", "10"), line("2", "5"))))
        .doesNotContain(I25);
  }

  @Test
  void totalAmount_variance_rounds_each_line_product_half_up() {
    // 3.333 × 3 = 9.999 → 10.00 per line, ×2 lines = 20.00 calculated.
    assertThat(codes(amount("SAL", "20.00", line("3.333", "3"), line("3.333", "3"))))
        .doesNotContain(I25);
  }

  @Test
  void totalAmount_variance_uses_sign_preserving_calc_for_ADJ() {
    // ADJ: (-10 × -10) is kept negative → calculated -100.00, submitted matches.
    assertThat(codes(amount("ADJ", "-100.00", line("-10", "-10")))).doesNotContain(I25);
  }

  @Test
  void totalAmount_variance_warns_for_ADJ_when_sign_ignored() {
    // ADJ calculated is -100.00; a submitted +100.00 is 200 off → warns.
    assertThat(codes(amount("ADJ", "100.00", line("-10", "-10")))).contains(I25);
  }

  @Test
  void totalAmount_variance_runs_when_submitted_absent_and_calculated_differs() {
    // Legacy behaviour (doc §7.1 #3): null submitted → 0; calculated 100.00 → warns.
    assertThat(codes(amount("SAL", null, line("10", "10")))).contains(I25);
  }

  @Test
  void totalAmount_variance_passes_when_submitted_absent_and_nothing_calculated() {
    assertThat(codes(amount("SAL", null))).doesNotContain(I25);
  }

  @Test
  void totalAmount_variance_skips_lines_missing_volume_or_price() {
    // The null-price line contributes nothing: calculated stays 100.00.
    assertThat(codes(amount("SAL", "100.00", line("10", "10"), line("5", null))))
        .doesNotContain(I25);
  }

  // --- I26: total volume cannot be negative (except ADJ) ---

  @Test
  void totalVolume_errors_when_negative() {
    Optional<Finding> f = finding(volume("SAL", "-0.001"), I26);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.ERROR);
    assertThat(f.get().args()).isEmpty();
  }

  @Test
  void totalVolume_passes_when_positive() {
    assertThat(codes(volume("SAL", "50.000"))).doesNotContain(I26);
  }

  @Test
  void totalVolume_passes_when_zero() {
    assertThat(codes(volume("SAL", "0"))).doesNotContain(I26);
  }

  @Test
  void totalVolume_is_exempt_for_ADJ_when_negative() {
    assertThat(codes(volume("ADJ", "-50.000"))).doesNotContain(I26);
  }

  @Test
  void totalVolume_negative_check_is_skipped_when_absent() {
    assertThat(codes(volume("SAL", null))).doesNotContain(I26);
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
    assertThat(codes(volume("SAL", submitted, "10.000", "15.000"))).doesNotContain(I27);
  }

  @Test
  void totalVolume_variance_warns_when_outside_five() {
    // calculated 25.000, submitted 15.000 → diff 10.00 > 5.00.
    Optional<Finding> f = finding(volume("SAL", "15.000", "10.000", "15.000"), I27);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.WARNING);
    assertThat(f.get().args()).containsExactly(new BigDecimal("15.000"));
  }

  @Test
  void totalVolume_variance_runs_when_submitted_absent_and_calculated_differs() {
    // Legacy behaviour (doc §7.1 #3): null submitted → 0; calculated 10.000 → warns.
    assertThat(codes(volume("SAL", null, "10.000"))).contains(I27);
  }

  @Test
  void totalVolume_variance_passes_when_submitted_absent_and_nothing_calculated() {
    assertThat(codes(volume("SAL", null))).doesNotContain(I27);
  }

  // --- I28: total pieces cannot be negative (except ADJ) ---

  @Test
  void totalPieces_errors_when_negative() {
    Optional<Finding> f = finding(pieces("SAL", -1), I28);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.ERROR);
    assertThat(f.get().args()).isEmpty();
  }

  @Test
  void totalPieces_passes_when_positive() {
    assertThat(codes(pieces("SAL", 42))).doesNotContain(I28);
  }

  @Test
  void totalPieces_passes_when_zero() {
    assertThat(codes(pieces("SAL", 0))).doesNotContain(I28);
  }

  @Test
  void totalPieces_is_exempt_for_ADJ_when_negative() {
    assertThat(codes(pieces("ADJ", -5))).doesNotContain(I28);
  }

  @Test
  void totalPieces_negative_check_is_skipped_when_absent() {
    assertThat(codes(pieces("SAL", null))).doesNotContain(I28);
  }

  // --- I29: submitted total pieces must exactly match calculated ---

  @Test
  void totalPieces_variance_passes_when_matching_calculated() {
    // Σ pieces = 3 + 4 = 7 calculated, submitted matches exactly.
    assertThat(codes(pieces("SAL", 7, 3, 4))).doesNotContain(I29);
  }

  @Test
  void totalPieces_variance_warns_when_off_by_one() {
    // calculated 7, submitted 6 → exact-match required, so warns.
    Optional<Finding> f = finding(pieces("SAL", 6, 3, 4), I29);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.WARNING);
    assertThat(f.get().args()).containsExactly(6);
  }

  @Test
  void totalPieces_variance_applies_to_ADJ() {
    // Exact-match check runs for all invoice types, including ADJ.
    assertThat(codes(pieces("ADJ", 6, 3, 4))).contains(I29);
  }

  @Test
  void totalPieces_variance_defaults_absent_submitted_to_zero() {
    // Absent submitted total defaults to 0; calculated 7 ≠ 0 → warns.
    assertThat(codes(pieces("SAL", null, 3, 4))).contains(I29);
  }

  @Test
  void totalPieces_variance_passes_when_absent_submitted_and_no_pieces() {
    // Absent submitted defaults to 0; calculated 0 → matches, no warning.
    assertThat(codes(pieces("SAL", null, 0))).doesNotContain(I29);
  }

  // --- aggregate ---

  @Test
  void validate_reports_nothing_for_a_fully_consistent_invoice() {
    InvoiceTotals t = new InvoiceTotals("SAL",
        new BigDecimal("100.00"), new BigDecimal("10"), 3,
        List.of(new InvoiceTotals.Line(new BigDecimal("10"), new BigDecimal("10"), 3)));

    assertThat(InvoiceTotalsRuleSet.validate(t)).isEmpty();
  }

  @Test
  void validate_collects_multiple_findings_in_catalogue_order() {
    // Negative amount (I24) + amount off calculated (I25) + pieces mismatch (I29).
    InvoiceTotals t = new InvoiceTotals("SAL",
        new BigDecimal("-90.00"), new BigDecimal("10"), 5,
        List.of(new InvoiceTotals.Line(new BigDecimal("10"), new BigDecimal("10"), 3)));

    assertThat(InvoiceTotalsRuleSet.validate(t))
        .extracting(Finding::code)
        .containsExactly(I24, I25, I29);
  }

  // --- helpers ---

  private static List<String> codes(InvoiceTotals t) {
    return InvoiceTotalsRuleSet.validate(t).stream().map(Finding::code).toList();
  }

  private static Optional<Finding> finding(InvoiceTotals t, String code) {
    return InvoiceTotalsRuleSet.validate(t).stream()
        .filter(f -> code.equals(f.code()))
        .findFirst();
  }

  private static InvoiceTotals amount(String invoiceType, String submittedAmount,
      InvoiceTotals.Line... lines) {
    return new InvoiceTotals(invoiceType,
        submittedAmount == null ? null : new BigDecimal(submittedAmount),
        null, null, List.of(lines));
  }

  private static InvoiceTotals volume(String invoiceType, String submittedVolume,
      String... lineVolumes) {
    List<InvoiceTotals.Line> lines = java.util.Arrays.stream(lineVolumes)
        .map(v -> new InvoiceTotals.Line(new BigDecimal(v), null, 0))
        .toList();
    return new InvoiceTotals(invoiceType, null,
        submittedVolume == null ? null : new BigDecimal(submittedVolume),
        null, lines);
  }

  private static InvoiceTotals pieces(String invoiceType, Integer submittedPieces,
      int... linePieces) {
    List<InvoiceTotals.Line> lines = java.util.Arrays.stream(linePieces)
        .mapToObj(p -> new InvoiceTotals.Line(null, null, p))
        .toList();
    return new InvoiceTotals(invoiceType, null, null, submittedPieces, lines);
  }

  private static InvoiceTotals.Line line(String volume, String price) {
    return new InvoiceTotals.Line(
        volume == null ? null : new BigDecimal(volume),
        price == null ? null : new BigDecimal(price),
        0);
  }
}
