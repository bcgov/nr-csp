package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.testsupport.AbstractReportIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for POST /api/R10 (log sales by species report) against the real
 * Oracle-backed pipeline: request validation (client lookup in THE.V_CLIENT_PUBLIC),
 * CSP_SP_RPT_10 ref-cursor fill, and PDF/CSV export.
 */
class R10ReportIT extends AbstractReportIT {

    @Test
    void generatesCsvFromSeededData() {
        ResponseEntity<byte[]> response = postReport("/api/R10", Map.of(
                "reportFormat", "CSV",
                "dateFrom", "20240101",
                "dateTo", "20240131",
                "sellerClientNumber", "00001001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).hasToString("text/csv");
        String disposition = response.getHeaders().getFirst("Content-Disposition");
        assertThat(disposition).contains("attachment").contains("R10_").contains(".csv");

        String csv = asText(response);
        assertThat(csv).contains("Hemlock").contains("Balsam");
    }

    @Test
    void generatesPdfFromSeededData() {
        ResponseEntity<byte[]> response = postReport("/api/R10", Map.of(
                "reportFormat", "PDF",
                "dateFrom", "20240101",
                "dateTo", "20240131"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("R10_").contains(".pdf");
        assertThat(asText(response)).startsWith("%PDF");
    }

    @Test
    void returns404WhenDateRangeMatchesNoRows() {
        ResponseEntity<byte[]> response = postReport("/api/R10", Map.of(
                "reportFormat", "CSV",
                "dateFrom", "20990101",
                "dateTo", "20991231"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returns400WhenDateFromMissing() {
        ResponseEntity<byte[]> response = postReport("/api/R10", Map.of(
                "reportFormat", "CSV"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(asText(response)).contains("VALIDATION_ERROR");
    }

    @Test
    void returns400WhenSellerClientUnknown() {
        ResponseEntity<byte[]> response = postReport("/api/R10", Map.of(
                "reportFormat", "CSV",
                "dateFrom", "20240101",
                "dateTo", "20240131",
                "sellerClientNumber", "99999999"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(asText(response)).contains("report.client.number.notfound.error");
    }
}
