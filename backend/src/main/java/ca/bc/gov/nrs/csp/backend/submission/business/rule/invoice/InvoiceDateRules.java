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

  /** The invoice date cannot be in the future. Template: invoice number, latest permitted date. */
  void dateNotInFuture(InvoiceRuleContext ctx) {
    LocalDate today = LocalDate.now(clock);
    LocalDate invoiceDate = ctx.invoiceDate();
    if (invoiceDate != null && invoiceDate.isAfter(today)) {
      ctx.error("invoice.date.in.future.error", new Object[] {ctx.invoiceNumber(), today});
    }
  }

  /** Invoice date must not be in a month already marked complete. Template: invoice number, date. */
  void invoiceMonthCompleted(InvoiceRuleContext ctx) {
    LocalDate invoiceDate = ctx.invoiceDate();
    String clientNumber = ctx.submitter().submitterClientNumber();
    String clientLocation = ctx.submitter().submitterLocnCode();

    boolean isMonthCompleted = ctx.referenceData().isMonthComplete(invoiceDate, clientNumber, clientLocation);

    if (isMonthCompleted) {
      ctx.warning("invoice.month.completed.warning", new Object[] {ctx.invoiceNumber(), invoiceDate});
    }
  }
}
