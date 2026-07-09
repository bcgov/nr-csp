package ca.bc.gov.nrs.csp.backend.invoice.shared.rules;

import ca.bc.gov.nrs.csp.backend.invoice.shared.model.Finding;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.InvoiceLine;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.Severity;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure, channel-agnostic line-item value rules (catalogue §3, L3–L9). No
 * Spring, no DB — the reference-data rules (L1–L2) stay channel-side. Fed an
 * {@link InvoiceLine}, returns the list of {@link Finding}s — each a message
 * key plus its {@code messages.properties} args (refactor doc §3.5); every
 * template here takes the line label as {@code {0}}.
 *
 * <p>Null handling (refactor doc §7.2, decided): a null {@code numberOfPieces},
 * {@code volume} or {@code price} is an ERROR under the existing negative-value
 * keys — matching the manual path and legacy's "value required" contract. The
 * electronic path's XSD makes these fields required, so null is unreachable
 * there and the check is a pure safety net. The value checks (L5–L9) are
 * relaxed for parent invoice type {@code ADJ}; grade (L3–L4) always applies.
 */
public final class InvoiceLineRuleSet {

  private InvoiceLineRuleSet() {}

  public static List<Finding> validate(InvoiceLine line) {
    List<Finding> out = new ArrayList<>();
    gradeRequired(line, out);          // L3
    gradeZWarning(line, out);          // L4
    numberOfPiecesPositive(line, out); // L5
    volumeNotNegative(line, out);      // L6
    volumeZeroWarning(line, out);      // L7
    priceNotNegative(line, out);       // L8
    priceZeroWarning(line, out);       // L9
    return out;
  }

  /** L3 — grade is required (non-null) (ERROR). */
  private static void gradeRequired(InvoiceLine line, List<Finding> out) {
    if (line.grade() == null) {
      out.add(new Finding("invoice.grade.invalid.required.error", Severity.ERROR,
          labelArgs(line)));
    }
  }

  /** L4 — grade {@code "Z"} raises a non-blocking warning. */
  private static void gradeZWarning(InvoiceLine line, List<Finding> out) {
    if ("Z".equals(line.grade())) {
      out.add(new Finding("invoice.grade.z.warning", Severity.WARNING, labelArgs(line)));
    }
  }

  /** L5 — number of pieces must be present and &gt; 0 (unless ADJ) (ERROR). */
  private static void numberOfPiecesPositive(InvoiceLine line, List<Finding> out) {
    if (isAdjustment(line)) {
      return;
    }
    Integer pieces = line.numberOfPieces();
    if (pieces == null || pieces <= 0) {
      out.add(new Finding("invoice.numberof.pieces.negative.or.zero.error", Severity.ERROR,
          labelArgs(line)));
    }
  }

  /** L6 — volume must be present and not negative (unless ADJ) (ERROR). */
  private static void volumeNotNegative(InvoiceLine line, List<Finding> out) {
    if (isAdjustment(line)) {
      return;
    }
    BigDecimal volume = line.volume();
    if (volume == null || volume.signum() < 0) {
      out.add(new Finding("invoice.volume.negative.value.error", Severity.ERROR,
          labelArgs(line)));
    }
  }

  /** L7 — a volume of zero raises a non-blocking warning (unless ADJ). */
  private static void volumeZeroWarning(InvoiceLine line, List<Finding> out) {
    if (isAdjustment(line)) {
      return;
    }
    BigDecimal volume = line.volume();
    if (volume != null && volume.signum() == 0) {
      out.add(new Finding("invoice.volume.zero.value.warning", Severity.WARNING,
          labelArgs(line)));
    }
  }

  /** L8 — price must be present and not negative (unless ADJ) (ERROR). */
  private static void priceNotNegative(InvoiceLine line, List<Finding> out) {
    if (isAdjustment(line)) {
      return;
    }
    BigDecimal price = line.price();
    if (price == null || price.signum() < 0) {
      out.add(new Finding("invoice.price.negative.value.error", Severity.ERROR,
          labelArgs(line)));
    }
  }

  /** L9 — a price of zero raises a non-blocking warning (unless ADJ). */
  private static void priceZeroWarning(InvoiceLine line, List<Finding> out) {
    if (isAdjustment(line)) {
      return;
    }
    BigDecimal price = line.price();
    if (price != null && price.signum() == 0) {
      out.add(new Finding("invoice.price.zero.value.warning", Severity.WARNING,
          labelArgs(line)));
    }
  }

  private static boolean isAdjustment(InvoiceLine line) {
    return ConstantsCode.INVTYPE_ADJUST.equals(line.invoiceType());
  }

  /** Every line template renders the channel-formatted line label as {@code {0}}. */
  private static Object[] labelArgs(InvoiceLine line) {
    return new Object[] {line.lineLabel()};
  }
}
