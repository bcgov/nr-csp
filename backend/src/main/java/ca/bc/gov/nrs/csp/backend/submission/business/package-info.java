/**
 * Phase 2 of submission validation: <b>business rules</b> (DB-backed,
 * cross-field, per-invoice). Runs after the structural phase passes; wired by
 * the {@code submission.SubmissionValidationService} orchestrator and gated by
 * {@code csp.submission.validation.business-rules-enabled} (default false until
 * the rules are implemented; the
 * {@link ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService}
 * facade is already wired to the repository layer).
 *
 * <h2>How to add a rule (the set structure — follow it, don't veer)</h2>
 * <ol>
 *   <li>Open the class for the rule's group:
 *     <ul>
 *       <li>submission-level → {@code rule.submission.SubmissionRules}</li>
 *       <li>invoice-level → the matching §2 section class under {@code rule.invoice}
 *           ({@code InvoiceTypeRules}, {@code InvoiceReferenceRules},
 *           {@code InvoicePartyRules}, {@code InvoiceCodeRules},
 *           {@code InvoiceTotalsRules}, {@code InvoiceSourceDocumentRules},
 *           {@code InvoiceDateRules}, {@code InvoiceLineItemPresenceRules})</li>
 *       <li>line-level → {@code rule.line.LineItemRules}</li>
 *     </ul>
 *   </li>
 *   <li>Add a package-private method {@code void xxx(<Level>RuleContext ctx)},
 *       header-commented with its catalogue ID (e.g. I12), then add a call to it
 *       from that class's {@code validate(...)} in catalogue order.</li>
 *   <li>Read the target off the context ({@code ctx.submission()} /
 *       {@code ctx.invoice()} / {@code ctx.line()} and convenience accessors).</li>
 *   <li>For any DB lookup, call the {@code ctx.referenceData()} facade — never a
 *       repository directly.</li>
 *   <li>Report findings with {@code ctx.error(messageKey, message)} (blocking)
 *       or {@code ctx.warning(messageKey, message)} (non-blocking). Use the
 *       message key from {@code docs/submission-validation-business-rules.md}.
 *       Never throw to signal a validation failure.</li>
 *   <li>Add a unit test for the method, mirroring the example method's test.</li>
 * </ol>
 *
 * <p>One method per rule. Submission (§1) and line (§3) are small, so each is a
 * single class ({@code SubmissionRules}, {@code LineItemRules}). Invoice (§2 has
 * ~41 rules) is split into one cohesive {@code @Component} per sub-section, so no
 * single file gets large. Every such bean is collected via {@code List<…Rule>}
 * and run automatically — adding a method, or a whole new section bean, needs no
 * other change. Avoid both extremes: no one-file-per-rule sprawl, and no single
 * 1,500-line validator.
 *
 * <p>The catalogue of rules to implement (S1, I1–I41, L1–L9, conversion) lives in
 * {@code docs/submission-validation-business-rules.md}; the implemented worked
 * examples to copy are {@code SubmissionRules#clientLocationExists} (S1),
 * {@code InvoiceDateRules#dateNotInFuture} (I39), and
 * {@code LineItemRules#gradeZWarning} (L4).
 */
package ca.bc.gov.nrs.csp.backend.submission.business;
