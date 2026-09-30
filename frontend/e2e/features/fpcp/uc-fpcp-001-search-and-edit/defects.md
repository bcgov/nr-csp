# Defects — UC-FPCP-001, maintain the Production Flat Price Conversion table

> New to these files? See [`defects-guide.md`](../../../defects-guide.md) at the e2e root for the
> register list, status lifecycle and how to read an entry.

Nothing in this file is a triaged defect yet. **BA/QA own triage** — no `JIRA-<key>` is set and
nothing is `CLOSED` here. Everything below was found while authoring against the running stack on
**2026-09-30**, and every behavioural claim was reproduced against the API or read from the source.

Stack: seeded DB `ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23` on
`localhost:1525` → `csp-backend-e2e` on `:8080` → Vite on `:3001`.

---

## Divergence (app behaves differently from the Gherkin)

_None._ Searching and editing behave as the slice describes, once re-grounded from inline editing
to a dialog. The differences below are a deliberate UI redesign (#1), restyled text (#2), a rule
that moved to a different control (#3) and a new-app behaviour (#4).

---

## Bug / Regression

_None found in this pass._ Notably, the permission gating here is done properly: the Edit, Delete
and Add controls each check their own permission (`canEdit` / `canDelete` / `canAdd`) and the
backend guards each endpoint with `@PreAuthorize`. That is worth recording because it is **not**
the case on the invoice screen, where the Unapprove button checks nothing — see BUG-001 in
`features/inbox/uc-inbox-005-unapprove/defects.md`.

---

## Coverage gap (something the spec covers that no test covers)

### #1 — Add and Delete are not covered — OPEN
**What's wrong:** the table supports three write operations and only Edit is tested. Adding a row
(S02) and deleting one (S03) both have dialogs and endpoints, and the suite already exercises both
endpoints — but from a fixture, not through the UI.
**Expected vs actual:** not a fault — untested surface.
**Next:** both are cheap now. The fixture's `seedOwnFlatPriceRow` shows the create shape, and the
cleanup registry means an Add scenario's row is deleted for free. A Delete scenario should assert
the row is gone from the API as well as the grid, and register nothing for cleanup (or tolerate the
404, which the registry already does).

### #2 — No validation slice is covered — OPEN
**What's wrong:** S04–S11 are all rejections, and none is tested: required field blank, relative
price out of range, invalid species/grade combination, invalid sort code, invalid maturity,
duplicate on add, effective date after expiry date, duplicate on inline edit. The edit path
enforces most of them.
**Expected vs actual:** not a fault — untested surface. Each has a distinct server-side response
(`BadRequestException`, `UnprocessableEntityException`, `ConflictException`), so they are
distinguishable.
**Next — the highest-value work here.** These are genuine no-write tests: assert the inline or
dialog error **and**, via a mutation spy, that no PUT was sent. Two are worth doing first because
they are pure business rules rather than form validation: **relative price out of range** (the
input declares `min={1} max={999}`, and the seeded table contains rows with a price of `0`, so the
range is worth pinning down — see Verified-not-a-defect #2) and **duplicate on edit**, which the
service checks explicitly while excluding the row being edited.

### #3 — Optimistic-locking conflict is not covered — OPEN
**What's wrong:** `update` rejects a stale `revisionCount` with *"Record has been modified by
another user. Please reload and try again."* The happy path asserts the revision advances, but the
conflict itself is untested.
**Expected vs actual:** untested surface.
**Next:** open the dialog, change the row via the API behind the UI's back, then save — the dialog
should surface the conflict. This is a realistic two-user scenario and the only place this table's
concurrency behaviour is visible.

### #4 — No VIEW-role scenario, and WebADE provisioning is not automatable — OPEN
**What's wrong:** the scenario runs as `CSP_ADMIN`. The Edit/Delete/Add controls are
permission-gated and the `VIEW` arm is unexercised. Separately, the legacy Background's
"prodFlatPriceConv/Edit and /Save permissions" has no direct equivalent — CSP uses Cognito groups,
and note the app has no separate *Save* permission for this screen; Edit covers the whole operation.
**Expected vs actual:** untested surface plus a changed mechanism.
**Next:** unlike the invoice Unapprove case, this one should **pass** today — the gating is wired
up — so it is a straightforward green test, not a divergence.

### #5 — The Model table is not covered at all — OPEN
**What's wrong:** this page is hardcoded to `modellingCode = 'P'` (Production). The Model table
(`'M'`) has its own permissions (`modelFlatPriceConv/*`), its own Copy Data and Clear All
operations, and its own use case — none of it is tested, and the Copy/Clear endpoints are
completely uncovered.
**Expected vs actual:** out of scope for UC-FPCP-001, recorded so the ledger does not imply the
whole screen family is covered.
**Next:** author under the UC-FPCM use cases, where it belongs.

### #6 — Remaining slices S02–S14 not authored — OPEN
**What's wrong:** this pass covers S01 only, which is 3 of the UC's 14 slices.
**Next:** author per concern, one `.feature` per concern in this folder.

---

## Spec gap (app enforces something the spec never described, or the spec is wrong)

### #1 — Editing is a dialog, not an inline row — OPEN
**What's wrong:** the slice describes editing in place: click an edit icon, modify a cell in the
"inline-editable row", then click an inline save icon that triggers `saveRowEditAction`. This app
opens an **"Edit row" modal**; no part of the row ever becomes editable, and there is no per-row
save control.
**Expected vs actual:** a deliberate UI redesign, not a fault.
**How caught:** re-grounding the edit steps against `pages/FlatPriceConversion/index.tsx`.
**What the test does:** clicks the row's Edit icon, changes the field in the dialog, and saves the
dialog. The dialog closing is itself the proof the update was accepted — `handleEditSuccess` closes
it only on success, so a rejected edit leaves it open with the error inside.
**Next:** BA/QA to restate the slice in terms of the dialog.

### #2 — The confirmation text differs from the spec — OPEN
**What's wrong:** the slice expects *"Record has been updated successfully."* (resource key
`update.successful.info`), with its own TODO noting the text was never confirmed against the live
app. This app shows a toast reading **`Row updated successfully.`**
**Expected vs actual:** as above.
**How caught:** captured from the live app while authoring — which is what the slice's TODO asked
for.
**Next:** BA/QA to replace the TODO with the actual text. Note this is the fifth use case in a row
whose success message was restyled; it is worth settling the convention once rather than slice by
slice.

### #3 — The Grade dropdown that narrows by Species is the dialog's, not the search form's — OPEN
**What's wrong:** the slice's third scenario says the **search form's** Grade dropdown "updates via
AJAX" when Species is selected. It does not: the filter-row Grade lists all 17 grades regardless of
species (`gradeFilterOptions` is the unfiltered lookup). The behaviour the slice describes exists,
but on the **Add/Edit dialog's** Grade field, which is narrowed to the species' valid pairs
(`GET /api/lookup/grade-by-species/{species}`) and disabled until a species is chosen.
**Expected vs actual:** verified — Birch has grades W, X, Y in the xref out of 17 total; the dialog
offers exactly those three, the filter offers all 17.
**How caught:** re-grounding the third scenario, and finding the rule on a different control.
**What the test does:** asserts **both** — that the filter is unnarrowed (the app's real behaviour)
and that the dialog is narrowed (where the rule lives). Asserting only the dialog would have let
the slice look covered while quietly testing something it does not describe.
**Next:** BA/QA to decide whether the *filter* should narrow too. There is an argument that it
should: offering a grade that cannot exist for the chosen species guarantees an empty result.

### #4 — The grid issues no query until Search is clicked — OPEN
**What's wrong:** the slice implies navigating to the screen and seeing the table. The new app shows
a placeholder row and calls no API until Search is pressed.
**Expected vs actual:** a deliberate new-app behaviour, consistent with the Inbox and Invoice
search — and the same trap: the placeholder is itself a `<tbody><tr>`, so a naive row count returns
1 on an empty grid.
**How caught:** the same pattern already documented for the Inbox and Search.
**What the test does:** asserts the awaiting-criteria state FIRST, so the later row assertions
cannot be satisfied by the placeholder.
**Next:** BA/QA to confirm the behaviour once, across all three screens that share it.

---

## Verified — not a defect

### #1 — The suite creates its own pricing row rather than editing a seeded one — VERIFIED
**What's wrong:** nothing, but it is the main design decision in this pass and deserves the record.

These rows are **live pricing** — `PriceConversionService` reads them when an invoice is submitted,
which the SUBM journey does. Editing a seeded row would have meant mutating reference data and
restoring it, the pattern the INBOX review scenarios are forced into because the backend will not
let the suite approve an invoice it created. Here there is no such blocker: the API offers POST and
DELETE, so the scenario owns a dedicated key it creates and cleans up — the skill's preferred shape.

**Verified round trip before authoring:** POST → id 35935, `revisionCount` 0; PUT → same id,
`revisionCount` 1; DELETE → 204; Birch back to 10 rows. **After `--repeat-each=5 --workers=3`:** the
production table is back to 4,025 rows, Birch to 10, and no row carries
`entryUserid = local-dev-user`.

**Two constraints this imposes on anyone extending the domain**, both enforced in code and
documented in the fixture: each worker must create on its own effective date (the backend rejects a
duplicate on modellingCode + sortCode + species/grade + maturity + effectiveDate, and every
scenario shares the first four), and the domain must stay in a species the SUBM journey does not
use — it works in **Birch**, SUBM's invoices use **Balsam**.

### #2 — The seeded table contains relative prices of 0, below the input's own minimum — VERIFIED
**What's wrong:** nothing in this pass's scope, but it is an inconsistency worth a look. The
Relative price input declares `min={1} max={999}`, yet two seeded Birch rows (ids 14819 and 14379)
carry a relative price of **0**.
**Expected vs actual:** the data predates the constraint, or the constraint is advisory — the input
is `type="number"` with min/max, which browsers do not enforce on programmatic or pasted values, and
the real check is server-side.
**How caught:** profiling the Birch rows while choosing test data.
**Why not a defect here:** nothing is broken; a reader can open such a row and the dialog shows 0.
But it means **the true accepted range is unknown from the UI alone**, which is exactly why the
out-of-range slice (S05) is worth authoring — see Coverage gap #2. BA/QA may also want to know that
editing one of those rows without touching the price could be rejected if the server enforces
`>= 1`.

### #3 — Dropdowns show descriptions, the API takes codes — VERIFIED
**What's wrong:** nothing, but it fails a re-ground confusingly. Maturity, Species and Sort code
render their **description** in both the filters and the grid ("Cants / Export", "Birch",
"Deciduous"), while Grade renders its bare **code**. The API takes codes throughout.
**How caught:** building the page object; a locator written from a code matches nothing.
**Why not a defect:** intended presentation. Both forms are pinned in the fixture. Note the Invoice
search page additionally *rewrites* "Cants / Export" to "Cants" and hides "Export" in its maturity
filter, while this page does not — the same lookup reads differently on the two screens, which is
worth knowing but is a cosmetic inconsistency rather than a fault.
