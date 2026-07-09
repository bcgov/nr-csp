package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class R08ReportRequestTest {

    @Test
    @DisplayName("defaults to PDF format")
    void defaults() {
        assertThat(new R08ReportRequest().getReportFormat()).isEqualTo(ReportFormat.PDF);
    }

    @Test
    @DisplayName("setters and getters round-trip for every property")
    void settersAndGettersRoundTrip() {
        R08ReportRequest r = new R08ReportRequest();

        r.setReportFormat(ReportFormat.CSV);
        r.setYear(2020);
        r.setMonth(6);
        r.setDateFrom("20200101");
        r.setDateTo("20200131");
        r.setTimeFrame("3");
        r.setSellerClientNumber("00012345");
        r.setSellerClientName("ACME");
        r.setSellerLocCode("00");
        r.setBuyerClientNumber("00067890");
        r.setBuyerClientName("BUYCO");
        r.setBuyerLocCode("01");
        r.setMaturityCodes("O,S");
        r.setInvoiceType("LOG");
        r.setInvoiceStatus("APP");
        r.setSubmissionStatus("SUB");
        r.setSubmissionNumber("123");
        r.setSubmissionYearMonth("202001");
        r.setUserId("tester");

        assertThat(r.getReportFormat()).isEqualTo(ReportFormat.CSV);
        assertThat(r.getYear()).isEqualTo(2020);
        assertThat(r.getMonth()).isEqualTo(6);
        assertThat(r.getDateFrom()).isEqualTo("20200101");
        assertThat(r.getDateTo()).isEqualTo("20200131");
        assertThat(r.getTimeFrame()).isEqualTo("3");
        assertThat(r.getSellerClientNumber()).isEqualTo("00012345");
        assertThat(r.getSellerClientName()).isEqualTo("ACME");
        assertThat(r.getSellerLocCode()).isEqualTo("00");
        assertThat(r.getBuyerClientNumber()).isEqualTo("00067890");
        assertThat(r.getBuyerClientName()).isEqualTo("BUYCO");
        assertThat(r.getBuyerLocCode()).isEqualTo("01");
        assertThat(r.getMaturityCodes()).isEqualTo("O,S");
        assertThat(r.getInvoiceType()).isEqualTo("LOG");
        assertThat(r.getInvoiceStatus()).isEqualTo("APP");
        assertThat(r.getSubmissionStatus()).isEqualTo("SUB");
        assertThat(r.getSubmissionNumber()).isEqualTo("123");
        assertThat(r.getSubmissionYearMonth()).isEqualTo("202001");
        assertThat(r.getUserId()).isEqualTo("tester");
    }

    @Test
    @DisplayName("toString serialises the request as JSON")
    void toStringIsJson() {
        R08ReportRequest r = new R08ReportRequest();
        r.setBuyerClientName("BUYCO");

        assertThat(r.toString())
                .contains("\"buyerClientName\"")
                .contains("BUYCO");
    }
}
