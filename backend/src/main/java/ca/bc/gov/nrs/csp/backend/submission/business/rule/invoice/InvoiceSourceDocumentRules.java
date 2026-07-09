package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * §2.6 Source-document reference rules (Boom Numbers / Timber Marks / Weigh
 * Slips). All three fields are comma-separated values carried on the invoice
 * details. Most rules are pure CSV checks; I38 uses {@code ctx.referenceData()}.
 * The de-duplication transform (I31) runs first and rewrites the details in
 * place, so the count (I32–I34), token-length (I35–I37) and duplicate (I38)
 * checks all see the de-duplicated list — matching the legacy ordering (see
 * {@code docs/submission-validation-business-rules.md}).
 *
 * <ul>
 *   <li>I30 — at least one of boom / timber / weigh must be present (ERROR)</li>
 *   <li>I31 — duplicate values within a CSV are de-duplicated (transform, not a finding)</li>
 *   <li>I32 — at most 10 boom numbers (ERROR)</li>
 *   <li>I33 — at most 10 timber marks (ERROR)</li>
 *   <li>I34 — at most 10 weigh slips (ERROR)</li>
 *   <li>I35 — each boom-number token ≤ 20 chars (ERROR)</li>
 *   <li>I36 — each timber-mark token ≤ 6 chars (ERROR)</li>
 *   <li>I37 — each weigh-slip token ≤ 100 chars (ERROR)</li>
 *   <li>I38 — boom number already used by another invoice (WARNING)</li>
 * </ul>
 */
@Component
public class InvoiceSourceDocumentRules implements InvoiceRule {

  private static final String BOOM_LABEL = "Boom Numbers";
  private static final String TIMBER_LABEL = "Timber Marks";
  private static final String WEIGH_LABEL = "Weigh Slips";

  /** For keys whose messages.properties template takes no placeholders. */
  private static final Object[] NO_ARGS = new Object[0];

  @Override
  public void validate(InvoiceRuleContext ctx) {
    atLeastOneSourceDocument(ctx);            // I30
    deduplicateSourceDocuments(ctx);          // I31 (transform — rewrites the details)
    boomNumbersWithinMax(ctx);                // I32
    timberMarksWithinMax(ctx);                // I33
    weighSlipsWithinMax(ctx);                 // I34
    boomTokensWithinMaxLength(ctx);           // I35
    timberTokensWithinMaxLength(ctx);         // I36
    weighTokensWithinMaxLength(ctx);          // I37
    boomNumbersNotUsedByAnotherInvoice(ctx);  // I38
  }

  /** At least one of Boom Number / Timber Mark / Weigh Slip must be present (ERROR). */
  void atLeastOneSourceDocument(InvoiceRuleContext ctx) {
    if (isBlank(boomNumbers(ctx)) && isBlank(timberMarks(ctx)) && isBlank(weighSlips(ctx))) {
      ctx.error("invoice.oneofthe.boom.timber.wiegh.requiered.error", NO_ARGS);
    }
  }

  /**
   * Silently de-duplicates each source-document CSV in place (I31). Duplicate
   * tokens are dropped, not rejected; the remaining tokens keep their order.
   * Runs before the count / length / duplicate checks so they see the reduced
   * list.
   */
  void deduplicateSourceDocuments(InvoiceRuleContext ctx) {
    CSPInvoiceDetailsType details = ctx.invoice().getCSPInvoiceDetails();
    if (details == null) {
      return;
    }
    details.setBoomNumbers(deduplicateCsv(details.getBoomNumbers()));
    details.setTimberMarks(deduplicateCsv(details.getTimberMarks()));
    details.setWeighSlipNumbers(deduplicateCsv(details.getWeighSlipNumbers()));
  }

  /** At most 10 Boom Numbers (ERROR). */
  void boomNumbersWithinMax(InvoiceRuleContext ctx) {
    csvWithinMax(ctx, boomNumbers(ctx), ConstantsCode.MAXOFCSVFORBOOMNUMBERS,
        "invoice.morethan.Max.boomnumbers.error", BOOM_LABEL);
  }

  /** At most 10 Timber Marks (ERROR). */
  void timberMarksWithinMax(InvoiceRuleContext ctx) {
    csvWithinMax(ctx, timberMarks(ctx), ConstantsCode.MAXOFCSVFORTIMBERMARKS,
        "invoice.morethan.Max.timbermarks.error", TIMBER_LABEL);
  }

  /** At most 10 Weigh Slips (ERROR). */
  void weighSlipsWithinMax(InvoiceRuleContext ctx) {
    csvWithinMax(ctx, weighSlips(ctx), ConstantsCode.MAXOFCSVFORWEIGHSLIPS,
        "invoice.morethan.Max.weighslips.error", WEIGH_LABEL);
  }

  /** Each Boom Number token ≤ 20 chars (ERROR). */
  void boomTokensWithinMaxLength(InvoiceRuleContext ctx) {
    csvTokensWithinMaxLength(ctx, boomNumbers(ctx), ConstantsCode.MAXTOKENLENGTHFORBOOMNUMBERS, BOOM_LABEL);
  }

  /** Each Timber Mark token ≤ 6 chars (ERROR). */
  void timberTokensWithinMaxLength(InvoiceRuleContext ctx) {
    csvTokensWithinMaxLength(ctx, timberMarks(ctx), ConstantsCode.MAXTOKENLENGTHFORTIMBERMARKS, TIMBER_LABEL);
  }

  /** Each Weigh Slip token ≤ 100 chars (ERROR). */
  void weighTokensWithinMaxLength(InvoiceRuleContext ctx) {
    csvTokensWithinMaxLength(ctx, weighSlips(ctx), ConstantsCode.MAXTOKENLENGTHFORWEIGHSLIPS, WEIGH_LABEL);
  }

  /**
   * A Boom Number already recorded on another invoice raises a duplicate
   * warning (I38). Every token is checked — the legacy loop skipped the last
   * token (an off-by-one bug); the new implementation reports all of them.
   */
  void boomNumbersNotUsedByAnotherInvoice(InvoiceRuleContext ctx) {
    String boomNumbers = boomNumbers(ctx);
    if (isBlank(boomNumbers)) {
      return;
    }
    List<String> duplicates = new ArrayList<>();
    for (String token : boomNumbers.split(",")) {
      String boomNumber = token.trim();
      if (boomNumber.isEmpty()) {
        continue;
      }
      if (ctx.referenceData().boomNumberUsedByAnotherInvoice(boomNumber)) {
        duplicates.add(boomNumber);
      }
    }
    if (!duplicates.isEmpty()) {
      // Template: the joined list of already-used boom numbers (manual join separator).
      ctx.warning("invoice.boomnumber.duplicate.warning",
          new Object[] {String.join(" , ", duplicates)});
    }
  }

  /** Reports an ERROR when a CSV holds more than {@code max} items. Template: the max. */
  private void csvWithinMax(
      InvoiceRuleContext ctx, String csv, int max, String code, String label) {
    if (isBlank(csv)) {
      return;
    }
    if (csv.split(",").length > max) {
      ctx.error(code, new Object[] {max});
    }
  }

  /**
   * Reports an ERROR listing every token longer than {@code maxLength}.
   * Template: field label, max length, joined offending tokens.
   */
  private void csvTokensWithinMaxLength(
      InvoiceRuleContext ctx, String csv, int maxLength, String label) {
    if (isBlank(csv)) {
      return;
    }
    List<String> tooLong = new ArrayList<>();
    for (String token : csv.split(",")) {
      String value = token.trim();
      if (value.length() > maxLength) {
        tooLong.add(value);
      }
    }
    if (!tooLong.isEmpty()) {
      ctx.error("invoice.tokennumber.lenght.error",
          new Object[] {label, maxLength, String.join(", ", tooLong)});
    }
  }

  /**
   * Returns the CSV with duplicate (trimmed) tokens removed, preserving order;
   * a null / blank value is returned unchanged.
   */
  private static String deduplicateCsv(String csv) {
    if (isBlank(csv)) {
      return csv;
    }
    Set<String> unique = new LinkedHashSet<>();
    for (String token : csv.split(",")) {
      String value = token.trim();
      if (!value.isEmpty()) {
        unique.add(value);
      }
    }
    return String.join(",", unique);
  }

  private static String boomNumbers(InvoiceRuleContext ctx) {
    CSPInvoiceDetailsType details = ctx.invoice().getCSPInvoiceDetails();
    return details == null ? null : details.getBoomNumbers();
  }

  private static String timberMarks(InvoiceRuleContext ctx) {
    CSPInvoiceDetailsType details = ctx.invoice().getCSPInvoiceDetails();
    return details == null ? null : details.getTimberMarks();
  }

  private static String weighSlips(InvoiceRuleContext ctx) {
    CSPInvoiceDetailsType details = ctx.invoice().getCSPInvoiceDetails();
    return details == null ? null : details.getWeighSlipNumbers();
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
