# Defects — UC-SUBM-001 / 002 / 003 / 004, happy-path journey

> New to these files? See [`defects-guide.md`](../../../defects-guide.md) at the e2e root for the
> register list, status lifecycle and how to read an entry.

Nothing in this file is a triaged defect yet. **BA/QA own triage** — no `JIRA-<key>` is set and
nothing is `CLOSED` here. Every entry below was found by authoring the journey against the running
stack on **2026-09-25** and was reproduced directly against the API, not inferred from the UI.

Stack it was found on: seeded DB `ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23`
on `localhost:1525` → `csp-backend-e2e` on `:8080` → Vite on `:3001`.

---

## Divergence (app behaves differently from the Gherkin)

### DIV-001 — The "you still need to submit" reminder never appears the first time you save — OPEN

**What's wrong.** When someone enters an invoice by hand and presses Save for the first time, the
app is supposed to remind them that saving is not submitting. It doesn't. The invoice saves fine,
but nothing on screen says it is still sitting in Draft, waiting to be submitted. The reminder does
appear if they press Save a *second* time.

**Expected vs actual.**
* Expected: after saving, the warning *"If you want the Invoice to be Submitted for Processing
  ensure you click on the Submit button"* is shown.
* Actual: nothing is shown on the first save. On any later save of the same invoice, it appears
  correctly.

**How caught.** The journey's first assertion of this warning failed. Reproduced against the API,
which shows the message is being produced correctly and then lost in the browser:

| Call | Warning returned? |
|---|---|
| `POST /api/invoices` (first save) | **yes** |
| `GET /api/invoices/{id}` | no |
| `PUT /api/invoices/{id}` (later save) | **yes** |

**Cause (verified in-session).** `handleSave` stores the warning from the create response and then
redirects the browser from `/invoice` to `/invoice/{id}`. That URL change triggers the page's
reset-on-id-change effect, which clears the warnings; the page then reloads the invoice with a
`GET`, and the `GET` deliberately does not produce the reminder (it validates with
`ActionType.OTHER`, whereas the reminder is only raised on a save). So the message is generated,
stored, and then wiped a moment later by the redirect.

**Tracked by.** `submit-reminder.feature` — a deliberately failing scenario tagged
`@discovered-divergence`. It is **not** skipped: the red is the tracking signal, and it will turn
green by itself when the app is fixed. Exclude it from a "is anything newly broken?" run with
`--grep-invert @discovered-divergence`.

**Spec citations.** UC-SUBM-001-S01 (expected outcome: "reminder to submit shown"; message WRN-004);
UC-SUBM-003-S01 (message WRN-001, exact text as above); `messages.properties`
`invoice.submit.saved.warning`.

---

## Bug / Regression

### BUG-001 — Save is offered before the invoice can actually be saved (source-document fields) — OPEN

**What's wrong.** An invoice must carry at least one Boom Number, Timber Mark or Weigh Slip. None
of those three fields is marked as required on screen, and the Save button turns on without them —
so someone can fill in every field the form marks with an asterisk, press Save, and get an error
telling them something else was missing.

**Expected vs actual.**
* Expected: the requirement is visible up front (an asterisk, and Save disabled until it is met),
  the way the other nine required fields behave.
* Actual: Save is enabled; pressing it returns *"One of Boom Number, Timber Mark or Weigh Slip must
  have a value."* as a validation error banner.

**How caught.** Reading the write path while choosing the journey's test data. The server-side rule
(`InvoiceValidator.checkSourceDocumentRefs`) has no counterpart in the screen's own
`requiredFieldsFilled` gate, which lists the other nine required fields but not these three.

**Impact.** Cosmetic-to-moderate: nothing is lost or mis-saved, but it is an avoidable round trip
and the error arrives detached from the field that caused it (it lands in the page banner, because
the message key has no field mapping in `messageKeyMap.ts`).

**Not covered by a test in this pass** — it belongs to the "required fields missing" exception
slices (UC-SUBM-001-S04 / S12), which are out of scope here. See Coverage gap #2. The journey
complies with the rule by entering a boom number.

### BUG-002 — Deleting an invoice leaves its submission record behind — OPEN

**What's wrong.** Creating a manual invoice also creates a parent "submission" record. Deleting the
invoice removes the invoice, its line items, its boom/timber/weigh references, its related-invoice
links and its participant rows — but not that submission record, which is left orphaned with no
invoice attached.

**Expected vs actual.**
* Expected: deleting the only invoice on a submission the app itself created leaves nothing behind.
* Actual: one orphan `csp_submission` row per created-then-deleted invoice.

**How caught.** Checking the test suite's own cleanup for residue. Measured on the seeded DB: the
snapshot has 50 submissions; after this authoring session's runs it holds 50 real plus **11
orphans**, exactly one per invoice created.

**Impact on the seeded test DB.** Low and bounded. An orphan is invisible to the app — the Inbox
inner-joins invoices, so a submission with none never appears — and it does not move any number the
suite's preflight fingerprints. It is nonetheless unbounded growth over time.

**Impact in production.** For BA/QA to judge: the same code path runs there, so every deleted
manual invoice presumably leaves a row too.

**Why the suite does not clean it up.** There is no API for it, and adding a direct database
teardown would give the suite an Oracle/Docker dependency at runtime that it deliberately does not
have (every other check goes over HTTP). `./scripts/reset-db.sh` clears the orphans if they ever
need clearing.

---

## Coverage gap (something the spec covers that no test covers)

### #1 — The Sale-invoice happy path is not covered — OPEN
**What's wrong:** the journey covers the Purchase arm (UC-SUBM-001-S01) but not the Sale arm
(UC-SUBM-001-S21). These are not cosmetic variants: Purchase must be submitted by the Buyer and Sale
by the Seller, so the Sale arm exercises the other branch of the submitter/type rule, and a different
`clientNumber`/`clientLocation` derivation in the request body.
**Expected vs actual:** not a fault — untested surface.
**Next:** author `sale-happy-path.feature` in this folder, reusing every step already defined; only
the invoice type and submitted-by values change.

### #2 — 77 of the 81 slices across the four UCs are not authored — OPEN
**What's wrong:** this pass covers the four S01 happy paths only. UC-SUBM-001 has 22 slices,
UC-SUBM-002 21, UC-SUBM-003 16, UC-SUBM-004 22. Everything else — missing required fields, invalid
type/maturity for the invoice date, Seller-submits-Purchase, same-client, adjustment-invoice
references, duplicate-number and month-complete warnings, submit-with-no-line-items, and the whole
line-item validation matrix — has no scenario.
**Expected vs actual:** not a fault — deliberately deferred scope.
**How caught:** scope of this pass, recorded so the ledger does not imply these UCs are covered.
**Next:** author per concern, one `.feature` per concern in this folder. The negative slices will
need the mutation spy already wired in `steps/fixtures/subm.ts` (`invoiceCreateCalls` /
`invoiceUpdateCalls`) to prove no write happened, not merely that an error appeared.

### #3 — Submission-status transitions (LOB → INB) are not asserted — OPEN
**What's wrong:** the spec says a draft invoice's submission sits in Lobby (`LOB`) and moves to
Inbox (`INB`) on submit. The journey asserts the *invoice* status but not the *submission* status.
**Expected vs actual:** not a fault — untested surface. The behaviour was observed working while
authoring (a submitted invoice does appear in an unfiltered Inbox query).
**Next:** assert it through `/api/inbox`, not the DB. Note the parallel-safety caveat in
`coverage.md` before adding an unfiltered Inbox assertion inside the chromium project.

### #4 — WebADE authentication and per-action provisioning are not automatable as specified — OPEN
**What's wrong:** all four legacy Backgrounds require "authenticated via WebADE" and provisioning
for named actions (`invoiceDetails/Save`, `.../Submit`, `.../New Line Item`). CSP replaced WebADE
with Cognito groups, and locally `MockAuthProvider` grants one role unconditionally.
**Expected vs actual:** not a fault; the mechanism changed between the legacy and the new app.
**Next:** drop WebADE provisioning from the spec; express role-gated behaviour as roles (see #5).

### #5 — No VIEW-role scenario — OPEN
**What's wrong:** every scenario runs as `CSP_ADMIN`. The Save / Submit / Delete buttons are also
permission-gated, and the `VIEW` arm is unexercised.
**Expected vs actual:** not a fault — untested surface.
**Next:** author with `Given I am signed in to the Invoice screen as a CSP VIEW`; the step already
supports it and `assertMockRole` will reject a role the app does not define.

---

## Spec gap (app enforces something the spec never described, or the spec is wrong)

### #1 — UC-SUBM-001-S01 contradicts itself: Purchase cannot be submitted by the Seller — OPEN
**What's wrong:** the happy-path slice says to pick Invoice Type "Purchase" **and** Submitted By
"Seller". The *same slice's* business-rule table says "Seller cannot submit PUR", and UC-SUBM-001-S06
is an exception slice titled "Save Fails — Seller Submits Purchase Invoice". The happy path as
written cannot pass.
**Expected vs actual:** following the Gherkin literally produces *"The Invoice submitted by Seller
cannot be type PUR."* and no invoice is created.
**How caught:** reading `InvoiceValidator.checkSenderBuyerForInvoiceType` while choosing test data,
then cross-checking the slice catalogue, which contradicts its own steps.
**What the test does:** pairs Purchase with **Buyer**, i.e. follows the rule rather than the steps.
**Next:** BA/QA to correct UC-SUBM-001-S01 (and its Gherkin) to say Buyer.

### #2 — Submitting lands the invoice in Processing, not Unapproved — OPEN
**What's wrong:** UC-SUBM-004-S01's Gherkin and BR-SUBM-004-05 both say the status becomes
UNAPPROVED (`UNA`) on submit. This app sets PROCESSING (`PRO`), and so did the legacy one:
UC-SUBM-004-**detailed** step 5 says the status "is set to `PRO` (PROCESSING) in transit" and only
step 6 describes it reaching `UNA`. The Gherkin is asserting a state that arrives later, via a
different process, not the one visible when the button is clicked.
**Expected vs actual:** Expected (per Gherkin) `UNA`; actual `PRO`, confirmed via
`POST /api/invoices/{id}/submit`.
**How caught:** the API dry run during authoring; corroborated by the app's own code comment
("SUBMIT (DRAFT or UNAPPROVED → PROCESSING)") and the legacy detailed UC.
**What the test does:** asserts `PRO`, the observable immediately after submitting. It also asserts
Submit becomes **disabled** afterwards, where the legacy slice says it "remains enabled in UNA" —
consistent, since the invoice is in `PRO`, not `UNA`.
**Next:** BA/QA to decide whether the spec should say `PRO` at this step, and whether anything is
expected to move it on to `UNA`. **This is the one entry worth a second opinion** — if `UNA` really
is meant to be reached synchronously, this is a bug, not a spec gap.

### #3 — "Submission ID" is blank on a manually-entered invoice — OPEN
**What's wrong:** UC-SUBM-001-S01 asserts the Submission ID field is populated after save. In this
app that field shows the *business* submission number, which only an electronically-submitted (ESF)
invoice has; a manual one has none and the field shows an em-dash. The internal key the legacy
screen displayed is no longer exposed to users.
**Expected vs actual:** Expected a value; actual "—" for manual invoices (by design).
**How caught:** re-grounding the assertion against `InvoiceResponse.submissionNumber`.
**What the test does:** asserts the **opposite** of the legacy Gherkin, deliberately and with the
reason recorded in the step.
**Next:** BA/QA to confirm and update the spec.

### #4 — Success-message texts in the spec are placeholders or no longer accurate — OPEN
**What's wrong:** the slice catalogues carry `[TODO — capture from live app on first test run]` for
most success messages, and where a literal is given (`The Invoice has been Saved successfully.`,
`The Invoice has been Submitted successfully.`) the new app does not use it.
**Expected vs actual:** the app shows a toast naming the invoice instead:

| Action | Actual message |
|---|---|
| Create | `Invoice '<number>' created.` |
| Save (existing) | `Invoice '<number>' saved.` |
| Add line item | `Line item added.` |
| Submit | `Invoice '<number>' submitted.` |

Warning texts, by contrast, come from the backend bundle and **are** unchanged from legacy — the
submit reminder matches the spec's literal exactly.
**How caught:** captured from the live app while authoring, which is what the `[TODO]`s asked for.
**Next:** BA/QA to replace the `[TODO]`s with the table above, or confirm the legacy wording should
be restored.

---

## Verified — not a defect

### #1 — An unfiltered Inbox query briefly shows 51 rows during a journey run — VERIFIED
**What's wrong:** nothing. Between the submit step and cleanup, the journey's own invoice legitimately
sits in the Inbox, so an unfiltered `/api/inbox` reports 51 instead of the seeded 50.
**Expected vs actual:** expected 50 at rest; 51 while a scenario is mid-flight; back to 50 after
cleanup (all three verified).
**How caught:** checking whether the new journey could destabilise the existing Inbox smoke scenario
or the preflight fingerprint.
**Why not a defect:** it is correct behaviour, and nothing today observes it at the wrong moment —
the Inbox smoke scenario filters to the seeded 2015–2019 date window (a new submission is dated
today and falls outside it), and the preflight runs in the `setup` project that `chromium` depends
on, so it always completes before any scenario writes. Recorded because it is a real trap for the
next person adding an unfiltered Inbox assertion.

### #2 — Line items post immediately instead of on the next Save — VERIFIED
**What's wrong:** nothing. The legacy screen queued a new line item in the page and wrote it on the
next Save; this app posts it to `/api/invoices/{id}/line-items` the moment the modal is submitted,
and the invoice's stored totals are recalculated server-side at the same time.
**Expected vs actual:** a behavioural change between the apps, not a fault. It is also why a line
item cannot be added before the invoice has been saved once.
**How caught:** re-grounding UC-SUBM-002-S01, whose steps assume the deferred-write model.
**Why not a defect:** the observable outcome the spec cares about — the line item persists with the
right values and the totals update — holds, and is asserted against the API read-back.
