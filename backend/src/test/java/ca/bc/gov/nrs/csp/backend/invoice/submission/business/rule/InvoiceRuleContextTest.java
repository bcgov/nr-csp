package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.Severity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the invoice rule context: the parent-submission accessor and
 * the null-safe convenience accessors over the (optional) invoice details.
 */
class InvoiceRuleContextTest {

  @Test
  void submission_returns_the_parent_submission() {
    CSPSubmissionType submission = new CSPSubmissionType();
    InvoiceRuleContext ctx = context(submission, invoice(null), new ValidationCollector());

    assertThat(ctx.submission()).isSameAs(submission);
  }

  @Test
  void details_accessors_return_values_when_details_present() {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setTotalAmount(new BigDecimal("100.00"));
    details.setTotalVolume(new BigDecimal("25.5"));
    details.setTotalPieces(7);
    details.setMaturity("M");
    details.setPrimarySortCode("PS");
    InvoiceRuleContext ctx =
        context(new CSPSubmissionType(), invoice(details), new ValidationCollector());

    assertThat(ctx.totalAmount()).isEqualByComparingTo("100.00");
    assertThat(ctx.totalVolume()).isEqualByComparingTo("25.5");
    assertThat(ctx.totalPieces()).isEqualTo(7);
    assertThat(ctx.maturity()).isEqualTo("M");
    assertThat(ctx.primarySortCode()).isEqualTo("PS");
  }

  @Test
  void details_accessors_return_null_when_details_absent() {
    InvoiceRuleContext ctx =
        context(new CSPSubmissionType(), invoice(null), new ValidationCollector());

    assertThat(ctx.totalAmount()).isNull();
    assertThat(ctx.totalVolume()).isNull();
    assertThat(ctx.totalPieces()).isNull();
    assertThat(ctx.maturity()).isNull();
    assertThat(ctx.primarySortCode()).isNull();
  }

  @Test
  void error_records_an_error_scoped_to_the_invoice_index() {
    ValidationCollector collector = new ValidationCollector();
    InvoiceRuleContext ctx = context(new CSPSubmissionType(), invoice(null), collector);

    ctx.error("some.error.code", new Object[0]);

    assertThat(collector.entries()).hasSize(1);
    ValidationCollector.Entry entry = collector.entries().get(0);
    assertThat(entry.invoiceIndex()).isZero();
    assertThat(entry.error().path()).isEqualTo("invoice #1 (INV-1)");
    assertThat(entry.error().code()).isEqualTo("some.error.code");
    assertThat(entry.error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void warning_records_a_warning_scoped_to_the_invoice_index() {
    ValidationCollector collector = new ValidationCollector();
    InvoiceRuleContext ctx = context(new CSPSubmissionType(), invoice(null), collector);

    ctx.warning("some.warning.code", new Object[0]);

    assertThat(collector.entries()).hasSize(1);
    ValidationCollector.Entry entry = collector.entries().get(0);
    assertThat(entry.invoiceIndex()).isZero();
    assertThat(entry.error().path()).isEqualTo("invoice #1 (INV-1)");
    assertThat(entry.error().code()).isEqualTo("some.warning.code");
    assertThat(entry.error().severity()).isEqualTo(Severity.WARNING);
  }

  // --- helpers ----------------------------------------------------------------

  private static CSPInvoiceType invoice(CSPInvoiceDetailsType details) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setCSPInvoiceDetails(details);
    return invoice;
  }

  private static InvoiceRuleContext context(
      CSPSubmissionType submission, CSPInvoiceType invoice, ValidationCollector collector) {
    return new InvoiceRuleContext(submission, invoice, 0, null, null, collector);
  }
}
