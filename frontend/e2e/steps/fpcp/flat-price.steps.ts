import {
  PRODUCTION_MODELLING_CODE,
  allGradeCount,
  birchGrades,
  birchSpecies,
  flatPriceColumnHeaders,
  ownRow,
  renderedEffectiveDate,
} from '../../fixtures/fpcp/flat-price-test-data';
import { assertMockRole } from '../../pages/common/authNav';
import { Given, When, Then, expect } from '../fixtures';

/**
 * FPCP domain steps — Production Flat Price Conversion maintenance (UC-FPCP-001).
 *
 * No DOM selectors here; those belong to `pages/fpcp/FlatPriceConversionPage.ts`.
 *
 * These rows are LIVE PRICING — `PriceConversionService` reads them when an invoice is submitted —
 * so the scenario works only on a row it created itself and deletes on teardown. It never edits
 * seeded data.
 */

type FlatPriceRow = {
  id: number;
  modellingCode: string;
  maturity: string;
  species: string;
  grade: string;
  sortCode: string;
  flatPriceConversion: number;
  effectiveDate: string;
  expiryDate: string | null;
  revisionCount: number;
};

/** Column index by header name, so row assertions never depend on column order. */
const columnIndex = (header: (typeof flatPriceColumnHeaders)[number]): number =>
  flatPriceColumnHeaders.indexOf(header);

Given('I am on the flat price conversion page as a CSP {word}', async ({ flatPricePage }, role: string) => {
  // assertMockRole throws on a role the app does not define — without it an unknown role would
  // silently fall back to ADMIN and any permission assertion would pass for the wrong reason.
  await flatPricePage.open(assertMockRole(role));
});

Given('a production flat price row exists for Birch', async ({ seedOwnFlatPriceRow, world }) => {
  const row = await seedOwnFlatPriceRow();
  world.flatPriceRowId = row.id;
  world.flatPriceEffectiveDate = row.effectiveDate;
});

Then('the results grid is awaiting criteria', async ({ flatPricePage }) => {
  // No query runs until Search is clicked; until then the grid holds a single EMPTY-STATE row.
  // Asserting that FIRST is what stops the later row assertions from being satisfied by the
  // placeholder — the same trap the Inbox and Invoice search scenarios document.
  await expect(flatPricePage.emptyState).toHaveCount(1);
  await expect(flatPricePage.resultRows).toHaveCount(0);
});

Then('the results grid shows the flat price columns', async ({ flatPricePage }) => {
  for (const header of flatPriceColumnHeaders) {
    await expect(flatPricePage.columnHeader(header)).toBeVisible();
  }
});

// ---------------------------------------------------------------------------
// UC-FPCP-001-S01 sc.1 — search by species, then edit a row
// ---------------------------------------------------------------------------

When('I search the production table for Birch', async ({ flatPricePage }) => {
  await flatPricePage.filterBySpecies(birchSpecies.label);
  await flatPricePage.search();
});

Then('every returned row is for Birch', async ({ flatPricePage }) => {
  // No exact count, deliberately. The seeded table holds 10 Birch rows, but this scenario has just
  // added an 11th of its own, and a concurrent copy adds another — so the total is legitimately
  // variable. What must always hold is that the filter was applied, which is the property under
  // test and the thing an "at least one row" assertion (all the legacy slice asks for) cannot see.
  // POLL ON THE CELL VALUES, not on the row count — a count-then-read can pass the count while the
  // grid is still repainting and then read empty cells.
  await expect
    .poll(
      async () => {
        const rows = await flatPricePage.allRowCells();
        if (rows.length === 0) return 'no rows';
        const species = new Set(rows.map((cells) => cells[columnIndex('Species')]));
        return [...species].sort().join(',');
      },
      {
        timeout: 30_000,
        message:
          `Every row of a ${birchSpecies.label} search should be for that species. An empty string ` +
          `here means the grid was read while still repainting.`,
      },
    )
    .toBe(birchSpecies.label);
});

Then('my row appears with its original relative price', async ({ flatPricePage, world }) => {
  const rendered = renderedEffectiveDate(world.flatPriceEffectiveDate as string);
  const row = flatPricePage.rowByEffectiveDate(rendered);
  await expect(
    row,
    `No row for effective date ${rendered}. The grid exposes no id, so this scenario's own row is ` +
      `identified by its effective date — which is why each worker creates one on a distinct date.`,
  ).toHaveCount(1);

  // Per CELL, not against the joined row text: "111" would also match inside a date or another
  // column's value.
  const cells = await flatPricePage.rowCells(row);
  expect(cells[columnIndex('Maturity')], 'Maturity').toBe(ownRow.maturityLabel);
  expect(cells[columnIndex('Sort code')], 'Sort code').toBe(ownRow.sortCodeLabel);
  expect(cells[columnIndex('Grade')], 'Grade').toBe(ownRow.grade);
  expect(cells[columnIndex('Relative price')], 'Relative price').toBe(String(ownRow.initialPrice));
  // No expiry date was set, and the grid prints a dash rather than leaving the cell blank.
  expect(cells[columnIndex('Expiry date')], 'Expiry date').toBe('-');
});

When('I edit my row to a new relative price', async ({ flatPricePage, world }) => {
  const rendered = renderedEffectiveDate(world.flatPriceEffectiveDate as string);
  const row = flatPricePage.rowByEffectiveDate(rendered);
  await flatPricePage.openEditFor(row);

  // The dialog opens pre-filled from the row. Asserting that BEFORE changing anything is what
  // makes the edit meaningful: it proves the form is bound to the row that was clicked, rather
  // than to a default or to whichever row happened to be first.
  await expect(flatPricePage.modalField('edit', 'flat-price-conversion')).toHaveValue(
    String(ownRow.initialPrice),
  );

  await flatPricePage.changeRelativePriceTo(ownRow.editedPrice);
});

Then('the row update is confirmed on screen', async ({ flatPricePage }) => {
  // RE-GROUNDED. The legacy slice expects the growl "Record has been updated successfully."
  // (resource key `update.successful.info`, which its own TODO says was never confirmed). This app
  // shows a toast reading "Row updated successfully."
  await flatPricePage.expectToastShown('Row updated successfully.');
});

Then('the grid shows my row at the new relative price', async ({ flatPricePage, world }) => {
  const rendered = renderedEffectiveDate(world.flatPriceEffectiveDate as string);
  const row = flatPricePage.rowByEffectiveDate(rendered);
  // Poll: the grid repaints when the invalidated search query resolves, so a single-shot read can
  // race the refetch.
  await expect
    .poll(
      async () => {
        const cells = await flatPricePage.rowCells(row);
        return cells[columnIndex('Relative price')];
      },
      {
        timeout: 30_000,
        message:
          'The grid never repainted with the edited price. The dialog closed, which means the PUT ' +
          'succeeded — so the results query was probably not invalidated.',
      },
    )
    .toBe(String(ownRow.editedPrice));
});

Then('the edit reads back from the API', async ({ request, world }) => {
  // The read-back that makes this a real check rather than "the grid repainted": the page
  // re-renders from its own query cache, so a UI-only assertion cannot distinguish a persisted
  // change from a cached one.
  let latest: FlatPriceRow | undefined;
  await expect
    .poll(
      async () => {
        const res = await request.get(
          `/api/flat-price-conversions?modellingCode=${PRODUCTION_MODELLING_CODE}` +
            `&species=${ownRow.speciesCode}`,
        );
        if (!res.ok()) return `HTTP ${res.status()}`;
        const rows = (await res.json()) as FlatPriceRow[];
        latest = rows.find((r) => r.id === world.flatPriceRowId);
        return latest?.flatPriceConversion ?? 'row absent';
      },
      {
        timeout: 30_000,
        message:
          `Row ${world.flatPriceRowId} never reported the edited price. The screen showed the ` +
          `change, so it did not reach Oracle or was rolled back.`,
      },
    )
    .toBe(ownRow.editedPrice);

  const row = latest as FlatPriceRow;
  // Assert the WHOLE row, not just the field that changed — an update that silently reset another
  // column would otherwise pass. The service rebuilds the record from the request body on every
  // PUT, so this is a real risk rather than a theoretical one.
  expect(row.modellingCode, 'modelling code must stay Production').toBe(PRODUCTION_MODELLING_CODE);
  expect(row.maturity, 'maturity').toBe(ownRow.maturityCode);
  expect(row.species, 'species').toBe(ownRow.speciesCode);
  expect(row.grade, 'grade').toBe(ownRow.grade);
  expect(row.sortCode, 'sort code').toBe(ownRow.sortCode);
  expect(row.effectiveDate, 'effective date').toBe(world.flatPriceEffectiveDate);
  expect(row.expiryDate, 'expiry date').toBeNull();
  // The row is versioned for optimistic locking, and a successful edit must advance it — that is
  // what makes a later concurrent edit fail loudly instead of overwriting silently.
  expect(row.revisionCount, 'revisionCount must advance on a successful edit').toBeGreaterThan(0);
});

// ---------------------------------------------------------------------------
// UC-FPCP-001-S01 sc.2 — search with no filters
// ---------------------------------------------------------------------------

When('I search without setting any filter', async ({ flatPricePage }) => {
  await flatPricePage.search();
});

Then('the grid returns production rows', async ({ flatPricePage }) => {
  // An unfiltered search spans the whole production table (4,025 rows at the time of writing) and
  // the grid pages at 100, so the exact total is not worth pinning — the seed could be
  // re-extracted, and this scenario is not about the count.
  //
  // But "some rows exist" is NOT enough on its own, and asserting only that was a bug: the loading
  // skeleton renders ten blank rows and no placeholder, so a row count plus an absent empty state
  // are BOTH satisfied while the query is still in flight. This scenario could pass even if the
  // search returned nothing. So assert the rows carry REAL DATA — every row must have a populated
  // Species and Relative price — which no skeleton row can satisfy.
  await expect
    .poll(
      async () => {
        const rows = await flatPricePage.allRowCells();
        if (rows.length === 0) return 'no rows';
        const blank = rows.filter(
          (cells) =>
            (cells[columnIndex('Species')] ?? '') === '' ||
            (cells[columnIndex('Relative price')] ?? '') === '',
        ).length;
        return blank === 0 ? 'all populated' : `${blank} blank rows`;
      },
      {
        timeout: 30_000,
        message:
          'An unfiltered search should return populated production rows. "blank rows" here means ' +
          'the grid was read while the loading skeleton was still up.',
      },
    )
    .toBe('all populated');
  await expect(flatPricePage.emptyState).toHaveCount(0);
});

// ---------------------------------------------------------------------------
// UC-FPCP-001-S01 sc.3 — Grade narrows by Species
// ---------------------------------------------------------------------------

Then('the filter Grade dropdown is not narrowed by the chosen species', async ({ flatPricePage }) => {
  // RE-GROUNDED, and it inverts the legacy expectation. The slice says the SEARCH FORM's Grade
  // dropdown updates by AJAX when Species is picked. In this app the filter Grade lists every
  // grade regardless (`gradeFilterOptions` is the unfiltered lookup) — it is the ADD/EDIT MODAL
  // whose Grade is narrowed. Asserting the actual behaviour, with the divergence logged, rather
  // than quietly testing the modal and calling the slice covered.
  const options = await flatPricePage.filterDropdownOptions('#grade-filter');
  expect(
    options.length,
    'the filter Grade dropdown is expected to list every grade, unnarrowed by species',
  ).toBe(allGradeCount);
});

Then('the edit dialog offers only the grades valid for Birch', async ({ flatPricePage, world }) => {
  const rendered = renderedEffectiveDate(world.flatPriceEffectiveDate as string);
  await flatPricePage.openEditFor(flatPricePage.rowByEffectiveDate(rendered));

  // POLL. `editGradeQuery` is enabled only once the modal enters edit mode, so
  // GET /api/lookup/grade-by-species/BI starts at the moment the dialog opens — until it resolves
  // the ComboBox has NO options, and a single-shot read would compare against [].
  await expect
    .poll(async () => (await flatPricePage.modalDropdownOptions('edit', 'grade')).sort().join(','), {
      timeout: 30_000,
      message:
        `The edit dialog's Grade list should be narrowed to the species' valid pairs ` +
        `(GET /api/lookup/grade-by-species/${birchSpecies.code}), not the full ` +
        `${allGradeCount}-grade lookup. An empty result means the lookup had not resolved yet.`,
    })
    .toBe([...birchGrades].sort().join(','));
});
