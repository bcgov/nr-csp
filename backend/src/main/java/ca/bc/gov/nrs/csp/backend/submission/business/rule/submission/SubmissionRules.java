package ca.bc.gov.nrs.csp.backend.submission.business.rule.submission;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.SubmissionRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.SubmissionRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmitterType;
import org.springframework.stereotype.Component;

@Component
public class SubmissionRules implements SubmissionRule {

  @Override
  public void validate(SubmissionRuleContext ctx) {
    clientLocationExists(ctx);
  }

  /** Submission Client Number + Client Location must exist in CSP (ERROR) */
  void clientLocationExists(SubmissionRuleContext ctx) {
    CSPSubmitterType submitter = ctx.submission().getCSPSubmitter();
    String clientNumber = submitter.getSubmissionClientNumber();
    String locnCode = submitter.getSubmissionClientLocnCode();

    boolean missing = isBlank(clientNumber) || isBlank(locnCode);
    if (missing || !ctx.referenceData().clientLocationExists(clientNumber, locnCode)) {
      ctx.error(
          "invoice.submitter.client.location.invalid.error",
          "The combination of the submissionClientNumber " + clientNumber
              + " and submissionClientLocnCode " + locnCode + " cannot be found in CSP.");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
