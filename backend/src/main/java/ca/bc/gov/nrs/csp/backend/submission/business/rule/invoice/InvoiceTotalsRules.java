package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.rules.Finding;
import ca.bc.gov.nrs.csp.backend.invoice.rules.InvoiceTotals;
import ca.bc.gov.nrs.csp.backend.invoice.rules.InvoiceTotalsRuleSet;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Electronic-path glue for the totals rules (catalogue §2.5, I24–I29). The
 * logic lives in the channel-agnostic {@link InvoiceTotalsRuleSet} (refactor
 * doc §3–§4); this adapter maps the JAXB invoice onto {@link InvoiceTotals},
 * runs the shared rule set, and forwards each {@link Finding} to the collector
 * as a message key + template args (resolved at the HTTP boundary, doc §3.5).
 */
@Component
public class InvoiceTotalsRules implements InvoiceRule {

  @Override
  public void validate(InvoiceRuleContext ctx) {
    for (Finding f : InvoiceTotalsRuleSet.validate(toTotals(ctx))) {
      // NB: f.severity() is invoice.rules.Severity — NOT submission.shared.Severity.
      // Map by branching to the two ctx sinks.
      if (f.severity() == ca.bc.gov.nrs.csp.backend.invoice.rules.Severity.ERROR) {
        ctx.error(f.code(), f.args());
      } else {
        ctx.warning(f.code(), f.args());
      }
    }
  }

  private static InvoiceTotals toTotals(InvoiceRuleContext ctx) {
    List<InvoiceTotals.Line> lines = new ArrayList<>();
    for (CSPLineItemType li : ctx.invoice().getCSPLineItem()) {
      lines.add(new InvoiceTotals.Line(li.getVolume(), li.getPrice(), li.getNumberOfPieces()));
    }
    return new InvoiceTotals(
        ctx.invoiceType(),
        ctx.totalAmount(), ctx.totalVolume(), ctx.totalPieces(), lines);
  }
}
