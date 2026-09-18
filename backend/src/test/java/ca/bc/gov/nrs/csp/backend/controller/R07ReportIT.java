package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.testsupport.AbstractReportIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for POST /api/R07 (reconciliation report) against the real Oracle-backed
 * pipeline: seller resolution through THE.V_CLIENT_PUBLIC/CLIENT_LOCATION and the
 * CSP_SP_RPT_07 stub procedure.
 */
class R07ReportIT extends AbstractReportIT {

    @Test
    void generatesCsvFromSeededData() {
        ResponseEntity<byte[]> response = postReport("/api/R07", Map.of(
                "reportFormat", "CSV",
                "dateFrom", "20240101",
                "dateTo", "20240131",
                "sellerClientNumber", "00001001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).hasToString("text/csv");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("R07_").contains(".csv");
        assertThat(asText(response)).contains("INV-7001");
    }

    @Test
    void returns404WhenDateRangeMatchesNoRows() {
        ResponseEntity<byte[]> response = postReport("/api/R07", Map.of(
                "reportFormat", "CSV",
                "dateFrom", "20990101",
                "dateTo", "20991231",
                "sellerClientNumber", "00001001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returns400WithoutAnyFilter() {
        // year/month, date range, seller, buyer, and submission number all absent —
        // the validator requires at least one (report.r07.filter.required.error).
        ResponseEntity<byte[]> response = postReport("/api/R07", Map.of(
                "reportFormat", "CSV"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(asText(response)).contains("report.r07.filter.required.error");
    }
}
