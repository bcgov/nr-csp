package ca.bc.gov.nrs.csp.backend.submission.business.rule.line;

import ca.bc.gov.nrs.csp.backend.invoice.rules.Finding;
import ca.bc.gov.nrs.csp.backend.invoice.rules.InvoiceLine;
import ca.bc.gov.nrs.csp.backend.invoice.rules.InvoiceLineRuleSet;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.LineItemRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.LineItemRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import org.springframework.stereotype.Component;

/**
 * Line-item business rules (catalogue §3, L1–L9). The reference-data rules
 * (L1–L2) live here — they need {@code ctx.referenceData()} and stay
 * channel-side. The pure value rules (L3–L9) are delegated to the shared
 * channel-agnostic {@link InvoiceLineRuleSet} (refactor doc §5) — the same core
 * the manual path runs, so the two channels cannot drift; each {@link Finding}
 * is forwarded as a message key + template args (resolved at the HTTP boundary,
 * doc §3.5).
 */
@Component
public class LineItemRules implements LineItemRule {

  @Override
  public void validate(LineItemRuleContext ctx) {
    secondarySortCodeValid(ctx); // L1
    speciesGradeCombinationValid(ctx); // L2
    for (Finding f : InvoiceLineRuleSet.validate(toLine(ctx))) { // L3–L9
      // NB: f.severity() is invoice.rules.Severity — NOT submission.shared.Severity.
      if (f.severity() == ca.bc.gov.nrs.csp.backend.invoice.rules.Severity.ERROR) {
        ctx.error(f.code(), f.args());
      } else {
        ctx.warning(f.code(), f.args());
      }
    }
  }

  /**
   * L1 — secondary sort code must be a recognised code active on the invoice
   * date. Template: code, date, line label.
   */
  void secondarySortCodeValid(LineItemRuleContext ctx) {
    String sortCode = ctx.line().getSecondarySortCode();
    if (isBlank(sortCode) || !ctx.referenceData().sortCodeValidOn(sortCode, ctx.invoiceDate())) {
      ctx.error("invoice.secondry.sortcode.invalid.error",
          new Object[] {sortCode, ctx.invoiceDate(), lineLabel(ctx)});
    }
  }

  /**
   * L2 — the species + grade combination must exist in CSP_SPECIES_GRADE_XREF.
   * Template: species, grade, line label.
   */
  void speciesGradeCombinationValid(LineItemRuleContext ctx) {
    String species = ctx.line().getSpecies();
    String grade = ctx.line().getGrade();
    if (!ctx.referenceData().speciesGradeCombinationExists(species, grade)) {
      ctx.error("invoice.species.grade.combination.error",
          new Object[] {species, grade, lineLabel(ctx)});
    }
  }

  private static InvoiceLine toLine(LineItemRuleContext ctx) {
    CSPLineItemType line = ctx.line();
    return new InvoiceLine(
        ctx.invoiceType(),
        lineLabel(ctx),
        line.getGrade(),
        line.getNumberOfPieces(),
        line.getVolume(),
        line.getPrice());
  }

  /** Channel-formatted line reference the templates render as {@code {0}} (or the last arg). */
  private static String lineLabel(LineItemRuleContext ctx) {
    return "Line " + ctx.lineNumber();
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
