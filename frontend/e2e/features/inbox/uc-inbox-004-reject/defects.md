# Defects — UC-INBOX-004, reject an invoice

> New to these files? See [`defects-guide.md`](../../../defects-guide.md) at the e2e root for the
> register list, status lifecycle and how to read an entry.

Nothing in this file is a triaged defect yet. **BA/QA own triage** — no `JIRA-<key>` is set and
nothing is `CLOSED` here. Everything below was found while authoring the scenario against the
running stack on **2026-09-25**, and every behavioural claim was reproduced against the API.

Stack: seeded DB `ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23` on
`localhost:1525` → `csp-backend-e2e` on `:8080` → Vite on `:3001`.

---

## Divergence (app behaves differently from the Gherkin)

_None found._ Rejection behaves as the slice describes: an UNAPPROVED invoice with a reviewer
comment becomes REJECTED, the reason is stored, and the Reject button goes disabled. The two
differences below are a wrong precondition and restyled message text, not faults.

---

## Bug / Regression

_None found._

---

## Coverage gap (something the spec covers that no test covers)

### #1 — Rejecting WITHOUT a reviewer comment is not tested — OPEN
**What's wrong:** the reviewer comment is what makes a rejection auditable, and it is required —
enforced twice over. Only the happy path (a comment supplied) is covered.
**Expected vs actual:** not a fault — untested surface. Both guards were confirmed working while
authoring:
* client-side, `runStatusChange('REJ', true)` refuses an empty box with *"Reviewer comment is
  required for this action."* and never issues the request;
* server-side, `PATCH /api/invoices/{id}/status {"status":"REJ","reviewComments":""}` returns 400
  `invoice.reject.need.reviewer.comment.error` — *"For rejecting an invoice , reviewer comment is
  required."* (the odd spacing before the comma is in the real message).

**Next — this is the highest-value thing to author here.** It is a genuine no-write test, and the
suite already has the pieces: clear the comment box, click Reject, and assert both the inline error
**and**, via a mutation spy, that the status PATCH was never sent — proving nothing was written
rather than merely that an error appeared. The message text and the error string are already pinned
in `fixtures/inbox/review-test-data.ts` (`rejectWithoutCommentError`) for exactly this.

### #2 — The remaining UC-INBOX-004 slices are not authored — OPEN
**What's wrong:** this pass covers S01 only.
**Expected vs actual:** not a fault — deliberately deferred scope.
**Next:** author per concern, one `.feature` per concern in this folder, reusing the borrow fixture.

### #3 — Reaching the invoice via Invoice Search is not covered — OPEN
**What's wrong:** UC-INBOX-004-S01 reaches the invoice through `search.xhtml`. This scenario opens
`/invoice/:id` directly, because navigation is already covered end-to-end by the UC-INBOX-001..003
journey (Inbox → submission → invoice) and repeating it here would test the same links twice while
making this scenario about something other than rejection.
**Expected vs actual:** not a fault — a deliberate scoping choice, recorded so the ledger does not
imply the Search → invoice path is covered. It is **not**: the journey uses the Inbox path, not
Search.
**Next:** cover the Search → invoice link under the UC-SRCH use cases, where it belongs.

### #4 — No VIEW-role scenario, and WebADE provisioning is not automatable — OPEN
**What's wrong:** the scenario runs as `CSP_ADMIN`. Reject is permission-gated
(`invoiceDetails/Reject`) and the `VIEW` arm is unexercised. Separately, the legacy Background's
"authenticated and provisioned for invoiceDetails/Reject" has no equivalent — CSP uses Cognito
groups and the local mock provider grants one role unconditionally.
**Expected vs actual:** not a fault; untested surface plus a changed mechanism.
**Next:** author the VIEW arm with `Given I am signed in as a CSP VIEW`; drop WebADE provisioning
from the spec.

---

## Spec gap (app enforces something the spec never described, or the spec is wrong)

### #1 — The reviewer comment box is NOT empty when the reviewer arrives — OPEN
**What's wrong:** UC-INBOX-004-S01's precondition is that *"the Reviewer_Comment field has not yet
been filled"*. On this app it is populated: the box hydrates from the invoice's stored
`reviewComments`, and every seeded UNAPPROVED invoice already carries a note left by a legacy user.
(The note texts are production content and are not reproduced here.)
**Expected vs actual:** Expected an empty box; actual a box holding the existing note.
**How caught:** profiling the pinned pool through `GET /api/invoices/{id}` while working out what
the teardown would have to restore.
**What the test does:** asserts the box is **non-empty** on arrival, then replaces the contents. That
is deliberately stronger than the spec's version — filling a blank box would not prove the new
reason *replaced* the old note rather than being appended to it, and replacement is what actually
happens to the record.
**Consequence worth flagging to BA/QA:** rejecting **overwrites** the invoice's previous reviewer
note; there is no history of what it said before. Whether that is acceptable is a business
question, not a test one.
**Next:** BA/QA to correct the precondition, and to confirm the overwrite behaviour is intended.

### #2 — The rejection success text differs from the spec — OPEN
**What's wrong:** UC-INBOX-004-S01 expects the growl *"The Invoice has been Rejected successfully."*
This app shows a bottom-right toast reading **`Invoice '<number>' rejected.`**
**Expected vs actual:** as above. Consistent with the same finding across the SUBM use cases and
UC-INBOX-003 — every success message was restyled to name the invoice.
**How caught:** captured from the live app while authoring.
**What the test does:** asserts the toast, matching on the verb rather than pinning the invoice
number, because invoice numbers are not unique within a submission.
**Next:** BA/QA to decide which wording is canonical, once, for all of these.

---

## Verified — not a defect

### #1 — "The Reject button is disabled" re-grounds literally, where its Approve twin did not — VERIFIED
**What's wrong:** nothing, and the contrast is worth recording because it looks inconsistent. After
rejecting, the Reject button **stays on screen and goes disabled**, exactly as the legacy slice
says. After approving, the Approve button **disappears**, replaced by an Unapprove button (logged
as Spec gap #2 in the UC-INBOX-001..003 folder).
**Why they differ:** Approve and Unapprove share one slot in the button row
(`canUnapprove ? <Unapprove/> : <Approve/>`), so approving swaps the control. Reject has no paired
opposite, so it simply falls out of `STATUS_CHANGEABLE` (= {PRO, UNA}) and disables.
**Why not a defect:** both are coherent; only the Gherkin's uniform wording made them look alike.

### #2 — Rejecting does not move the submission out of the inbox — VERIFIED
**What's wrong:** nothing. Measured on invoice 2005369: rejecting left submission 119351 in
"Lobby", its `invRejected` count going 0 → 1 → 0 across the round trip, and the Inbox total at 50
throughout.
**Why:** `applySubmissionStatusOnStatusChange` returns early while any PROCESSING invoice remains
in the submission, and both pinned submissions keep many. Same behaviour as approving — see Spec
gap #5 in the UC-INBOX-001..003 folder, where the spec's "removed from the queue" wording is
raised.
**Why not a defect:** correct per the implemented rule. It is also the premise the borrow-and-restore
teardown depends on, which is why it was measured before the scenario was written.

### #3 — Borrowing and restoring survives a reject, including the reviewer note — VERIFIED
**What's wrong:** nothing, but this is the one way the reject scenario is riskier than its approve
twin, so it carries the record. Rejecting writes the reviewer note as well as the status, so a
teardown that restored only the status would leave the test's rejection reason on a seeded row
permanently — invisible to every existing guard, since no count moves.
**Expected vs actual:** measured round trip on one pinned invoice — status `UNA → REJ → UNA`, with
its original note replaced by the test's reason and then restored verbatim; submission "Lobby"
throughout; Inbox total 50.
After a `--repeat-each=5` run all twelve pool rows re-read as UNA with their original notes
verbatim.
**Guard:** the teardown captures both fields at borrow time, restores both in one PATCH, and reads
both back, failing loud on any difference. The fixture additionally refuses to borrow a row whose
note is NULL, because the API cannot write NULL back (the service skips the update when
`reviewComments` is null) — so such a row could never be restored after a reject. The preflight
asserts that invariant across the whole pool.
