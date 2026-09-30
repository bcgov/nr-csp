import { test as base } from 'playwright-bdd';

import {
  allocateBorrowableInvoice,
  poolExhausted,
  borrowableInvoicePool,
  invoiceStatus,
  type BorrowableInvoice,
} from '../../fixtures/inbox/review-test-data';
import { InboxPage } from '../../pages/inbox/InboxPage';
import { ViewSubmissionPage } from '../../pages/inbox/ViewSubmissionPage';

/**
 * INBOX domain fixtures. One file per domain (see ./index.ts) so two authors adding two domains
 * never edit the same file. Everything here is lazy and per-scenario — Playwright only builds the
 * fixtures a scenario actually touches, and nothing is shared mutable module state, so scenarios
 * stay order-independent under fullyParallel.
 *
 * The Invoice screen's page object is NOT here — it is shared with the SUBM domain and lives in
 * ./global.
 */
export type InboxFixtures = {
  inboxPage: InboxPage;
  viewSubmissionPage: ViewSubmissionPage;
  /**
   * A seeded UNAPPROVED invoice this scenario may act on — approve it (UC-INBOX-003) or reject it
   * (UC-INBOX-004) — borrowed for the scenario and RESTORED on teardown.
   *
   * Why borrowed rather than created: the backend refuses an approval by the invoice's own entry
   * user, and local mock auth is a single fixed username — so the suite can never approve an
   * invoice it created. Reject has no such rule, but it shares the pool so both reviewer decisions
   * exercise the same, proven restore path. The full reasoning and the evidence that the round trip
   * leaves nothing behind are in `fixtures/inbox/review-test-data.ts`.
   */
  borrowedInvoice: BorrowableInvoice;
};

export const inboxTest = base.extend<InboxFixtures>({
  inboxPage: async ({ page }, use) => {
    await use(new InboxPage(page));
  },

  viewSubmissionPage: async ({ page }, use) => {
    await use(new ViewSubmissionPage(page));
  },

  borrowedInvoice: async ({ request }, use, testInfo) => {
    // Index by parallelIndex, which Playwright keeps unique across the workers running at any one
    // moment — so a --repeat-each flake check, or the approve and reject scenarios running at the
    // same time, each get their own invoice instead of racing on one. Fail loudly rather than
    // silently double-booking if the pool is ever smaller than the worker count.
    if (poolExhausted(testInfo.config.workers)) {
      throw new Error(
        `[borrowedInvoice] ${testInfo.config.workers} workers but only ${borrowableInvoicePool.length} ` +
          `borrowable UNA invoices. Two workers would act on the same row and race. Add rows to ` +
          `borrowableInvoicePool (fixtures/inbox/review-test-data.ts) or run with fewer workers.`,
      );
    }
    const invoice = allocateBorrowableInvoice(testInfo.parallelIndex);

    // ---- Capture the "as found" state, so teardown can put it back exactly ----
    // Status alone is not enough: REJECTING writes the reviewer comment
    // (`InvoiceService.changeStatus` -> `updateReviewerNotes` whenever reviewComments is non-null),
    // so a reject scenario overwrites the seeded note. Restoring only the status would silently
    // leave the test's rejection reason on a seeded row forever.
    const before = await request.get(`/api/invoices/${invoice.invoiceId}`);
    if (!before.ok()) {
      throw new Error(
        `[borrowedInvoice] GET /api/invoices/${invoice.invoiceId} returned HTTP ${before.status()}. ` +
          `Re-ground fixtures/inbox/review-test-data.ts against the seeded DB.`,
      );
    }
    const asFound = (await before.json()) as { invStatus: string; reviewComments: string | null };

    // Precondition check, not an assertion about the app: if this row is not UNA the scenario
    // cannot mean what it says, and the most likely cause is a previous run whose restore failed.
    // Naming that cause here beats failing later on a missing Approve/Reject button, which reads
    // like a UI bug.
    if (asFound.invStatus !== invoiceStatus.unapproved) {
      throw new Error(
        `[borrowedInvoice] invoice ${invoice.invoiceId} is ${asFound.invStatus}, expected ` +
          `${invoiceStatus.unapproved}. A previous run most likely failed to restore it. Reset with ` +
          `./scripts/reset-db.sh, or PATCH it back: ` +
          `curl -X PATCH .../api/invoices/${invoice.invoiceId}/status -d '{"status":"UNA"}'`,
      );
    }
    // The reviewer note cannot be restored to NULL through the API — the service SKIPS the write
    // when reviewComments is null, so a null-noted row could never have a test's comment removed
    // again. Every pinned row currently carries a note; this guards a future pool addition.
    if (asFound.reviewComments === null) {
      throw new Error(
        `[borrowedInvoice] invoice ${invoice.invoiceId} has a NULL reviewer comment. A reject ` +
          `scenario would overwrite it and the API cannot write NULL back (the service skips the ` +
          `update when reviewComments is null), so it could never be restored. Remove this row from ` +
          `borrowableInvoicePool, or give it a note in the seed.`,
      );
    }

    await use(invoice);

    // ---- RESTORE, and fail loud if it does not take ----
    // Runs whether the scenario passed or failed, so a mid-scenario failure still hands the row
    // back.
    //
    // ⚠ A NO-OP NOTE WRITE IS REJECTED, which is why this is not one simple PATCH. Reject,
    // unapprove and cancel now require the reviewer comment to have CHANGED from the stored value
    // (`InvoiceValidator.isReviewerCommentUpdate`, restored from legacy). So:
    //
    //   * after a reject or unapprove the note holds the test's reason, and writing the original
    //     back IS a change — one PATCH does it;
    //   * after an approve the note was never touched, so writing the original back is a no-op and
    //     would be refused with "Please enter the changes in the Reviewer Comment field below!".
    //     Restoring the STATUS there still needs a comment change, so it takes two steps: a
    //     transient marker to carry the status change, then the original note over the top.
    const currentRes = await request.get(`/api/invoices/${invoice.invoiceId}`);
    const current = currentRes.ok()
      ? ((await currentRes.json()) as { invStatus: string; reviewComments: string | null })
      : null;
    const noteAlreadyCorrect = current?.reviewComments === asFound.reviewComments;
    const statusAlreadyCorrect = current?.invStatus === asFound.invStatus;

    const patch = (reviewComments: string | null) =>
      request.patch(`/api/invoices/${invoice.invoiceId}/status`, {
        data: { status: asFound.invStatus, reviewComments },
      });

    const failRestore = async (res: { status: () => number; text: () => Promise<string> }) => {
      throw new Error(
        `[cleanup] FAILED to restore borrowed invoice ${invoice.invoiceId} to ` +
          `${asFound.invStatus}: HTTP ${res.status()} — ${await res.text()}. The seeded DB is ` +
          `now drifted: later runs will not find this invoice reviewable. Restore it by hand or run ` +
          `./scripts/reset-db.sh.`,
      );
    };

    if (!noteAlreadyCorrect) {
      const res = await patch(asFound.reviewComments);
      if (!res.ok()) await failRestore(res);
    } else if (!statusAlreadyCorrect) {
      // The note is right but the status is not, and moving the status needs a comment change.
      const marker = await patch(`e2e teardown ${Date.now()}`);
      if (!marker.ok()) await failRestore(marker);
      const res = await patch(asFound.reviewComments);
      if (!res.ok()) await failRestore(res);
    }
    // Nothing to do when both already match — the scenario never acted on the row.
    // Read back rather than trusting the 200 — a restore that silently no-ops is exactly the drift
    // this teardown exists to prevent. Both fields are checked, because a reject scenario can only
    // be fully undone if the note went back too.
    const after = await request.get(`/api/invoices/${invoice.invoiceId}`);
    if (!after.ok()) {
      throw new Error(
        `[cleanup] could not read invoice ${invoice.invoiceId} back after restoring it ` +
          `(HTTP ${after.status()}), so the restore is unverified. Check the seeded DB.`,
      );
    }
    const restored = (await after.json()) as { invStatus: string; reviewComments: string | null };
    const drift: string[] = [];
    if (restored.invStatus !== asFound.invStatus) {
      drift.push(`status is "${restored.invStatus}", expected "${asFound.invStatus}"`);
    }
    if (restored.reviewComments !== asFound.reviewComments) {
      drift.push(
        `reviewer comment is ${JSON.stringify(restored.reviewComments)}, expected ` +
          `${JSON.stringify(asFound.reviewComments)}`,
      );
    }
    if (drift.length > 0) {
      throw new Error(
        `[cleanup] invoice ${invoice.invoiceId} did not restore cleanly although the PATCH reported ` +
          `success: ${drift.join('; ')}. The seeded DB is drifted — reset with ./scripts/reset-db.sh.`,
      );
    }
  },
});
