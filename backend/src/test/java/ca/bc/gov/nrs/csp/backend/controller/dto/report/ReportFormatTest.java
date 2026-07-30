package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportFormatTest {

    @Test
    @DisplayName("getValue and getExtension")
    void valueAndExtension() {
        assertThat(ReportFormat.PDF.getValue()).isEqualTo("PDF");
        assertThat(ReportFormat.PDF.getExtension()).isEqualTo("pdf");
        assertThat(ReportFormat.CSV.getValue()).isEqualTo("CSV");
        assertThat(ReportFormat.CSV.getExtension()).isEqualTo("csv");
    }

    @Test
    @DisplayName("fromValue is case-insensitive")
    void fromValueCaseInsensitive() {
        assertThat(ReportFormat.fromValue("pdf")).isEqualTo(ReportFormat.PDF);
        assertThat(ReportFormat.fromValue("PDF")).isEqualTo(ReportFormat.PDF);
        assertThat(ReportFormat.fromValue("csv")).isEqualTo(ReportFormat.CSV);
    }

    @Test
    @DisplayName("fromValue rejects unknown formats")
    void fromValueUnknown() {
        assertThatThrownBy(() -> ReportFormat.fromValue("XLSX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown report format: XLSX");
    }
}
