import type { Page } from '@playwright/test';
import { test as base } from 'playwright-bdd';

// TODO(author): import this domain's page objects and test-data builders here, e.g.
//   import { buildExampleCreateBody } from '../../fixtures/example/example-test-data';
//   import { ExampleCreatePage } from '../../pages/example/ExampleCreatePage';

/**
 * ============================================================================
 * EXAMPLE DOMAIN — the canonical per-domain fixtures file. COPY THIS PER DOMAIN.
 * ============================================================================
 *
 * Copy to `./<domain>.ts`, rename the exports (`exampleTest` -> `<domain>Test`,
 * `ExampleFixtures` -> `<Domain>Fixtures`), replace `<your-resource>` and the endpoints with your
 * app's, then add it to `mergeTests` in `./index.ts`. Nothing here is referenced by another domain, so
 * this file can be edited without touching anyone else's coverage — cross-domain things (`world`, shared
 * page objects) live in `./global`.
 *
 * ---- THE FOUR PIECES A DOMAIN USUALLY NEEDS -------------------------------
 *   1. The page-object fixture(s): `xPage: async ({ page }, use) => use(new XPage(page))`.
 *   2. A cleanup registry (`createdXIds`) that undoes whatever a scenario created/mutated on teardown
 *      and FAILS LOUD on residue.
 *   3. A `seedX` API helper that creates a record and registers its id for cleanup.
 *   4. A transparent `page.route` mutation spy + a `xCreateCalls()` getter, so a negative test can
 *      PROVE the write never left the browser (count === 0).
 * Keep every fixture LAZY (only instantiated when a scenario references it) and per-scenario (no shared
 * mutable module state) so scenarios stay order-independent and parallel-safe.
 *
 * Scratch state this domain passes between steps goes in the `World` union in `./global`, under its own
 * comment group — not here.
 */

type ExampleMutationCounts = { create: number; update: number };

export type ExampleFixtures = {
  /**
   * Page object for the <your-resource> create screen (selectors live there).
   * TODO(author): swap `Page` for your own `ExampleCreatePage` type once the page object exists — the
   * bare `Page` is only a placeholder so the scaffold type-checks under `strict` before it is wired.
   */
  exampleCreatePage: Page;
  /** Per-scenario cleanup registry — each id is DELETEd via the app API on teardown (fails loud). */
  createdExampleIds: string[];
  /** Create a fresh, DB-grounded <your-resource> via the API and register it for cleanup; returns its id. */
  seedExample: () => Promise<string>;
  exampleMutationCounts: ExampleMutationCounts;
  /** Getter over the create spy — lets a negative step PROVE nothing was submitted (count === 0). */
  exampleCreateCalls: () => number;
  exampleUpdateCalls: () => number;
};

export const exampleTest = base.extend<ExampleFixtures>({
  exampleCreatePage: async ({ page }, use) => {
    // TODO(author): await use(new ExampleCreatePage(page));
    await use(page); // placeholder so the scaffold type-checks before you wire the page object
  },

  // Cleanup FAILS LOUD: at scale, a silently-leaked row poisons the shared seed and causes phantom
  // failures elsewhere. 404 is idempotent-OK (already gone — e.g. a delete-record scenario). Any other
  // non-OK status throws so residue surfaces immediately. All ids are attempted first.
  createdExampleIds: async ({ request }, use) => {
    const ids: string[] = [];
    await use(ids);
    const residue: string[] = [];
    for (const id of ids) {
      let status: number;
      try {
        // TODO(author): point at your app's DELETE endpoint for this resource.
        const res = await request.delete(`/api/<your-resource>/${id}`);
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
        `[cleanup] left DB residue — these <your-resource> records were not removed and will pollute ` +
          `the seeded DB: ${residue.join(', ')}. Investigate the DELETE endpoint / DB state before re-running.`,
      );
    }
  },

  // Seed a fresh record via the API and register it for teardown the moment the id is known. Update/read
  // scenarios seed their OWN record this way rather than mutating a shared seed row (keeps tests
  // independent + parallel-safe). Mock auth => no token needed.
  seedExample: async ({ request, createdExampleIds }, use) => {
    await use(async () => {
      // TODO(author): call your app's create endpoint with a builder from fixtures/<domain>/.
      const res = await request.post('/api/<your-resource>', { data: {} /* buildExampleCreateBody() */ });
      if (!res.ok()) {
        throw new Error(`[seed] create failed: HTTP ${res.status()} — ${await res.text()}`);
      }
      const id = String(((await res.json()) as { id: number }).id);
      createdExampleIds.push(id); // register for teardown the moment the id is known
      return id;
    });
  },

  // Single transparent spy over ALL <your-resource> mutations, classified by method+path. `auto: true`
  // so it is armed before any step navigates/submits. ONE handler (not several overlapping page.route
  // globs) avoids Playwright's last-registered-handler-wins clobbering. route.continue() => positive
  // scenarios are unaffected; only the counters observe.
  exampleMutationCounts: [
    async ({ page }, use) => {
      const counts: ExampleMutationCounts = { create: 0, update: 0 };
      await page.route('**/api/<your-resource>**', async (route) => {
        const req = route.request();
        const method = req.method();
        const path = new URL(req.url()).pathname;
        if (method === 'POST' && path.endsWith('/<your-resource>')) counts.create += 1;
        else if (method === 'PUT' && /\/<your-resource>\/\d+$/.test(path)) counts.update += 1;
        await route.continue();
      });
      await use(counts);
    },
    { auto: true },
  ],
  exampleCreateCalls: async ({ exampleMutationCounts }, use) => {
    await use(() => exampleMutationCounts.create);
  },
  exampleUpdateCalls: async ({ exampleMutationCounts }, use) => {
    await use(() => exampleMutationCounts.update);
  },
});
