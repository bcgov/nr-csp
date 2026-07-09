package ca.bc.gov.nrs.csp.backend.invoice.submission.shared;

/**
 * A single validation message, shared by both validation phases.
 *
 * <ul>
 *   <li>{@code path} — best-effort locator. For structural errors this is the
 *       XML position (e.g. {@code "line 12, col 8"}); for business messages it
 *       is the invoice/line locator (e.g. {@code "invoice INV-001, line 3"}).
 *       May be null.</li>
 *   <li>{@code code} — stable machine code: a structural code (XSD / JAXB /
 *       XML_PARSE / ENVELOPE_* / FORMAT_UNRECOGNIZED) or a business message key
 *       (e.g. {@code invoice.type.invalid.error}).</li>
 *   <li>{@code args} — the {@code {0}}/{@code {1}}… substitution args the
 *       message key's {@code messages.properties} template expects. There is
 *       deliberately no rendered-text field: every message resolves from the
 *       bundle at the HTTP boundary (refactor doc §3.5 Step C), so inline
 *       English cannot be emitted anywhere in the pipeline.</li>
 *   <li>{@code severity} — {@link Severity#ERROR} (blocking) or
 *       {@link Severity#WARNING} (informational).</li>
 * </ul>
 *
 * <p>The controller maps these onto the app's {@code ValidationMessageResponse}
 * at the HTTP boundary.
 */
public record SubmissionValidationError(
    String path, String code, Object[] args, Severity severity) {

  // ── Structural factories (always ERROR) ─────────────────────────────

  /** A structural error carrying its messages.properties template args. */
  public static SubmissionValidationError of(String code, Object[] args) {
    return new SubmissionValidationError(null, code, args, Severity.ERROR);
  }

  /** Locator-scoped variant of {@link #of(String, Object[])}. */
  public static SubmissionValidationError of(String path, String code, Object[] args) {
    return new SubmissionValidationError(path, code, args, Severity.ERROR);
  }

  // ── Business factories (explicit severity, locator-aware) ────────────

  /** A blocking business error at the given locator, carrying template args. */
  public static SubmissionValidationError error(String locator, String code, Object[] args) {
    return new SubmissionValidationError(locator, code, args, Severity.ERROR);
  }

  /** A non-blocking business warning at the given locator, carrying template args. */
  public static SubmissionValidationError warning(String locator, String code, Object[] args) {
    return new SubmissionValidationError(locator, code, args, Severity.WARNING);
  }
}
