package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * §2.7 Date &amp; month rules. One method per rule; add a call in
 * {@link #validate} in catalogue order (see
 * {@code docs/submission-validation-business-rules.md}).
 *
 * <ul>
 *   <li>I39 — invoice date cannot be in the future (ERROR) — <b>implemented; the worked template</b></li>
 *   <li>I40 — invoice date in a month flagged Complete (WARNING) — uses {@code ctx.referenceData()}</li>
 * </ul>
 *
 * <p>This is the template section: copy {@link #dateNotInFuture} when adding a
 * rule — a package-private {@code void xxx(InvoiceRuleContext ctx)} method
 * header-commented with its catalogue ID, then a call to it in {@link #validate}.
 * Date-sensitive rules read "today" from the injected {@link Clock} (zoned to BC
 * business time) rather than {@code LocalDate.now()}, so the zone is explicit and
 * the rule is deterministically testable with a fixed clock.
 */
@Component
public class InvoiceDateRules implements InvoiceRule {

  private final Clock clock;

  public InvoiceDateRules(Clock clock) {
    this.clock = clock;
  }

  @Override
  public void validate(InvoiceRuleContext ctx) {
    dateNotInFuture(ctx); // I39
    // TODO: I40 — month-complete warning.
  }

  /** I39 — the invoice date cannot be in the future. */
  void dateNotInFuture(InvoiceRuleContext ctx) {
    LocalDate invoiceDate = ctx.invoiceDate();
    if (invoiceDate != null && invoiceDate.isAfter(LocalDate.now(clock))) {
      ctx.error(
          "invoice.date.in.future.error",
          "invoiceDate for invoiceNumber " + ctx.invoiceNumber() + " cannot be in the future.");
    }
  }
}
