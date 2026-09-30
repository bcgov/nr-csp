# Coverage — UC-SUBM-001 / 002 / 003 / 004, happy-path journey

> New to these files? See [`coverage-guide.md`](../../../coverage-guide.md) at the e2e root for the
> column + status-flag legend.

Sources reconciled (csp-bmad `_bmad-output/`):

| Source | Path |
|---|---|
| Gherkin (4 slices) | `implementation-artifacts/tests/UC-SUBM-00{1,2,3,4}/gherkin/UC-SUBM-00{1,2,3,4}-S01.feature` |
| Slice catalogues (control / message / field / rule matrices) | `planning-artifacts/requirements/use-cases/UC-SUBM-00{1,2,3,4}/UC-SUBM-00{1,2,3,4}-slices.md` |
| Detailed UCs | `.../UC-SUBM-00{1,2,3,4}-detailed.md` |
| Message catalogue | `.../UC-SUBM-004-technical.md` + `backend/src/main/resources/messages.properties` |

…reconciled against the app's **actual write path**, which is the source of truth:
`pages/Invoice/index.tsx` → `services/invoice.service` → `InvoiceController` → `InvoiceService` →
`InvoiceValidator` / `InvoiceLineValidator` / `InvoiceTotalsRuleSet` → `InvoiceRepository`.

Test data (real, discovered 2026-09-25): pinned in `fixtures/subm/invoice-test-data.ts`, with the
discovery command in the comment above each value. Seed image
`ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23`.

**SCOPE OF THIS PASS — the four S01 happy-path slices only**, as requested. Between them,
UC-SUBM-001..004 hold **81 slices** (22 / 21 / 16 / 22). The other 77 are **not authored** and are
listed as coverage gaps below so nothing looks silently covered.

## Scenario inventory

| Scenario | File | Tags | Result |
|---|---|---|---|
| A manual Purchase invoice is created, itemised, re-saved as a draft, and submitted | `journey.feature` | `@p0 @UC-SUBM-001 @UC-SUBM-002 @UC-SUBM-003 @UC-SUBM-004 @S01 @journey` | green |
| Saving a brand-new invoice reminds the submitter that it still needs submitting | `submit-reminder.feature` | `@p1 @UC-SUBM-001 @S01 @discovered-divergence` | **red on purpose** (DIV-001) |

## Coverage matrix

| Source item | Source citation | App enforcement point | Scenario (tags) | Status | Gap/defect |
|---|---|---|---|---|---|
| **UC-SUBM-001-S01** — new invoice entered with no id in the URL | S01 `:20`; slices "Entry Point Reachability" | `ROUTES.INVOICE` + `useParams` id branch (`Invoice/index.tsx:251`) | `journey` `@S01 @p0` | covered | — |
| Invoice Number, Type, Date, Submitted By, Submitting Client + Location, Other Party + Location, Maturity, FOB all entered | S01 `:24-32`; slices "Relevant Controls"/"Relevant Fields" (11 fields) | `requiredFieldsFilled` (`Invoice/index.tsx:516`) | `journey` `@S01 @p0` | covered | — |
| Client auto-complete resolves a real client and fills number + location | S01 `:27-28` | `ClientAutocomplete` → `/api/clients` → `handleSubmittingClientSelect` | `journey` `@S01 @p0` | covered | — |
| Save creates the invoice in **DFT** | S01 `:34`; BR "transitions to DFT after full save" | `InvoiceService.create` → `INVENTRYSTATUS_DRAFT` | `journey` `@S01 @p0` | covered (UI **and** API read-back) | — |
| STA-002 — after save, Submit / Add-line-item become available | slices STA-002 | `canSubmit`, `canAddLineItem` (`Invoice/index.tsx:808,815`) | `journey` `@S01 @p0` | covered (implicitly — both are exercised next) | — |
| SUC-001 — success message on save | slices SUC-001 (`[TODO — capture from live app]`) | toast `Invoice '<num>' created.` (`handleSave`) | `journey` `@S01 @p0` | covered (+ captured — see Spec gap #4) | Spec gap #4 |
| WRN-004 — submit-reminder warning after save | slices WRN-004; `invoice.submit.saved.warning` | `InvoiceValidator.isSubmitProcessRequiered` | `submit-reminder` `@discovered-divergence`; and `journey` on the **re-save** | **covered by a failing test** | **DIV-001** |
| Submission ID populated after save | S01 `:35`; slices control `cspSubmissionId` | `InvoiceResponse.submissionNumber` (null when manual) | `journey` `@S01 @p0` (asserts the **opposite**) | covered, re-grounded | Spec gap #3 |
| Seller cannot submit PUR / Buyer cannot submit SAL | slices BR; `InvoiceValidator.java` | `checkSenderBuyerForInvoiceType` | — (journey *complies* with the rule; no negative test) | partially covered | Spec gap #1, Coverage gap #2 |
| Submitter and other party must differ | slices BR | `isSameSellerAndBuyer` | — (journey complies) | partially covered | Coverage gap #2 |
| **UC-SUBM-002-S01** — line item added to a DRAFT invoice | S01 `:20-21` | `POST /api/invoices/{id}/line-items` | `journey` `@S01 @p0` | covered | — |
| Secondary sort, Species, Grade, Pieces, Volume, Price entered | S01 `:23-29`; slices "Relevant Fields" | Add New Item modal (`Invoice/index.tsx:2311`) | `journey` `@S01 @p0` | covered | — |
| Species+Grade must be a valid combination | slices BR; `InvoiceLineValidator` | `checkSpeciesGradeCombination` + `filteredGradeItems` UI filter | `journey` `@S01 @p0` (complies; valid pair used) | partially covered | Coverage gap #2 |
| Sort code valid on the invoice date | slices "Relevant Fields" | `InvoiceLineValidator.isValidSortCode` | `journey` `@S01 @p0` (complies) | partially covered | Coverage gap #2 |
| STA-002 / CNT-001 — group summary row + totals update | S01 `:30-33`; slices STA-002, CNT-001 | `groupLineItems` + `groupTotalsRow` (`Invoice/index.tsx:174,1296`) | `journey` `@S01 @p0` | covered (per-cell, both renderings) | — |
| Line amount = volume x price | slices BR (implied); `LineAmount.compute` | backend recomputes and persists | `journey` `@S01 @p0` (API read-back of the **stored** amount) | covered | — |
| SUC-001 — success message on add | slices SUC-001 (`[TODO]`, "no explicit key") | toast `Line item added.` | `journey` `@S01 @p0` (modal close asserted) | covered, re-grounded | Spec gap #4 |
| **UC-SUBM-003-S01** — save a valid invoice as draft | S01 `:22-29` | `PUT /api/invoices/{id}` → status stays DFT | `journey` `@S01 @p0` | covered | — |
| Save is an UPDATE, not a second create | not in the Gherkin (new-app behaviour) | `handleSave` `isExisting` branch | `journey` `@S01 @p0` (route-spy verb count) | covered | — |
| SUC-001 — `The Invoice has been Saved successfully.` | slices SUC-001 (exact text given) | toast `Invoice '<num>' saved.` | `journey` `@S01 @p0` | covered, re-grounded | Spec gap #4 |
| WRN-001 — submit reminder on a draft save | slices WRN-001 (exact text given) | `invoice.submit.saved.warning` on the PUT | `journey` `@S01 @p0` | covered (exact legacy text, unchanged) | — |
| At least one of Boom / Timber Mark / Weigh Slip | S01 `:26`; `InvoiceValidator` | `checkSourceDocumentRefs` (server only) | `journey` `@S01 @p0` (complies — a boom number is entered) | partially covered | **BUG-001**, Coverage gap #2 |
| DRAFT invoices are not in the reviewer's inbox (`LOB`) | slices BR | `SUBMSTATUS_LOBBY` on create | — | deferred | Coverage gap #3 |
| **UC-SUBM-004-S01** — submit a valid DRAFT invoice | S01 `:22-31` | `POST /api/invoices/{id}/submit` | `journey` `@S01 @p0` | covered | — |
| Submit requires at least one line item | `invoice.noline.item.error`; `checkInvoiceLines` | validator on `ActionType.SUBMIT` + `!hasLineItems` gate | — (journey complies) | partially covered | Coverage gap #2 |
| Status transitions on submit | S01 `:33`; BR-SUBM-004-05 (UNA); detailed step 5 (PRO in transit) | `updateStatus(..., INVENTRYSTATUS_PROCESSING)` | `journey` `@S01 @p0` (asserts **PRO**) | covered, re-grounded | **Spec gap #2** |
| Submission moves to the reviewer's inbox (`INB`) | S01 outcome; BR | `submissionRepo.updateSubmissionStatus(..., SUBMSTATUS_INBOX)` | — | deferred | Coverage gap #3 |
| SUC-001 — `The Invoice has been Submitted successfully.` | UC-SUBM-004 detailed `:46` | toast `Invoice '<num>' submitted.` | `journey` `@S01 @p0` | covered, re-grounded | Spec gap #4 |
| Submit button state after submit | slices STA-002 ("remains enabled in UNA") | `SUBMITTABLE_STATUSES` excludes PRO | `journey` `@S01 @p0` (asserts visible **but disabled**) | covered, re-grounded | Spec gap #2 |
| Background — "authenticated via WebADE", per-action provisioning | all four S01 Backgrounds | Cognito groups / `usePermission` | — | not-applicable | Coverage gap #4 |

**Symmetry check.** Two mirror matrices exist in these UCs and **neither arm is covered** by this
pass, which is consistent rather than lopsided:

| Mirror matrix | Arm A | Arm B | Status |
|---|---|---|---|
| Invoice type x submitter | Seller submits PUR (S-001-S06) | Buyer submits SAL (S-001-S07) | both deferred — Coverage gap #2 |
| Purchase vs Sale happy path | Purchase (S-001-S01) — **covered** | Sale (S-001-S21) | **asymmetric** — Coverage gap #1 |

The Purchase/Sale pair is the one genuine asymmetry: the journey covers the Purchase arm only.
Because the Purchase arm forced the `Buyer` pairing (Spec gap #1), the Sale arm — which must pair
with `Seller` — exercises a different branch of `checkSenderBuyerForInvoiceType` and is worth
authoring next.

**Role / permission coverage:** every scenario runs as `CSP_ADMIN`, which holds all seven
`invoiceDetails/*` permissions the page gates on (`context/auth/permissions.ts`). The
`VIEW`-role arms (Save/Submit hidden or disabled) are not authored — Coverage gap #5.

## Parallel-safety notes for anyone extending this UC

* Each scenario mints its **own** invoice number and boom number and deletes its own invoice — there
  is no shared mutable fixture. Reusing a number would trip
  `invoice.number.duplicate.same.type.warning` / `invoice.boomnumber.duplicate.warning`.
* **Between submit and cleanup, an UNFILTERED `/api/inbox` query returns 51 rows, not 50** (verified:
  the submitted invoice's submission moves to `INB` and the Inbox INNER JOINs `coastal_log_sale`).
  This does not affect anything today — the Inbox smoke scenario searches the **seeded date window**
  (2015–2019), which a freshly-created submission falls outside of, and the preflight's unfiltered
  fingerprint runs in the `setup` project that `chromium` depends on, so it always completes before
  any scenario writes. Anyone adding an unfiltered Inbox assertion **inside** the chromium project
  must account for it. See Verified-not-a-defect #1.
