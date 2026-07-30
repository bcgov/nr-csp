package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class R12ReportRequestTest {

    @Test
    @DisplayName("defaults to PDF format")
    void defaults() {
        assertThat(new R12ReportRequest().getReportFormat()).isEqualTo(ReportFormat.PDF);
    }

    @Test
    @DisplayName("setters and getters round-trip for every property")
    void settersAndGettersRoundTrip() {
        R12ReportRequest r = new R12ReportRequest();

        r.setReportFormat(ReportFormat.CSV);
        r.setYear(2020);
        r.setMonth(6);
        r.setDateFrom("20200101");
        r.setDateTo("20200131");
        r.setTimeFrame("3");
        r.setLogSaleTypeCode("LS");
        r.setUserId("tester");

        assertThat(r.getReportFormat()).isEqualTo(ReportFormat.CSV);
        assertThat(r.getYear()).isEqualTo(2020);
        assertThat(r.getMonth()).isEqualTo(6);
        assertThat(r.getDateFrom()).isEqualTo("20200101");
        assertThat(r.getDateTo()).isEqualTo("20200131");
        assertThat(r.getTimeFrame()).isEqualTo("3");
        assertThat(r.getLogSaleTypeCode()).isEqualTo("LS");
        assertThat(r.getUserId()).isEqualTo("tester");
    }

    @Test
    @DisplayName("toString serialises the request as JSON")
    void toStringIsJson() {
        R12ReportRequest r = new R12ReportRequest();
        r.setLogSaleTypeCode("LS");

        assertThat(r.toString())
                .contains("\"logSaleTypeCode\"")
                .contains("LS");
    }
}
