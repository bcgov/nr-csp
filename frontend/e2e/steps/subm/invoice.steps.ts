import {
  boomNumber,
  invoiceDate,
  invoiceHeaderCodes,
  singleLineItem,
  uniqueInvoiceNumber,
} from '../../fixtures/subm/invoice-test-data';
import { assertMockRole } from '../../pages/common/authNav';
import { Given, When, Then, expect } from '../fixtures';

/**
 * SUBM domain steps — manual invoice submission (UC-SUBM-001..004).
 *
 * No DOM selectors here; those belong to pages/invoice/InvoicePage.ts. Steps hold the domain
 * vocabulary and the assertions.
 *
 * VERIFICATION POLICY for this domain: every write is asserted TWICE — once on screen (what the
 * user sees) and once by reading the record back from `GET /api/invoices/{id}` (what was actually
 * persisted, including child line-item rows). The UI alone is not proof: the page hydrates from the
 * mutation's own response, so a screen can show a saved-looking invoice that never reached Oracle.
 */

/** Shapes of the bits of `InvoiceResponse` these steps assert on (services/invoice.service.ts). */
type LineItemReadBack = {
  lineItemID: number;
  secondSort: string;
  species: string;
  grade: string;
  numOfPieces: number;
  volume: number;
  price: number;
  amount: number;
};
type InvoiceReadBack = {
  invID: number;
  invNumber: string;
  invStatus: string;
  invType: string;
  invoiceDate: string;
  maturity: string | null;
  fobCode: string | null;
  submittedBy: string;
  submitterClientNum: string;
  submitterLocation: string;
  otherClientNum: string | null;
  otherClientLocation: string | null;
  boomNumbers: string[];
  totalPieces: number | null;
  totalVol: number | null;
  totalAmt: number | null;
  lineItems: LineItemReadBack[];
};

// ---------------------------------------------------------------------------
// Background / navigation
// ---------------------------------------------------------------------------

Given('I am signed in to the Invoice screen as a CSP {word}', async ({ invoicePage }, role: string) => {
  // assertMockRole throws on a role the app does not define — without it an unknown role would
  // silently fall back to ADMIN inside MockAuthProvider and the scenario would prove nothing.
  await invoicePage.openNewInvoice(assertMockRole(role));
});

Given('I start a new invoice', async ({ invoicePage }) => {
  // The blank form is the `/invoice` route with no id. Proving the page opened in the "New" state
  // FIRST is what makes the later "DFT" assertion meaningful — otherwise a stale loaded invoice
  // could satisfy it.
  await invoicePage.waitForFormReady();
  await expect(invoicePage.statusTag).toHaveText('New');
  expect(
    await invoicePage.currentInvoiceId(),
    'Expected the blank /invoice route, but the URL already carries an invoice id.',
  ).toBeNull();
});

// ---------------------------------------------------------------------------
// UC-SUBM-001-S01 — Create Purchase Invoice (header entry)
// ---------------------------------------------------------------------------

When('I enter a unique invoice number', async ({ invoicePage, world }) => {
  world.invoiceNumber = uniqueInvoiceNumber();
  await invoicePage.setInvoiceNumber(world.invoiceNumber);
});

When('I select {string} as the invoice type', async ({ invoicePage }, label: string) => {
  await invoicePage.selectInvoiceType(label);
});

When('I select {string} as the submitted-by party', async ({ invoicePage }, label: string) => {
  await invoicePage.selectSubmittedBy(label);
});

When('I choose the seeded submitting client', async ({ invoicePage, submissionClients, world }) => {
  // The client is whatever the seeded database resolved at run time — no name or number is
  // committed to this public repo. The full resolved name is used as the search term so the exact
  // option is guaranteed to appear, however short the client's first word happens to be.
  const [submitter] = submissionClients;
  world.submitterClient = submitter;
  await invoicePage.chooseSubmittingClient(submitter.clientName, submitter.clientName);
  // Selecting in the NAME autocomplete mirrors into the paired number + location fields
  // (`handleSubmittingClientSelect`). Assert that here: those two are what the request body
  // actually carries, and a silent mirror failure would otherwise surface as an opaque
  // "submitter client/location cannot be found in CSP" 400 several steps later.
  await expect(invoicePage.submittingClientNumberInput).toHaveValue(submitter.clientNumber);
  await expect(invoicePage.submittingClientLocationInput).toHaveValue(submitter.clientLocnCode);
});

When('I choose the seeded other party', async ({ invoicePage, submissionClients, world }) => {
  // A DIFFERENT client from the submitter — `isSameSellerAndBuyer` rejects an invoice whose two
  // parties share a number and location. `pickTwoDistinctClients` guarantees they differ.
  const [, other] = submissionClients;
  world.otherClient = other;
  await invoicePage.chooseOtherParty(other.clientName, other.clientName);
  await expect(invoicePage.otherClientNumberInput).toHaveValue(other.clientNumber);
  await expect(invoicePage.otherClientLocationInput).toHaveValue(other.clientLocnCode);
});

When('I select {string} as the maturity', async ({ invoicePage }, label: string) => {
  await invoicePage.selectMaturity(label);
});

When('I enter the seeded FOB code', async ({ invoicePage }) => {
  await invoicePage.setFobCode(invoiceHeaderCodes.fobCode);
});

When('I add a boom number as the source document reference', async ({ invoicePage, world }) => {
  world.boomNumber = boomNumber();
  await invoicePage.addBoomNumber(world.boomNumber);
  // TagInput only commits to the request body once the value is a chip. Asserting the chip exists
  // is what stops a silently-uncommitted value producing an "One of Boom Number, Timber Mark or
  // Weigh Slip must have a value" error that reads like a test-data problem.
  await expect(invoicePage.boomNumberChip(world.boomNumber)).toBeVisible();
});

When("I enter yesterday's date as the invoice date", async ({ invoicePage, world }) => {
  world.invoiceDate = invoiceDate();
  await invoicePage.setInvoiceDate(world.invoiceDate);
});

// ---------------------------------------------------------------------------
// Save / submit actions
// ---------------------------------------------------------------------------

When('I save the invoice', async ({ invoicePage }) => {
  await invoicePage.save();
});

When('I submit the invoice', async ({ invoicePage }) => {
  await invoicePage.submit();
});

// ---------------------------------------------------------------------------
// UC-SUBM-001-S01 — outcomes
// ---------------------------------------------------------------------------

Then('the invoice is created and opened in DFT status', async ({ invoicePage, world, createdInvoiceIds }) => {
  // Creating is the only thing that puts an id in the URL (`handleSave` -> navigate on success),
  // so this both captures the id and proves the POST was accepted.
  const id = await invoicePage.waitForSavedInvoiceId();
  world.invoiceId = id;
  // Register for teardown the MOMENT the id is known — a failure in any later step must still
  // clean this invoice up.
  createdInvoiceIds.push(id);

  await expect(invoicePage.statusTag).toHaveText('DFT');
  await expect(invoicePage.toast(`Invoice '${world.invoiceNumber}' created.`)).toBeVisible();
});

Then('the submit-reminder warning is displayed', async ({ invoicePage }) => {
  // messages.properties: invoice.submit.saved.warning. The backend raises it on every manual SAVE
  // (`InvoiceValidator.isSubmitProcessRequiered`), which is the new app's equivalent of the legacy
  // screen's growl. Asserted on the resolved TEXT, not the message key, because the key is not
  // rendered anywhere the user can see.
  await expect(
    invoicePage.warningBanner(
      'If you want the Invoice to be Submitted for Processing ensure you click on the Submit button',
    ),
  ).toBeVisible();
});

Then('the Submission ID stays blank because the invoice is manual', async ({ invoicePage }) => {
  // RE-GROUNDED, and deliberately the opposite of the legacy Gherkin's
  // `"[id$='cspSubmissionId']" is not empty`. In this app "Submission ID" shows
  // `InvoiceResponse.submissionNumber`, the BUSINESS submission number, which only an ESF
  // submission has — a manually-entered invoice has none and renders an em-dash. The surrogate
  // `csp_submission_id` the legacy field showed is an internal join key that is no longer exposed.
  await expect(invoicePage.metaValue('Submission ID')).toHaveText('—');
});

// ---------------------------------------------------------------------------
// UC-SUBM-002-S01 — Add Single Line Item
// ---------------------------------------------------------------------------

When('I add the seeded line item', async ({ invoicePage }) => {
  await invoicePage.addLineItem({
    secondarySortLabel: singleLineItem.secondarySortLabel,
    species: singleLineItem.species,
    grade: singleLineItem.grade,
    pieces: singleLineItem.pieces,
    volume: singleLineItem.volume,
    price: singleLineItem.price,
  });
});

Then('the group summary shows one group for the added line item', async ({ invoicePage }) => {
  await expect(invoicePage.groupRows).toHaveCount(1);
  const row = invoicePage.groupRowFor(singleLineItem.species, singleLineItem.secondarySortCode);
  await expect(
    row,
    `No group row for species ${singleLineItem.species} / secondary sort ` +
      `${singleLineItem.secondarySortCode}. Line items are grouped by (species, secondSort, exact ` +
      `price), so a change to any of those in the fixture moves the row.`,
  ).toHaveCount(1);

  // Assert per CELL, not against the whole joined row text: a substring check for "10" would also
  // be satisfied by a volume of "100.000" or an amount in a different column.
  const cells = await invoicePage.rowCells(row);
  expect(cells, 'Group row cells').toContain(singleLineItem.secondarySortDescription);
  expect(cells, 'Group row cells').toContain(singleLineItem.rendered.pieces);
  expect(cells, 'Group row cells').toContain(singleLineItem.rendered.volume);
  expect(cells, 'Group row cells').toContain(singleLineItem.rendered.amount);
});

Then('the invoice totals reflect the added line item', async ({ invoicePage }) => {
  // Two independent renderings of the same totals: the header meta values (derived from the
  // page's `lineItems` state) and the table's bold footer row (derived from the grouped rows).
  // Both must agree, which is what catches a grouping bug that leaves the header right and the
  // summary wrong.
  await expect(invoicePage.metaValue('Total pieces')).toHaveText(singleLineItem.rendered.pieces);
  await expect(invoicePage.metaValue('Total volume (m3)')).toHaveText(singleLineItem.rendered.volume);
  await expect(invoicePage.metaValue('Total amount')).toHaveText(singleLineItem.rendered.amount);

  const totals = await invoicePage.rowCells(invoicePage.invoiceTotalsRow);
  expect(totals, 'Invoice totals footer row').toContain(singleLineItem.rendered.pieces);
  expect(totals, 'Invoice totals footer row').toContain(singleLineItem.rendered.volume);
  expect(totals, 'Invoice totals footer row').toContain(singleLineItem.rendered.amount);
});

// ---------------------------------------------------------------------------
// UC-SUBM-003-S01 — Successfully Save Draft Invoice
// ---------------------------------------------------------------------------

Then('the invoice stays in DFT status', async ({ invoicePage, world }) => {
  await expect(invoicePage.statusTag).toHaveText('DFT');
  await expect(invoicePage.toast(`Invoice '${world.invoiceNumber}' saved.`)).toBeVisible();
});

Then('the save updated the existing invoice instead of creating another', async ({
  invoiceCreateCalls,
  invoiceUpdateCalls,
}) => {
  // The screen looks identical after a create and after an update — same route, same toast shape.
  // Only the HTTP verb distinguishes them, so this reads the transparent route spy. A second POST
  // here would mean the page lost its id and silently created a DUPLICATE invoice, which is
  // exactly the kind of failure a UI-only assertion cannot see.
  expect(invoiceCreateCalls(), 'POST /api/invoices calls (expected exactly the initial create)').toBe(1);
  expect(invoiceUpdateCalls(), 'PUT /api/invoices/{id} calls (expected the draft re-save)').toBe(1);
});

// ---------------------------------------------------------------------------
// UC-SUBM-004-S01 — Submit Valid DRAFT Invoice
// ---------------------------------------------------------------------------

Then('the invoice moves to {string} status on screen', async ({ invoicePage, world }, status: string) => {
  await expect(invoicePage.statusTag).toHaveText(status);
  await expect(invoicePage.toast(`Invoice '${world.invoiceNumber}' submitted.`)).toBeVisible();
});

Then('Submit is no longer offered', async ({ invoicePage }) => {
  // SUBMITTABLE_STATUSES is {DFT, UNA}: a PROCESSING invoice is already submitted, so the button
  // stays visible but disabled. Asserting this is what proves the status change actually took
  // effect in the page's own state rather than only in the status pill's text.
  await expect(invoicePage.submitButton).toBeVisible();
  await expect(invoicePage.submitButton).toBeDisabled();
});

// ---------------------------------------------------------------------------
// API read-back — the assertions that prove the writes reached the database
// ---------------------------------------------------------------------------

Then('the invoice reads back from the API in {string} status', async ({ request, world }, status: string) => {
  expect(world.invoiceId, 'No invoice id captured — the create step must run first.').toBeTruthy();

  // POLL, never a single-shot GET: the UI-triggered write can still be committing when the button
  // re-enables, so a bare GET races the commit and flakes. The last-seen body is kept so the
  // assertions below run against the very response that satisfied the poll — no second round trip.
  let latest: InvoiceReadBack | undefined;
  await expect
    .poll(
      async () => {
        const res = await request.get(`/api/invoices/${world.invoiceId}`);
        if (!res.ok()) return `HTTP ${res.status()}`;
        latest = (await res.json()) as InvoiceReadBack;
        return latest.invStatus;
      },
      {
        timeout: 30_000,
        message:
          `GET /api/invoices/${world.invoiceId} never returned the invoice in ${status} status. ` +
          `The screen showed the change, so the write either did not reach Oracle or was rolled back.`,
      },
    )
    .toBe(status);
  const body = latest as InvoiceReadBack;

  // Assert the FULL header, not merely the status — a status-only check would pass on an invoice
  // whose other fields were dropped or defaulted on the way to the database.
  expect(body.invNumber, 'persisted invoice number').toBe(world.invoiceNumber);
  expect(body.invType, 'persisted invoice type').toBe(invoiceHeaderCodes.purchaseTypeCode);
  expect(body.submittedBy, 'persisted submitted-by').toBe(invoiceHeaderCodes.submittedBy);
  expect(body.invoiceDate, 'persisted invoice date').toBe(world.invoiceDate);
  expect(body.maturity, 'persisted maturity').toBe(invoiceHeaderCodes.maturityCode);
  expect(body.fobCode, 'persisted FOB code').toBe(invoiceHeaderCodes.fobCode);
  // Compared against the clients the scenario RESOLVED, not against committed literals.
  expect(body.submitterClientNum, 'persisted submitter client number').toBe(
    world.submitterClient?.clientNumber,
  );
  expect(body.submitterLocation, 'persisted submitter location').toBe(
    world.submitterClient?.clientLocnCode,
  );
  expect(body.otherClientNum, 'persisted other-party client number').toBe(world.otherClient?.clientNumber);
  expect(body.otherClientLocation, 'persisted other-party location').toBe(
    world.otherClient?.clientLocnCode,
  );
  // Boom numbers are a CHILD table (log sources), not a column — read them back explicitly.
  expect(body.boomNumbers, 'persisted boom numbers').toEqual([world.boomNumber]);
});

Then('the line item reads back from the API with its calculated amount', async ({ request, world }) => {
  let latest: InvoiceReadBack | undefined;
  await expect
    .poll(
      async () => {
        const res = await request.get(`/api/invoices/${world.invoiceId}`);
        if (!res.ok()) return -1;
        latest = (await res.json()) as InvoiceReadBack;
        return latest.lineItems.length;
      },
      {
        timeout: 30_000,
        message:
          `The line item never appeared on GET /api/invoices/${world.invoiceId}. The group summary ` +
          `rendered it, so the POST /line-items response was applied to the page without the row ` +
          `being persisted.`,
      },
    )
    .toBe(1);
  const invoice = latest as InvoiceReadBack;

  const [line] = invoice.lineItems;
  expect(line.secondSort, 'persisted secondary sort').toBe(singleLineItem.secondarySortCode);
  expect(line.species, 'persisted species').toBe(singleLineItem.species);
  expect(line.grade, 'persisted grade').toBe(singleLineItem.grade);
  expect(line.numOfPieces, 'persisted pieces').toBe(singleLineItem.pieces);
  expect(Number(line.volume), 'persisted volume').toBe(Number(singleLineItem.volume));
  expect(Number(line.price), 'persisted price').toBe(Number(singleLineItem.price));
  // The BACKEND is canonical for the amount (volume x price, HALF_UP to 2dp) — the modal's
  // "$Amount" box is only a preview. Asserting the stored value is what makes this a real check.
  expect(Number(line.amount), 'amount calculated and stored by the backend').toBe(singleLineItem.expectedAmount);

  // The header totals are recalculated and PERSISTED from the line items on every write
  // (`InvoiceService.persistCalculatedTotals`), so they are part of the read-back too.
  expect(invoice.totalPieces, 'persisted total pieces').toBe(singleLineItem.pieces);
  expect(Number(invoice.totalVol), 'persisted total volume').toBe(Number(singleLineItem.volume));
  expect(Number(invoice.totalAmt), 'persisted total amount').toBe(singleLineItem.expectedAmount);
});
