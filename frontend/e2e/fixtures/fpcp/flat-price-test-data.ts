/**
 * FPCP domain (UC-FPCP-001) — Production Flat Price Conversion table maintenance.
 *
 * Seed image: ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23
 *
 * ---------------------------------------------------------------------------
 * WHY THIS SCENARIO CREATES ITS OWN ROW INSTEAD OF BORROWING ONE
 * ---------------------------------------------------------------------------
 * This is the suite's first table-maintenance write, and it edits LIVE PRICING used by invoice
 * submission (`PriceConversionService` reads these rows when an invoice is submitted). Editing a
 * seeded row would mean mutating reference data and restoring it afterwards — the pattern the
 * INBOX review scenarios are forced into, because the backend will not let the suite approve an
 * invoice it created.
 *
 * Here there is no such blocker: the page has "Add new row" and the API has POST and DELETE. So the
 * scenario CREATES its own production row, edits that, and deletes it on teardown — the skill's
 * preferred shape (a mutating test owns a dedicated key it creates and cleans up), with zero
 * mutation of seeded data. Verified end to end before authoring:
 *   POST   /api/flat-price-conversions            -> 201, id 35935, revisionCount 0
 *   PUT    /api/flat-price-conversions/35935      -> 200, same id, revisionCount 1
 *   DELETE /api/flat-price-conversions/35935      -> 204
 *   GET    ?modellingCode=P&species=BI            -> back to 10 rows (zero residue)
 *
 * The values are still real — every code below comes from the app's own lookups.
 */

/** Production table. The page hardcodes `modellingCode = 'P'`; 'M' is the separate Model table. */
export const PRODUCTION_MODELLING_CODE = 'P';

/**
 * The species this scenario works in: Birch.
 *
 * Chosen for two reasons:
 *   * it is the smallest meaningful slice of the production table (10 seeded rows out of 4,025),
 *     so a species-filtered search is quick and easy to reason about;
 *   * the SUBM journey submits invoices with **Balsam** (BA) lines, and submission is what reads
 *     these conversion rows — so working in Birch keeps the two domains from touching the same
 *     pricing data even though both write.
 *
 * Provenance:
 *   curl -s 'http://localhost:8080/api/flat-price-conversions?modellingCode=P&species=BI'
 *   -> 10 rows, grades W / X / Y, sort codes A / B / D, maturities C / E / O / S
 *   curl -s http://localhost:8080/api/lookup/species -> {"code":"BI","description":"Birch"}
 */
export const birchSpecies = {
  code: 'BI',
  /** The filter and modal dropdowns list DESCRIPTIONS, not codes. */
  label: 'Birch',
  /** Seeded rows only — NOT what a search returns while this scenario's own row exists. */
  seededRowCount: 10,
} as const;

/**
 * Grades valid for Birch, and the full grade list, for the species-narrows-grade assertion.
 *
 * Provenance:
 *   curl -s http://localhost:8080/api/lookup/grade-by-species/BI -> W, X, Y
 *   curl -s http://localhost:8080/api/lookup/grade               -> 17 grades
 *
 * ⚠ ONLY THE MODAL NARROWS. The legacy slice expects the SEARCH FORM's Grade dropdown to update by
 * AJAX when Species is picked. In this app the filter-row Grade lists all 17 grades regardless
 * (`gradeFilterOptions` is the unfiltered lookup); it is the ADD/EDIT MODAL whose Grade is
 * narrowed by species and disabled until one is chosen. See defects.md.
 */
export const birchGrades = ['W', 'X', 'Y'] as const;
export const allGradeCount = 17;

/**
 * The row each scenario creates, edits and deletes.
 *
 * Every code is real and active on the effective date: maturity C, species BI + grade W (a valid
 * pair in `csp_species_grade_xref`), sort code D. `update` re-validates all of them against the
 * effective date, so an arbitrary combination would fail with a 422 even when only the price is
 * being changed.
 *
 * Provenance:
 *   curl -s http://localhost:8080/api/lookup/maturity  -> {"code":"C","description":"Cants / Export"}
 *   curl -s http://localhost:8080/api/lookup/sort-code -> {"code":"D","description":"Deciduous"}
 *   curl -s http://localhost:8080/api/lookup/species-grade-combinations -> includes BI/W
 */
export const ownRow = {
  maturityCode: 'C',
  /** As the Maturity dropdown and the results grid render it. */
  maturityLabel: 'Cants / Export',
  speciesCode: birchSpecies.code,
  speciesLabel: birchSpecies.label,
  grade: 'W',
  sortCode: 'D',
  /** As the results grid renders the Sort code column (it resolves the description). */
  sortCodeLabel: 'Deciduous',
  /** Relative price the row is created with, and what it is edited to. */
  initialPrice: 111,
  editedPrice: 222,
  expiryDate: null,
} as const;

/**
 * A per-worker effective date, so concurrent copies of this scenario never collide.
 *
 * The backend rejects a duplicate on
 * (modellingCode, sortCode, speciesGradeXrefId, maturity, effectiveDate) — and every scenario
 * creates the same combination of the first four. The effective date is therefore what has to
 * differ, and Playwright's `parallelIndex` is unique across the workers running at any one moment,
 * so indexing by it gives each concurrent copy its own key.
 *
 * Deliberately deterministic rather than randomised: if a previous run leaked a row, the next run
 * collides with a 409 whose message names the exact key, instead of silently drifting the table.
 *
 * March 2026 is inside the window where maturity C, sort D and the BI/W xref are all active —
 * confirmed by a create at 2026-03-17 succeeding.
 */
export const effectiveDateForWorker = (parallelIndex: number): string => {
  const day = String(1 + (parallelIndex % 28)).padStart(2, '0');
  return `2026-03-${day}`;
};

/** How the results grid renders an effective date (`formatDisplayDate`, en-CA long month). */
export const renderedEffectiveDate = (isoDate: string): string => {
  const [y, m, d] = isoDate.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('en-CA', { month: 'long', day: 'numeric', year: 'numeric' });
};

/** Column headers the results grid renders, exactly as the DOM shows them. */
export const flatPriceColumnHeaders = [
  'Maturity',
  'Species',
  'Sort code',
  'Grade',
  'Relative price',
  'Effective date',
  'Expiry date',
  'Actions',
] as const;

/** Anchors the preflight re-checks, so a re-extracted DB fails fast rather than mid-scenario. */
export const fpcpAnchors = {
  modellingCode: PRODUCTION_MODELLING_CODE,
  species: birchSpecies,
  grades: birchGrades,
  row: ownRow,
} as const;
