package ca.bc.gov.nrs.csp.backend.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.shared.Severity;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Glue smoke test (refactor doc §8): the exhaustive I24–I29 matrix lives in the
 * core {@code InvoiceTotalsRuleSetTest}. Here we only prove the adapter — the
 * JAXB invoice maps onto {@code InvoiceTotals}, findings surface through
 * {@link InvoiceRuleContext}, and the core severity maps onto the collector's
 * ERROR / WARNING sinks with the message key + template args (doc §3.5).
 */
class InvoiceTotalsRulesTest {

  private final InvoiceTotalsRules rules = new InvoiceTotalsRules();

  @Test
  void error_finding_surfaces_through_the_collector_with_code_and_args() {
    ValidationCollector collector = new ValidationCollector();

    // Negative total amount → I24 ERROR (and I25 stays quiet: |0 − (−1.00)| ≤ 5).
    rules.validate(context(collector, "SAL", new BigDecimal("-1.00"), null, null));

    assertThat(collector.entries()).hasSize(1);
    SubmissionValidationError err = collector.entries().get(0).error();
    assertThat(err.code()).isEqualTo("invoice.totalamount.negative.error");
    assertThat(err.severity()).isEqualTo(Severity.ERROR);
    assertThat(err.args()).isEmpty();
    assertThat(err.message()).isNull();
    assertThat(err.path()).contains("INV-1");
  }

  @Test
  void warning_finding_surfaces_with_the_submitted_value_as_template_arg() {
    ValidationCollector collector = new ValidationCollector();

    // Σ(10 × 10) = 100.00 calculated vs submitted 90.00 → I25 WARNING. The
    // submitted volume matches the line's Σ so I27 stays quiet (the null-submitted
    // variance check runs under the legacy behaviour, doc §7.1 #3).
    rules.validate(context(collector, "SAL", new BigDecimal("90.00"), new BigDecimal("10"), null,
        line("10", "10", 0)));

    assertThat(collector.entries()).hasSize(1);
    SubmissionValidationError err = collector.entries().get(0).error();
    assertThat(err.code()).isEqualTo("invoice.totalamount.dismatch.warning");
    assertThat(err.severity()).isEqualTo(Severity.WARNING);
    assertThat(err.args()).containsExactly(new BigDecimal("90.00"));
  }

  @Test
  void consistent_invoice_produces_no_findings() {
    ValidationCollector collector = new ValidationCollector();

    // Amount 100.00, volume 10, pieces 3 — all match the single line's Σ.
    rules.validate(context(collector, "SAL",
        new BigDecimal("100.00"), new BigDecimal("10"), 3,
        line("10", "10", 3)));

    assertThat(collector.entries()).isEmpty();
  }

  private InvoiceRuleContext context(ValidationCollector collector, String invoiceType,
      BigDecimal totalAmount, BigDecimal totalVolume, Integer totalPieces, CSPLineItemType... lines) {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setTotalAmount(totalAmount);
    details.setTotalVolume(totalVolume);
    details.setTotalPieces(totalPieces);

    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("INV-1");
    invoice.setInvoiceType(invoiceType);
    invoice.setCSPInvoiceDetails(details);
    for (CSPLineItemType line : lines) {
      invoice.getCSPLineItem().add(line);
    }

    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector);
  }

  private static CSPLineItemType line(String volume, String price, int pieces) {
    CSPLineItemType line = new CSPLineItemType();
    line.setVolume(new BigDecimal(volume));
    line.setPrice(new BigDecimal(price));
    line.setNumberOfPieces(pieces);
    return line;
  }
}
