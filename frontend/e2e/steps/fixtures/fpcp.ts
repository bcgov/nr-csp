import { test as base } from 'playwright-bdd';

import {
  PRODUCTION_MODELLING_CODE,
  effectiveDateForWorker,
  ownRow,
} from '../../fixtures/fpcp/flat-price-test-data';
import { FlatPriceConversionPage } from '../../pages/fpcp/FlatPriceConversionPage';

/**
 * FPCP domain fixtures — Production Flat Price Conversion table maintenance (UC-FPCP-001).
 *
 * This is the suite's first table-maintenance write. Unlike the INBOX review scenarios — which are
 * forced to borrow and restore seeded rows — this domain CREATES its own production row and
 * deletes it, so no seeded data is ever mutated. The reasoning is in
 * `fixtures/fpcp/flat-price-test-data.ts`.
 */

/** The row a scenario created, as the API returned it. */
export type SeededFlatPriceRow = {
  id: number;
  effectiveDate: string;
  flatPriceConversion: number;
};

export type FpcpFixtures = {
  flatPricePage: FlatPriceConversionPage;
  /** Per-scenario cleanup registry — every created row is DELETEd on teardown (fails loud). */
  createdFlatPriceRowIds: number[];
  /**
   * Create this worker's own production row via the API and register it for cleanup.
   *
   * Arranged over HTTP rather than through the "Add new row" dialog on purpose: UC-FPCP-001-S01 is
   * about SEARCHING and EDITING an existing row, and adding one through the UI is its own slice
   * (S02). Driving the add form here would make the scenario test two things and obscure which one
   * failed.
   */
  seedOwnFlatPriceRow: () => Promise<SeededFlatPriceRow>;
};

export const fpcpTest = base.extend<FpcpFixtures>({
  flatPricePage: async ({ page }, use) => {
    await use(new FlatPriceConversionPage(page));
  },

  // Cleanup FAILS LOUD. A leaked row is worse here than elsewhere: these rows are LIVE PRICING
  // that `PriceConversionService` reads when an invoice is submitted, and a stray one also
  // collides with the next run's create (the backend rejects a duplicate on
  // modellingCode + sortCode + species/grade + maturity + effectiveDate), which would surface as a
  // confusing 409 in an unrelated run. 404 is idempotent-OK.
  createdFlatPriceRowIds: async ({ request }, use) => {
    const ids: number[] = [];
    await use(ids);
    const residue: string[] = [];
    for (const id of ids) {
      let status: number;
      try {
        const res = await request.delete(`/api/flat-price-conversions/${id}`);
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
        `[cleanup] left production flat-price rows behind: ${residue.join(', ')}. These are live ` +
          `pricing rows read by invoice submission, and a leaked one will also make the next run's ` +
          `create fail as a duplicate. Remove them by hand (DELETE /api/flat-price-conversions/{id}) ` +
          `or reset with ./scripts/reset-db.sh.`,
      );
    }
  },

  seedOwnFlatPriceRow: async ({ request, createdFlatPriceRowIds }, use, testInfo) => {
    await use(async () => {
      // Each concurrent worker creates the row on its OWN effective date — the rest of the
      // duplicate key is identical across scenarios, so the date is what keeps them apart.
      const effectiveDate = effectiveDateForWorker(testInfo.parallelIndex);
      const res = await request.post('/api/flat-price-conversions', {
        data: {
          modellingCode: PRODUCTION_MODELLING_CODE,
          details: {
            maturity: ownRow.maturityCode,
            species: ownRow.speciesCode,
            grade: ownRow.grade,
            sortCode: ownRow.sortCode,
            flatPriceConversion: ownRow.initialPrice,
            effectiveDate,
            expiryDate: ownRow.expiryDate,
          },
        },
      });
      if (!res.ok()) {
        const body = await res.text();
        throw new Error(
          `[seed] could not create the production flat-price row for ${effectiveDate}: ` +
            `HTTP ${res.status()} — ${body}\n\n` +
            (res.status() === 409
              ? `A 409 here means a row already exists on this key — almost certainly one a ` +
                `previous run failed to delete. Remove it and re-run; the cleanup fails loud, so ` +
                `check that run's output.`
              : `Check that maturity ${ownRow.maturityCode}, sort code ${ownRow.sortCode} and the ` +
                `${ownRow.speciesCode}/${ownRow.grade} species-grade pair are all active on ` +
                `${effectiveDate} — the backend re-validates each against the effective date.`),
        );
      }
      const row = (await res.json()) as SeededFlatPriceRow;
      // Register for teardown the moment the id is known, so a mid-scenario failure still cleans up.
      createdFlatPriceRowIds.push(row.id);
      return row;
    });
  },
});
