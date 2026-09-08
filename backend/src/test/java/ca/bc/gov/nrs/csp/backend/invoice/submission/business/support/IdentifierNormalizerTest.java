package ca.bc.gov.nrs.csp.backend.invoice.submission.business.support;

import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceDetailsType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.invoice.submission.generated.CSPSubmissionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

  @Test
  void normalizes_coded_fields_in_place() {
    CSPInvoiceDetailsType details = new CSPInvoiceDetailsType();
    details.setMaturity(" o ");
    details.setPrimarySortCode("g");
    details.setLocationFOB("porv");
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceType("sal");
    invoice.setCSPInvoiceDetails(details);
    invoice.getCSPLineItem().add(line("a", "fi", "b"));

    normalizer.normalizeInvoiceCodes(invoice);

    assertThat(invoice.getInvoiceType()).isEqualTo("SAL");
    assertThat(details.getMaturity()).isEqualTo("O");
    assertThat(details.getPrimarySortCode()).isEqualTo("G");
    assertThat(details.getLocationFOB()).isEqualTo("PORV");
    CSPLineItemType normalized = invoice.getCSPLineItem().get(0);
    assertThat(normalized.getSecondarySortCode()).isEqualTo("A");
    assertThat(normalized.getSpecies()).isEqualTo("FI");
    assertThat(normalized.getGrade()).isEqualTo("B");
  }

  @Test
  void leaves_absent_codes_null() {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setCSPInvoiceDetails(new CSPInvoiceDetailsType());
    invoice.getCSPLineItem().add(new CSPLineItemType());

    normalizer.normalizeInvoiceCodes(invoice);

    assertThat(invoice.getInvoiceType()).isNull();
    assertThat(invoice.getCSPInvoiceDetails().getMaturity()).isNull();
    assertThat(invoice.getCSPLineItem().get(0).getSpecies()).isNull();
  }

  @Test
  void normalizes_every_invoice_in_a_submission() {
    CSPSubmissionType submission = new CSPSubmissionType();
    submission.getCSPInvoice().add(codedInvoice("inv-1", "sal", "fi"));
    submission.getCSPInvoice().add(codedInvoice("inv-2", "pur", "he"));

    normalizer.normalizeSubmission(submission);

    assertThat(submission.getCSPInvoice()).extracting(CSPInvoiceType::getInvoiceNumber)
        .containsExactly("INV-1", "INV-2");
    assertThat(submission.getCSPInvoice()).extracting(CSPInvoiceType::getInvoiceType)
        .containsExactly("SAL", "PUR");
    assertThat(submission.getCSPInvoice())
        .extracting(i -> i.getCSPLineItem().get(0).getSpecies())
        .containsExactly("FI", "HE");
  }

  @Test
  void ignores_a_tree_that_is_not_a_submission() {
    // The structural phase hands the tree back as Object and leaves it null on a
    // parse failure, so both reach here; tolerating them is the whole behaviour
    // under test, which is why the assertions are on the absence of a throw.
    assertThatCode(() -> normalizer.normalizeSubmission(null)).doesNotThrowAnyException();
    assertThatCode(() -> normalizer.normalizeSubmission("not a submission")).doesNotThrowAnyException();
  }

  private static CSPInvoiceType codedInvoice(String number, String type, String species) {
    CSPInvoiceType invoice = new CSPInvoiceType();
    invoice.setInvoiceNumber(number);
    invoice.setInvoiceType(type);
    invoice.getCSPLineItem().add(line("a", species, "b"));
    return invoice;
  }

  private static CSPLineItemType line(String secondarySortCode, String species, String grade) {
    CSPLineItemType line = new CSPLineItemType();
    line.setSecondarySortCode(secondarySortCode);
    line.setSpecies(species);
    line.setGrade(grade);
    return line;
  }
}
