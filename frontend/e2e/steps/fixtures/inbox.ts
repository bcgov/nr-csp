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
import {
  checkBorrowedInvoiceRestorable,
  restoreBorrowedInvoice,
  type BorrowedInvoiceState,
} from '../inbox/borrowedInvoiceDbRestore';

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
    const asFound = (await before.json()) as BorrowedInvoiceState;

    // Precondition check, not an assertion about the app: if this row is not UNA the scenario
    // cannot mean what it says, and the most likely cause is a previous run whose restore failed.
    // Naming that cause here beats failing later on a missing Approve/Reject button, which reads
    // like a UI bug.
    if (asFound.invStatus !== invoiceStatus.unapproved) {
      throw new Error(
        `[borrowedInvoice] invoice ${invoice.invoiceId} is ${asFound.invStatus}, expected ` +
          `${invoiceStatus.unapproved}. A previous run most likely failed to restore it. Reset with ` +
          `./scripts/reset-db.sh.`,
      );
    }
    checkBorrowedInvoiceRestorable(invoice.invoiceId, asFound);

    await use(invoice);

    // ---- RESTORE, and fail loud if it does not take ----
    // Runs whether the scenario passed or failed, so a mid-scenario failure still hands the row
    // back. It writes the DB directly because the status API cannot undo a reject.
    const current = await request.get(`/api/invoices/${invoice.invoiceId}`);
    if (!current.ok()) {
      throw new Error(
        `[cleanup] could not read invoice ${invoice.invoiceId} before restoring it (HTTP ${current.status()}), ` +
          `so it was not restored. Check the seeded DB.`,
      );
    }
    const afterScenario = (await current.json()) as BorrowedInvoiceState;
    if (
      afterScenario.invStatus !== asFound.invStatus ||
      afterScenario.reviewComments !== asFound.reviewComments
    ) {
      restoreBorrowedInvoice(invoice.invoiceId, afterScenario, asFound);
    }

    // Read back through the API rather than trusting the update. Both fields are checked, because a
    // reject or unapprove scenario is only undone if the note went back too.
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
        `[cleanup] invoice ${invoice.invoiceId} did not restore cleanly although the update reported ` +
          `success: ${drift.join('; ')}. The seeded DB is drifted — reset with ./scripts/reset-db.sh.`,
      );
    }
  },
});
