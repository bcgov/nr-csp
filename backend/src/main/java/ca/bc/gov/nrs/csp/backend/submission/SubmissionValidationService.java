package ca.bc.gov.nrs.csp.backend.submission;

import ca.bc.gov.nrs.csp.backend.submission.business.BusinessValidationOutcome;
import ca.bc.gov.nrs.csp.backend.submission.business.BusinessValidationService;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationResult;
import ca.bc.gov.nrs.csp.backend.submission.structural.StructuralValidationService;
import ca.bc.gov.nrs.csp.backend.submission.structural.SubmissionValidationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Submission validation entry point and orchestrator. Runs the two phases in
 * order:
 *
 * <ol>
 *   <li><b>Structural</b> — format detection, ESF envelope stripping, XSD/JAXB
 *       parse ({@link StructuralValidationService}).</li>
 *   <li><b>Business</b> — DB-backed business rules
 *       ({@link BusinessValidationService}), run only if structural passed (a
 *       document that didn't parse can't be rule-checked) and only when
 *       {@code csp.submission.validation.business-rules-enabled} is true.</li>
 * </ol>
 *
 * <p>Each phase is independently injectable; this orchestrator is the one place
 * that knows about both and merges their findings into a single
 * {@link SubmissionValidationResult}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionValidationService {

  private final StructuralValidationService structuralValidationService;
  private final BusinessValidationService businessValidationService;
  private final SubmissionValidationProperties properties;

  public SubmissionValidationResult validate(byte[] xml) {
    StructuralValidationService.ValidationOutcome structural =
        structuralValidationService.validateAndParse(xml);

    // Short-circuit: business rules can't run on a document that failed to parse,
    // and stay off entirely until enabled.
    if (!structural.result().valid()
        || !properties.isBusinessRulesEnabled()
        || structural.submission() == null) {
      return structural.result();
    }

    BusinessValidationOutcome business =
        businessValidationService.validate(structural.submission());

    // Structural errors are empty here (we only reach this on a structural pass),
    // but merge defensively so the contract is obvious.
    List<SubmissionValidationError> merged = new ArrayList<>(structural.result().errors());
    merged.addAll(business.messages());
    return new SubmissionValidationResult(business.valid(), merged, business.acceptance());
  }
}
