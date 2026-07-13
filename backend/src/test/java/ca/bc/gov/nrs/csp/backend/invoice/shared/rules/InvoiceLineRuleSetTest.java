package ca.bc.gov.nrs.csp.backend.invoice.shared.rules;

import ca.bc.gov.nrs.csp.backend.invoice.shared.model.Finding;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.InvoiceLine;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The single authoritative test of the shared line-item value rules (catalogue
 * §3, L3–L9) — the matrix formerly in the electronic {@code LineItemRulesTest}
 * now lives here against the pure core (refactor doc §8). L1–L2 (reference
 * data) stay channel-side. Null pieces/volume/price are ERRORs per the decided
 * §7.2 semantics; the value rules (L5–L9) are relaxed for ADJ.
 *
 * <p>{@code validate} runs all seven rules, so assertions target the specific
 * message code under test. The fixture defaults are rule-clean (grade "1",
 * pieces 3, volume 10, price 5) and each test perturbs one field.
 */
class InvoiceLineRuleSetTest {

  private static final String L3 = "invoice.grade.invalid.required.error";
  private static final String L4 = "invoice.grade.z.warning";
  private static final String L5 = "invoice.numberof.pieces.negative.or.zero.error";
  private static final String L6 = "invoice.volume.negative.value.error";
  private static final String L7 = "invoice.volume.zero.value.warning";
  private static final String L8 = "invoice.price.negative.value.error";
  private static final String L9 = "invoice.price.zero.value.warning";

  private static final String LABEL = "Line 1";

  // --- L3 grade required ---

  @Test
  void grade_errors_when_null() {
    Optional<Finding> f = finding(line("SAL", null, 3, "10", "5"), L3);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.ERROR);
    assertThat(f.get().args()).containsExactly(LABEL);
  }

  @Test
  void grade_passes_when_present() {
    assertThat(codes(line("SAL", "1", 3, "10", "5"))).doesNotContain(L3);
  }

  // --- L4 grade Z warning ---

  @Test
  void gradeZ_warns_on_grade_z() {
    Optional<Finding> f = finding(line("SAL", "Z", 3, "10", "5"), L4);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.WARNING);
    assertThat(f.get().args()).containsExactly(LABEL);
  }

  @Test
  void gradeZ_silent_for_other_grades() {
    assertThat(codes(line("SAL", "A", 3, "10", "5"))).doesNotContain(L4);
  }

  @Test
  void gradeZ_warns_even_for_adjustment() {
    // L3/L4 are not relaxed for ADJ — only the value rules are.
    assertThat(codes(line("ADJ", "Z", 3, "10", "5"))).contains(L4);
  }

  // --- L5 number of pieces ---

  @ParameterizedTest(name = "errors when pieces={0}")
  @CsvSource(value = {"0", "-1", "null"}, nullValues = "null")
  void pieces_errors_when_zero_negative_or_null(Integer pieces) {
    Optional<Finding> f = finding(line("SAL", "1", pieces, "10", "5"), L5);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.ERROR);
    assertThat(f.get().args()).containsExactly(LABEL);
  }

  @Test
  void pieces_passes_when_positive() {
    assertThat(codes(line("SAL", "1", 3, "10", "5"))).doesNotContain(L5);
  }

  @Test
  void pieces_relaxed_for_adjustment() {
    assertThat(codes(line("ADJ", "1", 0, "10", "5"))).doesNotContain(L5);
    assertThat(codes(line("ADJ", "1", null, "10", "5"))).doesNotContain(L5);
  }

  // --- L6 / L7 volume ---

  @ParameterizedTest(name = "errors when volume={0}")
  @CsvSource(value = {"-1", "-0.001", "null"}, nullValues = "null")
  void volume_errors_when_negative_or_null(String volume) {
    Optional<Finding> f = finding(line("SAL", "1", 3, volume, "5"), L6);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.ERROR);
    assertThat(f.get().args()).containsExactly(LABEL);
  }

  @Test
  void volume_negative_and_null_relaxed_for_adjustment() {
    assertThat(codes(line("ADJ", "1", 3, "-1", "5"))).doesNotContain(L6);
    assertThat(codes(line("ADJ", "1", 3, null, "5"))).doesNotContain(L6);
  }

  @Test
  void volume_warns_when_zero() {
    Optional<Finding> f = finding(line("SAL", "1", 3, "0", "5"), L7);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.WARNING);
    assertThat(f.get().args()).containsExactly(LABEL);
  }

  @Test
  void volume_zero_relaxed_for_adjustment() {
    assertThat(codes(line("ADJ", "1", 3, "0", "5"))).doesNotContain(L7);
  }

  @Test
  void volume_null_does_not_also_warn_zero() {
    // A null volume is the L6 error only — L7's zero warning must stay quiet.
    assertThat(codes(line("SAL", "1", 3, null, "5"))).contains(L6).doesNotContain(L7);
  }

  // --- L8 / L9 price ---

  @ParameterizedTest(name = "errors when price={0}")
  @CsvSource(value = {"-1", "-0.01", "null"}, nullValues = "null")
  void price_errors_when_negative_or_null(String price) {
    Optional<Finding> f = finding(line("SAL", "1", 3, "10", price), L8);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.ERROR);
    assertThat(f.get().args()).containsExactly(LABEL);
  }

  @Test
  void price_negative_and_null_relaxed_for_adjustment() {
    assertThat(codes(line("ADJ", "1", 3, "10", "-1"))).doesNotContain(L8);
    assertThat(codes(line("ADJ", "1", 3, "10", null))).doesNotContain(L8);
  }

  @Test
  void price_warns_when_zero() {
    Optional<Finding> f = finding(line("SAL", "1", 3, "10", "0"), L9);

    assertThat(f).isPresent();
    assertThat(f.get().severity()).isEqualTo(Severity.WARNING);
    assertThat(f.get().args()).containsExactly(LABEL);
  }

  @Test
  void price_zero_relaxed_for_adjustment() {
    assertThat(codes(line("ADJ", "1", 3, "10", "0"))).doesNotContain(L9);
  }

  @Test
  void price_null_does_not_also_warn_zero() {
    assertThat(codes(line("SAL", "1", 3, "10", null))).contains(L8).doesNotContain(L9);
  }

  // --- aggregate ---

  @Test
  void validate_reports_nothing_for_a_clean_line() {
    assertThat(InvoiceLineRuleSet.validate(line("SAL", "1", 3, "10", "5"))).isEmpty();
  }

  @Test
  void validate_collects_multiple_findings_in_catalogue_order() {
    // Grade Z (L4) + zero pieces (L5) + zero volume (L7) + negative price (L8).
    assertThat(InvoiceLineRuleSet.validate(line("SAL", "Z", 0, "0", "-1")))
        .extracting(Finding::code)
        .containsExactly(L4, L5, L7, L8);
  }

  @Test
  void adjustment_relaxes_all_value_rules_at_once() {
    // Same broken values as above, but ADJ: only the grade-Z warning remains.
    assertThat(InvoiceLineRuleSet.validate(line("ADJ", "Z", 0, "0", "-1")))
        .extracting(Finding::code)
        .containsExactly(L4);
  }

  // --- helpers ---

  private static List<String> codes(InvoiceLine line) {
    return InvoiceLineRuleSet.validate(line).stream().map(Finding::code).toList();
  }

  private static Optional<Finding> finding(InvoiceLine line, String code) {
    return InvoiceLineRuleSet.validate(line).stream()
        .filter(f -> code.equals(f.code()))
        .findFirst();
  }

  private static InvoiceLine line(
      String invoiceType, String grade, Integer pieces, String volume, String price) {
    return new InvoiceLine(invoiceType, LABEL, grade, pieces,
        volume == null ? null : new BigDecimal(volume),
        price == null ? null : new BigDecimal(price));
  }
}
