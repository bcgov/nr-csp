package ca.bc.gov.nrs.csp.backend.submission.shared;

/**
 * Severity of a validation message.
 *
 * <ul>
 *   <li>{@link #ERROR} — blocking. Rejects the invoice it is scoped to (and,
 *       if submission-level, the whole submission).</li>
 *   <li>{@link #WARNING} — non-blocking. Informational only; does not reject.</li>
 * </ul>
 *
 * <p>Structural (format/envelope/schema) problems are always {@code ERROR}.
 * Business rules emit either, per the rules catalogue in
 * {@code docs/submission-validation-business-rules.md}.
 */
public enum Severity {
  ERROR,
  WARNING
}
