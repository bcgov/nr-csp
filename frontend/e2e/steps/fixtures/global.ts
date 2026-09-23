import { test as base } from 'playwright-bdd';

// TODO(author): import the page objects that are NOT owned by any single domain, e.g. the app shell or
// a landing page every domain navigates through:
//   import { AppShellPage } from '../../pages/common/AppShellPage';

/**
 * Global (cross-domain) fixtures for the BDD suite.
 *
 * Only things MORE THAN ONE domain touches live here: the per-scenario `world` scratch, and page objects
 * not owned by a subject area (an app shell, a landing/context page every domain establishes its working
 * context through). Anything a single domain owns belongs in that domain's file instead — that is the
 * whole point of the split, so two people adding coverage to two domains never edit the same file.
 *
 * This is also where a suite-wide identity belongs, if your app has one: overriding the `page` fixture
 * to seed a mock user (see the commented example below) declares it once for every scenario rather than
 * relying on a Given that some page objects would bypass by navigating directly.
 */

/**
 * Per-scenario scratch object for state passed between steps (a key set by a precondition, a seeded id,
 * a value a later Then reads back).
 *
 * `World` is the UNION of every domain's scratch fields — kept in ONE place because a single `world`
 * object is shared across all steps of a scenario regardless of which domain contributed them. This is
 * the one deliberate cross-domain coupling left after the split, so keep the fields grouped and
 * commented by domain to keep additions conflict-cheap.
 */
export type World = {
  // --- inbox domain ---
  /** Rows the Inbox API returned for the scenario's request, for UI-vs-API read-back. */
  inboxApiRows?: { submissionId: string | null; invTotal: number }[];

  // --- example domain (delete/rename; one group per real domain) ---
  /** Id of an <your-resource> created via the UI or API-seeded, for read-back + cleanup. */
  exampleId?: string;
  /** Values a scenario changed via the UI, for the API read-back assertion. */
  expected?: Record<string, unknown>;
};

export type GlobalFixtures = {
  world: World;
  // TODO(author): add cross-domain page objects here, e.g.
  //   appShell: AppShellPage;
};

export const globalTest = base.extend<GlobalFixtures>({
  world: async ({}, use) => {
    await use({});
  },

  // TODO(author): if your app has a suite-wide identity, declare it once by overriding `page` — the
  // identity is a property of the browser, not of any one Given:
  //   page: async ({ page }, use) => {
  //     await seedMockUser(page, 'submitter');
  //     await use(page);
  //   },

  // TODO(author): cross-domain page objects, e.g.
  //   appShell: async ({ page }, use) => {
  //     await use(new AppShellPage(page));
  //   },
});
