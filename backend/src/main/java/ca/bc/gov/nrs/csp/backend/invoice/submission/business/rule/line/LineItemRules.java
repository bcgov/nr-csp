package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.line;

import ca.bc.gov.nrs.csp.backend.invoice.shared.model.Finding;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.InvoiceLine;
import ca.bc.gov.nrs.csp.backend.invoice.shared.rules.InvoiceLineRuleSet;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.LineItemRule;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.LineItemRuleContext;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import org.springframework.stereotype.Component;

/**
 * Line-item business rules. The reference-data rules
 * live here — they need {@code ctx.referenceData()} and stay
 * channel-side. The pure value rules are delegated to the shared
 * channel-agnostic {@link InvoiceLineRuleSet} — the same core
 * the manual path runs, so the two channels cannot drift; each {@link Finding}
 * is forwarded as a message key + template args.
 */
@Component
public class LineItemRules implements LineItemRule {

  @Override
  public void validate(LineItemRuleContext ctx) {
    secondarySortCodeValid(ctx); // L1
    speciesGradeCombinationValid(ctx); // L2
    for (Finding f : InvoiceLineRuleSet.validate(toLine(ctx))) { // L3–L9
      // NB: f.severity() is invoice.shared.model.Severity — NOT submission.shared.Severity.
      if (f.severity() == ca.bc.gov.nrs.csp.backend.invoice.shared.model.Severity.ERROR) {
        ctx.error(f.code(), f.args());
      } else {
        ctx.warning(f.code(), f.args());
      }
    }
  }

  /**
   * Secondary sort code is required, and must be a recognised code active on the
   * invoice date. A missing value is reported as such rather than as an
   * unrecognised code — the schema lets the element be present but empty, so a
   * blank here means "not supplied", not "wrong". Template: code, date, line label.
   */
  void secondarySortCodeValid(LineItemRuleContext ctx) {
    String sortCode = ctx.line().getSecondarySortCode();
    if (isBlank(sortCode)) {
      ctx.error("invoice.secondry.sortcode.required.error", new Object[] {lineLabel(ctx)});
      return;
    }
    if (!ctx.referenceData().sortCodeValidOn(sortCode, ctx.invoiceDate())) {
      ctx.error("invoice.secondry.sortcode.invalid.error",
          new Object[] {sortCode, ctx.invoiceDate(), lineLabel(ctx)});
    }
  }

  /**
   * Species is required and, together with grade, must exist in
   * CSP_SPECIES_GRADE_XREF. Template: species, grade, line label.
   *
   * <p>Either code being blank is reported as that field being required — species
   * here, grade by {@link InvoiceLineRuleSet} — and the combination lookup is
   * skipped. Running it on a blank would only be able to report the pair as
   * unknown, which is what hid the missing field in the first place.
   */
  void speciesGradeCombinationValid(LineItemRuleContext ctx) {
    String species = ctx.line().getSpecies();
    String grade = ctx.line().getGrade();
    if (isBlank(species)) {
      ctx.error("invoice.species.required.error", new Object[] {lineLabel(ctx)});
    }
    if (isBlank(species) || isBlank(grade)) {
      return;
    }
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
