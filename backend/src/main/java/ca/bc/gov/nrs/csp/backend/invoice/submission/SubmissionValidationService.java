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
    BusinessValidationOutcome business =
        businessValidationService.validate(structural.submission());
    return new SubmissionValidationResult(
        business.valid(), business.messages(), business.acceptance());
  }
}
