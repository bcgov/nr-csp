import { invoiceStatus, reviewSubmissions } from '../../fixtures/inbox/review-test-data';
import { ViewSubmissionPage } from '../../pages/inbox/ViewSubmissionPage';
import { Given, When, Then, expect } from '../fixtures';

/**
 * INBOX domain — review/approve steps (UC-INBOX-001 / 002 / 003).
 *
 * Search steps are NOT redefined here: `steps/inbox/inbox.steps.ts` already owns
 * "I am signed in as a CSP {word}" and "I search the full seeded date range", and this journey
 * reuses them rather than adding near-duplicates.
 *
 * No DOM selectors here — they belong to `pages/inbox/InboxPage.ts`,
 * `pages/inbox/ViewSubmissionPage.ts` and `pages/invoice/InvoicePage.ts`.
 */

type InvoiceReadBack = {
  invID: number;
  invStatus: string;
  submissionNumber: number | null;
  lineItems: { lineItemID: number }[];
  boomNumbers: string[];
};

// ---------------------------------------------------------------------------
// UC-INBOX-001-S01 — find the submission that needs reviewing
// ---------------------------------------------------------------------------

Given('an UNAPPROVED invoice is waiting to be reviewed', async ({ borrowedInvoice, world }) => {
  // Resolving the fixture here is what borrows the row and arms the restore-on-teardown. It also
  // verifies the row really is UNA before the journey leans on it — so "waiting to be reviewed" is
  // a checked precondition, not an assumption.
  world.reviewInvoiceId = borrowedInvoice.invoiceId;
  world.reviewSubmissionId = borrowedInvoice.submissionId;
  expect(
    borrowedInvoice.enteredByLegacyUser,
    'The borrowed invoice must have been entered by someone OTHER than the mock user, or the ' +
      'backend refuses the approval (invoice.entry.user.cannot.approve.it.error).',
  ).toBe(true);
});

Then("the submission holding that invoice is listed in the inbox results", async ({ inboxPage, world }) => {
  const submissionId = world.reviewSubmissionId as string;
  const row = inboxPage.rowBySubmissionId(submissionId);
  await expect(
    row,
    `Submission ${submissionId} is not in the Inbox results for the seeded date window. Either the ` +
      `DB image was rebuilt (re-ground fixtures/inbox/review-test-data.ts) or the window no longer ` +
      `spans its submission date.`,
  ).toHaveCount(1);

  // Assert the row's own data, per column — proving we found the right submission rather than just
  // some row containing the digits.
  const expected = reviewSubmissions[submissionId];
  const cells = await inboxPage.rowCells(row);
  expect(cells, `Inbox row for submission ${submissionId}`).toContain(expected.submissionStatus);
  // The RENDERED date ("April 22, 2016"), not the API's ISO form — the grid formats it.
  expect(cells, `Inbox row for submission ${submissionId}`).toContain(expected.submissionDateRendered);
});

// ---------------------------------------------------------------------------
// UC-INBOX-002-S01 — open the submission, then the invoice
// ---------------------------------------------------------------------------

When('I open that submission from the inbox', async ({ page, inboxPage, world }) => {
  const submissionId = world.reviewSubmissionId as string;
  await inboxPage.openSubmission(submissionId);
  await expect(page).toHaveURL(new RegExp(`${ViewSubmissionPage.routeFor(submissionId)}(?:[/?#]|$)`));
});

Then('the submission detail page lists its invoices', async ({ viewSubmissionPage, world }) => {
  await viewSubmissionPage.waitForLoaded();
  await expect(viewSubmissionPage.summaryText).toContainText(`Submission ID ${world.reviewSubmissionId}`);
  // The invoice we are about to open must actually be on this submission — otherwise the journey
  // would be navigating by URL rather than by following the app's own links.
  await expect(viewSubmissionPage.invoiceRow(world.reviewInvoiceId as number)).toHaveCount(1);
});

Then('that invoice is shown as UNAPPROVED in the submission', async ({ viewSubmissionPage, world }) => {
  const row = viewSubmissionPage.invoiceRow(world.reviewInvoiceId as number);
  const cells = await viewSubmissionPage.rowCells(row);
  // The "Decision" column shows the status DESCRIPTION ("Unapproved"), not the code — this
  // endpoint resolves it, unlike /api/invoices/{id}. See the note in review-test-data.ts.
  expect(cells, `Decision column for invoice ${world.reviewInvoiceId}`).toContain(
    invoiceStatus.unapprovedLabel,
  );
});

When('I open that invoice from the submission', async ({ viewSubmissionPage, invoicePage, world }) => {
  await viewSubmissionPage.openInvoice(world.reviewInvoiceId as number);
  await invoicePage.waitForExistingInvoiceLoaded();
});

Then('the invoice details page shows it as UNAPPROVED', async ({ invoicePage }) => {
  await expect(invoicePage.statusTag).toHaveText(invoiceStatus.unapproved);
});

Then('the invoice shows its line items and source documents', async ({ invoicePage }) => {
  // UC-INBOX-002-S01 asserts the group summary and the Boom / Timber / Weigh panels are present.
  // In this app all three source-document fields always render (they are TagInputs on the form),
  // so their presence alone would prove nothing — assert the group summary actually has rows, and
  // that the boom field carries a real committed value.
  await expect(invoicePage.groupSummaryTable).toBeVisible();
  await expect(invoicePage.groupRows.first()).toBeVisible();
  expect(await invoicePage.groupRows.count(), 'group summary rows').toBeGreaterThan(0);

  await expect(invoicePage.boomNumbersInput).toBeVisible();
  await expect(invoicePage.timberMarksInput).toBeVisible();
  await expect(invoicePage.weighSlipsInput).toBeVisible();
  expect(
    await invoicePage.tagChipsFor('#boom-numbers').count(),
    'boom-number chips on the borrowed invoice — the pinned rows all carry exactly one',
  ).toBeGreaterThan(0);
});

Then('Approve and Reject are offered and Unapprove is not', async ({ invoicePage }) => {
  // UC-INBOX-002-S01 words this as "Unapprove is disabled". Re-grounded: Approve and Unapprove
  // share one slot in the button row (`canUnapprove ? <Unapprove/> : <Approve/>`), so on an UNA
  // invoice the Unapprove button is ABSENT, not present-and-disabled.
  await expect(invoicePage.approveButton).toBeVisible();
  await expect(invoicePage.approveButton).toBeEnabled();
  await expect(invoicePage.rejectButton).toBeVisible();
  await expect(invoicePage.rejectButton).toBeEnabled();
  await expect(invoicePage.unapproveButton).toHaveCount(0);
});

// ---------------------------------------------------------------------------
// UC-INBOX-003-S01 — approve
// ---------------------------------------------------------------------------

When('I approve the invoice', async ({ invoicePage }) => {
  await invoicePage.approve();
});

Then('the invoice becomes APPROVED on screen', async ({ invoicePage }) => {
  await expect(invoicePage.statusTag).toHaveText(invoiceStatus.approved);
  // The toast names the invoice; the invoice number is not unique across a submission, so this
  // asserts the shape and the verb rather than pinning the number.
  await expect(invoicePage.toast('approved.')).toBeVisible();
});

Then('Approve is replaced by Unapprove', async ({ invoicePage }) => {
  // The legacy wording is "Approve becomes disabled and Unapprove becomes enabled". Same intent,
  // re-grounded to how this app renders it — see the note on the previous button-state step.
  await expect(invoicePage.unapproveButton).toBeVisible();
  await expect(invoicePage.unapproveButton).toBeEnabled();
  await expect(invoicePage.approveButton).toHaveCount(0);
});

Then('the approval reads back from the API', async ({ request, world }) => {
  // POLL, never a single-shot GET: the click-triggered write can still be committing when the
  // button swap renders, so a bare GET races the commit.
  let latest: InvoiceReadBack | undefined;
  await expect
    .poll(
      async () => {
        const res = await request.get(`/api/invoices/${world.reviewInvoiceId}`);
        if (!res.ok()) return `HTTP ${res.status()}`;
        latest = (await res.json()) as InvoiceReadBack;
        return latest.invStatus;
      },
      {
        timeout: 30_000,
        message:
          `GET /api/invoices/${world.reviewInvoiceId} never reported ${invoiceStatus.approved}. The ` +
          `screen showed the approval, so the status change did not reach Oracle or was rolled back.`,
      },
    )
    .toBe(invoiceStatus.approved);

  const body = latest as InvoiceReadBack;
  expect(body.invID, 'the API returned a different invoice than the one approved').toBe(
    world.reviewInvoiceId,
  );
  // Approving must not disturb the invoice's contents — only its status.
  expect(body.lineItems.length, 'line items must survive the approval untouched').toBeGreaterThan(0);
  expect(body.boomNumbers.length, 'source documents must survive the approval untouched').toBeGreaterThan(0);
});

Then('the submission is not knocked out of the inbox by this approval', async ({ request, world }) => {
  // The submission only moves once NO processing invoices remain in it, and both pinned
  // submissions keep many. Asserting this is what proves the journey's blast radius is limited to
  // the one invoice — which is the premise the borrow-and-restore teardown depends on.
  const res = await request.get('/api/inbox?page=0&size=100');
  expect(res.ok(), `GET /api/inbox returned HTTP ${res.status()}`).toBeTruthy();
  const body = (await res.json()) as {
    content: { submissionId: string | null; submissionStatus: string; invApproved: number }[];
  };
  const row = body.content.find((r) => r.submissionId === world.reviewSubmissionId);
  expect(row, `submission ${world.reviewSubmissionId} vanished from the Inbox after the approval`).toBeTruthy();
  expect(
    row?.submissionStatus,
    'the submission status changed, which it should not while processing invoices remain in it',
  ).toBe(reviewSubmissions[world.reviewSubmissionId as string].submissionStatus);
});
