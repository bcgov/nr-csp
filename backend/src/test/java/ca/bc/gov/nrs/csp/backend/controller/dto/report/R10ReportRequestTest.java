package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class R10ReportRequestTest {

    @Test
    @DisplayName("defaults to PDF format")
    void defaults() {
        assertThat(new R10ReportRequest().getReportFormat()).isEqualTo(ReportFormat.PDF);
    }

    @Test
    @DisplayName("setters and getters round-trip for every property")
    void settersAndGettersRoundTrip() {
        R10ReportRequest r = new R10ReportRequest();

        r.setReportFormat(ReportFormat.CSV);
        r.setDateFrom("20200101");
        r.setDateTo("20200131");
        r.setTimeFrame("3");
        r.setSellerClientNumber("00012345");
        r.setSellerLocnCode("00");
        r.setBuyerClientNumber("00067890");
        r.setBuyerLocnCode("01");
        r.setMaturityCodes("O,S");
        r.setInvoiceTypeCode("LOG");
        r.setUserId("tester");

        assertThat(r.getReportFormat()).isEqualTo(ReportFormat.CSV);
        assertThat(r.getDateFrom()).isEqualTo("20200101");
        assertThat(r.getDateTo()).isEqualTo("20200131");
        assertThat(r.getTimeFrame()).isEqualTo("3");
        assertThat(r.getSellerClientNumber()).isEqualTo("00012345");
        assertThat(r.getSellerLocnCode()).isEqualTo("00");
        assertThat(r.getBuyerClientNumber()).isEqualTo("00067890");
        assertThat(r.getBuyerLocnCode()).isEqualTo("01");
        assertThat(r.getMaturityCodes()).isEqualTo("O,S");
        assertThat(r.getInvoiceTypeCode()).isEqualTo("LOG");
        assertThat(r.getUserId()).isEqualTo("tester");
    }

    @Test
    @DisplayName("toString serialises the request as JSON")
    void toStringIsJson() {
        R10ReportRequest r = new R10ReportRequest();
        r.setDateFrom("20200101");

        assertThat(r.toString())
                .contains("\"dateFrom\"")
                .contains("20200101");
    }
}
