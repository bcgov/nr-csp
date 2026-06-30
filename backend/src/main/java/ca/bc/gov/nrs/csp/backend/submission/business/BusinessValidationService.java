package ca.bc.gov.nrs.csp.backend.submission.business;

import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.InvoiceRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.LineItemRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.LineItemRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.SubmissionRule;
import ca.bc.gov.nrs.csp.backend.submission.business.rule.SubmissionRuleContext;
import ca.bc.gov.nrs.csp.backend.submission.business.support.IdentifierNormalizer;
import ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterInfo;
import ca.bc.gov.nrs.csp.backend.submission.business.support.SubmitterResolver;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPInvoiceType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPLineItemType;
import ca.bc.gov.nrs.csp.backend.submission.generated.CSPSubmissionType;
import ca.bc.gov.nrs.csp.backend.submission.shared.Severity;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionAcceptance;
import ca.bc.gov.nrs.csp.backend.submission.shared.SubmissionValidationError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Phase 2 — business-rule validation. Runs every auto-discovered rule against a
 * parsed submission (one pass per submission, per invoice, per line item),
 * accumulates findings, and derives per-invoice acceptance.
 *
 * <p>Rules run in an <b>accumulate</b> model: every rule runs and records its
 * findings; a failing rule does not abort the others. An invoice is rejected
 * iff it (or one of its lines) produced an ERROR; the submission is valid iff
 * there is no submission-level ERROR and at least one invoice is accepted.
 *
 * <p>The rule collections are injected by Spring — adding a {@code @Component}
 * rule of the right type makes it run, with no change here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessValidationService {

  private final List<SubmissionRule> submissionRules;
  private final List<InvoiceRule> invoiceRules;
  private final List<LineItemRule> lineItemRules;
  private final SubmitterResolver submitterResolver;
  private final IdentifierNormalizer identifierNormalizer;
  private final ReferenceDataService referenceData;

  /**
   * @param parsedSubmission the JAXB tree from the structural phase — the body
   *     type {@code CSPSubmissionType}, passed as {@code Object} so the
   *     structural module stays decoupled from the generated types. Cast here,
   *     at the single business-module boundary.
   */
  public BusinessValidationOutcome validate(Object parsedSubmission) {
    CSPSubmissionType submission = (CSPSubmissionType) parsedSubmission;
    ValidationCollector collector = new ValidationCollector();

    // Submission-level rules.
    SubmissionRuleContext submissionCtx =
        new SubmissionRuleContext(submission, referenceData, collector);
    submissionRules.forEach(rule -> rule.validate(submissionCtx));

    // Per-invoice and per-line rules. Keyed on the invoice INDEX (its identity),
    // so blank/duplicate invoice numbers never cross-contaminate accept/reject.
    List<CSPInvoiceType> invoices = submission.getCSPInvoice();
    for (int i = 0; i < invoices.size(); i++) {
      CSPInvoiceType invoice = invoices.get(i);
      identifierNormalizer.normalizeInvoiceIdentifiers(invoice);
      SubmitterInfo submitter = submitterResolver.resolve(submission, invoice);

      InvoiceRuleContext invoiceCtx =
          new InvoiceRuleContext(submission, invoice, i, submitter, referenceData, collector);
      invoiceRules.forEach(rule -> rule.validate(invoiceCtx));

      List<CSPLineItemType> lines = invoice.getCSPLineItem();
      for (int j = 0; j < lines.size(); j++) {
        LineItemRuleContext lineCtx = new LineItemRuleContext(
            invoice, lines.get(j), i, j + 1, submitter, referenceData, collector);
        lineItemRules.forEach(rule -> rule.validate(lineCtx));
      }
    }

    return buildOutcome(submission, collector);
  }

  private BusinessValidationOutcome buildOutcome(
      CSPSubmissionType submission, ValidationCollector collector) {

    boolean submissionLevelOk = collector.entries().stream()
        .noneMatch(e -> e.invoiceIndex() == null && e.error().severity() == Severity.ERROR);

    // Reject/accept is keyed on each invoice's INDEX (its identity), never its
    // user-supplied number — two invoices can share a blank or duplicate number.
    Set<Integer> rejectedIndices = collector.entries().stream()
        .filter(e -> e.invoiceIndex() != null && e.error().severity() == Severity.ERROR)
        .map(ValidationCollector.Entry::invoiceIndex)
        .collect(LinkedHashSet::new, Set::add, Set::addAll);

    // Build the accepted/rejected number lists by walking invoices positionally,
    // preserving genuine duplicate numbers (no distinct()).
    List<CSPInvoiceType> invoices = submission.getCSPInvoice();
    List<String> accepted = new ArrayList<>();
    List<String> rejected = new ArrayList<>();
    for (int i = 0; i < invoices.size(); i++) {
      String number = invoices.get(i).getInvoiceNumber();
      if (rejectedIndices.contains(i)) {
        rejected.add(number);
      } else {
        accepted.add(number);
      }
    }

    boolean valid = submissionLevelOk && !accepted.isEmpty();

    List<SubmissionValidationError> messages = collector.entries().stream()
        .map(ValidationCollector.Entry::error)
        .toList();

    return new BusinessValidationOutcome(valid, messages, new SubmissionAcceptance(accepted, rejected));
  }
}
