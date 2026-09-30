import { type Page, type Locator, expect } from '@playwright/test';

/**
 * Page Object — View Submission (`/submission-history/:submissionId`).
 *
 * The middle screen of the review journey: the Inbox links here by submission number, and the
 * Invoices table here links on to `/invoice/:id`.
 *
 * ⚠ THE ROUTE PARAM IS THE BUSINESS SUBMISSION NUMBER, not the internal `csp_submission_id` — the
 * detail endpoint is keyed on it (see the comment on `useParams` in `pages/ViewSubmission/index.tsx`).
 * Passing the surrogate id yields a 404 that looks like missing data rather than a wrong key.
 *
 * ⚠ ROWS ARE ADDRESSED BY INVOICE ID, NEVER BY INVOICE NUMBER. Invoice numbers repeat inside a
 * submission (four invoices in 57547 are all "67061"), so a number-based locator matches several
 * rows and Playwright's strict mode fails — or worse, a `.first()` picks an arbitrary one. The
 * Invoice # cell is a link whose href carries the unique id, which is what this object keys on.
 */
export class ViewSubmissionPage {
  static readonly ROUTE = '/submission-history';

  constructor(private readonly page: Page) {}

  static routeFor(submissionId: string): string {
    return `${ViewSubmissionPage.ROUTE}/${submissionId}`;
  }

  /** Readiness: the Invoices card has rendered its table (the page loads behind a spinner). */
  async waitForLoaded(): Promise<void> {
    await expect(this.invoicesHeading).toBeVisible({ timeout: 30_000 });
    await expect(this.invoicesTable).toBeVisible({ timeout: 30_000 });
  }

  get invoicesHeading(): Locator {
    return this.page.getByRole('heading', { name: 'Invoices', exact: true });
  }

  /**
   * The OUTER Invoices table.
   *
   * A bare `getByRole('table')` resolves to 38 elements here: ResultsTable is expandable, and
   * Carbon renders every row's expansion content in the DOM whether or not it is expanded — so
   * each of the submission's 37 invoices contributes a nested line-item table. Filtering on the
   * "Invoice #" column header, which only the outer table has, picks it unambiguously without
   * resorting to `.first()`.
   */
  get invoicesTable(): Locator {
    return this.page
      .getByRole('table')
      .filter({ has: this.page.getByRole('columnheader', { name: 'Invoice #', exact: true }) });
  }

  /** The "Submission by … · Submission ID <n> · <date>" summary line above the detail sections. */
  get summaryText(): Locator {
    return this.page.locator('.view-submission-page__summary-text');
  }

  get submissionStatusTag(): Locator {
    return this.page.locator('.view-submission-page__summary .cds--tag').first();
  }

  /** The "n invoices · n line items · Total $x · y m³ · z pieces" line under the Invoices heading. */
  get invoicesSummary(): Locator {
    return this.page.locator('.view-submission-page__section-count');
  }

  /** The Invoice # link for one invoice, addressed by its unique id (see the class note). */
  invoiceLink(invoiceId: number): Locator {
    return this.page.locator(`a[href="/invoice/${invoiceId}"]`);
  }

  /** The table row holding that invoice's link. */
  invoiceRow(invoiceId: number): Locator {
    return this.invoicesTable.locator('tbody tr').filter({ has: this.invoiceLink(invoiceId) });
  }

  /** Text of every cell in one row, in column order. */
  async rowCells(row: Locator): Promise<string[]> {
    return (await row.locator('td').allInnerTexts()).map((t) => t.trim());
  }

  /**
   * Open an invoice from the table.
   *
   * The link's `onClick` calls `preventDefault()` and routes client-side, so this is an SPA
   * transition, not a document load — waitForURL is the right settle signal.
   */
  async openInvoice(invoiceId: number): Promise<void> {
    await this.invoiceLink(invoiceId).click();
    await this.page.waitForURL(new RegExp(`/invoice/${invoiceId}(?:[/?#]|$)`), { timeout: 30_000 });
  }
}
