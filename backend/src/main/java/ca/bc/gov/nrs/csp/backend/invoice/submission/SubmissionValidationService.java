package ca.bc.gov.nrs.csp.backend.invoice.submission;

import ca.bc.gov.nrs.csp.backend.invoice.submission.business.BusinessValidationOutcome;
import ca.bc.gov.nrs.csp.backend.invoice.submission.business.BusinessValidationService;
import ca.bc.gov.nrs.csp.backend.invoice.submission.shared.SubmissionValidationResult;
import ca.bc.gov.nrs.csp.backend.invoice.submission.structural.StructuralValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Submission validation entry point. The two phases are exposed as separate,
 * independently-callable operations (one per endpoint):
 *
 * <ul>
 *   <li>{@link #validateStructural(byte[])} — format detection, ESF envelope
 *       stripping, XSD/JAXB parse ({@link StructuralValidationService}).</li>
 *   <li>{@link #validateBusiness(byte[])} — DB-backed business rules
 *       ({@link BusinessValidationService}). Business needs a parsed submission,
 *       so this parses first; if the XML doesn't parse it returns those
 *       structural errors, because business rules cannot run on it.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionValidationService {

  private final StructuralValidationService structuralValidationService;
  private final BusinessValidationService businessValidationService;

  /** Phase 1 — structural validation only. */
  public SubmissionValidationResult validateStructural(byte[] xml) {
    return structuralValidationService.validate(xml);
  }

  /**
   * Structural validation that also returns the parsed JAXB tree, so a caller
   * can both report structural errors and surface the parsed content (e.g. to
   * populate the upload form). On a parse/schema failure the outcome's
   * submission is null and its result carries the structural errors.
   */
  public StructuralValidationService.ValidationOutcome parse(byte[] xml) {
    return structuralValidationService.validateAndParse(xml);
  }

  /**
   * Phase 2 — business-rule validation. Parses the submission first (the rules
   * operate on the JAXB tree); on a parse/schema failure, returns those
   * structural errors since business rules cannot run on an unparseable document.
   */
  public SubmissionValidationResult validateBusiness(byte[] xml) {
    StructuralValidationService.ValidationOutcome structural =
        structuralValidationService.validateAndParse(xml);
    if (!structural.result().valid() || structural.submission() == null) {
      return structural.result();
    }
    return validateBusiness(structural.submission());
  }

  /**
   * Phase 2 on an already-parsed submission tree. Used by the submit path, which
   * parses once, applies the user's metadata edits, then validates the exact tree
   * it is about to persist — so what is validated is what is saved. The tree is
   * passed as {@code Object} to keep this module decoupled from the generated
   * types; {@link BusinessValidationService} casts at its own boundary.
   */
  public SubmissionValidationResult validateBusiness(Object parsedSubmission) {
    BusinessValidationOutcome business = businessValidationService.validate(parsedSubmission);
    return new SubmissionValidationResult(
        business.valid(), business.messages(), business.acceptance());
  }
}
