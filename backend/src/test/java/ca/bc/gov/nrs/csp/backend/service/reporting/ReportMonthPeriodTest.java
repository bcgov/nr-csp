package ca.bc.gov.nrs.csp.backend.service.reporting;

import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportMonthPeriodTest {

    @ParameterizedTest(name = "{0} + '{1}' -> {2}")
    @CsvSource({
            "20240401, 1,     20240430",
            "20240401, 01,    20240430",
            "20240415, 1,     20240430",
            "20241201, 2,     20250131",
            "20231231, 3,     20240229",
            "20240101, 12,    20241231",
            "20240401, 0,     20240430",
            "20240401, ,      20240430",
            "20240401, '   ', 20240430"
    })
    void shouldEndOnLastDayOfFinalMonth_whenNoEndDate(String invoiceDateFrom, String timeFrame, String expectedDateTo) {
        assertThat(ReportMonthPeriod.resolveInvoiceDateTo(invoiceDateFrom, null, timeFrame))
                .isEqualTo(expectedDateTo);
    }

    @Test
    void shouldKeepProvidedEndDate() {
        assertThat(ReportMonthPeriod.resolveInvoiceDateTo("20240401", "20240415", "abc"))
                .isEqualTo("20240415");
    }

    @Test
    void shouldLeaveEndDateEmpty_whenNoStartDate() {
        assertThat(ReportMonthPeriod.resolveInvoiceDateTo(null, null, "1")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "99999999999"})
    void shouldRejectTimeFrameThatIsNotAnInteger(String timeFrame) {
        assertThatThrownBy(() -> ReportMonthPeriod.resolveInvoiceDateTo("20240401", null, timeFrame))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("timeFrame must be a numeric value");
    }

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
