package ca.bc.gov.nrs.csp.backend.submission.business.support;

import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceDetailsType;
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

  @Test
  void normalizes_detail_source_documents_in_place() {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setBoomNumbers(" b1,b2 ");
    details.setTimberMarks("t1");
    details.setWeighSlipNumbers(null);
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("inv-1");
    invoice.setCSPInvoiceDetails(details);

    normalizer.normalizeInvoiceIdentifiers(invoice);

    assertThat(details.getBoomNumbers()).isEqualTo("B1,B2");
    assertThat(details.getTimberMarks()).isEqualTo("T1");
    assertThat(details.getWeighSlipNumbers()).isNull();
  }

  @Test
  void tolerates_a_missing_details_block() {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber("inv-1");

    normalizer.normalizeInvoiceIdentifiers(invoice);

    assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-1");
  }
}
