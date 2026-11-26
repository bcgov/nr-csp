package ca.bc.gov.nrs.csp.backend.service.reporting;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReportMonthPeriodTest {

    @ParameterizedTest(name = "{0}..{1} -> {2}")
    @CsvSource({
            "20240401, 20240430, 1",
            "20240415, 20240415, 1",
            "20231201, 20240131, 2",
            "20240115, 20240310, 3",
            "20200101, 20201231, 12",
            "20231101, 20241231, 14"
    })
    void shouldCountCalendarMonthsInclusively(String invoiceDateFrom, String invoiceDateTo, String expectedMonthCount) {
        assertThat(ReportMonthPeriod.inclusiveMonthCount(invoiceDateFrom, invoiceDateTo))
                .isEqualTo(expectedMonthCount);
    }
}
