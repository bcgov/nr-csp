import { type Locator, type Page } from '@playwright/test';

/**
 * Cross-domain Carbon Design System DOM patterns — no domain vocabulary. Every Carbon
 * `invalid`/`invalidText` field (TextInput, TextArea, Select, DateInput, FilterableMultiSelect) renders
 * its error the same way, so this locator lives in common/ and is reused rather than re-derived per domain.
 */

/**
 * The Carbon requirement/error text rendered under a specific field, scoped to that field's form-item
 * so a short message (e.g. "Required.") is unambiguous. `id` is a `#`-prefixed selector.
 */
export function fieldError(page: Page, id: string): Locator {
  return page
    .locator(`xpath=//*[@id="${id.slice(1)}"]/ancestor::div[contains(@class,"cds--form-item")][1]`)
    .locator('.cds--form-requirement');
}

/**
 * ⚠ THE LOADING SKELETON IS A REAL TABLE, AND IT LOOKS LIKE RESULTS.
 *
 * While a query is in flight, `components/Form/ResultsTable` replaces the grid with Carbon's
 * `DataTableSkeleton`. That is not an overlay or a spinner — it renders a genuine `<table>` with
 * the REAL column headers in its `<th>`s and ten empty `<tbody><tr><td>` rows. Measured mid-flight
 * against the flat-price grid:
 *
 *   table matched by a columnheader filter : 1   (the skeleton)
 *   "Your search results will appear here" : 0   (the placeholder row is gone)
 *   rows that are not the placeholder      : 10  (all blank)
 *   first row's cells                      : ["","","","","","",""]
 *
 * So a naive grid locator cannot tell "still loading" from "ten results", and the pre-search empty
 * state vanishing is NOT a signal that results have arrived. That combination silently breaks two
 * things: a `search()` that settles on the placeholder disappearing returns while the query is
 * still running, and an assertion of "at least one row" passes against blank skeleton rows — the
 * test then either reads empty strings or, worse, proves nothing at all.
 *
 * Every grid whose page passes `isLoading` is exposed: Inbox, Invoice search and Flat price
 * conversion. (Submission detail and the invoice group summary do not pass it.)
 *
 * These two helpers are the fix, and they belong here rather than in one page object because the
 * trap is a property of the shared `ResultsTable`, not of any one screen.
 */

/**
 * The REAL results table for a grid — the loading skeleton is excluded structurally, so anything
 * chained off it (row counts, cell reads) can never see a blank skeleton row.
 *
 * `columnHeader` must be a header only the target grid has, which also keeps this correct on the
 * screens that nest a detail table inside every expandable row.
 */
export function resultsTable(page: Page, columnHeader: string): Locator {
  return page
    .locator('table:not(.cds--skeleton)')
    .filter({ has: page.getByRole('columnheader', { name: columnHeader, exact: true }) });
}

/** Carbon's `DataTableSkeleton` table. Present exactly while a grid query is in flight. */
export function loadingSkeleton(page: Page): Locator {
  return page.locator('table.cds--skeleton');
}
