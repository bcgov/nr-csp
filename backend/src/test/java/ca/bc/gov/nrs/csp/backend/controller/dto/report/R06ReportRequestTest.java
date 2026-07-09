package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class R06ReportRequestTest {

    @Test
    @DisplayName("defaults to PDF format")
    void defaults() {
        assertThat(new R06ReportRequest().getReportFormat()).isEqualTo(ReportFormat.PDF);
    }

    @Test
    @DisplayName("setters and getters round-trip for every property")
    void settersAndGettersRoundTrip() {
        R06ReportRequest r = new R06ReportRequest();

        r.setReportFormat(ReportFormat.CSV);
        r.setDateFrom("20200101");
        r.setDateTo("20200131");
        r.setSellerClientNumber("00012345");
        r.setSellerLocCode("00");
        r.setBuyerClientNumber("00067890");
        r.setBuyerLocCode("01");
        r.setMaturityCodes("O,S");
        r.setSubmissionId(42L);
        r.setInvoiceNumbers("INV1,INV2");
        r.setLogSaleEntryStatusCode("APP");
        r.setCspInvoiceTypeCode("LOG");
        r.setUserId("tester");

        assertThat(r.getReportFormat()).isEqualTo(ReportFormat.CSV);
        assertThat(r.getDateFrom()).isEqualTo("20200101");
        assertThat(r.getDateTo()).isEqualTo("20200131");
        assertThat(r.getSellerClientNumber()).isEqualTo("00012345");
        assertThat(r.getSellerLocCode()).isEqualTo("00");
        assertThat(r.getBuyerClientNumber()).isEqualTo("00067890");
        assertThat(r.getBuyerLocCode()).isEqualTo("01");
        assertThat(r.getMaturityCodes()).isEqualTo("O,S");
        assertThat(r.getSubmissionId()).isEqualTo(42L);
        assertThat(r.getInvoiceNumbers()).isEqualTo("INV1,INV2");
        assertThat(r.getLogSaleEntryStatusCode()).isEqualTo("APP");
        assertThat(r.getCspInvoiceTypeCode()).isEqualTo("LOG");
        assertThat(r.getUserId()).isEqualTo("tester");
    }

    @Test
    @DisplayName("toString serialises the request as JSON")
    void toStringIsJson() {
        R06ReportRequest r = new R06ReportRequest();
        r.setDateFrom("20200101");

        assertThat(r.toString())
                .contains("\"dateFrom\"")
                .contains("20200101");
    }
}
