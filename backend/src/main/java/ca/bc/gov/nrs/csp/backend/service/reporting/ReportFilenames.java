package ca.bc.gov.nrs.csp.backend.service.reporting;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds the timestamped download filename shared by all report services
 * (e.g. {@code R06_20260730-161151.pdf}). Centralised here so the format and
 * the business-zone {@link Clock} handling live in one place rather than being
 * duplicated across each RxxService.
 */
public final class ReportFilenames {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ReportFilenames() {
    }

    /**
     * @param prefix    report code, e.g. {@code "R06"}
     * @param extension file extension without the dot, e.g. {@code "pdf"} or {@code "csv"}
     * @param clock     business-zone clock used to stamp the current time
     * @return {@code <prefix>_<yyyyMMdd-HHmmss>.<extension>}
     */
    public static String timestamped(String prefix, String extension, Clock clock) {
        return String.format("%s_%s.%s", prefix, LocalDateTime.now(clock).format(TIMESTAMP), extension);
    }
}
