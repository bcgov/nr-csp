package ca.bc.gov.nrs.csp.backend.submission.business.support;

import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentifierNormalizerTest {

  private final IdentifierNormalizer normalizer = new IdentifierNormalizer();

  @Test
  void trims_and_uppercases() {
    assertThat(normalizer.normalize("  inv-1 ")).isEqualTo("INV-1");
  }

  @Test
  void null_is_null() {
    assertThat(normalizer.normalize(null)).isNull();
  }

  @Test
  void normalizes_invoice_identifiers_in_place() {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber(" inv-1 ");
    invoice.setReplacesInvoiceNumbers("a,b");
    invoice.setAdjustsInvoiceNumbers(null);

    normalizer.normalizeInvoiceIdentifiers(invoice);

    assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-1");
    assertThat(invoice.getReplacesInvoiceNumbers()).isEqualTo("A,B");
    assertThat(invoice.getAdjustsInvoiceNumbers()).isNull();
  }
}
