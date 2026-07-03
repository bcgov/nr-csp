package ca.bc.gov.nrs.csp.backend.submission.business.rule.line;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.LineItemRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.LineItemRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * All line-item-level business rules (catalogue §3, L1–L9), one method per rule.
 * Several checks are relaxed for parent invoice type {@code ADJ} (adjustment).
 *
 * <p><b>How to add a rule:</b> write a package-private
 * {@code void xxx(LineItemRuleContext ctx)} method (header-commented with its
 * catalogue ID), then add a call to it in {@link #validate} in catalogue order.
 * Report via {@code ctx.error(...)} (blocking) or {@code ctx.warning(...)}
 * (non-blocking) using the message key from
 * {@code docs/submission-validation-business-rules.md}; never throw. Unit-test
 * each method directly (see {@code LineItemRulesTest}).
 */
@Component
public class LineItemRules implements LineItemRule {

  /** Parent invoice type for which the value checks (L5–L9) are relaxed. */
  private static final String ADJUSTMENT = "ADJ";

  @Override
  public void validate(LineItemRuleContext ctx) {
    secondarySortCodeValid(ctx); // L1
    speciesGradeCombinationValid(ctx); // L2
    gradeRequired(ctx); // L3
    gradeZWarning(ctx); // L4
    numberOfPiecesPositive(ctx); // L5
    volumeNotNegative(ctx); // L6
    volumeZeroWarning(ctx); // L7
    priceNotNegative(ctx); // L8
    priceZeroWarning(ctx); // L9
  }

  /** L1 — secondary sort code must be a recognised code active on the invoice date. */
  void secondarySortCodeValid(LineItemRuleContext ctx) {
    String sortCode = ctx.line().getSecondarySortCode();
    if (isBlank(sortCode) || !ctx.referenceData().sortCodeValidOn(sortCode, ctx.invoiceDate())) {
      ctx.error(
          "invoice.secondry.sortcode.invalid.error",
          "secondarySortCode " + sortCode + " on line " + ctx.lineNumber()
              + " is not a recognized code active on the invoice date.");
    }
  }

  /** L2 — the species + grade combination must exist in CSP_SPECIES_GRADE_XREF. */
  void speciesGradeCombinationValid(LineItemRuleContext ctx) {
    String species = ctx.line().getSpecies();
    String grade = ctx.line().getGrade();
    if (!ctx.referenceData().speciesGradeCombinationExists(species, grade)) {
      ctx.error(
          "invoice.species.grade.combination.error",
          "species " + species + " / grade " + grade + " combination on line "
              + ctx.lineNumber() + " does not exist in CSP.");
    }
  }

  /** L3 — grade is required (non-null). */
  void gradeRequired(LineItemRuleContext ctx) {
    if (ctx.line().getGrade() == null) {
      ctx.error(
          "invoice.grade.invalid.required.error",
          "grade is required on line " + ctx.lineNumber() + ".");
    }
  }

  /** L4 — grade {@code "Z"} raises a non-blocking warning. */
  void gradeZWarning(LineItemRuleContext ctx) {
    if ("Z".equals(ctx.line().getGrade())) {
      ctx.warning("invoice.grade.z.warning", "Grade Z is used.");
    }
  }

  /** L5 — number of pieces must be &gt; 0 (unless the parent invoice is ADJ). */
  void numberOfPiecesPositive(LineItemRuleContext ctx) {
    if (isAdjustment(ctx)) {
      return;
    }
    if (ctx.line().getNumberOfPieces() <= 0) {
      ctx.error(
          "invoice.numberof.pieces.negative.or.zero.error",
          "numberOfPieces on line " + ctx.lineNumber() + " must be greater than zero.");
    }
  }

  /** L6 — volume cannot be negative (unless the parent invoice is ADJ). */
  void volumeNotNegative(LineItemRuleContext ctx) {
    if (isAdjustment(ctx)) {
      return;
    }
    BigDecimal volume = ctx.line().getVolume();
    if (volume != null && volume.signum() < 0) {
      ctx.error(
          "invoice.volume.negative.value.error",
          "volume on line " + ctx.lineNumber() + " cannot be negative.");
    }
  }

  /** L7 — a volume of zero raises a non-blocking warning (unless the parent invoice is ADJ). */
  void volumeZeroWarning(LineItemRuleContext ctx) {
    if (isAdjustment(ctx)) {
      return;
    }
    BigDecimal volume = ctx.line().getVolume();
    if (volume != null && volume.signum() == 0) {
      ctx.warning(
          "invoice.volume.zero.value.warning",
          "volume on line " + ctx.lineNumber() + " is zero.");
    }
  }

  /** L8 — price cannot be negative (unless the parent invoice is ADJ). */
  void priceNotNegative(LineItemRuleContext ctx) {
    if (isAdjustment(ctx)) {
      return;
    }
    BigDecimal price = ctx.line().getPrice();
    if (price != null && price.signum() < 0) {
      ctx.error(
          "invoice.price.negative.value.error",
          "price on line " + ctx.lineNumber() + " cannot be negative.");
    }
  }

  /** L9 — a price of zero raises a non-blocking warning (unless the parent invoice is ADJ). */
  void priceZeroWarning(LineItemRuleContext ctx) {
    if (isAdjustment(ctx)) {
      return;
    }
    BigDecimal price = ctx.line().getPrice();
    if (price != null && price.signum() == 0) {
      ctx.warning(
          "invoice.price.zero.value.warning",
          "price on line " + ctx.lineNumber() + " is zero.");
    }
  }

  private static boolean isAdjustment(LineItemRuleContext ctx) {
    return ADJUSTMENT.equals(ctx.invoiceType());
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
