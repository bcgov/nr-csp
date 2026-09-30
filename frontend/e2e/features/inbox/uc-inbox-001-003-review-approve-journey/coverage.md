# Coverage — UC-INBOX-001 / 002 / 003, review-and-approve journey

> New to these files? See [`coverage-guide.md`](../../../coverage-guide.md) at the e2e root for the
> column + status-flag legend.

Sources reconciled (csp-bmad `_bmad-output/`):

| Source | Path |
|---|---|
| Gherkin (3 slices) | `implementation-artifacts/tests/UC-INBOX-00{1,2,3}/gherkin/UC-INBOX-00{1,2,3}-S01.feature` |
| Slice catalogues | `planning-artifacts/requirements/use-cases/UC-INBOX-00{1,2,3}/UC-INBOX-00{1,2,3}-slices.md` |
| Detailed UCs | `.../UC-INBOX-00{1,2,3}-detailed.md` |

…reconciled against the app's **actual read and write path**, which is the source of truth:
`pages/Inbox` → `pages/ViewSubmission` → `pages/Invoice` → `services/invoice.service` →
`InvoiceController.changeStatus` → `InvoiceService.changeStatus` →
`InvoiceValidator.validateForChangeStatus` → `InvoiceRepository.updateStatus`.

Test data (real, discovered 2026-09-25): pinned in `fixtures/inbox/review-test-data.ts`, with the
discovery query in the comment above each value. Seed image
`ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23`.

**SCOPE OF THIS PASS — the three S01 happy-path slices only**, as requested. Everything else across
these UCs (reject, cancel, the entry-user rule, missing conversion factors, filter/pagination/sort
variants) is unauthored and listed as a coverage gap below.

**RELATIONSHIP TO `features/inbox/uc-inbox-001-search/smoke.feature`.** That scenario is also
UC-INBOX-001-S01. It stays: it is the suite's *connection smoke*, deliberately the first thing to
fail when the stack is misconfigured, and it asserts the UI against the Inbox API row-for-row. This
journey covers S01 again from a different angle — as the entry point of a reviewer's path — and
asserts the one thing the smoke does not: that a specific submission awaiting review is findable
and its row data is correct. Neither is redundant; the S01 row below cites both.

## Scenario inventory

| Scenario | File | Tags | Result |
|---|---|---|---|
| A reviewer searches the inbox, opens an UNAPPROVED invoice, and approves it | `journey.feature` | `@p0 @UC-INBOX-001 @UC-INBOX-002 @UC-INBOX-003 @S01 @journey` | green |

## Coverage matrix

| Source item | Source citation | App enforcement point | Scenario (tags) | Status | Gap/defect |
|---|---|---|---|---|---|
| **UC-INBOX-001-S01** — inbox searched by date range | S01 `:22-26` | `pages/Inbox` → `/api/inbox?submissionDateFrom/To` | `journey` `@S01 @p0`; also `smoke.feature` | covered | — |
| Results table visible with at least one row | S01 `:27-29` | `ResultsTable` | `journey` `@S01 @p0`; also `smoke.feature` | covered | — |
| Columns: Submission ID, date, status, type, invoice counts | S01 `:31` | `inboxColumns` (`pages/Inbox/index.tsx:127…`) | `journey` `@S01 @p0` (reuses the smoke's column step) | covered | — |
| A submission awaiting review is found, with correct row data | S01 Background ("at least one submission in INB in range") | `InboxRepository.search` | `journey` `@S01 @p0` (asserts status + rendered date per column) | covered | — |
| Paginator "Showing <count>" | S01 `:30` | `ResultsTable` pagination | — | deferred | Coverage gap #4 |
| **UC-INBOX-002-S01** — navigate from the inbox to the invoice | S01 `:22-25` | Submission ID link → `/submission-history/:id`; Invoice # link → `/invoice/:id` | `journey` `@S01 @p0` | covered, re-grounded (one extra screen — see the feature header) | Spec gap #3 |
| Invoice page shows status UNAPPROVED | S01 `:27` | status pill (`InvoiceStatusTag`) | `journey` `@S01 @p0` | covered | — |
| Submission ID is populated | S01 `:28` | submission detail summary line | `journey` `@S01 @p0` (asserted on the submission page, where it exists) | covered, re-grounded | — |
| Group summary fieldset + table visible | S01 `:29-30` | "Invoice group summary" accordion + `ResultsTable` | `journey` `@S01 @p0` (asserts rows exist, not just the table) | covered | — |
| Boom Numbers / Timber Marks / Weigh Slips visible | S01 `:31-33` | `#boom-numbers` / `#timber-marks` / `#weigh-slips` | `journey` `@S01 @p0` (+ asserts the boom field carries a value) | covered, strengthened | Spec gap #1 |
| Approve enabled, Reject enabled | S01 `:34-35` | `canChangeStatus` + `canApprovePerm` / `canRejectPerm` | `journey` `@S01 @p0` | covered | — |
| Unapprove disabled | S01 `:36` | `canUnapprove ? <Unapprove/> : <Approve/>` | `journey` `@S01 @p0` (asserts **absent**, not disabled) | covered, re-grounded | **Spec gap #2** |
| **UC-INBOX-003-S01** — approve an UNAPPROVED invoice | S01 `:23-28` | `PATCH /api/invoices/{id}/status {status:"APP"}` | `journey` `@S01 @p0` | covered (UI **and** API read-back) | — |
| Approver must not be the invoice entrant | S01 `:24` ("entered by a different user than me") | `validateForChangeStatus` → `invoice.entry.user.cannot.approve.it.error` | `journey` `@S01 @p0` *complies* (borrows a legacy-entered invoice); the negative arm is unauthored | partially covered | **Coverage gap #1** |
| Approve button becomes disabled | S01 `:30` | button is replaced, not disabled | `journey` `@S01 @p0` | covered, re-grounded | Spec gap #2 |
| Unapprove becomes enabled | S01 `:31` | `canUnapprove` once status is APP | `journey` `@S01 @p0` | covered | — |
| Success message `The Invoice has been Approved successfully.` | S01 `:32` | toast `Invoice '<number>' approved.` | `journey` `@S01 @p0` | covered, re-grounded | Spec gap #4 |
| Invoice leaves the active review queue | UC-INBOX-003 title/intent | `applySubmissionStatusOnStatusChange` | `journey` `@S01 @p0` (asserts the submission does **not** move while PRO invoices remain) | covered, re-grounded | Spec gap #5 |
| Reviewer comment must be unmodified before approving | S01 `:26` | no such rule on APPROVE (`isReviewerCommentUpdate` targets DELETE/REJ/UNA/CAN) | — | not-applicable | Spec gap #6 |
| All line-item price conversion factors exist | S01 `:25` | `PriceConversionService` runs on SUBMIT, not on approve | — | not-applicable | Spec gap #6 |
| Background — WebADE auth + `invoiceDetails/Approve`, `/Reject` provisioning | all three S01 Backgrounds | Cognito groups / `usePermission` | — | not-applicable | Coverage gap #5 |

**Symmetry check.** One mirror matrix is in play and **only one arm is covered** — a genuine
asymmetry, and the highest-value thing to author next:

| Mirror matrix | Arm A | Arm B | Status |
|---|---|---|---|
| Reviewer decision on an UNA invoice | **Approve** — covered | **Reject** (UC-INBOX-004) | **asymmetric** — Coverage gap #2 |
| Approver identity | different user — **covered** | same user as entrant (must fail) | **asymmetric** — Coverage gap #1 |

**Role / permission coverage:** runs as `CSP_ADMIN`, which holds `invoiceDetails/Approve` and
`/Reject`. The `VIEW` arm (buttons absent or disabled) is unauthored — Coverage gap #5.

## Parallel-safety notes for anyone extending this UC

* **This journey mutates seeded data and puts it back.** It borrows one of twelve pinned UNAPPROVED
  invoices, indexed by Playwright's `parallelIndex` so concurrent copies never take the same row,
  and restores it to UNA in a fail-loud teardown that reads the row back. Verified with
  `--repeat-each=5`: 5/5 green, and all twelve pool members re-read as UNA afterwards with both
  submissions' statuses and the Inbox total unchanged.
* **The pool is finite.** With more workers than pool entries the fixture throws rather than
  double-booking a row. Add entries to `approvableInvoicePool` before raising the worker count.
* **`preflight/anchors.setup.ts` re-checks the whole pool is UNA** before any scenario runs, because
  nothing else would notice a failed restore — the snapshot fingerprint counts Inbox rows and
  lookup sizes, and neither moves when one invoice changes status.
* **Cap the workers when flake-checking** (`--workers=3`). At the default count a `--repeat-each=5`
  run over the whole suite saturates the single-container stack and the journeys blow the 60 s test
  timeout — it presents as `Request context disposed` inside an `expect.poll`, which looks like a
  test bug but is not. Measured: 6 workers → 4-5 timeouts (load avg ~11 on 14 cores); 3 workers →
  22/22 green. CI runs `workers: 1`. See the flake-check note in `e2e/README.md`.
* **Approving does not move the submission**, and the journey asserts that. It holds only because
  both pinned submissions keep many PROCESSING invoices
  (`applySubmissionStatusOnStatusChange` returns early while any remain). A future pool entry drawn
  from a submission whose *last* processing invoice it is would cascade the submission status, and
  the restore would NOT undo that — check before adding one.
