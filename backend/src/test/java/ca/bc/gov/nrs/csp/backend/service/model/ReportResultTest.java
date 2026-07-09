package ca.bc.gov.nrs.csp.backend.service.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the content-aware equals/hashCode/toString added for the byte[] component.
 */
class ReportResultTest {

    @Test
    void equals_equalContent_isTrue_andHashCodesMatch() {
        ReportResult a = new ReportResult(new byte[]{1, 2, 3}, "r06.pdf");
        ReportResult b = new ReportResult(new byte[]{1, 2, 3}, "r06.pdf");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void equals_differentDataOrFilename_isFalse() {
        ReportResult base = new ReportResult(new byte[]{1, 2, 3}, "r06.pdf");
        assertThat(base)
                .isNotEqualTo(new ReportResult(new byte[]{9}, "r06.pdf"))
                .isNotEqualTo(new ReportResult(new byte[]{1, 2, 3}, "other.pdf"));
    }

    @Test
    void equals_nullAndOtherType_isFalse() {
        ReportResult r = new ReportResult(new byte[]{1}, "r.pdf");
        assertThat(r).isNotIn(null, "not a report");
    }

    @Test
    void toString_showsByteCountNotContents() {
        assertThat(new ReportResult(new byte[]{1, 2, 3}, "r06.pdf"))
                .hasToString("ReportResult[data=3 bytes, filename=r06.pdf]");
    }

    @Test
    void toString_nullData_showsNull() {
        assertThat(new ReportResult(null, "r06.pdf"))
                .hasToString("ReportResult[data=null, filename=r06.pdf]");
    }
}
