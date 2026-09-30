# Coverage — UC-FPCP-001, maintain the Production Flat Price Conversion table

> New to these files? See [`coverage-guide.md`](../../../coverage-guide.md) at the e2e root for the
> column + status-flag legend.

Sources reconciled (csp-bmad `_bmad-output/`):

| Source | Path |
|---|---|
| Gherkin | `implementation-artifacts/tests/UC-FPCP-001/gherkin/UC-FPCP-001-S01.feature` |
| Slice catalogue | `planning-artifacts/requirements/use-cases/UC-FPCP-001/UC-FPCP-001-slices.md` |
| Detailed UC | `.../UC-FPCP-001-detailed.md` |

…reconciled against the app's **actual write path**: `pages/FlatPriceConversion` →
`services/flatPriceConversion.service` → `FlatPriceConversionController` (guarded by
`prodFlatPriceConv/Edit`) → `FlatPriceConversionService.update` → `FlatPriceConversionRepository`.

Test data: `fixtures/fpcp/flat-price-test-data.ts`, with the discovery command above each value.

**SCOPE OF THIS PASS — UC-FPCP-001-S01 only**, as requested. Note S01 is **three** scenarios, not
one, and all three are covered. UC-FPCP-001 has **14** slices in total (S01–S14); the other 13 are
unauthored and listed as coverage gaps.

**This is the suite's first table-maintenance write** and its first domain outside invoices.

## Scenario inventory

| Scenario | File | Tags | Result |
|---|---|---|---|
| Searching by species finds a row, which can be edited to a new relative price | `search-and-edit.feature` | `@p0 @UC-FPCP-001 @S01` | green |
| Searching with no filters returns production rows | `search-variants.feature` | `@p1 @UC-FPCP-001 @S01` | green |
| Grade options are narrowed by species in the edit dialog, but not in the filters | `search-variants.feature` | `@p1 @UC-FPCP-001 @S01` | green |

## Coverage matrix

| Source item | Source citation | App enforcement point | Scenario (tags) | Status | Gap/defect |
|---|---|---|---|---|---|
| **S01 sc.1** — table contains a row matching a known species | S01 `:19` | — | `search-and-edit` `@S01 @p0` (created via `POST`, then deleted) | covered | — |
| Navigate to Table Maintenance (Production) → Flat Price Conversion | S01 `:20` | `ROUTES.FLAT_PRICE_CONVERSION`, `modellingCode = 'P'` | `search-and-edit` `@S01 @p0` | covered, re-grounded (one route, no menu drill-down) | — |
| Select a species and Search | S01 `:21-22` | `#species-filter` → `species` param | `search-and-edit` `@S01 @p0` (asserts every row matches) | covered, strengthened | — |
| Results panel is visible | S01 `:26` | `ResultsTable` | all three | covered | — |
| Click the edit icon on a row | S01 `:23` | row `IconButton label="Edit"` → `setModal({kind:'edit'})` | `search-and-edit` `@S01 @p0` | covered | — |
| Modify a field value in the inline-editable row | S01 `:24` | the "Edit row" **dialog**, `#edit-flat-price-conversion` | `search-and-edit` `@S01 @p0` | covered, re-grounded | **Spec gap #1** |
| Click the inline save icon (`saveRowEditAction`) | S01 `:25` | the dialog's Save → `PUT /{id}` | `search-and-edit` `@S01 @p0` (dialog closes only on success) | covered, re-grounded | Spec gap #1 |
| A confirmation message is displayed | S01 `:27` (`update.successful.info`, TODO unconfirmed) | toast `Row updated successfully.` | `search-and-edit` `@S01 @p0` | covered, re-grounded | Spec gap #2 |
| The edit is persisted | implied by "reflect the correct parameters immediately" | `updateFlatPriceConversion` | `search-and-edit` `@S01 @p0` (API read-back of the **whole** row) | covered, strengthened | — |
| The dialog is bound to the clicked row | not in the Gherkin (new assertion) | `modal.row` → `useFlatPriceConversionForm` | `search-and-edit` `@S01 @p0` (asserts the pre-filled price) | covered (added) | — |
| Optimistic locking advances on edit | not in the Gherkin | `revisionCount` check in `update` | `search-and-edit` `@S01 @p0` | covered (added) | Coverage gap #3 |
| **S01 sc.2** — search with no filters returns all records | S01 `:30-34` | `handleSearch` with every filter null | `search-variants` `@S01 @p1` | covered (no count pinned — see the note below) | — |
| **S01 sc.3** — Grade dropdown updates when Species is selected | S01 `:36-39` | filter Grade is **not** narrowed; the dialog's is | `search-variants` `@S01 @p1` (asserts **both**) | covered, re-grounded | **Spec gap #3** |
| Grid issues no query until Search is clicked | not in the Gherkin (new-app behaviour) | `hasSearched` gate | all three | covered (+ spec-gap) | Spec gap #4 |
| Background — WebADE auth + `prodFlatPriceConv/Edit`, `/Save` | S01 Background | `usePermission(PROD_FLAT_PRICE_CONV_EDIT)` + `@PreAuthorize` | — | not-applicable as written | Coverage gap #4 |

Three assertions are **additions**, not re-groundings: that the dialog is bound to the row that was
clicked (a form bound to the wrong row would otherwise pass), that the whole row survives the edit
(the service rebuilds the record from the request body on every PUT, so a dropped column is a real
risk), and that `revisionCount` advances.

**Symmetry check.** The write matrix for this table is almost entirely uncovered — this pass does
one of its three arms:

| Mirror matrix | Arm A | Arm B | Arm C | Status |
|---|---|---|---|---|
| Table maintenance operations | **Edit — covered** | Add (S02) | Delete (S03) | **asymmetric** — Coverage gap #1 |
| Edit accepted / rejected | accepted — **covered** | rejected (S05–S11: range, duplicate, invalid combo/sort/maturity, date order) | — | **asymmetric** — Coverage gap #2 |
| Production table / Model table | Production — **covered** | Model (`modellingCode = 'M'`, its own UC) | — | deferred |

**Role / permission coverage:** runs as `CSP_ADMIN`. The Edit, Delete and Add controls are each
permission-gated (`canEdit` / `canDelete` / `canAdd`), and unlike the invoice screen's Unapprove
button these **are** wired up — so a `VIEW` scenario asserting the row has no Edit icon would pass
today. Unauthored — Coverage gap #4.

## Parallel-safety notes

* **This scenario creates its own row and deletes it — it never edits seeded data.** That is
  possible here (unlike the INBOX review scenarios) because the API offers POST and DELETE, so the
  test can own a dedicated key. Verified: after `--repeat-each=5 --workers=3`, the production table
  is back to 4,025 rows, Birch to 10, and no row is left carrying `entryUserid = local-dev-user`.
* **Each worker creates its row on its own effective date.** The backend rejects a duplicate on
  (modellingCode, sortCode, species/grade, maturity, effectiveDate) and every scenario uses the
  same first four, so the date is the discriminator — derived from `parallelIndex`, deliberately
  deterministic so a leaked row collides loudly instead of drifting the table silently.
* **These rows are live pricing.** `PriceConversionService` reads them when an invoice is
  submitted, which the SUBM journey does. The two domains are kept apart by species: this works in
  **Birch**, the SUBM journey's invoices use **Balsam**. Anyone extending this must not move it to
  a species the SUBM journey uses.
* **No count is pinned for either search.** A Birch search legitimately returns 10 seeded rows plus
  one per concurrent scenario, and an unfiltered search spans 4,025 rows paged at 100. Both
  scenarios assert the per-row property (every row matches the filter) instead, which is what a
  search test is actually for.
* `preflight/anchors.setup.ts` checks the pinned codes still resolve **and** that no suite-created
  row was left behind — the latter matters because nothing else would notice.
