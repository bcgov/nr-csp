import { test as base } from 'playwright-bdd';

import { InboxPage } from '../../pages/inbox/InboxPage';

/**
 * INBOX domain fixtures. One file per domain (see ./index.ts) so two authors adding two domains
 * never edit the same file. Everything here is lazy and per-scenario — Playwright only builds the
 * fixtures a scenario actually touches, and nothing is shared mutable module state, so scenarios
 * stay order-independent under fullyParallel.
 */
export type InboxFixtures = {
  inboxPage: InboxPage;
};

export const inboxTest = base.extend<InboxFixtures>({
  inboxPage: async ({ page }, use) => {
    await use(new InboxPage(page));
  },
});
