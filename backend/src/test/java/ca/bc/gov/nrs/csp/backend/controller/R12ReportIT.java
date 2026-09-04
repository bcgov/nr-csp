package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.testsupport.AbstractReportIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for POST /api/R12 (CFPA extract) against the real Oracle-backed
 * pipeline via the CSP_SP_RPT_12 stub procedure.
 */
class R12ReportIT extends AbstractReportIT {

    @Test
    void generatesCsvForYearAndMonth() {
        ResponseEntity<byte[]> response = postReport("/api/R12", Map.of(
                "reportFormat", "CSV",
                "year", 2024,
                "month", 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).hasToString("text/csv");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("R12_").contains(".csv");

        String csv = asText(response);
        assertThat(csv).contains("HEM").contains("BAL");
    }

    @Test
    void returns404WhenYearMatchesNoRows() {
        ResponseEntity<byte[]> response = postReport("/api/R12", Map.of(
                "reportFormat", "CSV",
                "year", 2099,
                "month", 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returns400WhenNeitherYearNorDateRangeGiven() {
        ResponseEntity<byte[]> response = postReport("/api/R12", Map.of(
                "reportFormat", "CSV"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(asText(response)).contains("VALIDATION_ERROR");
    }
}
