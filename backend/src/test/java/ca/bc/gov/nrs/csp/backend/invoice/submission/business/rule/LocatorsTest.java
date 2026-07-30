package ca.bc.gov.nrs.csp.backend.invoice.submission.business.rule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocatorsTest {

  @Test
  void invoice_uses_one_based_position_and_number() {
    assertThat(Locators.invoice(0, "INV-1")).isEqualTo("invoice #1 (INV-1)");
  }

  @Test
  void invoice_omits_parens_when_number_is_blank_or_null() {
    assertThat(Locators.invoice(1, "   ")).isEqualTo("invoice #2");
    assertThat(Locators.invoice(1, null)).isEqualTo("invoice #2");
  }

  @Test
  void line_appends_line_number() {
    assertThat(Locators.line(0, "INV-1", 3)).isEqualTo("invoice #1 (INV-1), line 3");
  }
}
