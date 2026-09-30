# Coverage — UC-INBOX-004, reject an invoice

> New to these files? See [`coverage-guide.md`](../../../coverage-guide.md) at the e2e root for the
> column + status-flag legend.

Sources reconciled (csp-bmad `_bmad-output/`):

| Source | Path |
|---|---|
| Gherkin | `implementation-artifacts/tests/UC-INBOX-004/gherkin/UC-INBOX-004-S01.feature` |
| Slice catalogue | `planning-artifacts/requirements/use-cases/UC-INBOX-004/UC-INBOX-004-slices.md` |
| Detailed UC | `.../UC-INBOX-004-detailed.md` |

…reconciled against the app's **actual write path**: `pages/Invoice` (`handleReject` →
`runStatusChange('REJ', true)`) → `PATCH /api/invoices/{id}/status` →
`InvoiceService.changeStatus` → `InvoiceValidator.validateForChangeStatus` →
`InvoiceRepository.updateReviewerNotes` + `updateStatus`.

Test data: the shared borrowable pool in `fixtures/inbox/review-test-data.ts`.

**SCOPE OF THIS PASS — the S01 happy path only**, as requested.

**This closes the Approve/Reject asymmetry** flagged as Coverage gap #2 in
`features/inbox/uc-inbox-001-003-review-approve-journey/defects.md`. Both arms of the reviewer's
decision are now covered, and both use the same borrow-and-restore fixture.

## Scenario inventory

| Scenario | File | Tags | Result |
|---|---|---|---|
| Rejecting an UNAPPROVED invoice records the status change and the reason | `reject.feature` | `@p0 @UC-INBOX-004 @S01` | green |

## Coverage matrix

| Source item | Source citation | App enforcement point | Scenario (tags) | Status | Gap/defect |
|---|---|---|---|---|---|
| Invoice is in UNAPPROVED status before rejecting | S01 `:19` | `STATUS_CHANGEABLE` = {PRO, UNA} | `reject` `@S01 @p0` | covered (asserted, and re-checked by the borrow fixture) | — |
| Reviewer Comment field starts empty | S01 `:20` | box hydrates from `reviewComments` | `reject` `@S01 @p0` (asserts the **opposite** — it starts populated) | covered, re-grounded | **Spec gap #1** |
| Navigate via Invoice Search to the invoice | S01 `:22-23` | `/search` → `/invoice/:id` | — (opens the invoice directly) | not-applicable here | Coverage gap #3 |
| Fill the Reviewer Comment with a rejection reason | S01 `:24` | `#reviewer-comment` → `reviewComments` in the PATCH body | `reject` `@S01 @p0` | covered (asserts the field took the value) | — |
| Click Reject | S01 `:25` | `handleReject` | `reject` `@S01 @p0` | covered | — |
| Status transitions to REJ | S01 `:26`, `:29` | `updateStatus(id, 'REJ', user)` | `reject` `@S01 @p0` (UI **and** API read-back) | covered | — |
| The rejection reason is persisted | implied by the UC's "audit trail" intent | `updateReviewerNotes` when `reviewComments != null` | `reject` `@S01 @p0` (API read-back of the exact text) | covered, strengthened | — |
| Success message `The Invoice has been Rejected successfully.` | S01 `:27` | toast `Invoice '<number>' rejected.` | `reject` `@S01 @p0` | covered, re-grounded | Spec gap #2 |
| Reject button becomes disabled | S01 `:28` | `canChangeStatus` false once status is REJ | `reject` `@S01 @p0` | **covered, unchanged** — the one button assertion that re-grounds literally | — |
| Approve also closed off after rejecting | not in the Gherkin (new assertion) | same `canChangeStatus` gate | `reject` `@S01 @p0` | covered (added — see note) | — |
| Reviewer comment is REQUIRED to reject | `invoice.reject.need.reviewer.comment.error`; `runStatusChange('REJ', true)` | client gate **and** `validateForChangeStatus` | — (the happy path supplies one) | partially covered | **Coverage gap #1** |
| Line items survive the rejection | not in the Gherkin | — | `reject` `@S01 @p0` | covered (added) | — |
| Background — WebADE auth + `invoiceDetails/Reject` provisioning | S01 Background | Cognito groups / `usePermission` | — | not-applicable | Coverage gap #4 |

Two assertions are **additions**, not re-groundings: that Approve is closed off too (a regression
reopening it on a rejected invoice would otherwise go unseen), and that the line items are
untouched. Both are cheap and guard the same write path.

**Symmetry check.** The decision matrix is now balanced; the remaining asymmetry is within this UC:

| Mirror matrix | Arm A | Arm B | Status |
|---|---|---|---|
| Reviewer decision on an UNA invoice | Approve — covered (UC-INBOX-003) | **Reject — covered (here)** | **symmetric** ✅ |
| Reject with / without a reviewer comment | with a comment — **covered** | without a comment (must fail) | **asymmetric** — Coverage gap #1 |

**Role / permission coverage:** runs as `CSP_ADMIN`, which holds `invoiceDetails/Reject`. The
`VIEW` arm is unauthored — Coverage gap #4.

## Parallel-safety notes

* **This scenario mutates seeded data and puts TWO fields back.** Rejecting overwrites the
  invoice's reviewer note as well as its status, so the teardown restores both and reads both back.
  That is the one way this differs from the approve journey, which only has to restore the status.
* It borrows from the **same pool** as the approve journey, indexed by `parallelIndex` — so the two
  scenarios running concurrently take different rows automatically. Verified with
  `--repeat-each=5 --workers=3`: 5/5 green, and afterwards all twelve pool rows re-read as UNA with
  their original notes verbatim.
* **A pool row with a NULL reviewer note would be unrestorable**, because the service skips the
  note write when `reviewComments` is null — the test's reason could never be removed again. The
  borrow fixture refuses such a row and the preflight asserts the invariant across the pool. Keep
  that in mind before adding entries.
* **Cap the workers when flake-checking** (`--workers=3`) — see the note in `e2e/README.md`.
