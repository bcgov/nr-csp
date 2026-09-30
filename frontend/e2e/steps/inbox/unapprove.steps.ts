import { invoiceStatus, unapproveReason } from '../../fixtures/inbox/review-test-data';
import { Given, When, Then, expect } from '../fixtures';

/**
 * INBOX domain — unapprove steps (UC-INBOX-005).
 *
 * The borrow step ("an UNAPPROVED invoice is waiting to be reviewed") lives in
 * `steps/inbox/review.steps.ts` and is reused — it is what arms the restore-on-teardown. Opening
 * the invoice directly reuses `steps/inbox/reject.steps.ts`. This file adds only the vocabulary
 * unapproving needs.
 *
 * No DOM selectors here — they belong to `pages/invoice/InvoicePage.ts`.
 */

type InvoiceReadBack = {
  invID: number;
  invStatus: string;
  reviewComments: string | null;
  lineItems: { lineItemID: number }[];
};

Given('that invoice has already been approved', async ({ request, world }) => {
  // ARRANGE, via the API rather than the UI. UC-INBOX-005 starts from an APPROVED invoice, and the
  // seeded pool holds UNAPPROVED ones — approving through the UI first would make this scenario a
  // second copy of UC-INBOX-003 and hide which action it is really testing.
  //
  // `reviewComments: null` is deliberate: the service skips the note write when it is null, so this
  // arrange leaves the invoice's seeded reviewer note untouched. That matters, because the point of
  // a later assertion is that UNAPPROVING is what replaced it.
  const res = await request.patch(`/api/invoices/${world.reviewInvoiceId}/status`, {
    data: { status: invoiceStatus.approved, reviewComments: null },
  });
  expect(
    res.ok(),
    `[arrange] could not approve invoice ${world.reviewInvoiceId} to set up the unapprove ` +
      `scenario: HTTP ${res.status()} — ${await res.text()}. Note the backend refuses an approval ` +
      `by the invoice's own entry user, so this only works on a borrowed, legacy-entered invoice.`,
  ).toBeTruthy();

  // The teardown restores the row to the status and note it had when borrowed (UNA + its seeded
  // note), so this arrange needs no undo of its own — it is covered either way.
});

Then('the invoice is shown as APPROVED with Unapprove available', async ({ invoicePage }) => {
  await expect(invoicePage.statusTag).toHaveText(invoiceStatus.approved);
  await expect(invoicePage.unapproveButton).toBeVisible();
  await expect(invoicePage.unapproveButton).toBeEnabled();
  // Approve and Unapprove share one slot, so on an APPROVED invoice there is no Approve button at
  // all. Asserting its absence is what proves the swap really happened rather than both rendering.
  await expect(invoicePage.approveButton).toHaveCount(0);
});

When('I replace the reviewer comment with an unapprove reason', async ({ invoicePage, world }) => {
  world.unapproveReason = unapproveReason();
  await invoicePage.setReviewerComment(world.unapproveReason);
  await expect(invoicePage.reviewerCommentInput).toHaveValue(world.unapproveReason);
});

When('I unapprove the invoice', async ({ invoicePage }) => {
  await invoicePage.unapprove();
});

Then('the invoice returns to UNAPPROVED on screen', async ({ invoicePage }) => {
  await expect(invoicePage.statusTag).toHaveText(invoiceStatus.unapproved);
  await expect(invoicePage.toast('unapproved.')).toBeVisible();
});

Then('Unapprove is replaced by an enabled Approve', async ({ invoicePage }) => {
  // The legacy slice words this as "Unapprove becomes disabled, Approve becomes enabled". Same
  // intent, re-grounded: the two share a slot, so unapproving swaps the control rather than
  // disabling it — the exact mirror of what approving does in UC-INBOX-003.
  await expect(invoicePage.approveButton).toBeVisible();
  await expect(invoicePage.approveButton).toBeEnabled();
  await expect(invoicePage.unapproveButton).toHaveCount(0);
});

Then('Submit becomes available again', async ({ invoicePage }) => {
  // UNA is back in SUBMITTABLE_STATUSES, so the invoice can be pushed through the workflow again —
  // which is the point of unapproving. Enabled (not merely visible) is the meaningful assertion:
  // the button renders in every status and is gated by `canSubmit` plus the permission, the
  // field-error state and the presence of line items.
  await expect(invoicePage.submitButton).toBeEnabled();
});

Then('the unapproval and its reason read back from the API', async ({ request, world }) => {
  // POLL, never a single-shot GET: the click-triggered write can still be committing when the
  // button swap renders.
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
          `GET /api/invoices/${world.reviewInvoiceId} never reported ${invoiceStatus.unapproved} ` +
          `after the unapproval. The screen showed it, so the status change did not reach Oracle ` +
          `or was rolled back.`,
      },
    )
    .toBe(invoiceStatus.unapproved);

  const body = latest as InvoiceReadBack;
  expect(body.invID, 'the API returned a different invoice than the one unapproved').toBe(
    world.reviewInvoiceId,
  );
  // The reason is the audit trail for reversing an approval — the status alone would not catch a
  // build that dropped it.
  expect(body.reviewComments, 'the unapprove reason must be persisted with the status change').toBe(
    world.unapproveReason,
  );
  expect(body.lineItems.length, 'line items must survive the unapproval untouched').toBeGreaterThan(0);
});
