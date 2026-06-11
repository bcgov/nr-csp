package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportFormat {
    PDF("PDF"),
    CSV("CSV");

    private final String value;

    ReportFormat(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getExtension() {
        return value.toLowerCase();
    }

    @JsonCreator
    public static ReportFormat fromValue(String value) {
        for (ReportFormat f : ReportFormat.values()) {
            if (f.value.equalsIgnoreCase(value)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown report format: " + value);
    }
}
