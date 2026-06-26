package ca.bc.gov.nrs.csp.backend.submission;

import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationResult;
import ca.bc.gov.nrs.csp.backend.submission.structural.StructuralValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Submission validation entry point and orchestrator. Runs the two
 * phases in order:
 *
 * <ol>
 *   <li><b>Structural</b> — format detection, ESF envelope stripping,
 *       XSD/JAXB parse ({@link StructuralValidationService}).</li>
 *   <li><b>Business</b> — DB-backed business rules, run only if the
 *       structural phase passed (a document that didn't parse can't be
 *       rule-checked). Wired in step 2.</li>
 * </ol>
 *
 * <p>Each phase is independently injectable, so callers that only need
 * one can use the phase service directly; this orchestrator is the
 * one place that knows about both and merges their results into a
 * single {@link SubmissionValidationResult}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionValidationService {

  private final StructuralValidationService structuralValidationService;

  public SubmissionValidationResult validate(byte[] xml) {
    // Phase 1: structural. The business-rule phase (phase 2) short-circuits
    // on structural failure and is wired here once the business/ module lands.
    StructuralValidationService.ValidationOutcome structural =
        structuralValidationService.validateAndParse(xml);
    return structural.result();
  }
}
