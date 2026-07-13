package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class InvoiceSourceDocumentRules implements InvoiceRule {

  private static final String BOOM_LABEL = "Boom Numbers";
  private static final String TIMBER_LABEL = "Timber Marks";
  private static final String WEIGH_LABEL = "Weigh Slips";

  /** For keys whose messages.properties template takes no placeholders. */
  private static final Object[] NO_ARGS = new Object[0];

  @Override
  public void validate(InvoiceRuleContext ctx) {
    atLeastOneSourceDocument(ctx);
    deduplicateSourceDocuments(ctx);
    boomNumbersWithinMax(ctx);
    timberMarksWithinMax(ctx);
    weighSlipsWithinMax(ctx);
    boomTokensWithinMaxLength(ctx);
    timberTokensWithinMaxLength(ctx);
    weighTokensWithinMaxLength(ctx);
    boomNumbersNotUsedByAnotherInvoice(ctx);
  }

  /** At least one of Boom Number / Timber Mark / Weigh Slip must be present (ERROR). */
  void atLeastOneSourceDocument(InvoiceRuleContext ctx) {
    if (isBlank(boomNumbers(ctx)) && isBlank(timberMarks(ctx)) && isBlank(weighSlips(ctx))) {
      ctx.error("invoice.oneofthe.boom.timber.wiegh.requiered.error", NO_ARGS);
    }
  }

  /**
   * Silently de-duplicates each source-document CSV in place. Duplicate
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

  /** At most 5 Boom Numbers (ERROR). */
  void boomNumbersWithinMax(InvoiceRuleContext ctx) {
    csvWithinMax(ctx, boomNumbers(ctx), ConstantsCode.MAXOFCSVFORBOOMNUMBERS,
        "invoice.morethan.Max.boomnumbers.error");
  }

  /** At most 5 Timber Marks (ERROR). */
  void timberMarksWithinMax(InvoiceRuleContext ctx) {
    csvWithinMax(ctx, timberMarks(ctx), ConstantsCode.MAXOFCSVFORTIMBERMARKS,
        "invoice.morethan.Max.timbermarks.error");
  }

  /** At most 5 Weigh Slips (ERROR). */
  void weighSlipsWithinMax(InvoiceRuleContext ctx) {
    csvWithinMax(ctx, weighSlips(ctx), ConstantsCode.MAXOFCSVFORWEIGHSLIPS,
        "invoice.morethan.Max.weighslips.error");
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
   * warning. Every token is checked — the legacy loop skipped the last
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
  private void csvWithinMax(InvoiceRuleContext ctx, String csv, int max, String code) {
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
