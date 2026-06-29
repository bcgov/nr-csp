package ca.bc.gov.nrs.csp.backend.submission.business.referencedata;

import ca.bc.gov.nrs.csp.backend.repository.FlatPriceConversionRepository;
import ca.bc.gov.nrs.csp.backend.repository.InvoiceRepository;
import ca.bc.gov.nrs.csp.backend.repository.ValidationLookupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Confirms the facade delegates to the right repository and the only two bits of
 * logic at this layer: the C3 "maturity M → O" substitution and the
 * RelatedInvoice → InvoiceRef mapping. Simple delegations are spot-checked.
 */
@ExtendWith(MockitoExtension.class)
class DefaultReferenceDataServiceTest {

  @Mock ValidationLookupRepository validationLookups;
  @Mock InvoiceRepository invoices;
  @Mock FlatPriceConversionRepository flatPriceConversions;

  @InjectMocks DefaultReferenceDataService service;

  private static final LocalDate DATE = LocalDate.of(2024, 1, 15);

  @Test
  void delegates_client_location_lookup() {
    given(validationLookups.existsClientLocation("100", "00")).willReturn(true);
    assertThat(service.clientLocationExists("100", "00")).isTrue();
  }

  @Test
  void conversion_factor_substitutes_M_with_O() {
    given(flatPriceConversions.findApplicableFactor("O", "SC", "SP", "G", DATE))
        .willReturn(Optional.of(42));

    assertThat(service.conversionFactor("M", "SC", "SP", "G", DATE)).contains(42);
  }

  @Test
  void conversion_factor_passes_other_maturities_through() {
    given(flatPriceConversions.findApplicableFactor("S", "SC", "SP", "G", DATE))
        .willReturn(Optional.empty());

    assertThat(service.conversionFactor("S", "SC", "SP", "G", DATE)).isEmpty();
  }

  @Test
  void findInvoices_maps_number_and_status() {
    given(invoices.findByInvoiceNoAndClient("INV-1", "100", "00"))
        .willReturn(List.of(new InvoiceRepository.RelatedInvoice(1L, "CAN")));

    assertThat(service.findInvoices("INV-1", "100", "00"))
        .singleElement()
        .satisfies(ref -> {
          assertThat(ref.invoiceNumber()).isEqualTo("INV-1");
          assertThat(ref.statusCode()).isEqualTo("CAN");
        });
  }

  @Test
  void month_complete_excludes_no_current_invoice() {
    given(invoices.isMonthCompleted(DATE, "100", "00", null)).willReturn(true);
    assertThat(service.isMonthComplete(DATE, "100", "00")).isTrue();
  }
}
