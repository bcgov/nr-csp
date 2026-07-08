package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.CreateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.UpdateInvoiceRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the I31 source-document de-duplication applied in
 * {@link InvoiceMapper#toDetails}: tokens are trimmed, blanks dropped, and
 * duplicates removed keeping first-seen order — mirroring the electronic
 * path's {@code InvoiceSourceDocumentRules#deduplicateCsv} and the legacy
 * {@code ignoreCSVForDuplicates}, so the validator and the persisted log
 * sources both see the de-duplicated values.
 */
class InvoiceMapperTest {

    // toDetails does not touch the MessageSource (only toResponse does).
    private final InvoiceMapper mapper = new InvoiceMapper(null);

    @Test
    void i31_create_dedupsBoomNumbers_keepingFirstSeenOrder() {
        InvoiceDetails details = mapper.toDetails(
                createRequest(List.of("B1", "B2", "B1", "B3", "B2"), null, null), "user");

        assertThat(details.boomNumbers()).containsExactly("B1", "B2", "B3");
    }

    @Test
    void i31_create_trimsTokensAndDedupsOnTrimmedValue() {
        InvoiceDetails details = mapper.toDetails(
                createRequest(List.of(" B1 ", "B1", "B2 "), null, null), "user");

        assertThat(details.boomNumbers()).containsExactly("B1", "B2");
    }

    @Test
    void i31_create_dropsBlankAndNullTokens() {
        InvoiceDetails details = mapper.toDetails(
                createRequest(Arrays.asList("B1", "  ", null, "B2"), null, null), "user");

        assertThat(details.boomNumbers()).containsExactly("B1", "B2");
    }

    @Test
    void i31_create_nullListsMapToEmptyLists() {
        InvoiceDetails details = mapper.toDetails(createRequest(null, null, null), "user");

        assertThat(details.boomNumbers()).isEmpty();
        assertThat(details.timberMarks()).isEmpty();
        assertThat(details.weightSlips()).isEmpty();
    }

    @Test
    void i31_create_dedupsTimberMarksAndWeighSlipsToo() {
        InvoiceDetails details = mapper.toDetails(
                createRequest(null, List.of("T1", "T1"), List.of("W1", " W1", "W2")), "user");

        assertThat(details.timberMarks()).containsExactly("T1");
        assertThat(details.weightSlips()).containsExactly("W1", "W2");
    }

    @Test
    void i31_update_dedupsAllThreeSourceDocumentLists() {
        InvoiceDetails details = mapper.toDetails(
                updateRequest(List.of("B1", "B1"), List.of("T1", "T1"), List.of("W1", "W1")),
                7L, "DFT", "user");

        assertThat(details.boomNumbers()).containsExactly("B1");
        assertThat(details.timberMarks()).containsExactly("T1");
        assertThat(details.weightSlips()).containsExactly("W1");
        assertThat(details.invID()).isEqualTo(7L);
    }

    @Test
    void i31_distinctValuesPassThroughUnchanged() {
        InvoiceDetails details = mapper.toDetails(
                createRequest(List.of("B1", "B2", "B3"), null, null), "user");

        assertThat(details.boomNumbers()).containsExactly("B1", "B2", "B3");
    }

    private static CreateInvoiceRequest createRequest(
            List<String> boomNumbers, List<String> timberMarks, List<String> weightSlips) {
        return new CreateInvoiceRequest(
                "INV-1", LocalDate.of(2026, 5, 19), "SAL",
                null, null, null, null, null, null,
                "00001234", "00", "Seller",
                null, null, null, null, null, null, null,
                boomNumbers, timberMarks, weightSlips,
                null, null, null, null,
                true, List.of());
    }

    private static UpdateInvoiceRequest updateRequest(
            List<String> boomNumbers, List<String> timberMarks, List<String> weightSlips) {
        return new UpdateInvoiceRequest(
                "INV-1", LocalDate.of(2026, 5, 19), "SAL",
                null, null, null, null, null, null,
                "00001234", "00", "Seller",
                null, null, null, null, null, null, null,
                boomNumbers, timberMarks, weightSlips,
                null, null, null, null,
                true, List.of());
    }
}
