package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class R13ReportRequestTest {

    private static void assertRoundTripState(R13ReportRequest r, R13ShowOptions opts) {
        assertThat(r.getShowOptions()).isSameAs(opts);
        assertThat(r).extracting(
                R13ReportRequest::getReportName,
                R13ReportRequest::getReportFormat,
                R13ReportRequest::getInvoiceDateFrom,
                R13ReportRequest::getInvoiceDateTo,
                R13ReportRequest::getTimeFrame,
                R13ReportRequest::getInvoiceNumberFrom,
                R13ReportRequest::getInvoiceNumberTo,
                R13ReportRequest::getInvoiceReplacesAdjustsFrom,
                R13ReportRequest::getInvoiceReplacesAdjustsTo,
                R13ReportRequest::getInvoiceBoomNumberFrom,
                R13ReportRequest::getInvoiceBoomNumberTo,
                R13ReportRequest::getInvoiceTimberMarkFrom,
                R13ReportRequest::getInvoiceTimberMarkTo,
                R13ReportRequest::getInvoiceWeighSlipFrom,
                R13ReportRequest::getInvoiceWeighSlipTo,
                R13ReportRequest::getSubmissionMonthYear,
                R13ReportRequest::getSubmissionNumber,
                R13ReportRequest::getEntryUserId,
                R13ReportRequest::getApprovalMonthYear,
                R13ReportRequest::getSellerName,
                R13ReportRequest::getBuyerName,
                R13ReportRequest::getUserId,
                R13ReportRequest::getUserName)
            .containsExactly(
                "R13 Report",
                ReportFormat.CSV,
                "20200101",
                "20200131",
                "3",
                "A1",
                "Z9",
                "RA-FROM",
                "RA-TO",
                "B-FROM",
                "B-TO",
                "TM-FROM",
                "TM-TO",
                "WS-FROM",
                "WS-TO",
                "202001",
                "123",
                "jdoe",
                "202002",
                "ACME",
                "BUYCO",
                "tester",
                "Tester T");
        assertThat(r).extracting(
                R13ReportRequest::getInvoiceNumbers,
                R13ReportRequest::getInvoiceStatus,
                R13ReportRequest::getInvoiceReplacesAdjusts,
                R13ReportRequest::getInvoiceBoomNumbers,
                R13ReportRequest::getInvoiceTimberMarks,
                R13ReportRequest::getInvoiceWeighSlips,
                R13ReportRequest::getInvoiceTypes,
                R13ReportRequest::getMaturityCodes,
                R13ReportRequest::getSpecies,
                R13ReportRequest::getSortCodes,
                R13ReportRequest::getGrades,
                R13ReportRequest::getSubmissionStatus,
                R13ReportRequest::getSubmissionTypes,
                R13ReportRequest::getApprovedBy,
                R13ReportRequest::getSellerNumbers,
                R13ReportRequest::getBuyerNumbers,
                R13ReportRequest::getSellerClientLocnCodes,
                R13ReportRequest::getBuyerClientLocnCodes)
            .containsExactly(
                List.of("INV1"),
                List.of("APP"),
                List.of("RA1"),
                List.of("B1"),
                List.of("TM1"),
                List.of("WS1"),
                List.of("LOG"),
                List.of("O"),
                List.of("FI"),
                List.of("01"),
                List.of("A"),
                List.of("SUB"),
                List.of("Manual"),
                List.of("APPROVER"),
                List.of("12345"),
                List.of("67890"),
                List.of("00"),
                List.of("01"));
    }

    @Test
    @DisplayName("defaults: PDF format and empty (non-null) list filters")
    void defaults() {
        R13ReportRequest r = new R13ReportRequest();

        assertThat(r.getReportFormat()).isEqualTo(ReportFormat.PDF);
        assertThat(r.getShowOptions()).isNull();
        assertThat(r.getInvoiceNumbers()).isEmpty();
        assertThat(r.getInvoiceStatus()).isEmpty();
        assertThat(r.getInvoiceReplacesAdjusts()).isEmpty();
        assertThat(r.getInvoiceBoomNumbers()).isEmpty();
        assertThat(r.getInvoiceTimberMarks()).isEmpty();
        assertThat(r.getInvoiceWeighSlips()).isEmpty();
        assertThat(r.getInvoiceTypes()).isEmpty();
        assertThat(r.getMaturityCodes()).isEmpty();
        assertThat(r.getSpecies()).isEmpty();
        assertThat(r.getSortCodes()).isEmpty();
        assertThat(r.getGrades()).isEmpty();
        assertThat(r.getSubmissionStatus()).isEmpty();
        assertThat(r.getSubmissionTypes()).isEmpty();
        assertThat(r.getApprovedBy()).isEmpty();
        assertThat(r.getSellerNumbers()).isEmpty();
        assertThat(r.getBuyerNumbers()).isEmpty();
        assertThat(r.getSellerClientLocnCodes()).isEmpty();
        assertThat(r.getBuyerClientLocnCodes()).isEmpty();
    }

    @Test
    @DisplayName("setters and getters round-trip for every property")
    void settersAndGettersRoundTrip() {
        R13ReportRequest r = new R13ReportRequest();
        R13ShowOptions opts = new R13ShowOptions();

        r.setReportName("R13 Report");
        r.setReportFormat(ReportFormat.CSV);
        r.setShowOptions(opts);
        r.setInvoiceDateFrom("20200101");
        r.setInvoiceDateTo("20200131");
        r.setTimeFrame("3");
        r.setInvoiceNumbers(List.of("INV1"));
        r.setInvoiceNumberFrom("A1");
        r.setInvoiceNumberTo("Z9");
        r.setInvoiceStatus(List.of("APP"));
        r.setInvoiceReplacesAdjusts(List.of("RA1"));
        r.setInvoiceReplacesAdjustsFrom("RA-FROM");
        r.setInvoiceReplacesAdjustsTo("RA-TO");
        r.setInvoiceBoomNumbers(List.of("B1"));
        r.setInvoiceBoomNumberFrom("B-FROM");
        r.setInvoiceBoomNumberTo("B-TO");
        r.setInvoiceTimberMarks(List.of("TM1"));
        r.setInvoiceTimberMarkFrom("TM-FROM");
        r.setInvoiceTimberMarkTo("TM-TO");
        r.setInvoiceWeighSlips(List.of("WS1"));
        r.setInvoiceWeighSlipFrom("WS-FROM");
        r.setInvoiceWeighSlipTo("WS-TO");
        r.setInvoiceTypes(List.of("LOG"));
        r.setMaturityCodes(List.of("O"));
        r.setSpecies(List.of("FI"));
        r.setSortCodes(List.of("01"));
        r.setGrades(List.of("A"));
        r.setSubmissionMonthYear("202001");
        r.setSubmissionStatus(List.of("SUB"));
        r.setSubmissionNumber("123");
        r.setEntryUserId("jdoe");
        r.setSubmissionTypes(List.of("Manual"));
        r.setApprovalMonthYear("202002");
        r.setApprovedBy(List.of("APPROVER"));
        r.setSellerName("ACME");
        r.setBuyerName("BUYCO");
        r.setSellerNumbers(List.of("12345"));
        r.setBuyerNumbers(List.of("67890"));
        r.setSellerClientLocnCodes(List.of("00"));
        r.setBuyerClientLocnCodes(List.of("01"));
        r.setUserId("tester");
        r.setUserName("Tester T");

        assertRoundTripState(r, opts);
    }

    @Test
    @DisplayName("toString serialises the request as JSON")
    void toStringIsJson() {
        R13ReportRequest r = new R13ReportRequest();
        r.setReportName("R13 Report");
        r.setSellerName("ACME");

        String s = r.toString();

        assertThat(s)
                .contains("\"reportName\"")
                .contains("R13 Report")
                .contains("ACME");
    }
}
