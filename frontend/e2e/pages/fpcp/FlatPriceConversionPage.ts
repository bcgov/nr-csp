import { type Page, type Locator, expect } from '@playwright/test';

import { signInAsMockUser, type MockRole } from '../common/authNav';
import { loadingSkeleton, resultsTable } from '../common/carbonHelpers';
import { expectToastShown } from '../common/toastRecorder';

/**
 * Page Object — Flat price conversion, Production table
 * (`/table-maintenance/flat-price-conversion`).
 *
 * Selectors only; business assertions live in steps/fpcp/flat-price.steps.ts.
 *
 * THREE THINGS THAT BITE HERE:
 *
 * 1. THE GRID DOES NOT AUTO-LOAD — same as Invoice search and the Inbox. No query runs until
 *    Search is clicked, and until then the body holds one EMPTY-STATE row ("Your search results
 *    will appear here."). `resultRows` excludes it; never count `tbody tr` directly.
 *
 * 2. EDITING IS A MODAL, NOT AN INLINE ROW. The legacy screen edited in place — an edit icon, then
 *    editable cells, then a save icon in the row. This app opens an "Edit row" dialog
 *    (`FormModal`) whose fields are `#edit-*`. Nothing about the row itself becomes editable.
 *
 * 3. DROPDOWNS SHOW DESCRIPTIONS, THE API TAKES CODES. Maturity, Species and Sort code all render
 *    their description in both the filters and the grid ("Cants / Export", "Birch", "Deciduous"),
 *    while Grade renders its bare code. A locator built from a code will not match.
 *
 * Filter values also persist (`usePersistentState`, namespace `csp.table.flatPriceConversion.v2`),
 * which is invisible to the suite because every scenario gets a fresh browser context — but it is
 * why a manual repro may open with filters already set.
 */
export class FlatPriceConversionPage {
  /** Route under test — ROUTES.FLAT_PRICE_CONVERSION in frontend/src/routes/routePaths.ts. */
  static readonly ROUTE = '/table-maintenance/flat-price-conversion';

  /** Text Carbon renders in the single placeholder row before any search has run. */
  private static readonly EMPTY_STATE = 'Your search results will appear here';

  constructor(private readonly page: Page) {}

  /** Seed the mock identity, then load the page. */
  async open(role: MockRole = 'ADMIN'): Promise<void> {
    await signInAsMockUser(this.page, role);
    await this.page.goto(FlatPriceConversionPage.ROUTE);
    await this.waitForLoaded();
  }

  /** Readiness: the page heading and the filter row are painted. */
  async waitForLoaded(): Promise<void> {
    await expect(this.page.getByRole('heading', { name: 'Flat price conversion' })).toBeVisible({
      timeout: 30_000,
    });
    await expect(this.searchButton).toBeVisible({ timeout: 30_000 });
  }

  // -------------------------------------------------------------------------
  // Filters
  // -------------------------------------------------------------------------

  /**
   * Choose a value in one of the filter dropdowns.
   *
   * These are `SearchableSelect` — a Carbon `ComboBox` over a LOCAL list (no API call, unlike the
   * client autocompletes elsewhere in the app). Typing narrows the list; this fills the input to
   * narrow it and then clicks the exact option, which keeps the click unambiguous when several
   * descriptions share a prefix.
   */
  private async selectFilter(fieldId: string, optionLabel: string): Promise<void> {
    const input = this.page.locator(fieldId);
    await input.click();
    await input.fill(optionLabel);
    await this.page.getByRole('option', { name: optionLabel, exact: true }).click();
  }

  async filterBySpecies(label: string): Promise<void> {
    await this.selectFilter('#species-filter', label);
  }

  async filterByMaturity(label: string): Promise<void> {
    await this.selectFilter('#maturity-filter', label);
  }

  async filterBySortCode(code: string): Promise<void> {
    await this.selectFilter('#sort-code-filter', code);
  }

  /** The filter-row Grade dropdown. Lists ALL grades — it is not narrowed by species (see 3 above). */
  get gradeFilterInput(): Locator {
    return this.page.locator('#grade-filter');
  }

  /**
   * The option labels a FILTER dropdown currently offers.
   *
   * Carbon renders a ComboBox's options only while it is open, so this opens the field, reads the
   * options, then closes it with Escape — leaving the filter row as it found it. Scoped to the
   * filter row so a simultaneously-open modal dropdown cannot bleed into the result.
   */
  async filterDropdownOptions(fieldId: string): Promise<string[]> {
    const input = this.page.locator(fieldId);
    await input.click();
    const options = await this.page
      .locator('.flat-price-conversion-page__filter-row')
      .getByRole('option')
      .allInnerTexts();
    await this.page.keyboard.press('Escape');
    return options.map((t) => t.trim());
  }

  // -------------------------------------------------------------------------
  // Actions
  // -------------------------------------------------------------------------

  /** Scoped to the filter row so it can never collide with the results toolbar's controls. */
  get searchButton(): Locator {
    return this.page
      .locator('.flat-price-conversion-page__filter-row--with-btn')
      .getByRole('button', { name: 'Search', exact: true });
  }

  get clearFiltersButton(): Locator {
    return this.page.getByRole('button', { name: 'Clear filters', exact: true });
  }

  get addNewRowButton(): Locator {
    return this.page.getByRole('button', { name: 'Add new row', exact: true });
  }

  /**
   * Run the search and wait for REAL results.
   *
   * Settling on the pre-search placeholder disappearing is not enough, and was a bug: the loading
   * skeleton has no placeholder row either, so that condition is met the instant the query starts.
   * See the note on `resultsTable` in pages/common/carbonHelpers.ts.
   */
  async search(): Promise<void> {
    const responded = this.page.waitForResponse(
      (r) => r.url().includes('/api/flat-price-conversions') && r.request().method() === 'GET',
      { timeout: 30_000 },
    );
    await this.searchButton.click();
    await responded;
    await expect(loadingSkeleton(this.page)).toHaveCount(0, { timeout: 30_000 });
  }

  // -------------------------------------------------------------------------
  // Results grid
  // -------------------------------------------------------------------------

  /** The results table, filtered on a header only it has. */
  get table(): Locator {
    return resultsTable(this.page, 'Relative price');
  }

  columnHeader(name: string): Locator {
    return this.page.getByRole('columnheader', { name, exact: true });
  }

  /** Every body row, INCLUDING the empty-state placeholder. Prefer `resultRows`. */
  get allBodyRows(): Locator {
    return this.table.locator('tbody tr');
  }

  /** Real result rows only. */
  get resultRows(): Locator {
    return this.allBodyRows.filter({ hasNotText: FlatPriceConversionPage.EMPTY_STATE });
  }

  get emptyState(): Locator {
    return this.allBodyRows.filter({ hasText: FlatPriceConversionPage.EMPTY_STATE });
  }

  async resultRowCount(): Promise<number> {
    return this.resultRows.count();
  }

  async rowCells(row: Locator): Promise<string[]> {
    return (await row.locator('td').allInnerTexts()).map((t) => t.trim());
  }

  async allRowCells(): Promise<string[][]> {
    const rows = await this.resultRows.all();
    return Promise.all(rows.map((row) => this.rowCells(row)));
  }

  /**
   * The row for a given effective date.
   *
   * The grid exposes no id, and every row this scenario works with shares its maturity, species,
   * grade and sort code with seeded rows — so the EFFECTIVE DATE is the only rendered column that
   * identifies the scenario's own row. That is exactly why each worker creates its row on a
   * distinct date (see `effectiveDateForWorker`).
   *
   * `renderedDate` must be the value as the grid prints it ("March 1, 2026"), not the ISO string.
   */
  rowByEffectiveDate(renderedDate: string): Locator {
    return this.resultRows.filter({
      has: this.page.getByRole('cell', { name: renderedDate, exact: true }),
    });
  }

  /** The Edit icon button within a row. Rendered only when the user holds prodFlatPriceConv/Edit. */
  editButtonIn(row: Locator): Locator {
    return row.getByRole('button', { name: 'Edit', exact: true });
  }

  /** The Delete icon button within a row. */
  deleteButtonIn(row: Locator): Locator {
    return row.getByRole('button', { name: 'Delete', exact: true });
  }

  // -------------------------------------------------------------------------
  // Add / Edit modal
  // -------------------------------------------------------------------------

  get modal(): Locator {
    return this.page.getByRole('dialog');
  }

  /** The modal's Save button (FormModal's default submit label). */
  get modalSaveButton(): Locator {
    return this.modal.getByRole('button', { name: 'Save', exact: true });
  }

  /**
   * A modal field, by the mode that owns it. The add and edit forms render the SAME field names
   * under different id prefixes (`#add-*` vs `#edit-*`), so the mode has to be explicit.
   */
  modalField(mode: 'add' | 'edit', field: string): Locator {
    return this.modal.locator(`#${mode}-${field}`);
  }

  /** Open the Edit dialog for a row and wait for it to be ready. */
  async openEditFor(row: Locator): Promise<void> {
    await this.editButtonIn(row).click();
    await expect(this.modal).toBeVisible();
    await expect(this.modal.getByText('Edit row')).toBeVisible();
  }

  /**
   * Replace the relative price in the open Edit dialog and save.
   *
   * Settles on the dialog closing: `handleEditSuccess` closes it only on a SUCCESSFUL update, so
   * its disappearance is the proof the PUT was accepted rather than rejected back into the form.
   */
  async changeRelativePriceTo(value: number): Promise<void> {
    const price = this.modalField('edit', 'flat-price-conversion');
    await price.fill(String(value));
    await expect(price).toHaveValue(String(value));
    await this.modalSaveButton.click();
    await expect(this.modal).toBeHidden({ timeout: 30_000 });
  }

  /**
   * The option labels currently offered by a modal dropdown.
   *
   * Carbon renders a ComboBox's options only while it is open, so this opens the field, reads the
   * options, then closes it with Escape — leaving the form as it found it.
   */
  async modalDropdownOptions(mode: 'add' | 'edit', field: string): Promise<string[]> {
    const input = this.modalField(mode, field);
    await input.click();
    const options = await this.modal.getByRole('option').allInnerTexts();
    await this.page.keyboard.press('Escape');
    return options.map((t) => t.trim());
  }

  /**
   * A bottom-right Carbon toast by its text.
   *
   * ⚠ DO NOT ASSERT ON THIS — use `expectToastShown`. Toasts auto-dismiss after 5s, so asserting
   * the live DOM races the app's own timer. See `pages/common/toastRecorder.ts`.
   */
  toast(text: string): Locator {
    return this.page.locator('.layout-toast-container').filter({ hasText: text });
  }

  /** Assert the app showed a toast containing `text` at any point in this scenario. */
  async expectToastShown(text: string): Promise<void> {
    await expectToastShown(this.page, text);
  }
}
