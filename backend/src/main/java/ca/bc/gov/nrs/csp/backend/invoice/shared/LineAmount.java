package ca.bc.gov.nrs.csp.backend.invoice.shared;

import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single source of truth for a line item's {@code $Amount} (volume × price).
 *
 * <p>The amount is never stored — {@code coastal_log_sale_detail} has no amount
 * column, so every channel derives it. It therefore has to be derived the SAME
 * way everywhere: the read path that feeds the invoice screen and the CSV/PDF
 * exports, the inbound mapper, and the totals-variance rules. Previously each
 * did its own {@code volume.multiply(price)} and only the rules applied the
 * adjustment sign fix, so an ADJ line showed a positive amount on screen while
 * the validator calculated a negative one.
 */
public final class LineAmount {

  private LineAmount() {}

  /**
   * One line's amount, HALF_UP to 2dp, or {@code null} when either input is absent.
   *
   * <p>Adjustment invoices are the only type allowed to carry a negative volume
   * or price (every other type rejects them as errors — see
   * {@link ca.bc.gov.nrs.csp.backend.invoice.shared.rules.InvoiceLineRuleSet}).
   * A plain multiply of two negatives yields a positive, but an adjustment's
   * amount must stay negative, so the volume is flipped positive and the
   * negative price carries the sign.
   *
   * @param volume      the line's volume
   * @param price       the line's price
   * @param invoiceType the parent invoice's type code, e.g. {@code ADJ}
   * @return the signed amount rounded to 2dp, or null if volume or price is null
   */
  public static BigDecimal compute(BigDecimal volume, BigDecimal price, String invoiceType) {
    if (volume == null || price == null) {
      return null;
    }
    BigDecimal effectiveVolume = volume;
    if (isAdjustment(invoiceType) && volume.signum() < 0 && price.signum() < 0) {
      effectiveVolume = volume.abs();
    }
    return effectiveVolume.multiply(price).setScale(2, RoundingMode.HALF_UP);
  }

  /** Whether the given invoice type code is an adjustment. */
  public static boolean isAdjustment(String invoiceType) {
    return ConstantsCode.INVTYPE_ADJUST.equals(invoiceType);
  }
}
