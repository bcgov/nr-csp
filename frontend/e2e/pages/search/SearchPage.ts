import { type Page, type Locator, expect } from '@playwright/test';

import { signInAsMockUser, setDateField, type MockRole } from '../common/authNav';
import { loadingSkeleton, resultsTable } from '../common/carbonHelpers';

/**
 * Page Object — Invoice search (`/search`).
 *
 * Selectors only; business assertions live in steps/search/search.steps.ts.
 *
 * TWO THINGS THAT BITE HERE:
 *
 * 1. THE GRID DOES NOT AUTO-LOAD. Like the Inbox, it issues no query until Search is clicked, and
 *    until then the table body holds a single EMPTY-STATE row ("Your search results will appear
 *    here."). A naive `tbody tr` count therefore returns 1 on an empty grid and silently satisfies
 *    an "at least one row" assertion. `resultRows` excludes it; use that, never `tbody tr`.
 *
 * 2. THERE ARE TWO "SEARCH" CONTROLS. The filter row has the Search BUTTON, and the results
 *    toolbar has a keyword search INPUT ("Search by keyword"). `searchButton` is scoped to the
 *    filter row so the two can never be confused.
 *
 * The filter values also persist across reloads (`usePersistentState`, namespace
 * `csp.table.search.v1`). That is invisible to the suite because Playwright gives every scenario a
 * fresh browser context, but it is why a manual repro may show filters already filled in.
 */
export class SearchPage {
  /** Route under test — ROUTES.SEARCH in frontend/src/routes/routePaths.ts. */
  static readonly ROUTE = '/search';

  /** Text Carbon renders in the single placeholder row before any search has run. */
  private static readonly EMPTY_STATE = 'Your search results will appear here';

  constructor(private readonly page: Page) {}

  /** Seed the mock identity, then load the search screen. */
  async open(role: MockRole = 'ADMIN'): Promise<void> {
    await signInAsMockUser(this.page, role);
    await this.page.goto(SearchPage.ROUTE);
    await this.waitForLoaded();
  }

  /** Readiness: the filter row and the grid's header are painted. */
  async waitForLoaded(): Promise<void> {
    await expect(this.invoiceNumberInput).toBeVisible({ timeout: 30_000 });
    await expect(this.table).toBeVisible({ timeout: 30_000 });
  }

  // -------------------------------------------------------------------------
  // Filters
  // -------------------------------------------------------------------------

  get invoiceNumberInput(): Locator {
    return this.page.locator('#invoice-number');
  }

  /** Carbon DatePickers — via the shared click/fill/Escape/blur helper. */
  async setDateRange(startIso: string, endIso: string): Promise<void> {
    await setDateField(this.page, '#start-date', startIso);
    await setDateField(this.page, '#end-date', endIso);
  }

  async setInvoiceNumber(value: string): Promise<void> {
    await this.invoiceNumberInput.fill(value);
  }

  /**
   * Pick from one of the filter dropdowns (Carbon `Dropdown` via the app's SingleSelect).
   * `optionLabel` is the DESCRIPTION — these dropdowns render `item.description`, so the Type
   * option reads "Purchase", not "PUR".
   */
  private async selectFromDropdown(fieldId: string, optionLabel: string): Promise<void> {
    await this.page.locator(fieldId).click();
    await this.page.getByRole('option', { name: optionLabel, exact: true }).click();
  }

  async selectType(label: string): Promise<void> {
    await this.selectFromDropdown('#type-filter', label);
  }

  async selectStatus(label: string): Promise<void> {
    await this.selectFromDropdown('#status-filter', label);
  }

  async selectMaturity(label: string): Promise<void> {
    await this.selectFromDropdown('#maturity-filter', label);
  }

  // -------------------------------------------------------------------------
  // Actions
  // -------------------------------------------------------------------------

  /**
   * The Search button in the filter row — NOT the results toolbar's keyword input. Scoped to the
   * filter row that contains it, so the two can never collide.
   */
  get searchButton(): Locator {
    return this.page
      .locator('.search-page__filter-row--with-btn')
      .getByRole('button', { name: 'Search', exact: true });
  }

  get clearFiltersButton(): Locator {
    return this.page.getByRole('button', { name: 'Clear filters', exact: true });
  }

  /**
   * Run the search and wait for REAL results.
   *
   * Settling on the pre-search placeholder disappearing is not enough, and was a bug: the loading
   * skeleton has no placeholder row either, so that condition is met the instant the query starts.
   * This waits for the response and then for the skeleton to be replaced — see the note on
   * `resultsTable` in pages/common/carbonHelpers.ts.
   *
   * `waitForResponse` is armed BEFORE the click, so a fast response cannot be missed.
   */
  async search(): Promise<void> {
    const responded = this.page.waitForResponse(
      (r) => r.url().includes('/api/search') && r.request().method() === 'GET',
      { timeout: 30_000 },
    );
    await this.searchButton.click();
    await responded;
    // The response landing is still not enough — React has to re-render out of the skeleton.
    await expect(loadingSkeleton(this.page)).toHaveCount(0, { timeout: 30_000 });
  }

  // -------------------------------------------------------------------------
  // Results grid
  // -------------------------------------------------------------------------

  /**
   * The results table, filtered on a header only it has. This grid is not expandable today, so a
   * bare `getByRole('table')` would work — but every other page object here scopes the same way,
   * and it stays correct if an expandable detail row is ever added.
   */
  get table(): Locator {
    return resultsTable(this.page, 'Invoice number');
  }

  /** Header cell by visible label. */
  columnHeader(name: string): Locator {
    return this.page.getByRole('columnheader', { name, exact: true });
  }

  /** Every body row, INCLUDING the empty-state placeholder. Prefer `resultRows`. */
  get allBodyRows(): Locator {
    return this.table.locator('tbody tr');
  }

  /** Real result rows only — the empty-state placeholder is filtered out. */
  get resultRows(): Locator {
    return this.allBodyRows.filter({ hasNotText: SearchPage.EMPTY_STATE });
  }

  /** True while the grid is showing its pre-search placeholder. */
  get emptyState(): Locator {
    return this.allBodyRows.filter({ hasText: SearchPage.EMPTY_STATE });
  }

  async resultRowCount(): Promise<number> {
    return this.resultRows.count();
  }

  /** Text of every cell in one row, in column order. */
  async rowCells(row: Locator): Promise<string[]> {
    return (await row.locator('td').allInnerTexts()).map((t) => t.trim());
  }

  /** Every result row's cells, for per-row filter-correctness assertions. */
  async allRowCells(): Promise<string[][]> {
    const rows = await this.resultRows.all();
    return Promise.all(rows.map((row) => this.rowCells(row)));
  }

  /**
   * The Invoice number link for one invoice.
   *
   * Addressed by the LINK TEXT rather than an href: this grid's link has `href="#"` and routes
   * through an onClick handler, so the id is not in the DOM. Invoice numbers repeat across the
   * seeded data in general, but the pinned target's number is unique — which is exactly why it was
   * chosen (see fixtures/search/search-test-data.ts).
   */
  invoiceLink(invoiceNumber: string): Locator {
    return this.resultRows.getByRole('link', { name: invoiceNumber, exact: true });
  }

  /**
   * The row holding that invoice's link.
   *
   * The `has:` locator must be rooted at the PAGE, not chained off `resultRows`: Playwright
   * resolves `has:` relative to each candidate row, so passing a locator that itself starts with
   * `table >> tbody tr` would look for a nested table inside a `<tr>` and match nothing.
   */
  invoiceRow(invoiceNumber: string): Locator {
    return this.resultRows.filter({
      has: this.page.getByRole('link', { name: invoiceNumber, exact: true }),
    });
  }

  /**
   * Open an invoice from the results.
   *
   * The link calls `preventDefault()` and navigates client-side with `state: { fromSearch: true }`
   * — that state is what makes the invoice page show an "Invoice search" breadcrumb, which is this
   * app's equivalent of the legacy screen's Back button.
   */
  async openInvoice(invoiceNumber: string, invoiceId: number): Promise<void> {
    await this.invoiceLink(invoiceNumber).click();
    await this.page.waitForURL(new RegExp(`/invoice/${invoiceId}(?:[/?#]|$)`), { timeout: 30_000 });
  }
}
