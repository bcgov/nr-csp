import { test as base } from 'playwright-bdd';

import { SearchPage } from '../../pages/search/SearchPage';

/**
 * SEARCH domain fixtures — invoice search and view (UC-SRCH-001 / 002).
 *
 * These scenarios are READ-ONLY: nothing is created, mutated or deleted, so there is no cleanup
 * registry and no mutation spy here. Their parallel safety comes from the data they pin rather
 * than from a lock — the filter they search on excludes every invoice the suite's writing
 * scenarios touch (see fixtures/search/search-test-data.ts).
 *
 * The Invoice screen's page object is NOT here — it is shared across domains and lives in ./global.
 */
export type SearchFixtures = {
  searchPage: SearchPage;
};

export const searchTest = base.extend<SearchFixtures>({
  searchPage: async ({ page }, use) => {
    await use(new SearchPage(page));
  },
});
