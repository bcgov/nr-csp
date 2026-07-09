package ca.bc.gov.nrs.csp.backend.invoice.rules;

import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure, channel-agnostic totals rules (catalogue §2.5, I24–I29). No Spring, no
 * DB. Fed an {@link InvoiceTotals}, returns the list of {@link Finding}s — each
 * a message key plus its {@code messages.properties} args, never rendered text
 * (refactor doc §3.5).
 *
 * <p>The behaviour is reconciled against the legacy system of record
 * ({@code ca.bc.gov.mof.csp} {@code InvoiceValidator}) — see the refactor doc
 * §7.1: the variance check mirrors legacy {@code checkValues} (double
 * comparison, always run, null submitted treated as zero) and the calculated
 * amount mirrors legacy {@code bigDecimalMultiplication[ForAdj]} (per-line
 * HALF_UP to 2dp, ADJ keeps a neg×neg product negative).
 */
public final class InvoiceTotalsRuleSet {

  /** For keys whose messages.properties template takes no placeholders. */
  private static final Object[] NO_ARGS = new Object[0];

  private InvoiceTotalsRuleSet() {}

  public static List<Finding> validate(InvoiceTotals t) {
    List<Finding> out = new ArrayList<>();
    totalAmountNotNegative(t, out);       // I24
    totalAmountWithinVariance(t, out);    // I25
    totalVolumeNotNegative(t, out);       // I26
    totalVolumeWithinVariance(t, out);    // I27
    totalPiecesNotNegative(t, out);       // I28
    totalPiecesMatchesCalculated(t, out); // I29
    return out;
  }

  /** I24 — total amount cannot be negative (except ADJ) (ERROR). */
  private static void totalAmountNotNegative(InvoiceTotals t, List<Finding> out) {
    if (isAdjustment(t)) {
      return;
    }
    BigDecimal v = t.submittedAmount();
    if (v != null && v.signum() < 0) {
      out.add(new Finding("invoice.totalamount.negative.error", Severity.ERROR, NO_ARGS));
    }
  }

  /** I25 — submitted total amount within ±$5.00 of calculated (WARNING). Applies to all types. */
  private static void totalAmountWithinVariance(InvoiceTotals t, List<Finding> out) {
    BigDecimal submitted = t.submittedAmount();
    BigDecimal calculated = calculatedTotalAmount(t);
    if (!withinVariance(submitted, calculated, ConstantsCode.TOTALAMOUNT_MAXPERMITTEDVARIANCE)) {
      out.add(new Finding("invoice.totalamount.dismatch.warning", Severity.WARNING,
          new Object[] {submitted}));
    }
  }

  /** I26 — total volume cannot be negative (except ADJ) (ERROR). */
  private static void totalVolumeNotNegative(InvoiceTotals t, List<Finding> out) {
    if (isAdjustment(t)) {
      return;
    }
    BigDecimal v = t.submittedVolume();
    if (v != null && v.signum() < 0) {
      out.add(new Finding("invoice.totalvolume.negative.error", Severity.ERROR, NO_ARGS));
    }
  }

  /** I27 — submitted total volume within ±5.00 of calculated (WARNING). */
  private static void totalVolumeWithinVariance(InvoiceTotals t, List<Finding> out) {
    BigDecimal submitted = t.submittedVolume();
    BigDecimal calculated = calculatedTotalVolume(t);
    if (!withinVariance(submitted, calculated, ConstantsCode.TOTALVOLUME_MAXPERMITTEDVARIANCE)) {
      out.add(new Finding("invoice.totalvolume.dismatch.warning", Severity.WARNING,
          new Object[] {submitted}));
    }
  }

  /** I28 — total pieces cannot be negative (except ADJ) (ERROR). */
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
   * I29 — submitted total pieces must match calculated (WARNING). The permitted
   * variance constant is 0, so the window collapses to an exact match; an absent
   * submitted total defaults to 0 (pieces is the one optional total).
   */
  private static void totalPiecesMatchesCalculated(InvoiceTotals t, List<Finding> out) {
    Integer submitted = t.submittedPieces();
    int submittedPieces = submitted == null ? 0 : submitted;
    int calculated = calculatedTotalPieces(t);
    if (!withinVariance(submittedPieces, calculated, ConstantsCode.TOTALPIECES_MAXPERMITTEDVARIANCE)) {
      out.add(new Finding("invoice.totalpieces.dismatch.warning", Severity.WARNING,
          new Object[] {submittedPieces}));
    }
  }

  // ---- calculations (Σ over line items) ----

  /** Σ(volume × price), each product HALF_UP to 2dp; ADJ keeps neg×neg negative. */
  private static BigDecimal calculatedTotalAmount(InvoiceTotals t) {
    boolean adjustment = isAdjustment(t);
    BigDecimal total = BigDecimal.ZERO;
    for (InvoiceTotals.Line line : t.lines()) {
      BigDecimal amount = lineAmount(line.volume(), line.price(), adjustment);
      if (amount != null) {
        total = total.add(amount);
      }
    }
    return total.setScale(2, RoundingMode.HALF_UP);
  }

  /** One line's contribution to the total amount, or null if volume or price is absent. */
  private static BigDecimal lineAmount(BigDecimal volume, BigDecimal price, boolean adjustment) {
    if (volume == null || price == null) {
      return null;
    }
    // A default multiply of two negatives yields a positive; for ADJ the amount must stay negative.
    if (adjustment && volume.signum() < 0 && price.signum() < 0) {
      volume = volume.abs();
    }
    return volume.multiply(price).setScale(2, RoundingMode.HALF_UP);
  }

  /** Calculated total volume = Σ volume over the line items; lines missing a volume contribute nothing. */
  private static BigDecimal calculatedTotalVolume(InvoiceTotals t) {
    BigDecimal total = BigDecimal.ZERO;
    for (InvoiceTotals.Line line : t.lines()) {
      if (line.volume() != null) {
        total = total.add(line.volume());
      }
    }
    return total;
  }

  /** Calculated total pieces = Σ pieces over the line items. */
  private static int calculatedTotalPieces(InvoiceTotals t) {
    int total = 0;
    for (InvoiceTotals.Line line : t.lines()) {
      total += line.pieces();
    }
    return total;
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
