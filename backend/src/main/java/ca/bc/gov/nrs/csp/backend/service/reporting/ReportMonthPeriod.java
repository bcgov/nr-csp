package ca.bc.gov.nrs.csp.backend.service.reporting;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** "For the N month period" label shared by the R10 and R12 report headers. */
public final class ReportMonthPeriod {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ReportMonthPeriod() {
    }

    // Inclusive calendar-month count, e.g. 20231201..20240131 -> "2".
    // Same result and plain-integer format as legacy R10Bean/R12Bean.calculeTimeFrame.
    public static String inclusiveMonthCount(String invoiceDateFrom, String invoiceDateTo) {
        YearMonth fromMonth = YearMonth.from(LocalDate.parse(invoiceDateFrom, YYYYMMDD));
        YearMonth toMonth = YearMonth.from(LocalDate.parse(invoiceDateTo, YYYYMMDD));
        return String.valueOf(ChronoUnit.MONTHS.between(fromMonth, toMonth) + 1);
    }
}
