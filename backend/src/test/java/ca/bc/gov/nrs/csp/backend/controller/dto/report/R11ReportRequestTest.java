package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class R11ReportRequestTest {

    @Test
    @DisplayName("defaults to PDF format")
    void defaults() {
        assertThat(new R11ReportRequest().getReportFormat()).isEqualTo(ReportFormat.PDF);
    }

    @Test
    @DisplayName("setters and getters round-trip for every property")
    void settersAndGettersRoundTrip() {
        R11ReportRequest r = new R11ReportRequest();

        r.setReportFormat(ReportFormat.CSV);
        r.setDateFrom("20200101");
        r.setDateTo("20200131");
        r.setTimeFrame("3");
        r.setBlended(Boolean.TRUE);
        r.setModelingCode("MC");
        r.setMaturityCodes("O,S");
        r.setMaturityDescriptions("Old Growth, Second Growth");
        r.setUserId("tester");

        assertThat(r.getReportFormat()).isEqualTo(ReportFormat.CSV);
        assertThat(r.getDateFrom()).isEqualTo("20200101");
        assertThat(r.getDateTo()).isEqualTo("20200131");
        assertThat(r.getTimeFrame()).isEqualTo("3");
        assertThat(r.getBlended()).isTrue();
        assertThat(r.getModelingCode()).isEqualTo("MC");
        assertThat(r.getMaturityCodes()).isEqualTo("O,S");
        assertThat(r.getMaturityDescriptions()).isEqualTo("Old Growth, Second Growth");
        assertThat(r.getUserId()).isEqualTo("tester");
    }

    @Test
    @DisplayName("toString serialises the request as JSON")
    void toStringIsJson() {
        R11ReportRequest r = new R11ReportRequest();
        r.setModelingCode("MC");

        assertThat(r.toString())
                .contains("\"modelingCode\"")
                .contains("MC");
    }
}
