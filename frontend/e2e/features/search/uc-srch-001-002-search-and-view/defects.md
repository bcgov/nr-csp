# Defects — UC-SRCH-001 / 002, search and view an invoice

> New to these files? See [`defects-guide.md`](../../../defects-guide.md) at the e2e root for the
> register list, status lifecycle and how to read an entry.

Nothing in this file is a triaged defect yet. **BA/QA own triage** — no `JIRA-<key>` is set and
nothing is `CLOSED` here. Everything below was found while authoring against the running stack on
**2026-09-25**, and every behavioural claim was reproduced against the API or measured in the DOM.

Stack: seeded DB `ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23` on
`localhost:1525` → `csp-backend-e2e` on `:8080` → Vite on `:3001`.

---

## Divergence (app behaves differently from the Gherkin)

_None._ Search and view behave as the slices describe, once re-grounded. The differences are a
deliberate screen redesign (Spec gap #1), a renamed control (#2), and two spec inaccuracies (#3,
#4) — all recorded below rather than as faults.

---

## Bug / Regression

### BUG-001 — A validation error the app computes is shown to nobody — FIXED

**What's wrong.** Opening an APPROVED invoice, the system finds two things wrong with the record
and tells the reader about only one of them. The other is computed, routed, and then rendered
nowhere at all — with nothing on screen to suggest anything is missing.

**Expected vs actual.** For invoice 200388, `GET /api/invoices/200388` returns two errors:

| Message | Shown? |
|---|---|
| "One of Boom Number, Timber Mark or Weigh Slip must have a value." | **yes** — page banner |
| "The Invoice submitted by Seller cannot be type PUR." | **no** — nowhere |

**Cause (verified in-session).** `routeServerErrors` sends a message whose key is in
`MESSAGE_KEY_TO_FIELD` to that field as an inline error, and everything else to the page banner.
`invoice.type.invalid.submitter` maps to `invType` — but an APPROVED invoice's header fields are
disabled (`canEdit` is false outside DFT/PRO/UNA), and Carbon does not render a disabled field's
`invalidText`. Measured in the DOM on this record: the Invoice type button is `disabled`, carries
no invalid class and no `aria-invalid`, and the page contains **zero** `.cds--form-requirement`
elements — while both banners render correctly.

**Scope — wider than this one record.** It affects **every field-mapped validation error on any
locked invoice** (APPROVED, REJECTED, CANCELLED), not just this message and not just this invoice.
Most of `MESSAGE_KEY_TO_FIELD` is field-mapped, so most validation errors on a locked record are
invisible.

**Impact.** A reviewer auditing a historical invoice is shown a partial picture and cannot tell it
is partial. Given these screens exist "for audit or reference purposes" (the UC's own words), that
matters.

**FIXED** on branch `defects-from-e2e`. `applyServerErrors` in `pages/Invoice/index.tsx` now passes
an empty field map when the header is not editable, so a field-mapped error falls through to the
page banner — where the unmapped ones already went — instead of to a control that cannot render it.
The reviewer-comment box is the one header field that stays editable on a locked invoice, and no
error targets it there (the comment rule fires on reject/cancel/unapprove, which are only offered
in PRO/UNA, where the header is editable and the normal inline path applies).

The dedicated `suppressed-error.feature` scenario has been **retired**: the journey's
"the page reports the validation problems with this legacy record" step now asserts **both** errors
in the banner, which is what guards the fix. Awaiting BA/QA confirmation before this entry is
closed.

### BUG-002 — Breadcrumbs are not links and cannot be reached by keyboard — FIXED

**What's wrong.** The breadcrumb trail is the only way back to the search results from an invoice
(there is no Back button), but the crumbs are not links. `PageTitle` renders each as a Carbon
`BreadcrumbItem` with an `onClick` and **no** `href`, which produces a plain `<span>`: clickable
with a mouse, but with no link semantics, no keyboard focus and nothing for a screen reader to
announce as navigation.

**Expected vs actual.** Expected an `<a>`; actual a `<span>` with a click handler.

**How caught.** The scenario's first attempt located the crumb by its `link` role and found
nothing; the page object now has to locate it structurally, with a comment explaining why.

**Scope.** `PageTitle` is shared, so this affects the breadcrumb on **every** page that renders
one, not just the invoice screen.

**Impact.** An accessibility defect on a primary navigation control — a keyboard-only user cannot
go back. Worth confirming against the project's WCAG obligations.

**FIXED** on branch `defects-from-e2e`. `PageTitle` now gives a navigable crumb an `href`, which is
what makes Carbon render an anchor, and keeps the click handler for client-side routing
(`preventDefault` + `navigate`) — the same pattern the in-page links already use. Two further
corrections fell out of it: the last crumb is marked `isCurrentPage` rather than offered as a
destination, and a placeholder crumb (the Invoice screen uses `path: '#'` for "Invoice" and the
invoice number) is no longer presented as clickable at all.

Covered by three `PageTitle` browser tests — a navigable crumb is a focusable anchor with the right
`href`; the last crumb is current and not a link; a `'#'` crumb is not a link — all three confirmed
to fail before the fix. The e2e page object's breadcrumb locator has also moved from a structural
selector back to `getByRole('link')`, so the journey now asserts the crumb really is a link.
Awaiting BA/QA confirmation before this entry is closed.

---

## Coverage gap (something the spec covers that no test covers)

### #1 — The no-match path is not covered — OPEN
**What's wrong:** every search scenario here returns rows. The "No results found" state (the grid's
post-search empty state, distinct from the pre-search one) is unexercised.
**Expected vs actual:** untested surface.
**Next:** search for an invoice number that cannot exist and assert the "No results found" empty
state — and that it is NOT the pre-search placeholder, which is the trap the journey's first
assertion documents.

### #2 — Pagination and sorting are not covered — OPEN
**What's wrong:** UC-SRCH-001-S01 expects a paginator showing a record count. The grid is also
sortable and server-side paged (`serverSide`, default page size 100), and none of that is tested.
**Expected vs actual:** untested surface.
**Next:** a status search returns 347 rows — good material for paging and sorting assertions, as
long as they avoid exact totals (see the parallel-safety note in `coverage.md`).

### #3 — No VIEW-role scenario, and WebADE provisioning is not automatable — OPEN
**What's wrong:** both scenarios run as `CSP_ADMIN`. `VIEW` is the most interesting role for these
UCs — searching and viewing is exactly what it is for — and it is unexercised. Separately, the
legacy Backgrounds' WebADE provisioning for `search/Search`, `search/Clear` and `search/Details`
has no equivalent.
**Next:** author the VIEW arm with `Given I am on the invoice search screen as a CSP VIEW`; drop
WebADE provisioning from the spec.

### #4 — Only the APPROVED arm of "non-NEW invoice" is covered — OPEN
**What's wrong:** UC-SRCH-002-S01 accepts any of DRAFT / PROCESSING / APPROVED / REJECTED /
CANCELLED / UNAPPROVED. Only APPROVED is exercised, and the button-state assertions are specific to
it — a DRAFT invoice would legitimately offer Save, Submit, Delete and Duplicate.
**Expected vs actual:** untested surface.
**Next:** a `Scenario Outline` over statuses, with the expected button states per status, would be
a strong addition — it is the real content of UC-SRCH-002.

### #5 — Breadcrumb accessibility is asserted only incidentally — CLOSED
**What was missing:** the journey asserted the crumb was visible, which passed despite BUG-002.
**Now covered:** the journey locates the crumb by `link` role, so it fails if the crumb stops being
an anchor, and three `PageTitle` browser tests cover focusability, the current-page marker and
placeholder crumbs.
**Still open:** nothing here drives the crumb by keyboard end-to-end (tab to it, press Enter) —
worth adding if the project has explicit WCAG obligations to evidence.

### #6 — Remaining slices of both UCs not authored — OPEN
**What's wrong:** this pass covers the S01 slices only.
**Next:** author per concern, one `.feature` per concern in this folder.

---

## Spec gap (app enforces something the spec never described, or the spec is wrong)

### #1 — There is no read-only "view from search" screen — OPEN
**What's wrong:** UC-SRCH-002-S01 expects the invoice opened from search to carry **no** Save,
Approve, Reject, Submit, Delete or Cancel buttons. This app has ONE invoice screen used for both
viewing and acting; the buttons are always present and gated by status and permission instead.
**Expected vs actual:** Expected the buttons absent; actual present but disabled (on an APPROVED
invoice: Save, Submit, Reject, Cancel, Duplicate, Delete all disabled; Approve replaced by
Unapprove, which is enabled — see UC-INBOX-005 BUG-001 for that one).
**How caught:** re-grounding the button assertions against `pages/Invoice/index.tsx`.
**What the test does:** asserts each is **disabled** — deliberately stronger than absence, because
it would catch an action being re-enabled on an approved record.
**Next:** BA/QA to confirm the unified screen is intended and restate the slice in terms of which
actions are *available*, per status.

### #2 — There is no Back button — OPEN
**What's wrong:** the slice expects a "Back" button. This app has none; arriving from search sets
`state: { fromSearch: true }`, which adds an "Invoice search" breadcrumb — that is the route back.
**How caught:** re-grounding, then discovering the crumb is not a link (BUG-002).
**What the test does:** asserts the breadcrumb, which also proves the navigation carried its state
rather than the test having jumped straight to the URL.
**Next:** BA/QA to restate as a breadcrumb; see BUG-002 for the separate accessibility problem.

### #3 — "Submission ID" is blank on a manual invoice — OPEN
**What's wrong:** the slice expects the Submission ID to display. The pinned record is a manual
invoice, and that field shows the BUSINESS submission number, which only an ESF submission has.
**Expected vs actual:** Expected a value; actual "—", by design.
**How caught:** re-grounding — the same finding already recorded under UC-SUBM-001 (Spec gap #3).
**Next:** BA/QA to note the manual/ESF distinction in the spec, once, for both UCs.

### #4 — The slice's status code and invoice number are not real — OPEN
**What's wrong:** UC-SRCH-001-S01 searches for status **"APR"** and invoice number
**"INV-2024-001"**. Neither exists. The real status codes are PRO / UNA / APP / CAN / DFT / DVF /
REJ / VER, and the dropdown lists their DESCRIPTIONS, so the option reads "Approved". The invoice
number is invented — the seeded data has nothing resembling it.
**Expected vs actual:** following the slice literally selects nothing and finds nothing.
**How caught:** checking the slice's literals against `/api/lookup/status` and `/api/search`.
**What the test does:** uses "Approved" and a real seeded invoice number pinned in the fixture.
**Next:** BA/QA to correct the literals.

### #5 — The grid issues no query until Search is clicked — OPEN
**What's wrong:** the legacy slice implies navigating to the search screen and reading results. The
new app shows a placeholder row ("Your search results will appear here. Enter at least one criteria
to start the search.") and calls no API until Search is pressed.
**Expected vs actual:** a deliberate new-app behaviour, not a fault. It is also a trap: the
placeholder is itself a `<tbody><tr>`, so a naive row count returns 1 on an empty grid and
satisfies an "at least one row" assertion while proving nothing — which is exactly what the legacy
slice asserts.
**How caught:** the same pattern already documented for the Inbox (Spec gap #1 in
`features/inbox/uc-inbox-001-search/defects.md`).
**What the test does:** asserts the awaiting-criteria state FIRST, so the later row assertions
cannot be satisfied by the placeholder.
**Next:** BA/QA to confirm the empty-state-until-search behaviour, once, across Search and Inbox.

---

## Verified — not a defect

### #1 — A real APPROVED invoice breaks the rule that makes UC-SUBM-001-S01 contradictory — VERIFIED
**What's wrong:** nothing in the app, but this is the most interesting thing this pass turned up
and it belongs in front of BA/QA.

Invoice 200388 is a **Purchase invoice submitted by the Seller**. It is real, historical, and
**APPROVED**. Today's validator rejects exactly that combination
(`InvoiceValidator.checkSenderBuyerForInvoiceType` → "The Invoice submitted by Seller cannot be
type PUR."), which is why the record shows a validation error on read.

That is the same rule recorded as Spec gap #1 under UC-SUBM-001, where the legacy happy-path slice
instructs the tester to pick Purchase **and** Seller while its own business-rule table forbids it.
Here is the evidence that the legacy system genuinely allowed it: the data contains approved
invoices that do it.

**Why not a defect:** the new app is enforcing a rule the old one did not, on data created before
the rule existed. Nothing is broken. But it means **legacy records can be invalid by today's
rules**, which is worth a deliberate decision — is the rule right, is the data to be corrected, or
should historical records be exempt on read?
**Next:** BA/QA to decide, and to fix UC-SUBM-001-S01 either way.

### #2 — Two dates, two formats, and the grid shows the long one — VERIFIED
**What's wrong:** nothing. The API returns `2013-06-28`; the results grid renders
`June 28, 2013` (`formatDisplayDate`, `toLocaleDateString('en-CA', { month: 'long', … })`).
**How caught:** already learned on the Inbox (Verified-not-a-defect #3 in the UC-INBOX-001..003
folder); pinned here too so the next author does not rediscover it.
**Why not a defect:** intended display formatting. Both forms are in the fixture, with a note
saying which surface takes which.

### #3 — The group summary shows 3 rows for 7 line items — VERIFIED
**What's wrong:** nothing, but it is easy to assert wrongly. The summary groups by
(species, secondSort, **exact price**) — grade is not part of the key — so this invoice's seven
lines collapse into three groups, two of which differ only by price (HE/S at 58 and HE/S at 70).
**How caught:** the first run asserted 4 (counting distinct grades) and failed against 3.
**Why not a defect:** documented grouping behaviour (`groupLineItems`). The derivation is written
out in the fixture so the number is not magic.
