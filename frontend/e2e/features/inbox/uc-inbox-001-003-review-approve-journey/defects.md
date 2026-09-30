# Defects — UC-INBOX-001 / 002 / 003, review-and-approve journey

> New to these files? See [`defects-guide.md`](../../../defects-guide.md) at the e2e root for the
> register list, status lifecycle and how to read an entry.

Nothing in this file is a triaged defect yet. **BA/QA own triage** — no `JIRA-<key>` is set and
nothing is `CLOSED` here. Everything below was found by authoring the journey against the running
stack on **2026-09-25**, and every behavioural claim was reproduced directly against the API.

Stack: seeded DB `ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23` on
`localhost:1525` → `csp-backend-e2e` on `:8080` → Vite on `:3001`.

---

## Divergence (app behaves differently from the Gherkin)

_None found in this pass._ Every difference between the legacy Gherkin and this app turned out to
be an intended redesign or a spec inaccuracy, recorded under **Spec gap** below rather than as a
fault. The three slices' substance — find the submission, review the invoice, approve it — holds.

---

## Bug / Regression

_None found in this pass._

---

## Coverage gap (something the spec covers that no test covers)

### #1 — The "approver cannot be the invoice entrant" rule is never exercised — OPEN
**What's wrong:** UC-INBOX-003-S01's precondition is *"the invoice was entered by a different user
than me"*. The journey satisfies it, but nothing tests the negative — that approving your own
invoice is refused with *"The user who entered the invoice cannot approve it."*
**Expected vs actual:** not a fault — untested surface.
**Why it is not authored:** and why it is awkward. Locally every request is authenticated as ONE
fixed username (`auth.mock.username`, default `local-dev-user`, no per-request override in
`MockRequestFilter`). So the suite can easily produce the *failing* case — create an invoice, try to
approve it — but it can only reach the *passing* case by borrowing a seeded invoice entered by a
legacy user, which is what this journey does.
**Next:** the negative arm is cheap and worth having, because it is the rule this whole slice turns
on: create an invoice via the API, submit it to PRO, click Approve, and assert both the error and —
via the mutation spy — that no status change was written. That is a genuinely stronger test than
the happy path.

### #2 — Reject is not covered — OPEN
**What's wrong:** Approve and Reject are the two arms of the reviewer's decision, and only Approve
is authored. Reject additionally requires a reviewer comment
(`invoice.reject.need.reviewer.comment.error`), which is unexercised.
**Expected vs actual:** not a fault — untested surface. This is the asymmetry flagged in
`coverage.md`.
**Next:** author alongside this journey, reusing the same borrow-and-restore fixture — a rejected
invoice restores to UNA exactly the same way.

### #3 — Unapprove (APP → UNA) is not covered as a user action — OPEN
**What's wrong:** the journey asserts the Unapprove button appears after approving, but never
clicks it. Ironically the teardown performs that very transition over the API, so the path is known
to work — it just is not tested through the UI.
**Expected vs actual:** not a fault — untested surface.
**Next:** a short scenario continuing from the approval; the borrow-and-restore fixture already
leaves the row correct either way.

### #4 — Inbox paginator / result count not asserted — OPEN
**What's wrong:** UC-INBOX-001-S01 expects the paginator to show "Showing <count>". Neither this
journey nor the existing smoke scenario asserts it; both assert row counts directly.
**Expected vs actual:** not a fault — untested surface. (Also carried as Coverage gap #2 in
`features/inbox/uc-inbox-001-search/defects.md`; recorded in both places because either could be
the one authored.)
**Next:** assert the paginator text when the remaining UC-INBOX-001 slices are authored.

### #5 — No VIEW-role scenario, and WebADE provisioning is not automatable — OPEN
**What's wrong:** the journey runs as `CSP_ADMIN`. The Approve / Reject buttons are permission
gated (`invoiceDetails/Approve`, `/Reject`), and the `VIEW` arm is unexercised. Separately, the
legacy Backgrounds' "authenticated via WebADE" and per-action provisioning have no equivalent —
CSP uses Cognito groups, and the local mock provider grants one role unconditionally.
**Expected vs actual:** not a fault; untested surface plus a changed mechanism.
**Next:** author the VIEW arm with `Given I am signed in as a CSP VIEW` (the existing step already
supports it); drop WebADE provisioning from the spec.

---

## Spec gap (app enforces something the spec never described, or the spec is wrong)

### #1 — Source-document panels always render, so "is visible" proves nothing — OPEN
**What's wrong:** UC-INBOX-002-S01 asserts the Boom Numbers, Timber Marks and Weigh Slips elements
are *visible*. On this screen all three are ordinary form fields that render in every status
whether or not the invoice has any values, so the assertion passes on an invoice with none.
**Expected vs actual:** not a fault — the assertion is just weaker than it reads. All twelve pinned
UNAPPROVED invoices carry exactly one boom number and no timber marks or weigh slips.
**How caught:** profiling the pool through `GET /api/invoices/{id}` while pinning the fixtures.
**What the test does:** asserts all three fields are present **and** that the boom field carries at
least one committed value, so it cannot pass on an empty invoice.
**Next:** BA/QA to confirm the intent was "the invoice's source documents are shown", which is what
the test now checks.

### #2 — "Unapprove is disabled" — there is no Unapprove button to disable — OPEN
**What's wrong:** UC-INBOX-002-S01 expects a disabled Unapprove button on an UNAPPROVED invoice,
and UC-INBOX-003-S01 expects Approve to "become disabled" after approving. This app renders Approve
and Unapprove into the **same slot** — one or the other, never both
(`canUnapprove ? <Unapprove/> : <Approve/>`). So an UNA invoice has no Unapprove button at all, and
approving *replaces* Approve with Unapprove rather than disabling it.
**Expected vs actual:** Expected a disabled button; actual no button.
**How caught:** re-grounding the button-state assertions against `pages/Invoice/index.tsx`.
**What the test does:** asserts absence before, and the swap after — same intent, stated the way the
app behaves.
**Next:** BA/QA to update the wording in both slices.

### #3 — The journey passes through a screen the spec does not mention — OPEN
**What's wrong:** UC-INBOX-002-S01 goes straight from an inbox row to the invoice
(`invoiceDetails.xhtml`). This app puts a submission detail page in between: the Inbox links to
`/submission-history/:submissionId`, which lists the submission's invoices, each linking on to
`/invoice/:id`.
**Expected vs actual:** a deliberate redesign, not a fault.
**How caught:** re-grounding the navigation steps against `App.tsx` and `pages/Inbox/index.tsx`.
**What the test does:** follows the app's real path and asserts the middle screen too, rather than
jumping to the invoice URL — the linkage is the part worth testing.
**Next:** BA/QA to add the intermediate screen to the spec.

### #4 — The approval success text differs from the spec — OPEN
**What's wrong:** UC-INBOX-003-S01 expects the growl *"The Invoice has been Approved successfully."*
This app shows a bottom-right toast reading **`Invoice '<number>' approved.`**
**Expected vs actual:** as above. Consistent with the same finding across the SUBM use cases, where
every success message was likewise restyled to name the invoice.
**How caught:** captured from the live app while authoring.
**What the test does:** asserts the toast, matching on the verb rather than pinning the invoice
number — invoice numbers are **not unique within a submission** (see the observation below), so
pinning the number would be a weaker assertion, not a stronger one.
**Next:** BA/QA to decide which wording is canonical and update the spec or the app.

### #5 — Approving does not remove the submission from the inbox — OPEN
**What's wrong:** UC-INBOX-003's stated outcome is that the invoice "is removed from the active
inbox queue". Approving one invoice leaves the submission exactly where it was; the submission only
moves once **no PROCESSING invoices remain** in it, and then to Complete or Rejected
(`applySubmissionStatusOnStatusChange`).
**Expected vs actual:** verified — approving invoice 2005425 left submission 119351 in "Lobby"
throughout, with the Inbox total unchanged at 50.
**How caught:** the API dry run taken before writing the scenario, to measure the blast radius of a
borrowed-row approval.
**What the test does:** asserts the submission **stays put**, which is both the real behaviour and
the premise the borrow-and-restore teardown depends on.
**Next:** BA/QA to restate the outcome at the invoice level ("the invoice leaves the queue") rather
than the submission level.

### #6 — Two stated preconditions have no counterpart in this app — OPEN
**What's wrong:** UC-INBOX-003-S01 requires that "all line item price conversion factors exist" and
that "the Reviewer Comment field has not been modified since the page loaded". Neither constrains an
approval here: price conversion runs on **submit** (`PriceConversionService` in
`InvoiceService.submit`), not on approve; and the reviewer-comment rule
(`isReviewerCommentUpdate`) applies to DELETE / REJECT / UNAPPROVE / CANCEL — approve is explicitly
not in that set.
**Expected vs actual:** not a fault; the rules live on different actions than the slice claims.
**How caught:** reconciling the slice's preconditions against `InvoiceValidator`.
**What the test does:** nothing — both are marked `not-applicable` in `coverage.md` rather than
silently dropped.
**Next:** BA/QA to move these preconditions onto the actions that actually carry them.

---

## Verified — not a defect

### #1 — Invoice numbers repeat within a submission — VERIFIED
**What's wrong:** nothing, but it is a trap. Within one pinned submission four invoices share a
single invoice number, and several in the other share another. A test locating a row by invoice
number would match several rows, and a `.first()` would silently pick an arbitrary one. (The numbers
are client-supplied production data and are not reproduced here.)
**Expected vs actual:** expected uniqueness; actual duplicates, which the app itself anticipates —
`pages/ViewSubmission/index.tsx` keys its table on the invoice id "because invoice numbers can be
blank or duplicated within a submission".
**How caught:** profiling the twelve pinned invoices via `GET /api/invoices/{id}`.
**Why not a defect:** `client_invoice_no` is client-supplied and has never been unique. Every
locator in this journey addresses invoices by id, and the fixture file carries a warning.

### #2 — The same invoice status reads differently on two screens — VERIFIED
**What's wrong:** nothing, but it fails a re-ground in a confusing way. The invoice page shows the
status **code** ("UNA", "APP"); the submission detail page's Decision column shows the
**description** ("Unapproved", "Approved").
**Expected vs actual:** both are rendered by the same `InvoiceStatusTag`, which prints whatever it
is handed and maps either form to a colour. They differ because the endpoints do:
`InvoiceResponse.invStatus` returns the code, the submission-detail response returns the resolved
description.
**How caught:** the journey's first run failed asserting "UNA" against a cell reading "Unapproved".
**Why not a defect:** each screen is internally consistent and the tag handles both. Worth a note
for BA/QA only if the inconsistency is considered a UX issue. Both forms are pinned in
`fixtures/inbox/review-test-data.ts` so the next author does not rediscover it the hard way.

### #3 — The Inbox renders dates long-form, not ISO — VERIFIED
**What's wrong:** nothing. The API returns `2016-04-22`; the grid shows `April 22, 2016`
(`formatDisplayDate`, `toLocaleDateString('en-CA', { month: 'long', … })`).
**How caught:** the journey's first run failed asserting the ISO string against the rendered cell.
**Why not a defect:** intended display formatting. Both forms are pinned in the fixture, with a note
saying which surface takes which.

### #4 — Borrowing and restoring a seeded invoice leaves no functional residue — VERIFIED
**What's wrong:** nothing, but it was the main risk in authoring this journey and is worth the
record, since this is the suite's only scenario that mutates seeded data.
**Expected vs actual:** measured round trip on invoice 2005425 — status `UNA → APP → UNA`,
submission 119351 "Lobby" throughout, `invApproved` 0 → 1 → 0, Inbox total 50 throughout. After a
`--repeat-each=5` run all twelve pool members re-read as UNA and both submissions were unchanged.
**Why it holds:** `InvoiceRepository.updateStatus` writes only the status plus audit columns
(`revision_count`, `update_userid`, `update_timestamp`) — never `approved_userid` or
`reviewer_notes` — and the submission status does not cascade while PROCESSING invoices remain.
Residue is audit columns only.
**Guard:** the teardown fails loud and reads the row back; `preflight/anchors.setup.ts` re-checks
the whole pool is UNA before any scenario runs, because nothing else would notice a failed restore.
