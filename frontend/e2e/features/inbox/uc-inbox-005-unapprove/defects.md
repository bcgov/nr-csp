# Defects — UC-INBOX-005, unapprove an invoice

> New to these files? See [`defects-guide.md`](../../../defects-guide.md) at the e2e root for the
> register list, status lifecycle and how to read an entry.

Nothing in this file is a triaged defect yet. **BA/QA own triage** — no `JIRA-<key>` is set and
nothing is `CLOSED` here. Everything below was found while authoring the scenario against the
running stack on **2026-09-25**, and every behavioural claim was reproduced against the API or read
from the source.

Stack: seeded DB `ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23` on
`localhost:1525` → `csp-backend-e2e` on `:8080` → Vite on `:3001`.

---

## Divergence (app behaves differently from the Gherkin)

_None found._ Unapproving behaves as the slice describes: an APPROVED invoice returns to
UNAPPROVED, Approve and Submit become available again, and the reason is stored. The differences
below are restyled text and a button-rendering change, not faults.

---

## Bug / Regression

### BUG-001 — The Unapprove button is the one decision control with no permission check — FIXED

**What's wrong.** Every other decision button on the invoice screen is disabled unless the signed-in
user holds the matching permission — Approve checks `invoiceDetails/Approve`, Reject checks
`/Reject`, Cancel checks `/Cancel`, Delete checks `/Delete`. **Unapprove checks nothing.** Its only
condition is that no other request is in flight.

**Expected vs actual.**
* Expected: a user without `invoiceDetails/Unapprove` sees the button disabled, as they do for
  every other decision.
* Actual: the button is enabled for anyone who can open an APPROVED invoice. A VIEW-role user —
  who holds **no** invoice decision permission at all — gets a live-looking Unapprove button that
  fails with a 403 when clicked.

**How caught.** Re-grounding the slice's Background ("provisioned for the `invoiceDetails/Unapprove`
action") against the app, to decide what the test should assert about permissions. The permission
constant `INVOICE_DETAILS_UNAPPROVE` exists and is granted to the ADMIN and APPROVE roles in
`context/auth/permissions.ts`, but nothing reads it — the only references are its declaration and
the two role sets.

**Severity — lower than it first looks, and worth stating plainly.** This is a **UI-only**
inconsistency, not an authorization hole. The backend does enforce it:
`InvoiceController.changeStatus` carries
`@PreAuthorize("… 'invoiceDetails/Approve' or … 'Reject' or … 'Cancel' or … 'Unapprove'")`, and the
VIEW role holds none of those four, so the request is refused server-side. The consequence is a
misleading control and a raw failure instead of a disabled button.

**FIXED** on branch `defects-from-e2e`. The button now checks `INVOICE_DETAILS_UNAPPROVE`, matching
every other decision control, so a user without it sees the button disabled rather than live.

Covered two ways, and both were confirmed to fail before the fix: a unit test that denies **only**
`invoiceDetails/Unapprove` — proving the button is gated on its own permission rather than on any
permission being absent — and a `@VIEW-role` e2e scenario that opens an APPROVED invoice as
`CSP VIEW` and asserts Unapprove is visible but disabled, along with the rest of the decision set.
Awaiting BA/QA confirmation before this entry is closed.

### BUG-002 — Unapprove demands a reviewer comment in the UI but not in the backend — FIXED

**What's wrong.** To unapprove, the screen requires a reviewer comment and refuses an empty box with
*"Reviewer comment is required for this action."* The backend does not: it only enforces the
comment for **reject**.

**Expected vs actual.** Verified against the running backend:
* `PATCH /api/invoices/{id}/status {"status":"REJ","reviewComments":""}` → **400**
  `invoice.reject.need.reviewer.comment.error`
* `PATCH /api/invoices/{id}/status {"status":"UNA","reviewComments":""}` → **200**, the unapproval
  goes through with no reason recorded.

`InvoiceValidator.validateForChangeStatus` has exactly two rules — the entry-user check for APPROVE
and the blank-comment check for REJECT. Nothing covers UNAPPROVE or CANCEL, though the UI requires a
comment for both.

**Impact.** Anything that reverses an approval without going through this screen — another client,
a script, a future integration — could do so with no audit reason.

**FIXED** on branch `defects-from-e2e`, as a business rule, matched to the legacy app rather than
invented. The legacy source (`/home/rylanevans/nr-csp`) has the answer, and it is **stronger** than
"non-empty": `InvoiceValidator.isReviewerCommentUpdate` required the reviewer comment to have
**CHANGED** from the stored value, and legacy `InvoiceService.changeInvoiceStatus` called it from
the status-change path with the new status code (legacy `InvoiceService.java:665`); the delete path
called it too (`:422`). The message is the one already in this app's bundle:
*"Please enter the changes in the Reviewer Comment field below!"*

The rule had in fact been ported — but was unreachable. It was only ever called from
`validate(...)` with `action.toString()`, an `ActionType` whose values are SAVE/SUBMIT/DELETE/OTHER,
so its comparisons against the status codes `REJ`, `UNA` and `CAN` could never match, and
`validateForChangeStatus` never called it at all. It now does.

One subtlety worth recording: legacy's `manual` flag gates the rule, and legacy's
`getInvoiceValidator()` set it to `true` unconditionally — the only `setManual(false)` is in
`SubmissionValidator`, the ESF **ingestion** path. So the flag says which validator is running, not
where the invoice came from, and the rule applies to every invoice reviewed through the UI. A first
attempt at this fix derived `manual` from the invoice's origin and was wrong; the legacy source
settled it.

**Verified against the live seeded database:**

| Attempt | Result |
|---|---|
| same comment as stored | **400** — "Please enter the changes in the Reviewer Comment field below!" |
| comment omitted (`null`) | **400** |
| blank comment (`""`) | 200 — blanking IS a change, which legacy permitted |
| a genuinely new reason | 200 |
| **cancel** with the same comment | **400** — the rule covers CAN as well |

⚠ **The blank case is a deliberate match to legacy, not an oversight.** Legacy's rule is
"must have changed", so clearing a note counts. Only **reject** additionally requires a non-blank
comment (`invoice.reject.need.reviewer.comment.error`). If unapprove and cancel should also require
non-blank, that is a **departure from legacy** and needs BA/QA sign-off — it is a one-line addition.

Five validator tests and one service test cover it, including that omitting the comment cannot
bypass the rule. Awaiting BA/QA confirmation before this entry is closed.

---

## Coverage gap (something the spec covers that no test covers)

### #1 — Unapproving with an unchanged comment is not covered end-to-end — OPEN
**What's wrong:** the rule is now enforced server-side and covered by backend tests, but no UI
scenario drives it — open an approved invoice, leave the comment untouched, click Unapprove, and
assert the inline error.
**Expected vs actual:** not a fault — untested surface at the UI layer.
**Next:** worth authoring, and it is now assertable on BOTH sides (the error comes back from the
API), unlike when the rule was client-only. Note the UI's own gate is still "non-empty" rather than
"changed", so the client will happily submit an unchanged comment and surface the server's error
inline on the reviewer-comment field — which is the behaviour to assert.

### #2 — No VIEW-role scenario, which is where BUG-001 would show — CLOSED
**What was missing:** a VIEW arm, which is what would demonstrate the ungated Unapprove button.
**Now covered:** the `@VIEW-role` scenario in `unapprove.feature` opens an APPROVED invoice as
`CSP VIEW` and asserts Unapprove is visible but disabled, and that Reject and Cancel are too. It was
verified to fail before the BUG-001 fix, so it genuinely guards it.
**Still open for this UC:** the VIEW arm of the *happy path* (a viewer should not be able to
unapprove successfully) is implied by the above but not separately asserted.

### #3 — "Unapprove never moves the submission" is not asserted — OPEN
**What's wrong:** `applySubmissionStatusOnStatusChange` returns early for UNA, so unapproving can
never change the parent submission's status — unlike approve/reject/cancel, which can. The scenario
does not assert it.
**Expected vs actual:** untested surface; the behaviour was read from the source, not observed.
**Next:** one assertion against `/api/inbox`, mirroring the one the approve journey already makes.

### #4 — Re-approving after an unapprove is not covered — OPEN
**What's wrong:** the point of unapproving is to correct and re-approve. The scenario stops at UNA
and asserts Approve is available, but never completes the round trip.
**Expected vs actual:** untested surface.
**Next:** extend this scenario or add a sibling; the borrow fixture restores the row either way.

### #5 — The remaining UC-INBOX-005 slices are not authored — OPEN
**What's wrong:** this pass covers S01 only.
**Next:** author per concern, one `.feature` per concern in this folder.

---

## Spec gap (app enforces something the spec never described, or the spec is wrong)

### #1 — "The Unapprove button is disabled" — there is no Unapprove button left to disable — OPEN
**What's wrong:** UC-INBOX-005-S01 expects Unapprove to become disabled and Approve to become
enabled. This app renders the two into the **same slot**
(`canUnapprove ? <Unapprove/> : <Approve/>`), so unapproving *replaces* Unapprove with Approve.
**Expected vs actual:** Expected a disabled button; actual no button.
**How caught:** re-grounding the button-state assertions against `pages/Invoice/index.tsx`.
**What the test does:** asserts the swap — Approve present and enabled, Unapprove absent. Exactly
the mirror of the same finding on the approve side (Spec gap #2 in the UC-INBOX-001..003 folder),
and the same wording problem recurs in three slices now.
**Next:** BA/QA to fix the wording once, across UC-INBOX-003 and UC-INBOX-005.

### #2 — The unapprove success text differs from the spec — OPEN
**What's wrong:** the slice expects *"The Invoice has been Un-Approved successfully."* This app
shows a toast reading **`Invoice '<number>' unapproved.`**
**Expected vs actual:** as above. The fourth use case in a row with restyled success text — see the
matching entries under UC-SUBM-001..004, UC-INBOX-003 and UC-INBOX-004.
**How caught:** captured from the live app while authoring.
**Next:** BA/QA to settle the message convention once for all of these rather than slice by slice.

### #3 — The reviewer comment box is not empty on arrival — OPEN
**What's wrong:** every seeded invoice carries a reviewer note, so the box is populated when the
reviewer arrives; the scenario replaces its contents rather than filling a blank field.
**Expected vs actual:** not stated in this slice, but the same mismatch UC-INBOX-004-S01 states
explicitly ("has not yet been filled").
**Consequence worth flagging:** as with reject, unapproving **overwrites** the previous reviewer
note with no history of what it said.
**Next:** BA/QA to confirm the overwrite behaviour is intended — the question applies to reject and
unapprove alike.

---

## Verified — not a defect

### #1 — Approving the borrowed invoice as an ARRANGE step is legitimate — VERIFIED
**What's wrong:** nothing, but it deserves the record because it looks like the scenario is testing
two things. The pool holds UNAPPROVED invoices and this use case starts from an APPROVED one, so the
scenario approves via the **API** and unapproves via the **UI**. Approving through the UI first
would have made it a second copy of UC-INBOX-003 and obscured which action was under test.
**Why it works at all:** the backend refuses an approval by the invoice's own entry user, and the
borrowed rows were entered by legacy IDIR accounts — so `local-dev-user` may approve them. A
suite-created invoice could not be used here for the same reason it cannot be used in UC-INBOX-003.
**Why it leaves nothing behind:** the arrange passes `reviewComments: null`, so the service skips
the note write and the seeded note survives into the scenario; the teardown then restores the row to
the status and note it had when borrowed. Verified round trip on one pinned invoice —
`UNA → APP → UNA`, with its original note replaced by the test's reason and then restored verbatim.
After a `--repeat-each=5` run all twelve pool rows re-read as UNA with their original notes intact.
