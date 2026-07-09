package ca.bc.gov.nrs.csp.backend.service.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Holds the result of a report generation: the raw bytes and the suggested download filename.
 *
 * <p>equals/hashCode/toString are overridden because the record default compares the
 * {@code byte[]} component by reference; two results with identical bytes must be equal,
 * and toString must not dump the raw contents.
 */
public record ReportResult(byte[] data, String filename) {

    @Override
    public boolean equals(Object o) {
        return o instanceof ReportResult(byte[] data1, String filename1)
                && Arrays.equals(data, data1)
                && Objects.equals(filename, filename1);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(data) + Objects.hashCode(filename);
    }

    @Override
    public String toString() {
        return "ReportResult[data=" + (data == null ? "null" : data.length + " bytes")
                + ", filename=" + filename + "]";
    }
}
