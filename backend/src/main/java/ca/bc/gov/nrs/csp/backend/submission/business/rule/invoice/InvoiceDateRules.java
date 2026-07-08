package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class InvoiceDateRules implements InvoiceRule {

  private final Clock clock;

  public InvoiceDateRules(Clock clock) {
    this.clock = clock;
  }

  @Override
  public void validate(InvoiceRuleContext ctx) {
    dateNotInFuture(ctx);
    invoiceMonthCompleted(ctx);
  }

  /** The invoice date cannot be in the future. */
  void dateNotInFuture(InvoiceRuleContext ctx) {
    LocalDate invoiceDate = ctx.invoiceDate();
    if (invoiceDate != null && invoiceDate.isAfter(LocalDate.now(clock))) {
      ctx.error(
          "invoice.date.in.future.error",
          "invoiceDate for invoiceNumber " + ctx.invoiceNumber() + " cannot be in the future.");
    }
  }

  /** Invoice date must not be in a month already marked complete. */
  void invoiceMonthCompleted(InvoiceRuleContext ctx) {
    LocalDate invoiceDate = ctx.invoiceDate();
    String clientNumber = ctx.submitter().submitterClientNumber();
    String clientLocation = ctx.submitter().submitterLocnCode();

    boolean isMonthCompleted = ctx.referenceData().isMonthComplete(invoiceDate, clientNumber, clientLocation);

    if (isMonthCompleted) {
      ctx.warning(
          "invoice.month.completed.warning",
          "invoiceNumber " + ctx.invoiceNumber() + " has an invoiceDate in a month already marked complete.");
    }
  }
}
