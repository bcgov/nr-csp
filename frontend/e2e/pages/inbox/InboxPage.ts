import { type Page, type Locator, expect } from '@playwright/test';

import { signInAsMockUser, setDateField, type MockRole } from '../common/authNav';

/**
 * Page Object — Submission Inbox (`/inbox`).
 *
 * Selectors only; business assertions live in the steps. The results grid is a Carbon `DataTable`
 * (components/Form/ResultsTable) which renders a real <table>, so locators here are ROLE-based
 * rather than Carbon class names or nth-child — role semantics survive a Carbon upgrade.
 *
 * IMPORTANT — the Inbox does NOT auto-load. Until a search runs, the table body holds a single
 * EMPTY-STATE row ("Your search results will appear here..."). A naive `tbody tr` count therefore
 * returns 1 on an empty grid, which silently passes an "at least one row" assertion while proving
 * nothing. `resultRows` excludes that row; use it, not `tbody tr`.
 */
export class InboxPage {
  /** Route under test — ROUTES.INBOX in frontend/src/routes/routePaths.ts. */
  static readonly ROUTE = '/inbox';

  /** Text Carbon renders in the single placeholder row before any search has run. */
  private static readonly EMPTY_STATE = 'Your search results will appear here';

  constructor(private readonly page: Page) {}

  /** Seed the mock identity, then load the Inbox (safe as a full navigation — see authNav.ts). */
  async open(role: MockRole = 'ADMIN'): Promise<void> {
    await signInAsMockUser(this.page, role);
    await this.page.goto(InboxPage.ROUTE);
    await this.waitForLoaded();
  }

  /** Readiness: the grid (header row at minimum) is painted. */
  async waitForLoaded(): Promise<void> {
    await expect(this.table).toBeVisible({ timeout: 30_000 });
  }

  get table(): Locator {
    return this.page.getByRole('table');
  }

  /** Header cell by visible label, e.g. 'Submission ID', 'Submission date', 'Status'. */
  columnHeader(name: string): Locator {
    return this.page.getByRole('columnheader', { name, exact: true });
  }

  /** Every body row, INCLUDING the empty-state placeholder. Prefer `resultRows`. */
  get allBodyRows(): Locator {
    return this.table.locator('tbody tr');
  }

  /** Real result rows only — the empty-state placeholder is filtered out. */
  get resultRows(): Locator {
    return this.allBodyRows.filter({ hasNotText: InboxPage.EMPTY_STATE });
  }

  /** True while the grid is showing its pre-search placeholder. */
  get emptyState(): Locator {
    return this.allBodyRows.filter({ hasText: InboxPage.EMPTY_STATE });
  }

  async resultRowCount(): Promise<number> {
    return this.resultRows.count();
  }

  /**
   * Run a date-range search — the Inbox's primary filter, and the one UC-INBOX-001-S01 exercises.
   * Dates go through the shared Carbon DatePicker helper (click, fill, Escape, blur) because the
   * calendar overlay otherwise swallows the commit.
   */
  async searchByDateRange(startIso: string, endIso: string): Promise<void> {
    await setDateField(this.page, '#date-start', startIso);
    await setDateField(this.page, '#date-end', endIso);
    await this.searchButton.click();
  }

  get searchButton(): Locator {
    return this.page.getByRole('button', { name: 'Search', exact: true });
  }

  get clearFiltersButton(): Locator {
    return this.page.getByRole('button', { name: 'Clear filters', exact: true });
  }

  /** The row whose Submission ID cell holds `submissionId` (the business submission number). */
  rowBySubmissionId(submissionId: string): Locator {
    return this.resultRows.filter({
      has: this.page.getByRole('cell', { name: submissionId, exact: true }),
    });
  }

  /** Text of every cell in one row, in column order. */
  async rowCells(row: Locator): Promise<string[]> {
    return (await row.locator('td').allInnerTexts()).map((t) => t.trim());
  }

  /** Submission ID of the first real result row, as rendered. */
  async firstResultSubmissionId(): Promise<string> {
    return (await this.resultRows.first().locator('td').first().innerText()).trim();
  }

  /**
   * Every rendered Submission ID, for an ORDER-INDEPENDENT comparison against the API. The Inbox
   * orders by entry_timestamp DESC with no tiebreaker, so positional comparisons are unsafe.
   */
  async renderedSubmissionIds(): Promise<string[]> {
    const first = this.resultRows.locator('td:first-child');
    return (await first.allInnerTexts()).map((t) => t.trim());
  }
}
