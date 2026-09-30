import {
  approvedInvoiceDisabledActions,
  purchase2013Filter,
  searchColumnHeaders,
  viewTargetInvoice,
  viewTargetValidationMessages,
} from '../../fixtures/search/search-test-data';
import { assertMockRole } from '../../pages/common/authNav';
import { Given, When, Then, expect } from '../fixtures';

/**
 * SEARCH domain steps — invoice search and view (UC-SRCH-001 / 002).
 *
 * No DOM selectors here; those belong to `pages/search/SearchPage.ts` and
 * `pages/invoice/InvoicePage.ts`.
 *
 * These scenarios never write. Their assertions are therefore about FILTER CORRECTNESS (every row
 * the grid returned genuinely matches what was asked for) rather than about persistence — a search
 * that returns rows is worthless if they are the wrong rows, and "at least one row", which the
 * legacy Gherkin settles for, cannot tell the difference.
 */

type SearchApiRow = {
  coastalLogSaleId: number;
  invoiceNumber: string;
  invoiceStatus: string;
  invoiceDate: string;
  type: string;
  clientNumber: string;
  clientName: string;
};

/** Column index by header name, so row assertions never depend on column order. */
const columnIndex = (header: (typeof searchColumnHeaders)[number]): number =>
  searchColumnHeaders.indexOf(header);

Given('I am on the invoice search screen as a CSP {word}', async ({ searchPage }, role: string) => {
  // assertMockRole throws on a role the app does not define — without it an unknown role would
  // silently fall back to ADMIN and the scenario would prove nothing about permissions.
  await searchPage.open(assertMockRole(role));
});

Then('the search grid is awaiting criteria', async ({ searchPage }) => {
  // The screen issues no query until Search is clicked; until then the grid holds a single
  // EMPTY-STATE row. Proving that FIRST is what stops the later row assertions from being
  // satisfied by the placeholder — the same trap the Inbox smoke scenario documents.
  await expect(searchPage.emptyState).toHaveCount(1);
  await expect(searchPage.resultRows).toHaveCount(0);
});

Then('the results grid shows the search columns', async ({ searchPage }) => {
  for (const header of searchColumnHeaders) {
    await expect(searchPage.columnHeader(header)).toBeVisible();
  }
});

// ---------------------------------------------------------------------------
// UC-SRCH-001-S01 — search by multiple criteria
// ---------------------------------------------------------------------------

When('I search for Purchase invoices in the 2013 date range', async ({ searchPage }) => {
  await searchPage.setDateRange(purchase2013Filter.startDate, purchase2013Filter.endDate);
  await searchPage.selectType(purchase2013Filter.typeLabel);
  await searchPage.search();
});

Then('the grid returns only the matching invoices', async ({ searchPage }) => {
  // An EXACT count is assertable here precisely because this slice of data is immune to the
  // suite's writing scenarios: the review pool is all SAL/ADJ, and the SUBM journey's Purchase
  // invoices are dated today. See fixtures/search/search-test-data.ts for the argument.
  await expect
    .poll(async () => searchPage.resultRowCount(), {
      timeout: 30_000,
      message:
        `Expected exactly ${purchase2013Filter.expectedRowCount} Purchase invoices dated in ` +
        `${purchase2013Filter.startDate}..${purchase2013Filter.endDate}. If this moved, either the ` +
        `seed image was rebuilt (re-ground fixtures/search/search-test-data.ts) or something in the ` +
        `suite has started creating Purchase invoices with historical dates.`,
    })
    .toBe(purchase2013Filter.expectedRowCount);

  // FILTER CORRECTNESS, per row and per column. This is the assertion that makes the search
  // meaningful: a grid that ignored the filters entirely would still satisfy a row count.
  const rows = await searchPage.allRowCells();
  rows.forEach((cells, i) => {
    expect(cells[columnIndex('Type')], `row ${i + 1} Type`).toBe(purchase2013Filter.everyRowType);
    expect(cells[columnIndex('Invoice status')], `row ${i + 1} Invoice status`).toBe(
      purchase2013Filter.everyRowStatus,
    );
    // The grid renders dates long-form via formatDisplayDate, not as the API's ISO string.
    expect(cells[columnIndex('Invoice date')], `row ${i + 1} Invoice date`).toBe(
      purchase2013Filter.everyRowDateRendered,
    );
  });
});

Then('the rendered rows match the search API', async ({ searchPage, request }) => {
  // The read-back that makes this a real connection proof rather than "the grid painted": issue
  // the same query straight to the API and compare. Param names must match SearchParams exactly —
  // Spring IGNORES unknown request params, so a typo would return the UNFILTERED page and this
  // assertion would quietly stop testing the filter at all.
  const res = await request.get(
    `/api/search?invType=${purchase2013Filter.typeCode}` +
      `&startDate=${purchase2013Filter.startDate}&endDate=${purchase2013Filter.endDate}` +
      `&page=0&size=100`,
  );
  expect(res.ok(), `GET /api/search returned HTTP ${res.status()}`).toBeTruthy();
  const body = (await res.json()) as { content: SearchApiRow[]; totalElements: number };

  expect(
    body.totalElements,
    'the API disagrees with the pinned expectation for this filter — re-ground the fixture',
  ).toBe(purchase2013Filter.expectedRowCount);

  // Compare SETS of invoice numbers, not row 0 against row 0: the grid is sortable and
  // server-ordered, so a positional comparison would flake and then blame the app.
  const apiNumbers = new Set(body.content.map((r) => r.invoiceNumber));
  const uiRows = await searchPage.allRowCells();
  const uiNumbers = new Set(uiRows.map((cells) => cells[columnIndex('Invoice number')]));
  expect(
    [...uiNumbers].sort(),
    'The rendered invoice numbers do not match the set the API returned — the grid is not showing ' +
      'live API data, or the two queries were not equivalent.',
  ).toEqual([...apiNumbers].sort());
});

When('I search by the target invoice number', async ({ searchPage, request, world }) => {
  // The invoice number is client-supplied data and is not committed to this public repo — it is
  // read from the record at run time and remembered for the assertion that follows.
  const res = await request.get(`/api/invoices/${viewTargetInvoice.invoiceId}`);
  expect(res.ok(), `GET /api/invoices/${viewTargetInvoice.invoiceId} returned HTTP ${res.status()}`).toBeTruthy();
  const { invNumber } = (await res.json()) as { invNumber: string };
  world.searchInvoiceNumber = invNumber;
  await searchPage.setInvoiceNumber(invNumber);
  await searchPage.search();
});

Then('exactly the target invoice is returned', async ({ searchPage, request, world }) => {
  // Cross-check the rendered row against the SEARCH API's own row for the same record, rather than
  // against committed literals: no client name, client number or invoice number is stored in this
  // public repo. This is also the stronger assertion — it cannot pass by agreeing with a stale
  // expectation.
  const res = await request.get(
    `/api/search?invNumber=${encodeURIComponent(world.searchInvoiceNumber as string)}&page=0&size=10`,
  );
  expect(res.ok(), `GET /api/search returned HTTP ${res.status()}`).toBeTruthy();
  const body = (await res.json()) as { content: SearchApiRow[]; totalElements: number };
  expect(body.totalElements, 'the pinned invoice number should match exactly one record').toBe(1);
  const [apiRow] = body.content;
  expect(apiRow.coastalLogSaleId, 'the API matched a different invoice than the pinned one').toBe(
    viewTargetInvoice.invoiceId,
  );

  await expect
    .poll(async () => (await searchPage.allRowCells()).length, { timeout: 30_000 })
    .toBe(1);
  const [cells] = await searchPage.allRowCells();
  expect(cells[columnIndex('Invoice number')], 'Invoice number').toBe(apiRow.invoiceNumber);
  expect(cells[columnIndex('Invoice status')], 'Invoice status').toBe(viewTargetInvoice.statusLabel);
  expect(cells[columnIndex('Client number')], 'Client number').toBe(apiRow.clientNumber);
  expect(cells[columnIndex('Client name')], 'Client name').toBe(apiRow.clientName);
  expect(cells[columnIndex('Maturity')], 'Maturity').toBe(viewTargetInvoice.maturityLabel);
});

When('I search by the status {string}', async ({ searchPage }, statusLabel: string) => {
  await searchPage.selectStatus(statusLabel);
  await searchPage.search();
});

Then('every returned invoice has the status {string}', async ({ searchPage }, statusLabel: string) => {
  // No exact count here, deliberately. A status search spans the whole seed, and the review
  // scenarios move borrowed invoices in and out of APPROVED/REJECTED while they run — so the
  // TOTAL is legitimately variable. What must always hold is that every row matches the filter,
  // which is the property being tested.
  //
  // POLL ON THE CELL VALUES, not on the row count. A count-then-read would be two separate reads:
  // the count can be satisfied while the grid is still repainting, and the follow-up read then
  // returns empty strings. Polling the assertion itself closes that gap. (The page object also
  // excludes the loading skeleton structurally — see pages/common/carbonHelpers.ts — so this is
  // belt and braces rather than the only guard.)
  await expect
    .poll(
      async () => {
        const rows = await searchPage.allRowCells();
        if (rows.length === 0) return 'no rows';
        const statuses = new Set(rows.map((cells) => cells[columnIndex('Invoice status')]));
        return [...statuses].sort().join(',');
      },
      {
        timeout: 30_000,
        message:
          `Every row of a "${statusLabel}" search should carry that status. An empty string here ` +
          `means the grid was read while still repainting.`,
      },
    )
    .toBe(statusLabel);
});

// ---------------------------------------------------------------------------
// UC-SRCH-002-S01 — view the invoice from the results
// ---------------------------------------------------------------------------

When('I open the target invoice from the results', async ({ searchPage, invoicePage, request }) => {
  const res = await request.get(`/api/invoices/${viewTargetInvoice.invoiceId}`);
  const { invNumber } = (await res.json()) as { invNumber: string };
  await searchPage.openInvoice(invNumber, viewTargetInvoice.invoiceId);
  await invoicePage.waitForExistingInvoiceLoaded();
});

Then('the invoice header matches the record I picked', async ({ invoicePage, request }) => {
  await expect(invoicePage.statusTag).toHaveText(viewTargetInvoice.status);
  // Read the expected number from the record rather than from a committed literal.
  const res = await request.get(`/api/invoices/${viewTargetInvoice.invoiceId}`);
  const { invNumber } = (await res.json()) as { invNumber: string };
  await expect(invoicePage.invoiceNumberInput).toHaveValue(invNumber);
  // Manual invoice, so "Submission ID" is an em-dash — the same re-grounding as UC-SUBM-001
  // (the field shows the BUSINESS submission number, which only an ESF submission has).
  await expect(invoicePage.metaValue('Submission ID')).toHaveText('—');
});

Then('the invoice shows a Back route to the search results', async ({ invoicePage }) => {
  // RE-GROUNDED from "the Back button is visible". This app has no Back button; arriving from
  // search sets `state: { fromSearch: true }`, which adds an "Invoice search" breadcrumb. That
  // breadcrumb only appears on that route, so asserting it also proves the navigation carried its
  // state rather than the scenario having jumped straight to the URL.
  await expect(invoicePage.breadcrumb('Invoice search')).toBeVisible();
});

Then('the group summary and totals match the record', async ({ invoicePage }) => {
  await expect(invoicePage.groupSummaryTable).toBeVisible();
  await expect(invoicePage.groupRows).toHaveCount(viewTargetInvoice.groupCount);

  await expect(invoicePage.metaValue('Total pieces')).toHaveText(viewTargetInvoice.rendered.pieces);
  await expect(invoicePage.metaValue('Total volume (m3)')).toHaveText(viewTargetInvoice.rendered.volume);
  await expect(invoicePage.metaValue('Total amount')).toHaveText(viewTargetInvoice.rendered.amount);

  // The bold footer row is derived from the grouped rows while the meta values come from the
  // page's line-item state — two independent renderings that must agree.
  const totals = await invoicePage.rowCells(invoicePage.invoiceTotalsRow);
  expect(totals, 'Invoice totals footer row').toContain(viewTargetInvoice.rendered.pieces);
  expect(totals, 'Invoice totals footer row').toContain(viewTargetInvoice.rendered.volume);
  expect(totals, 'Invoice totals footer row').toContain(viewTargetInvoice.rendered.amount);
});

Then('the record reads back from the API with its line items', async ({ request }) => {
  const res = await request.get(`/api/invoices/${viewTargetInvoice.invoiceId}`);
  expect(res.ok(), `GET /api/invoices/${viewTargetInvoice.invoiceId} returned HTTP ${res.status()}`).toBeTruthy();
  const body = (await res.json()) as {
    invStatus: string;
    invType: string;
    invoiceDate: string;
    lineItems: unknown[];
    totalPieces: number;
    totalVol: number;
    totalAmt: number;
  };
  expect(body.invStatus, 'status').toBe(viewTargetInvoice.status);
  expect(body.invType, 'type').toBe(viewTargetInvoice.type);
  expect(body.invoiceDate, 'invoice date').toBe(viewTargetInvoice.invoiceDate);
  expect(body.lineItems.length, 'line item count').toBe(viewTargetInvoice.lineItemCount);
  expect(body.totalPieces, 'total pieces').toBe(viewTargetInvoice.totals.pieces);
  expect(Number(body.totalVol), 'total volume').toBe(viewTargetInvoice.totals.volume);
  expect(Number(body.totalAmt), 'total amount').toBe(viewTargetInvoice.totals.amount);
});

Then('the actions an APPROVED invoice withholds are all disabled', async ({ invoicePage }) => {
  // RE-GROUNDED. UC-SRCH-002-S01 expects the view-from-search screen to carry NO Save / Approve /
  // Reject / Submit / Delete / Cancel buttons at all. This app has ONE invoice screen for viewing
  // and acting, so the buttons are present and gated by status instead. Asserting they are
  // DISABLED is the faithful equivalent — and a stronger check than absence, because it would
  // catch a regression that re-enabled an action on an approved record.
  for (const label of approvedInvoiceDisabledActions) {
    await expect(invoicePage.actionButton(label), `${label} on an APPROVED invoice`).toBeDisabled();
  }
  // Approve is not merely disabled on an APPROVED invoice — it is replaced by Unapprove, which
  // shares its slot.
  await expect(invoicePage.approveButton).toHaveCount(0);
});

Then('the page reports the validation problems with this legacy record', async ({ invoicePage }) => {
  // These messages are the honest state of the screen. The GET re-runs the validator with
  // ActionType.OTHER and returns what it finds, and this historical record breaks two rules the
  // new app enforces. Asserting them stops a future change that silently suppresses validation on
  // read from going unnoticed — and documents that legacy data does not satisfy today's rules.
  //
  // Only the UNMAPPED error reaches the banner. The other one is mapped to the Invoice type field
  // and is silently suppressed on this locked invoice — asserted separately, and red, in
  // suppressed-error.feature (BUG-001). Asserting it here would make this journey fail for a
  // reason that has nothing to do with search-and-view.
  for (const message of viewTargetValidationMessages.pageErrors) {
    await expect(invoicePage.errorBanner(message), `error banner: ${message}`).toBeVisible();
  }
  await expect(
    invoicePage.warningBanner(viewTargetValidationMessages.warningFragment),
    'month-complete warning banner',
  ).toBeVisible();
});
