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

### BUG-001 — The Unapprove button is the one decision control with no permission check — OPEN

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

**Not covered by a test in this pass** — it needs a VIEW-role scenario, which is out of scope for
S01. See Coverage gap #2.

### BUG-002 — Unapprove demands a reviewer comment in the UI but not in the backend — OPEN

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
a script, a future integration — can do so with no audit reason. Whether the requirement is meant to
be a business rule (enforce it server-side) or merely a UI prompt (drop the pretence) is a BA/QA
call.

**Not covered by a test in this pass.** See Coverage gap #1.

---

## Coverage gap (something the spec covers that no test covers)

### #1 — Unapproving WITHOUT a reviewer comment is not tested — OPEN
**What's wrong:** only the happy path (a comment supplied) is covered. The client-side gate is
unexercised, and the backend's silence on it is BUG-002.
**Expected vs actual:** not a fault in the UI — the gate works; it is untested surface.
**Next:** a UI no-write test — clear the comment box, click Unapprove, assert the inline error and
(via a mutation spy) that no status PATCH was sent. Note this is a *client-only* rule, so unlike the
reject equivalent it cannot also be asserted against the API; that asymmetry is the point of
BUG-002.

### #2 — No VIEW-role scenario, which is where BUG-001 would show — OPEN
**What's wrong:** the scenario runs as `CSP_ADMIN`. The VIEW arm is unauthored, and it is the arm
that would demonstrate the missing permission check on Unapprove.
**Expected vs actual:** untested surface.
**Next:** open an APPROVED invoice as `CSP VIEW` and assert Unapprove is disabled. **This test will
FAIL today** — it is the natural `@discovered-divergence` red for BUG-001, and worth authoring for
exactly that reason once BA/QA confirm the button should be gated.

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
