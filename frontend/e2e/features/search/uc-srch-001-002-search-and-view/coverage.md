# Coverage — UC-SRCH-001 / 002, search and view an invoice

> New to these files? See [`coverage-guide.md`](../../../coverage-guide.md) at the e2e root for the
> column + status-flag legend.

Sources reconciled (csp-bmad `_bmad-output/`):

| Source | Path |
|---|---|
| Gherkin | `implementation-artifacts/tests/UC-SRCH-00{1,2}/gherkin/UC-SRCH-00{1,2}-S01.feature` |
| Slice catalogues | `planning-artifacts/requirements/use-cases/UC-SRCH-00{1,2}/UC-SRCH-00{1,2}-slices.md` |
| Detailed UCs | `.../UC-SRCH-00{1,2}-detailed.md` |

…reconciled against the app's **actual read path**: `pages/Search` → `services/search.service`
(`GET /api/search`) → `SearchController`, then `pages/Invoice` → `GET /api/invoices/{id}`.

Test data: pinned in `fixtures/search/search-test-data.ts`, with the discovery command above each
value.

**SCOPE OF THIS PASS — the S01 slices of both UCs.** Note UC-SRCH-001-S01 is not one scenario but
**three**, and all three are covered (see the inventory).

**This closes Coverage gap #3 from `features/inbox/uc-inbox-004-reject/defects.md`** — the Search →
invoice path had no coverage anywhere; the review journey covers Inbox → submission → invoice.

## Scenario inventory

| Scenario | File | Tags | Result |
|---|---|---|---|
| Searching by date range and type finds an invoice, which opens with its full details | `journey.feature` | `@p0 @UC-SRCH-001 @UC-SRCH-002 @S01 @journey` | green |
| Searching by invoice number returns just that invoice | `search-criteria.feature` | `@p1 @UC-SRCH-001 @S01` | green |
| Searching by status returns only invoices with that status | `search-criteria.feature` | `@p1 @UC-SRCH-001 @S01` | green |
| Every validation error the system found is shown to the reader | `suppressed-error.feature` | `@p1 @UC-SRCH-002 @S01 @discovered-divergence` | **red on purpose** (BUG-001) |

## Coverage matrix

| Source item | Source citation | App enforcement point | Scenario (tags) | Status | Gap/defect |
|---|---|---|---|---|---|
| **UC-SRCH-001-S01 sc.1** — search by date range + type | S01 `:22-26` | `#start-date` / `#end-date` / `#type-filter` → `GET /api/search` | `journey` `@S01 @p0` | covered | — |
| Results table is visible | S01 `:27-28` | `ResultsTable` | `journey` `@S01 @p0` | covered | — |
| At least one row is displayed | S01 `:30` | — | `journey` `@S01 @p0` (asserts the **exact** count, and every row against the filter) | covered, strengthened | — |
| Paginator shows "Showing" with a count | S01 `:29` | `ResultsTable` pagination | — | deferred | Coverage gap #2 |
| **UC-SRCH-001-S01 sc.2** — search by status | S01 `:33-40` | `#status-filter` → `invStatus` | `search-criteria` `@S01 @p1` | covered, re-grounded (the slice's "APR" is not a real code) | Spec gap #4 |
| **UC-SRCH-001-S01 sc.3** — search by invoice number | S01 `:42-49` | `#invoice-number` → `invNumber` | `search-criteria` `@S01 @p1` (asserts the row IS the record, field by field) | covered, strengthened | Spec gap #4 |
| Results reflect live API data | not in the Gherkin (new assertion) | `GET /api/search` | `journey` `@S01 @p0` (set comparison, UI vs API) | covered (added) | — |
| Grid issues no query until Search is clicked | not in the Gherkin (new-app behaviour) | `hasSearched` gate | `journey` `@S01 @p0` | covered (+ spec-gap) | Spec gap #5 |
| **UC-SRCH-002-S01** — open the invoice from the results | S01 `:24-26` | Invoice number `Link` → `navigate('/invoice/:id', { state: { fromSearch: true } })` | `journey` `@S01 @p0` | covered | — |
| Address / Detail / Group Summary fieldsets visible | S01 `:27-30` | the Carbon accordion sections | `journey` `@S01 @p0` (via the group summary + header assertions) | covered, re-grounded | Spec gap #1 |
| Group summary table visible | S01 `:30` | `ResultsTable` in "Invoice group summary" | `journey` `@S01 @p0` (asserts the exact group count too) | covered, strengthened | — |
| Status displayed | S01 `:31` | status pill | `journey` `@S01 @p0` | covered | — |
| Submission ID displayed | S01 `:32` | `InvoiceResponse.submissionNumber` (null when manual) | `journey` `@S01 @p0` (asserts the em-dash) | covered, re-grounded | Spec gap #3 |
| Total pieces / volume / amount displayed | S01 `:33-35` | header meta values + totals footer row | `journey` `@S01 @p0` (both renderings, against the API's stored totals) | covered, strengthened | — |
| No Save / Approve / Reject / Submit / Delete / Cancel buttons | S01 `:36-41` | one shared Invoice screen, buttons gated by status | `journey` `@S01 @p0` (asserts each is **disabled**) | covered, re-grounded | **Spec gap #1** |
| "Back" button visible | S01 `:42` | the "Invoice search" breadcrumb from `fromSearch` | `journey` `@S01 @p0` | covered, re-grounded | Spec gap #2 |
| Every validation error on the record is shown | implied by "review … for audit" | `routeServerErrors` + Carbon's disabled-field behaviour | `suppressed-error` `@discovered-divergence` | **covered by a failing test** | **BUG-001** |
| Background — WebADE auth + `search/Search`, `search/Clear`, `search/Details` | both Backgrounds | Cognito groups / `usePermission` | — | not-applicable | Coverage gap #3 |

**Symmetry check.** One mirror matrix is in play, and it is deliberately lopsided:

| Mirror matrix | Arm A | Arm B | Status |
|---|---|---|---|
| Search returns matches / returns nothing | matches — **covered** (three criteria) | no-match ("No results found") | **asymmetric** — Coverage gap #1 |
| Invoice viewed is non-NEW / is NEW | non-NEW (APPROVED) — **covered** | a NEW invoice (UC-SRCH-002's other slices) | deferred — Coverage gap #4 |

**Role / permission coverage:** runs as `CSP_ADMIN`. A `VIEW` user is the interesting arm for this
UC, since viewing is exactly what that role is for — Coverage gap #3.

## Parallel-safety notes

* **These scenarios are READ-ONLY** — no cleanup registry, no mutation spy, nothing to restore.
* **The exact row count is safe by construction, not by luck.** The journey's filter is
  Purchase-only and restricted to 2013:
  * every borrowable review invoice is SAL or ADJ, so the INBOX scenarios cannot enter this set;
  * the SUBM journey's Purchase invoices are dated `daysAgo(1)`, so they cannot either.
  Nothing else in the suite writes invoices. **If a future scenario starts creating Purchase
  invoices with historical dates, this count breaks** — `preflight/anchors.setup.ts` checks it up
  front and says so.
* **The status search deliberately asserts no count.** It spans the whole seed, and the review
  scenarios move borrowed invoices in and out of APPROVED while they run, so the total is
  legitimately variable. Only the per-row property ("every row has this status") is safe there —
  pinning a count would be a flake waiting for a parallel run.
