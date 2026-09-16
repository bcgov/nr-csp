package ca.bc.gov.nrs.csp.backend.invoice.shared.rules;

import ca.bc.gov.nrs.csp.backend.invoice.shared.LineAmount;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.Finding;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.InvoiceTotals;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.Severity;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure, channel-agnostic totals rules. No Spring, no
 * DB. Fed an {@link InvoiceTotals}, returns the list of {@link Finding}s — each
 * a message key plus its {@code messages.properties} args, never rendered text.
 */
public final class InvoiceTotalsRuleSet {

  /** For keys whose messages.properties template takes no placeholders. */
  private static final Object[] NO_ARGS = new Object[0];

  private InvoiceTotalsRuleSet() {}

  public static List<Finding> validate(InvoiceTotals t) {
    List<Finding> out = new ArrayList<>();
    Calculated calculated = calculate(t.invoiceType(), t.lines());
    totalAmountNotNegative(t, out);
    totalAmountWithinVariance(t, calculated, out);
    totalVolumeNotNegative(t, out);
    totalVolumeWithinVariance(t, calculated, out);
    totalPiecesNotNegative(t, out);
    totalPiecesMatchesCalculated(t, calculated, out);
    return out;
  }

  // ---- calculations (Σ over line items) ----

  /**
   * The totals derived from the line items — what the submitted totals get
   * compared against below.
   *
   * <p>Public because the manual (CRUD) channel also has to <em>persist</em>
   * these: its header totals are derived from the line items rather than typed
   * in, so every write recomputes them. Both uses go through {@link #calculate}
   * so the stored totals and the variance rules can never disagree.
   */
  public record Calculated(BigDecimal amount, BigDecimal volume, int pieces) {}

  /**
   * Σ over the line items: amount = Σ(volume × price) with each product HALF_UP
   * to 2dp (ADJ keeps neg×neg negative — see {@link LineAmount}), volume = Σ
   * volume, pieces = Σ pieces. Lines missing a volume or price contribute
   * nothing to the amount; a line missing a volume contributes nothing to the
   * volume either.
   */
  public static Calculated calculate(String invoiceType, List<InvoiceTotals.Line> lines) {
    BigDecimal amount = BigDecimal.ZERO;
    BigDecimal volume = BigDecimal.ZERO;
    int pieces = 0;
    if (lines != null) {
      for (InvoiceTotals.Line line : lines) {
        BigDecimal lineAmount = LineAmount.compute(line.volume(), line.price(), invoiceType);
        if (lineAmount != null) {
          amount = amount.add(lineAmount);
        }
        if (line.volume() != null) {
          volume = volume.add(line.volume());
        }
        pieces += line.pieces();
      }
    }
    return new Calculated(amount.setScale(2, RoundingMode.HALF_UP), volume, pieces);
  }

  // ---- rules ----

  /** Total amount cannot be negative (except ADJ) (ERROR). */
  private static void totalAmountNotNegative(InvoiceTotals t, List<Finding> out) {
    if (isAdjustment(t)) {
      return;
    }
    BigDecimal v = t.submittedAmount();
    if (v != null && v.signum() < 0) {
      out.add(new Finding("invoice.totalamount.negative.error", Severity.ERROR, NO_ARGS));
    }
  }

  /** Submitted total amount within ±$5.00 of calculated (WARNING). Applies to all types. */
  private static void totalAmountWithinVariance(InvoiceTotals t, Calculated calculated, List<Finding> out) {
    BigDecimal submitted = t.submittedAmount();
    if (!withinVariance(submitted, calculated.amount(), ConstantsCode.TOTALAMOUNT_MAXPERMITTEDVARIANCE)) {
      out.add(new Finding("invoice.totalamount.dismatch.warning", Severity.WARNING,
          new Object[] {submitted}));
    }
  }

  /** Total volume cannot be negative (except ADJ) (ERROR). */
  private static void totalVolumeNotNegative(InvoiceTotals t, List<Finding> out) {
    if (isAdjustment(t)) {
      return;
    }
    BigDecimal v = t.submittedVolume();
    if (v != null && v.signum() < 0) {
      out.add(new Finding("invoice.totalvolume.negative.error", Severity.ERROR, NO_ARGS));
    }
  }

  /** Submitted total volume within ±5.00 of calculated (WARNING). */
  private static void totalVolumeWithinVariance(InvoiceTotals t, Calculated calculated, List<Finding> out) {
    BigDecimal submitted = t.submittedVolume();
    if (!withinVariance(submitted, calculated.volume(), ConstantsCode.TOTALVOLUME_MAXPERMITTEDVARIANCE)) {
      out.add(new Finding("invoice.totalvolume.dismatch.warning", Severity.WARNING,
          new Object[] {submitted}));
    }
  }

  /** Total pieces cannot be negative (except ADJ) (ERROR). */
  private static void totalPiecesNotNegative(InvoiceTotals t, List<Finding> out) {
    if (isAdjustment(t)) {
      return;
    }
    Integer v = t.submittedPieces();
    if (v != null && v < 0) {
      out.add(new Finding("invoice.totalpieces.negative.error", Severity.ERROR, NO_ARGS));
    }
  }

  /**
   * Submitted total pieces must match calculated (WARNING). The permitted
   * variance constant is 0, so the window collapses to an exact match; an absent
   * submitted total defaults to 0 (pieces is the one optional total).
   */
  private static void totalPiecesMatchesCalculated(InvoiceTotals t, Calculated calculated, List<Finding> out) {
    Integer submitted = t.submittedPieces();
    int submittedPieces = submitted == null ? 0 : submitted;
    if (!withinVariance(submittedPieces, calculated.pieces(), ConstantsCode.TOTALPIECES_MAXPERMITTEDVARIANCE)) {
      out.add(new Finding("invoice.totalpieces.dismatch.warning", Severity.WARNING,
          new Object[] {submittedPieces}));
    }
  }

  // ---- helpers ----

  /**
   * Legacy {@code InvoiceValidator.checkValues}: the submitted total is within
   * variance when |calculated − submitted| ≤ variance, compared as a double. A
   * null submitted total is treated as zero (legacy always ran this check, even
   * for an absent total — refactor doc §7.1 item 3). Kept as a double comparison
   * for parity with the legacy system of record; if this is ever upgraded to
   * BigDecimal precision, change it here so BOTH channels move together.
   */
  private static boolean withinVariance(BigDecimal submitted, BigDecimal calculated, double variance) {
    BigDecimal s = submitted == null ? BigDecimal.ZERO : submitted;
    BigDecimal c = calculated == null ? BigDecimal.ZERO : calculated;
    double diff = c.subtract(s).doubleValue();
    return !(diff > variance || diff < -variance);
  }

  /** Integer overload (total pieces) — legacy {@code checkValues(int, int, double)}. */
  private static boolean withinVariance(int submitted, int calculated, double variance) {
    int diff = calculated - submitted;
    return !(diff > variance || diff < -variance);
  }

  private static boolean isAdjustment(InvoiceTotals t) {
    return ConstantsCode.INVTYPE_ADJUST.equals(t.invoiceType());
  }
}
