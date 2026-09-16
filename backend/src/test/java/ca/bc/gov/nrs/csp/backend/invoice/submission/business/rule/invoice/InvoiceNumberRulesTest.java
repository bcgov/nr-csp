package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.invoice;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.ValidationCollector;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.support.IdentifierNormalizer;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The invoice number may carry only uppercase letters, digits and hyphens.
 * Rejection and normalisation are two different answers, so the cases that the
 * normaliser already fixes (lower case, surrounding whitespace) are exercised
 * through it, exactly as production runs them.
 */
class InvoiceNumberRulesTest {

  private final InvoiceNumberRules rules = new InvoiceNumberRules();
  private final IdentifierNormalizer normalizer = new IdentifierNormalizer();

  @ParameterizedTest
  @ValueSource(strings = {"INV-001", "INV001", "123", "A-1-2", "-", "INVOICE-0000001"})
  void accepts_uppercase_digits_and_hyphens(String invoiceNumber) {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceNumberValid(context(collector, invoiceNumber));

    assertThat(collector.entries()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"INV 001", "INV_001", "INV#001", "INV.001", "INV/001", "INV*", "INV+1", "INV(1)", "INV,1"})
  void rejects_spaces_and_special_characters(String invoiceNumber) {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceNumberValid(context(collector, invoiceNumber));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.number.pattern.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @ParameterizedTest
  @ValueSource(strings = {"inv-001", " inv-001 ", "\n  Inv-001\n"})
  void accepts_lower_case_and_padding_because_the_parse_step_canonicalises_it(String asFiled) {
    // The value reaches the rules already trimmed and upper-cased, so these are
    // normalised on upload rather than rejected.
    ValidationCollector collector = new ValidationCollector();
    CSPInvoiceType invoice = invoice(asFiled);
    normalizer.normalizeInvoice(invoice);

    rules.invoiceNumberValid(context(collector, invoice));

    assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-001");
    assertThat(collector.entries()).isEmpty();
  }

  @Test
  void reports_a_blank_as_missing_not_as_a_bad_character() {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceNumberValid(context(collector, ""));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.number.required.error");
    assertThat(collector.entries().get(0).error().severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void reports_null_as_missing() {
    ValidationCollector collector = new ValidationCollector();

    rules.invoiceNumberValid(context(collector, (String) null));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.number.required.error");
  }

  @Test
  void validate_runs_the_rule() {
    ValidationCollector collector = new ValidationCollector();

    rules.validate(context(collector, "INV 1"));

    assertThat(collector.entries()).hasSize(1);
    assertThat(collector.entries().get(0).error().code()).isEqualTo("invoice.number.pattern.error");
  }

  private static CSPInvoiceType invoice(String invoiceNumber) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber(invoiceNumber);
    return invoice;
  }

  private static InvoiceRuleContext context(ValidationCollector collector, String invoiceNumber) {
    return context(collector, invoice(invoiceNumber));
  }

  private static InvoiceRuleContext context(ValidationCollector collector, CSPInvoiceType invoice) {
    return new InvoiceRuleContext(new CSPSubmissionType(), invoice, 0, null, null, collector);
  }
}
