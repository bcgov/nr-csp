# Coverage — UC-INBOX-005, unapprove an invoice

> New to these files? See [`coverage-guide.md`](../../../coverage-guide.md) at the e2e root for the
> column + status-flag legend.

Sources reconciled (csp-bmad `_bmad-output/`):

| Source | Path |
|---|---|
| Gherkin | `implementation-artifacts/tests/UC-INBOX-005/gherkin/UC-INBOX-005-S01.feature` |
| Slice catalogue | `planning-artifacts/requirements/use-cases/UC-INBOX-005/UC-INBOX-005-slices.md` |
| Detailed UC | `.../UC-INBOX-005-detailed.md` |

…reconciled against the app's **actual write path**: `pages/Invoice` (`handleUnapprove` →
`runStatusChange('UNA', true)`) → `PATCH /api/invoices/{id}/status` → `InvoiceService.changeStatus`
→ `InvoiceValidator.validateForChangeStatus` → `InvoiceRepository.updateReviewerNotes` +
`updateStatus`.

Test data: the shared borrowable pool in `fixtures/inbox/review-test-data.ts`.

**SCOPE OF THIS PASS — the S01 happy path only**, as requested.

**This completes the reviewer decision set**: Approve (UC-INBOX-003), Reject (UC-INBOX-004) and
Unapprove (here) are now all covered, sharing one borrow-and-restore fixture.

## Scenario inventory

| Scenario | File | Tags | Result |
|---|---|---|---|
| Unapproving an APPROVED invoice returns it to UNAPPROVED with a reason recorded | `unapprove.feature` | `@p0 @UC-INBOX-005 @S01` | green |

## Coverage matrix

| Source item | Source citation | App enforcement point | Scenario (tags) | Status | Gap/defect |
|---|---|---|---|---|---|
| An invoice exists in APPROVED status | S01 `:19` | — | `unapprove` `@S01 @p0` (arranged via `PATCH … {status:"APP"}`) | covered | — |
| The invoice is open on the Invoice Details page | S01 `:20` | `/invoice/:id` | `unapprove` `@S01 @p0` | covered | — |
| Unapprove button is enabled | S01 `:21` | `canUnapprove = isExisting && status === 'APP'` | `unapprove` `@S01 @p0` (+ asserts Approve is absent — they share a slot) | covered, strengthened | — |
| Click Unapprove | S01 `:22` | `handleUnapprove` | `unapprove` `@S01 @p0` | covered | — |
| Message `The Invoice has been Un-Approved successfully.` | S01 `:23` | toast `Invoice '<number>' unapproved.` | `unapprove` `@S01 @p0` | covered, re-grounded | Spec gap #2 |
| Status updated to UNAPPROVED | S01 `:24` | `updateStatus(id, 'UNA', user)` | `unapprove` `@S01 @p0` (UI **and** API read-back) | covered | — |
| Unapprove button becomes disabled | S01 `:25` | button is replaced by Approve, not disabled | `unapprove` `@S01 @p0` (asserts **absent**) | covered, re-grounded | **Spec gap #1** |
| Approve button becomes enabled | S01 `:26` | `canChangeStatus` (UNA ∈ {PRO, UNA}) + `canApprovePerm` | `unapprove` `@S01 @p0` | covered | — |
| Submit button becomes enabled | S01 `:27` | `canSubmit` (UNA ∈ SUBMITTABLE_STATUSES) | `unapprove` `@S01 @p0` (asserts **enabled**, not merely visible) | covered | — |
| The unapprove reason is persisted | implied by the UC's "corrections" intent | `updateReviewerNotes` when `reviewComments != null` | `unapprove` `@S01 @p0` (API read-back of the exact text) | covered, strengthened | — |
| A reviewer comment is required to unapprove | `runStatusChange('UNA', true)` — client only | UI gate only; the backend accepts a blank one | — (the happy path supplies one) | partially covered | **BUG-002**, Coverage gap #1 |
| Line items survive the unapproval | not in the Gherkin | — | `unapprove` `@S01 @p0` | covered (added) | — |
| Reviewer comment box starts empty | not stated in this slice, but implied by "fill" flows elsewhere | box hydrates from `reviewComments` | `unapprove` `@S01 @p0` (asserts it starts **populated**) | covered, re-grounded | Spec gap #3 |
| Unapprove never moves the submission | not in the Gherkin | `applySubmissionStatusOnStatusChange` returns early for UNA | — | deferred | Coverage gap #3 |
| Background — WebADE auth + `invoiceDetails/Unapprove` provisioning | S01 Background | **not checked on the button** — see BUG-001 | — | not-applicable as written | **BUG-001**, Coverage gap #2 |

**Symmetry check.** The decision set is now complete, and the remaining asymmetries are within
this UC:

| Mirror matrix | Arm A | Arm B | Status |
|---|---|---|---|
| Reviewer decision on an invoice | Approve ✅ / Reject ✅ | **Unapprove ✅ (here)** | **symmetric** ✅ |
| Approve ↔ Unapprove round trip | APP → UNA — **covered** | UNA → APP again (re-approve) | **asymmetric** — Coverage gap #4 |
| Unapprove with / without a comment | with — **covered** | without (UI refuses, backend does not) | **asymmetric** — Coverage gap #1 |

**Role / permission coverage:** runs as `CSP_ADMIN`. The `VIEW` arm is both unauthored *and*
interesting here, because the Unapprove button is the one decision control with no permission check
— see BUG-001.

## Parallel-safety notes

* Borrows from the **same pool** as the approve and reject scenarios, indexed by `parallelIndex`,
  so all three can run concurrently without taking the same row. Verified with
  `--repeat-each=5 --workers=3`: 5/5 green, and afterwards all twelve rows re-read as UNA with
  their original notes verbatim.
* **The arrange step needs no undo.** It approves the borrowed invoice via the API, and the
  teardown restores whatever the row had when borrowed (UNA + its seeded note) — so a scenario that
  fails between the arrange and the act still hands back a correctly-UNA row.
* The arrange deliberately passes `reviewComments: null` so the seeded note survives into the
  scenario; that is what makes "the box already holds the existing note" a real assertion rather
  than one about a value the test itself wrote.
* **Cap the workers when flake-checking** (`--workers=3`) — see the note in `e2e/README.md`.
