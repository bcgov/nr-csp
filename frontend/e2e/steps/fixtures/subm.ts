import { test as base } from 'playwright-bdd';

import {
  CLIENT_SEARCH_TERM,
  pickTwoDistinctClients,
  type ResolvedClient,
} from '../../fixtures/subm/invoice-test-data';

/**
 * SUBM domain fixtures — manual invoice submission (UC-SUBM-001..004).
 *
 * One file per domain (see ./index.ts) so two authors adding two domains never edit the same file.
 * Everything here is lazy and per-scenario, so scenarios stay order-independent under
 * `fullyParallel`. Scratch state shared between steps lives in the `World` union in ./global.
 *
 * NOTE: the `invoicePage` fixture is NOT here. The Invoice screen is driven by two domains (SUBM
 * creates and submits; INBOX reviews and approves), so it lives in ./global alongside the other
 * cross-domain page objects.
 */

type InvoiceMutationCounts = { create: number; update: number; submit: number; addLineItem: number };

export type SubmFixtures = {
  /**
   * Two distinct forest clients, looked up from the seeded database at run time.
   *
   * No client name or number is committed to this repository — it is public, and those are
   * production records. The scenario asserts against what it resolved here rather than against
   * literals. See the note above `CLIENT_SEARCH_TERM` in fixtures/subm/invoice-test-data.ts.
   */
  submissionClients: [ResolvedClient, ResolvedClient];
  /**
   * Per-scenario cleanup registry — every invoice id a scenario created is DELETEd on teardown.
   * Push the id the MOMENT it is known (right after the create), not at the end of the scenario:
   * a mid-scenario failure must still clean up.
   */
  createdInvoiceIds: string[];
  invoiceMutationCounts: InvoiceMutationCounts;
  /** Getters over the spy, so a step can prove WHICH write a click issued (or that none did). */
  invoiceCreateCalls: () => number;
  invoiceUpdateCalls: () => number;
};

export const submTest = base.extend<SubmFixtures>({
  submissionClients: async ({ request }, use) => {
    const res = await request.get(`/api/clients?name=${encodeURIComponent(CLIENT_SEARCH_TERM)}`);
    if (!res.ok()) {
      throw new Error(
        `[test data] client lookup for "${CLIENT_SEARCH_TERM}" returned HTTP ${res.status()}. The ` +
          `invoice journey resolves its clients at run time rather than pinning any, so it cannot ` +
          `proceed without this.`,
      );
    }
    await use(pickTwoDistinctClients((await res.json()) as ResolvedClient[]));
  },

  // Cleanup FAILS LOUD. A leaked invoice pollutes the shared seed for every later run: it would
  // add a row to the Inbox (which INNER JOINs coastal_log_sale), breaking the preflight's
  // snapshot fingerprint with a confusing "expected 50, got 51" in some UNRELATED scenario.
  //
  // DELETE /api/invoices/{id} is valid in every status this journey can reach — the service only
  // refuses an APPROVED invoice — so a scenario that failed after submitting still cleans up.
  // 404 is idempotent-OK (a delete-invoice scenario got there first). All ids are attempted before
  // throwing, so one bad id cannot hide the others.
  //
  // KNOWN RESIDUE: `InvoiceService.create` also inserts a `csp_submission` row, and `delete` does
  // NOT remove it, so each created invoice leaves one orphan submission behind. It is invisible to
  // the Inbox and to the preflight fingerprint (both require at least one invoice on the
  // submission), and there is no API to remove it. Logged as BUG-002 in the UC's defects.md;
  // `./scripts/reset-db.sh` is the remedy if the orphans ever need clearing.
  createdInvoiceIds: async ({ request }, use) => {
    const ids: string[] = [];
    await use(ids);
    const residue: string[] = [];
    for (const id of ids) {
      let status: number;
      try {
        const res = await request.delete(`/api/invoices/${id}`);
        status = res.status();
        if (res.ok() || status === 404) continue;
      } catch (err) {
        residue.push(`${id} (delete threw: ${(err as Error).message})`);
        continue;
      }
      residue.push(`${id} -> HTTP ${status}`);
    }
    if (residue.length > 0) {
      throw new Error(
        `[cleanup] left DB residue — these invoices were not removed and will pollute the seeded ` +
          `DB (they will show up as extra Inbox rows and break the preflight fingerprint): ` +
          `${residue.join(', ')}. Investigate DELETE /api/invoices/{id} or reset with ` +
          `./scripts/reset-db.sh before re-running.`,
      );
    }
  },

  // ONE transparent spy over every invoice mutation, classified by method + path. A single handler
  // (rather than several overlapping page.route globs, where the last registered one wins) keeps
  // the classification unambiguous. `route.continue()` means positive scenarios are completely
  // unaffected — the counters only observe.
  //
  // `auto: true` so it is armed before any step navigates, which is what lets a step distinguish a
  // POST (create) from a PUT (update) after a Save click — the two are indistinguishable from the
  // UI alone, since both land on the same screen showing the same toast shape.
  invoiceMutationCounts: [
    async ({ page }, use) => {
      const counts: InvoiceMutationCounts = { create: 0, update: 0, submit: 0, addLineItem: 0 };
      await page.route('**/api/invoices**', async (route) => {
        const req = route.request();
        const method = req.method();
        const path = new URL(req.url()).pathname;
        if (method === 'POST' && path.endsWith('/invoices')) counts.create += 1;
        else if (method === 'PUT' && /\/invoices\/\d+$/.test(path)) counts.update += 1;
        else if (method === 'POST' && /\/invoices\/\d+\/submit$/.test(path)) counts.submit += 1;
        else if (method === 'POST' && /\/invoices\/\d+\/line-items$/.test(path)) counts.addLineItem += 1;
        await route.continue();
      });
      await use(counts);
    },
    { auto: true },
  ],

  invoiceCreateCalls: async ({ invoiceMutationCounts }, use) => {
    await use(() => invoiceMutationCounts.create);
  },
  invoiceUpdateCalls: async ({ invoiceMutationCounts }, use) => {
    await use(() => invoiceMutationCounts.update);
  },
});
