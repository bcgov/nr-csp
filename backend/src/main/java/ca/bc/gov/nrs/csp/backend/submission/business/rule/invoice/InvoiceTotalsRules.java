package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class InvoiceTotalsRules implements InvoiceRule {

  /** Invoice type for which the negative-total checks are relaxed. */
  private static final String ADJUSTMENT = "ADJ";

  /** Submitted total amount must be within ±$5.00 of the calculated total. */
  private static final BigDecimal TOTAL_AMOUNT_VARIANCE = new BigDecimal("5.00");

  /** Submitted total volume must be within ±5.00 of the calculated total. */
  private static final BigDecimal TOTAL_VOLUME_VARIANCE = new BigDecimal("5.00");

  @Override
  public void validate(InvoiceRuleContext ctx) {
    totalAmountNotNegative(ctx);
    totalAmountWithinVariance(ctx);
    totalVolumeNotNegative(ctx);
    totalVolumeWithinVariance(ctx);
    totalPiecesNotNegative(ctx);
    totalPiecesMatchesCalculated(ctx);
  }

  /** Total amount cannot be negative (except ADJ) (ERROR) */
  void totalAmountNotNegative(InvoiceRuleContext ctx) {
    errorIfNegative(ctx, "totalAmount", "invoice.totalamount.negative.error", ctx.totalAmount());
  }

  /** Submitted total amount within ±$5.00 of calculated (WARNING) */
  void totalAmountWithinVariance(InvoiceRuleContext ctx) {
    BigDecimal submitted = ctx.totalAmount();
    if (submitted == null) {
      return;
    }
    BigDecimal calculated = calculatedTotalAmount(ctx);
    if (calculated.subtract(submitted).abs().compareTo(TOTAL_AMOUNT_VARIANCE) > 0) {
      ctx.warning(
          "invoice.totalamount.dismatch.warning",
          invoiceMessage("totalAmount " + submitted, ctx,
              "differs from the calculated total " + calculated + " by more than $" + TOTAL_AMOUNT_VARIANCE + "."));
    }
  }

  /** Total volume cannot be negative (except ADJ) (ERROR) */
  void totalVolumeNotNegative(InvoiceRuleContext ctx) {
    errorIfNegative(ctx, "totalVolume", "invoice.totalvolume.negative.error", ctx.totalVolume());
  }

  /** Submitted total volume within ±5.00 of calculated (WARNING) */
  void totalVolumeWithinVariance(InvoiceRuleContext ctx) {
    BigDecimal submitted = ctx.totalVolume();
    if (submitted == null) {
      return;
    }
    BigDecimal calculated = calculatedTotalVolume(ctx);
    if (calculated.subtract(submitted).abs().compareTo(TOTAL_VOLUME_VARIANCE) > 0) {
      ctx.warning(
          "invoice.totalvolume.dismatch.warning",
          invoiceMessage("totalVolume " + submitted, ctx,
              "differs from the calculated total " + calculated + " by more than " + TOTAL_VOLUME_VARIANCE + "."));
    }
  }

  /** Total pieces cannot be negative (except ADJ) (ERROR) */
  void totalPiecesNotNegative(InvoiceRuleContext ctx) {
    errorIfNegative(ctx, "totalPieces", "invoice.totalpieces.negative.error", ctx.totalPieces());
  }

  /** Submitted total pieces exactly matches calculated (WARNING) */
  void totalPiecesMatchesCalculated(InvoiceRuleContext ctx) {
    Integer submitted = ctx.totalPieces();
    int submittedPieces = submitted == null ? 0 : submitted;
    int calculated = calculatedTotalPieces(ctx);
    if (submittedPieces != calculated) {
      ctx.warning(
          "invoice.totalpieces.dismatch.warning",
          invoiceMessage("totalPieces " + submittedPieces, ctx,
              "does not match the calculated total " + calculated + "."));
    }
  }

  /** Calculated total pieces = Σ numberOfPieces over the line items. */
  private int calculatedTotalPieces(InvoiceRuleContext ctx) {
    int total = 0;
    for (CSPLineItemType line : ctx.invoice().getCSPLineItem()) {
      total += line.getNumberOfPieces();
    }
    return total;
  }

  /** Calculated total volume = Σ volume over the line items; lines missing a volume contribute nothing. */
  private BigDecimal calculatedTotalVolume(InvoiceRuleContext ctx) {
    BigDecimal total = BigDecimal.ZERO;
    for (CSPLineItemType line : ctx.invoice().getCSPLineItem()) {
      if (line.getVolume() != null) {
        total = total.add(line.getVolume());
      }
    }
    return total;
  }

  /**
   * Calculated total amount = Σ(volume × price) over the line items, each product
   * rounded HALF_UP to two decimals. For ADJ invoices a sign-preserving variant
   * keeps a negative-volume × negative-price product negative. Lines missing a
   * volume or price contribute nothing.
   */
  private BigDecimal calculatedTotalAmount(InvoiceRuleContext ctx) {
    boolean adjustment = isAdjustment(ctx);
    BigDecimal total = BigDecimal.ZERO;
    for (CSPLineItemType line : ctx.invoice().getCSPLineItem()) {
      BigDecimal amount = lineAmount(line.getVolume(), line.getPrice(), adjustment);
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

  private static boolean isAdjustment(InvoiceRuleContext ctx) {
    return ADJUSTMENT.equals(ctx.invoiceType());
  }

  /** Emits {@code code} when a decimal total is negative, unless the invoice is ADJ. */
  private static void errorIfNegative(InvoiceRuleContext ctx, String field, String code, BigDecimal value) {
    if (!isAdjustment(ctx) && value != null && value.signum() < 0) {
      ctx.error(code, invoiceMessage(field, ctx, "cannot be negative."));
    }
  }

  /** Integer overload (total pieces). */
  private static void errorIfNegative(InvoiceRuleContext ctx, String field, String code, Integer value) {
    if (!isAdjustment(ctx) && value != null && value < 0) {
      ctx.error(code, invoiceMessage(field, ctx, "cannot be negative."));
    }
  }

  /**
   * Builds a totals message scoped to this invoice, e.g.
   * {@code "totalAmount for invoiceNumber INV-1 cannot be negative."}. {@code prefix}
   * leads the message (the field name, optionally with the submitted value) and
   * {@code detail} is the clause following the invoice number (no leading space).
   */
  private static String invoiceMessage(String prefix, InvoiceRuleContext ctx, String detail) {
    return prefix + " for invoiceNumber " + ctx.invoiceNumber() + " " + detail;
  }
}
