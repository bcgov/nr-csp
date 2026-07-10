package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Template for testing an invoice rule: call the rule's method directly, build a
 * context, assert on the collector. This rule is a pure check against the parsed
 * line-item list, so it needs neither a clock nor reference-data.
 */
class InvoiceLineItemPresenceRulesTest {

  private final InvoiceLineItemPresenceRules rules = new InvoiceLineItemPresenceRules();

  @Test
  void validate_runs_the_line_item_check() {
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, false));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.noline.item.error");
  }

  @Test
  void lineItemExists_errors_when_the_line_item_list_is_null() {
    ValidationCollector collector = new ValidationCollector();
    CSPInvoiceType invoice = new CSPInvoiceType() {
      @Override
      public List<CSPLineItemType> getCSPLineItem() {
        return null;
      }
    };
    invoice.setInvoiceNumber("INV-1");

    rules.lineItemExists(
        new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.noline.item.error");
  }

  @Test
  void lineItemExists_errors_when_there_are_no_line_items() {
    ValidationCollector collector = new ValidationCollector();

    rules.lineItemExists(context(collector, false));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.noline.item.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
    assertThat(collector.entries().get(0).invoiceIndex()).isZero();
    assertThat(collector.entries().get(0).error().path()).isEqualTo("invoice #1 (INV-1)");
  }

  @Test
  void lineItemExists_passes_when_at_least_one_line_item_is_present() {
    ValidationCollector collector = new ValidationCollector();

    rules.lineItemExists(context(collector, true));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext context(ValidationCollector collector, boolean withLineItem) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    if (withLineItem) {
      invoice.getCSPLineItem().add(new CSPLineItemType());
    }
    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector);
  }
}
