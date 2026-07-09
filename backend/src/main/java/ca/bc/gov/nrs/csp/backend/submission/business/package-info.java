/**
 * Phase 2 of submission validation: <b>business rules</b> (DB-backed,
 * cross-field, per-invoice). Exposed as its own endpoint
 * ({@code POST /api/submissions/validate/business}) via the
 * {@code submission.SubmissionValidationService} orchestrator; it runs after the
 * structural phase parses the submission (the rules operate on the parsed tree).
 * DB lookups go through the
 * {@link ca.bc.gov.nrs.csp.backend.submission.business.referencedata.ReferenceDataService}
 * facade, which is wired to the repository layer.
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
 *       from that class's {@code validate(...)} in catalogue order. If (and only
 *       if) a later rule in the same class must run conditionally on an earlier
 *       rule's outcome — mirroring a legacy {@code if (checkX(...)) { checkY(...); }}
 *       gate — the earlier method may return {@code boolean} instead of
 *       {@code void} (see {@code InvoiceTypeRules#invoiceTypeValidOn}, which gates
 *       I2 on I1). Keep such returns package-private and local to the owning
 *       class; don't thread them through the context or {@code validate(ctx)}.</li>
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
 * examples to copy are {@code SubmissionRules#clientLocationExists} (S1) and
 * {@code InvoiceDateRules#dateNotInFuture} (I39).
 *
 * <p><b>Shared pure rules:</b> the channel-agnostic invoice rules — totals
 * (I24–I29) and line-item values (L3–L9) — live in the neutral
 * {@code ca.bc.gov.nrs.csp.backend.invoice.rules} core, shared with the manual
 * (CRUD) path so the two channels cannot drift. {@code InvoiceTotalsRules} and
 * {@code LineItemRules} are thin adapters over that core; see
 * {@code docs/refactor-shared-invoice-rules.md}.
 */
package ca.bc.gov.nrs.csp.backend.submission.business;
