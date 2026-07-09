package ca.bc.gov.nrs.csp.backend.submission.shared;

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
 *   <li>{@code message} — human-readable detail. Null for messages produced by
 *       rules on the messages.properties strategy, which carry {@code args}
 *       instead (the text is resolved at the HTTP boundary).</li>
 *   <li>{@code args} — the {@code {0}}/{@code {1}}… substitution args the
 *       message key's {@code messages.properties} template expects; null for
 *       messages that carry pre-rendered {@code message} text.</li>
 *   <li>{@code severity} — {@link Severity#ERROR} (blocking) or
 *       {@link Severity#WARNING} (informational).</li>
 * </ul>
 *
 * <p>The controller maps these onto the app's {@code ValidationMessageResponse}
 * at the HTTP boundary.
 */
public record SubmissionValidationError(
    String path, String code, String message, Object[] args, Severity severity) {

  // ── Structural factories (always ERROR) ─────────────────────────────

  public static SubmissionValidationError of(String code, String message) {
    return new SubmissionValidationError(null, code, message, null, Severity.ERROR);
  }

  public static SubmissionValidationError of(String path, String code, String message) {
    return new SubmissionValidationError(path, code, message, null, Severity.ERROR);
  }

  // ── Business factories (explicit severity, locator-aware) ────────────

  /** A blocking business error at the given locator, with pre-rendered text. */
  public static SubmissionValidationError error(String locator, String code, String message) {
    return new SubmissionValidationError(locator, code, message, null, Severity.ERROR);
  }

  /** A non-blocking business warning at the given locator, with pre-rendered text. */
  public static SubmissionValidationError warning(String locator, String code, String message) {
    return new SubmissionValidationError(locator, code, message, null, Severity.WARNING);
  }

  // ── Business factories (message-key args; text resolves at the edge) ──
  // Rules converted to the messages.properties strategy (refactor doc §3.5)
  // carry the template args instead of rendered text; the controller resolves
  // code + args against the bundle at the HTTP boundary.

  /** A blocking business error at the given locator, carrying template args. */
  public static SubmissionValidationError error(String locator, String code, Object[] args) {
    return new SubmissionValidationError(locator, code, null, args, Severity.ERROR);
  }

  /** A non-blocking business warning at the given locator, carrying template args. */
  public static SubmissionValidationError warning(String locator, String code, Object[] args) {
    return new SubmissionValidationError(locator, code, null, args, Severity.WARNING);
  }
}
