package ca.bc.gov.nrs.csp.backend.invoice.submission.structural.parser;

/**
 * Thrown by {@link SubmissionEnvelopeStripper} when the upload's root
 * element is not a recognised submission envelope (or extraction of the
 * inner submission body fails). The {@code code} maps to a
 * {@code SubmissionValidationError.code}.
 */
public class SubmissionEnvelopeException extends Exception {

  private final String code;
  private final Object[] args;

  /**
   * @param message rendered diagnostic detail — used for logs / stack traces only.
   * @param args the {@code messages.properties} template args for {@code code};
   *     this is what reaches the user-facing response (refactor doc §3.5).
   */
  public SubmissionEnvelopeException(String code, String message, Object[] args) {
    super(message);
    this.code = code;
    this.args = args;
  }

  public SubmissionEnvelopeException(String code, String message, Object[] args, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.args = args;
  }

  public String getCode() {
    return code;
  }

  public Object[] getArgs() {
    return args;
  }
}
