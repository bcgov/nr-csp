package ca.bc.gov.nrs.csp.backend.submission;

import ca.bc.gov.nrs.csp.backend.submission.business.BusinessValidationOutcome;
import ca.bc.gov.nrs.csp.backend.submission.business.BusinessValidationService;
import ca.bc.gov.nrs.csp.backend.submission.shared.Severity;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionAcceptance;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationResult;
import ca.bc.gov.nrs.csp.backend.submission.structural.StructuralValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Confirms the orchestrator wires the two phases correctly: the structural
 * phase delegates straight through, and the business phase parses first,
 * short-circuits on a structural failure (business rules cannot run on an
 * unparseable document), and otherwise merges the business outcome into the
 * shared result type.
 */
@ExtendWith(MockitoExtension.class)
class SubmissionValidationServiceTest {

  @Mock StructuralValidationService structuralValidationService;
  @Mock BusinessValidationService businessValidationService;

  @InjectMocks SubmissionValidationService service;

  private static final byte[] XML = "<CSPSubmission/>".getBytes();

  @Test
  void validateStructural_delegates_to_structural_service() {
    SubmissionValidationResult structural = SubmissionValidationResult.ok();
    given(structuralValidationService.validate(XML)).willReturn(structural);

    assertThat(service.validateStructural(XML)).isSameAs(structural);
    then(businessValidationService).shouldHaveNoInteractions();
  }

  @Test
  void validateBusiness_returns_structural_errors_when_parse_fails() {
    SubmissionValidationResult failed = SubmissionValidationResult.failed(
        List.of(SubmissionValidationError.of("XSD", "bad element")));
    given(structuralValidationService.validateAndParse(XML))
        .willReturn(new StructuralValidationService.ValidationOutcome(failed, null));

    assertThat(service.validateBusiness(XML)).isSameAs(failed);
    then(businessValidationService).shouldHaveNoInteractions();
  }

  @Test
  void validateBusiness_returns_structural_result_when_valid_but_no_submission() {
    SubmissionValidationResult okButUnparsed = SubmissionValidationResult.ok();
    given(structuralValidationService.validateAndParse(XML))
        .willReturn(new StructuralValidationService.ValidationOutcome(okButUnparsed, null));

    assertThat(service.validateBusiness(XML)).isSameAs(okButUnparsed);
    then(businessValidationService).shouldHaveNoInteractions();
  }

  @Test
  void validateBusiness_runs_business_rules_on_parsed_submission() {
    Object submission = new Object();
    given(structuralValidationService.validateAndParse(XML))
        .willReturn(new StructuralValidationService.ValidationOutcome(
            SubmissionValidationResult.ok(), submission));

    SubmissionValidationError warning = SubmissionValidationError.warning(
        "invoice INV-1", "invoice.grade.z.warning", "grade Z");
    SubmissionAcceptance acceptance =
        new SubmissionAcceptance(List.of("INV-1"), List.of());
    given(businessValidationService.validate(submission))
        .willReturn(new BusinessValidationOutcome(true, List.of(warning), acceptance));

    SubmissionValidationResult result = service.validateBusiness(XML);

    assertThat(result.valid()).isTrue();
    assertThat(result.errors()).containsExactly(warning);
    assertThat(result.acceptance()).isEqualTo(acceptance);
  }

  @Test
  void validateBusiness_carries_business_failure_into_result() {
    Object submission = new Object();
    given(structuralValidationService.validateAndParse(XML))
        .willReturn(new StructuralValidationService.ValidationOutcome(
            SubmissionValidationResult.ok(), submission));

    SubmissionValidationError error = SubmissionValidationError.error(
        "invoice INV-1", "invoice.date.in.future.error", "date in future");
    SubmissionAcceptance acceptance =
        new SubmissionAcceptance(List.of(), List.of("INV-1"));
    given(businessValidationService.validate(submission))
        .willReturn(new BusinessValidationOutcome(false, List.of(error), acceptance));

    SubmissionValidationResult result = service.validateBusiness(XML);

    assertThat(result.valid()).isFalse();
    assertThat(result.errors())
        .singleElement()
        .satisfies(e -> {
          assertThat(e.code()).isEqualTo("invoice.date.in.future.error");
          assertThat(e.severity()).isEqualTo(Severity.ERROR);
        });
    assertThat(result.acceptance().rejected()).containsExactly("INV-1");
  }
}
