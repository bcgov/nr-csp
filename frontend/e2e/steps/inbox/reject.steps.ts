import { invoiceStatus, rejectionReason } from '../../fixtures/inbox/review-test-data';
import { Given, When, Then, expect } from '../fixtures';

/**
 * INBOX domain — reject steps (UC-INBOX-004).
 *
 * The borrow step ("an UNAPPROVED invoice is waiting to be reviewed") is NOT redefined here:
 * `steps/inbox/review.steps.ts` already owns it, and it is what arms the restore-on-teardown. This
 * file adds only the vocabulary that rejection needs.
 *
 * No DOM selectors here — they belong to `pages/invoice/InvoicePage.ts`.
 */

type InvoiceReadBack = {
  invID: number;
  invStatus: string;
  reviewComments: string | null;
  lineItems: { lineItemID: number }[];
};

Given('I open that invoice directly', async ({ invoicePage, world }) => {
  // UC-INBOX-004 is about the rejection itself, not about how the reviewer got to the screen —
  // the UC-INBOX-001..003 journey already covers navigating in from the Inbox. Going straight to
  // the invoice keeps this scenario about the one thing it names.
  await invoicePage.openInvoiceById(world.reviewInvoiceId as number);
});

Then('the invoice is shown as UNAPPROVED with Reject available', async ({ invoicePage }) => {
  await expect(invoicePage.statusTag).toHaveText(invoiceStatus.unapproved);
  await expect(invoicePage.rejectButton).toBeVisible();
  await expect(invoicePage.rejectButton).toBeEnabled();
});

Then('the reviewer comment box already holds the existing note', async ({ invoicePage }) => {
  // RE-GROUNDED. UC-INBOX-004-S01's precondition is that the Reviewer Comment field "has not yet
  // been filled". Every seeded UNAPPROVED invoice arrives with a note already in it (the box
  // hydrates from `reviewComments`), so the real starting state is the opposite of the spec's.
  // Asserting it explicitly is what makes the next step meaningful: replacing a non-empty value is
  // a different act from filling an empty one, and it is what proves the new reason actually
  // overwrote the old note rather than being appended to it.
  await expect(invoicePage.reviewerCommentInput).not.toHaveValue('');
});

When('I replace the reviewer comment with a rejection reason', async ({ invoicePage, world }) => {
  world.rejectionReason = rejectionReason();
  await invoicePage.setReviewerComment(world.rejectionReason);
  await expect(invoicePage.reviewerCommentInput).toHaveValue(world.rejectionReason);
});

When('I reject the invoice', async ({ invoicePage }) => {
  await invoicePage.reject();
});

Then('the invoice becomes REJECTED on screen', async ({ invoicePage }) => {
  await expect(invoicePage.statusTag).toHaveText(invoiceStatus.rejected);
  // The toast names the invoice. Matching on the verb rather than pinning the number: invoice
  // numbers are not unique within a submission, so the number would be the weaker half.
  await expect(invoicePage.toast('rejected.')).toBeVisible();
});

Then('Reject is no longer offered', async ({ invoicePage }) => {
  // Unlike Approve — which is swapped out for an Unapprove button — Reject stays on screen and
  // goes disabled, because REJ is not in STATUS_CHANGEABLE. This is the one button-state
  // assertion in the legacy Gherkin that re-grounds unchanged.
  await expect(invoicePage.rejectButton).toBeVisible();
  await expect(invoicePage.rejectButton).toBeDisabled();
});

Then('Approve is no longer offered either', async ({ invoicePage }) => {
  // Same gate: a rejected invoice is out of the reviewer's decision set entirely, so the other
  // decision must be closed off too. Asserting only Reject would leave a regression that reopened
  // Approve on a rejected invoice undetected.
  await expect(invoicePage.approveButton).toBeDisabled();
});

Then('the rejection and its reason read back from the API', async ({ request, world }) => {
  // POLL, never a single-shot GET: the click-triggered write can still be committing when the
  // status pill repaints, so a bare GET races the commit.
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
          `GET /api/invoices/${world.reviewInvoiceId} never reported ${invoiceStatus.rejected}. The ` +
          `screen showed the rejection, so the status change did not reach Oracle or was rolled back.`,
      },
    )
    .toBe(invoiceStatus.rejected);

  const body = latest as InvoiceReadBack;
  expect(body.invID, 'the API returned a different invoice than the one rejected').toBe(
    world.reviewInvoiceId,
  );
  // The REASON is the point of this use case — a rejection with no recorded justification is the
  // failure mode worth catching, and the status alone would not catch it.
  expect(body.reviewComments, 'the rejection reason must be persisted with the status change').toBe(
    world.rejectionReason,
  );
  // Rejecting must not disturb the invoice's contents.
  expect(body.lineItems.length, 'line items must survive the rejection untouched').toBeGreaterThan(0);
});
