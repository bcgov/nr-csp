# Defects — UC-INBOX-001 Search Submission Inbox

> New to these files? See [`defects-guide.md`](../../../defects-guide.md) at the e2e root for the
> register list, status lifecycle and how to read an entry.

Nothing in this file is a triaged defect yet. BA/QA own triage; no `JIRA-<key>` or `CLOSED` status is
set here.

## Divergence (app behaves differently from the Gherkin)

_None found in this pass._ Only one slice was authored, and the app matched it once re-grounded.

## Bug / Regression

_None found._

## Coverage gap (something the spec covers that no test covers)

### #1 — Date-range search covered only at the "wide window" level — OPEN
**What's wrong:** the smoke scenario searches a date range wide enough to return every seeded
submission. It therefore proves searching works, but not that the range *filters* correctly.
**Expected vs actual:** no divergence observed — this is untested surface, not a fault.
**How caught:** authoring review against `UC-INBOX-001-S01.feature`.
**Next:** author boundary cases (start == earliest, end == latest, a window that excludes rows, and a
window matching nothing) when S01 is authored in full.

### #2 — Slices S02–S10 not yet authored — OPEN
**What's wrong:** this pass is a skeleton. Clear-filters (S02), the remaining filter controls,
pagination, sorting, no-match and navigation slices (S03–S10) have no scenarios.
**Expected vs actual:** not a fault — deliberately deferred scope.
**How caught:** scope of this pass, recorded so the ledger doesn't imply the UC is covered.
**Next:** author per slice; each gets its own `.feature` per concern under this folder.

### #3 — Authentication / provisioning preconditions not automatable as specified — OPEN
**What's wrong:** the legacy Background requires "authenticated via WebADE" and "provisioned for
inbox/Search". CSP replaced WebADE with Cognito groups, and locally `MockAuthProvider` grants a
single role unconditionally — so the *provisioning* precondition cannot be varied in a local run.
**Expected vs actual:** not a fault; the mechanism changed between the legacy and new app.
**How caught:** re-grounding the Background against `frontend/src/context/auth/`.
**Next:** if role-gated Inbox behaviour matters, author it by seeding `localStorage['csp.mockRole']`
per scenario; genuine WebADE provisioning has no equivalent and should be dropped from the spec.

## Spec gap (app enforces something the spec never described)

### #1 — The Inbox issues no query until search criteria are supplied — OPEN
**What's wrong:** the legacy Gherkin implies navigating to the Inbox shows results. The new app
shows a single placeholder row — "Your search results will appear here. Enter at least one criteria
to start the search." — and calls no API until a criterion is entered.
**Expected vs actual:** Expected (per legacy spec) results on load; actual an empty state until search.
**How caught:** the first authored assertion failed while *appearing* to pass — the placeholder is
itself a `<tbody><tr>`, so a naive row count returned 1 and satisfied an "at least one row" check.
This is now asserted explicitly (`the Inbox is awaiting search criteria`) so the later row-count
assertions cannot be satisfied by the placeholder.
**Next:** BA/QA to confirm the empty-state-until-search behaviour is intended, and update the spec.

## Verified — not a defect

### #1 — API `invTotal` (150) exceeds the actual invoice rows (147) for submission 56835 — VERIFIED
**What's wrong:** nothing. `invTotal` is `csp_submission.NUMBER_INVOICES_SUBMITTED`, the count
*declared* on the submission; it is not a count of `coastal_log_sale` rows.
**Expected vs actual:** declared 150, actual seeded invoice rows 147.
**How caught:** cross-checking the pinned fixture against the DB while authoring.
**Why not a defect:** the discrepancy exists in the SOURCE system (`fortmp1`); the extract pulls
every invoice whose `csp_submission_id` matches, so it did not introduce the gap. The fixture asserts
the declared value the app displays, and documents the distinction so nobody "fixes" it.
