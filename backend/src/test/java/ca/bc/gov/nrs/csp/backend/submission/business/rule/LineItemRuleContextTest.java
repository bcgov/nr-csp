package ca.bc.gov.nrs.csp.backend.submission.business.rule;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the line-item rule context: the parent-invoice and resolved
 * submitter accessors line rules rely on.
 */
class LineItemRuleContextTest {

  @Test
  void invoice_returns_the_parent_invoice() {
    CSPInvoiceType invoice = invoice();
    LineItemRuleContext ctx = context(invoice, null);

    assertThat(ctx.invoice()).isSameAs(invoice);
  }

  @Test
  void submitter_returns_the_resolved_submitter() {
    SubmitterInfo submitter = new SubmitterInfo(
        SubmitterInfo.SubmittedBy.SELLER,
        "100", "00",
        "200", "01",
        "Other Co", "Victoria", "BC",
        "100", "00");
    LineItemRuleContext ctx = context(invoice(), submitter);

    assertThat(ctx.submitter()).isSameAs(submitter);
  }

  @Test
  void line_and_line_number_return_the_scoped_line() {
    CSPLineItemType line = new CSPLineItemType();
    LineItemRuleContext ctx =
        new LineItemRuleContext(invoice(), line, 0, 3, null, null, new ValidationCollector());

    assertThat(ctx.line()).isSameAs(line);
    assertThat(ctx.lineNumber()).isEqualTo(3);
  }

  // --- helpers ----------------------------------------------------------------

  private static CSPInvoiceType invoice() {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType("SAL");
    return invoice;
  }

  private static LineItemRuleContext context(CSPInvoiceType invoice, SubmitterInfo submitter) {
    return new LineItemRuleContext(
        invoice, new CSPLineItemType(), 0, 1, submitter, null, new ValidationCollector());
  }
}
