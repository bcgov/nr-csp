package ca.bc.gov.nrs.csp.backend.invoice.manual;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.InvoiceTotals;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ManualInvoiceTotals} — the manual channel's derivation of
 * the header totals from the invoice's line items.
 */
class ManualInvoiceTotalsTest {

    // --- fixtures ---

    /** Claims $1250.75 / 100 pieces / 12.5 m³, none of which matches one `lineItem()`. */
    private static InvoiceDetails invoiceDetails(String invType) {
        return new InvoiceDetails(
                12345L, "INV-2026-001", LocalDate.of(2026, Month.MAY, 19), "DFT", invType,
                "M", "FOB01", "A", "A",
                new BigDecimal("1250.75"), 100, new BigDecimal("12.5"),
                "00001234", "00", "Seller", "00001234", "01",
                "00005678", "02", "ABC Logging Ltd.", "Nanaimo", "BC",
                List.of("B123"), List.of("TM1"), List.of("WS1"),
                null, null, "review it", "submitted via UI", "entryUser");
    }

    private static LineItem lineItem(String volume, String price, Integer pieces) {
        return new LineItem(1L, 12345L, "SORT01", "CLIENT-SORT", "SP1", "Cedar", "G1",
                pieces,
                price == null ? null : new BigDecimal(price),
                volume == null ? null : new BigDecimal(volume),
                null, null);
    }

    // --- withCalculatedTotals ---

    @Test
    void withCalculatedTotals_replacesOnlyTheTotals() {
        InvoiceDetails details = invoiceDetails("SAL");
        List<LineItem> lines = List.of(
                lineItem("6.25", "25.00", 50),
                lineItem("6.25", "25.00", 50));

        InvoiceDetails result = ManualInvoiceTotals.withCalculatedTotals(details, lines);

        // Two lines of 50 pieces, 6.25 m³ @ $25.00 → $312.50 / 100 pieces / 12.50 m³.
        assertThat(result.totalAmt()).isEqualByComparingTo("312.50");
        assertThat(result.totalPieces()).isEqualTo(100);
        assertThat(result.totalVol()).isEqualByComparingTo("12.50");
        // Every other field is carried over untouched.
        assertThat(result).usingRecursiveComparison()
                .ignoringFields("totalAmt", "totalPieces", "totalVol")
                .isEqualTo(details);
    }

    @Test
    void withCalculatedTotals_noLines_zeroesTheTotals() {
        InvoiceDetails result = ManualInvoiceTotals.withCalculatedTotals(invoiceDetails("SAL"), List.of());

        assertThat(result.totalAmt()).isEqualByComparingTo("0.00");
        assertThat(result.totalPieces()).isZero();
        assertThat(result.totalVol()).isEqualByComparingTo("0");
    }

    @Test
    void withCalculatedTotals_adjustment_keepsTheAmountNegative() {
        // ADJ is the one type allowed a negative volume and price, and its amount
        // has to stay negative rather than becoming a positive product.
        InvoiceDetails result = ManualInvoiceTotals.withCalculatedTotals(
                invoiceDetails("ADJ"), List.of(lineItem("-10", "-10.00", -5)));

        assertThat(result.totalAmt()).isEqualByComparingTo("-100.00");
        assertThat(result.totalVol()).isEqualByComparingTo("-10");
        assertThat(result.totalPieces()).isEqualTo(-5);
    }

    // --- toTotalsLines ---

    @Test
    void toTotalsLines_nullListIsEmpty() {
        assertThat(ManualInvoiceTotals.toTotalsLines(null)).isEmpty();
    }

    @Test
    void toTotalsLines_nullPiecesCountAsZero() {
        assertThat(ManualInvoiceTotals.toTotalsLines(List.of(lineItem("6.25", "25.00", null))))
                .containsExactly(new InvoiceTotals.Line(new BigDecimal("6.25"), new BigDecimal("25.00"), 0));
    }
}
