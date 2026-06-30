package ca.bc.gov.nrs.csp.backend.submission.business;

import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionAcceptance;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;

import java.util.List;

/**
 * Result of the business-rule phase, before the orchestrator merges it with the
 * structural result.
 *
 * <ul>
 *   <li>{@code valid} — submission accepted: no submission-level error and at
 *       least one invoice accepted.</li>
 *   <li>{@code messages} — flat list of ERROR + WARNING findings.</li>
 *   <li>{@code acceptance} — which invoices were accepted vs rejected.</li>
 * </ul>
 */
public record BusinessValidationOutcome(
    boolean valid,
    List<SubmissionValidationError> messages,
    SubmissionAcceptance acceptance) {}
