package ca.bc.gov.nrs.csp.backend.service.reporting;

import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Month-based report period: the default end date and the "For the N month period" label. */
public final class ReportMonthPeriod {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ReportMonthPeriod() {
    }

    // An explicit end date wins; otherwise the start month is month one, e.g. 20240401 + "1" -> 20240430.
    // Matches the UI and legacy calculateEndDateWithTimeFrame, not legacy's submit-time fallback (a month longer).
    public static String resolveInvoiceDateTo(String invoiceDateFrom, String invoiceDateTo, String timeFrameInMonths) {
        if (invoiceDateTo != null || invoiceDateFrom == null) {
            return invoiceDateTo;
        }
        LocalDate fromDate = LocalDate.parse(invoiceDateFrom, YYYYMMDD);
        int monthsAfterStartMonth = 0;
        if (timeFrameInMonths != null && !timeFrameInMonths.isBlank()) {
            try {
                monthsAfterStartMonth = Math.max(Integer.parseInt(timeFrameInMonths) - 1, 0);
            } catch (NumberFormatException e) {
                throw new BadRequestException("timeFrame must be a numeric value");
            }
        }
        return YearMonth.from(fromDate).plusMonths(monthsAfterStartMonth).atEndOfMonth().format(YYYYMMDD);
    }

    // Inclusive calendar-month count, e.g. 20231201..20240131 -> "2".
    // Same result and plain-integer format as legacy R10Bean/R12Bean.calculeTimeFrame.
    public static String inclusiveMonthCount(String invoiceDateFrom, String invoiceDateTo) {
        YearMonth fromMonth = YearMonth.from(LocalDate.parse(invoiceDateFrom, YYYYMMDD));
        YearMonth toMonth = YearMonth.from(LocalDate.parse(invoiceDateTo, YYYYMMDD));
        return String.valueOf(ChronoUnit.MONTHS.between(fromMonth, toMonth) + 1);
    }
}
