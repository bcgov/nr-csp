package ca.bc.gov.nrs.csp.backend.invoice.shared.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * One rule violation: message key, severity, and the {@code {0}}/{@code {1}}…
 * substitution args its {@code messages.properties} template expects. The core
 * never renders user-facing text — each channel resolves {@code code} + {@code args}
 * at its own edge (the manual path via {@code InvoiceMapper} + {@code MessageSource}.
 *
 * <p>equals/hashCode/toString are overridden because the record default compares the
 * {@code Object[]} component by reference; findings with identical args must be equal.
 */
public record Finding(String code, Severity severity, Object[] args) {

  @Override
  public boolean equals(Object o) {
    return o instanceof Finding(String otherCode, Severity otherSeverity, Object[] otherArgs)
        && Objects.equals(code, otherCode)
        && severity == otherSeverity
        && Arrays.deepEquals(args, otherArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, severity, Arrays.deepHashCode(args));
  }

  @Override
  public String toString() {
    return "Finding[code=" + code + ", severity=" + severity
        + ", args=" + Arrays.deepToString(args) + "]";
  }
}
