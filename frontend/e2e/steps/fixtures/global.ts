import { test as base } from 'playwright-bdd';

import { installToastRecorder } from '../../pages/common/toastRecorder';
import { InvoicePage } from '../../pages/invoice/InvoicePage';

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

  /** The invoice the review journey borrowed, opened and approved (UC-INBOX-001..003). */
  reviewInvoiceId?: number;
  /** The submission the review journey navigated through, for the UI-vs-API read-back. */
  reviewSubmissionId?: string;
  /** The rejection reason this scenario typed, for the API read-back (UC-INBOX-004). */
  rejectionReason?: string;
  /** The unapprove reason this scenario typed, for the API read-back (UC-INBOX-005). */
  unapproveReason?: string;

  /** The pinned invoice's client-supplied number, resolved at run time (never committed). */
  searchInvoiceNumber?: string;

  // --- fpcp domain (flat price conversion maintenance, UC-FPCP-001) ---
  /** The production flat-price row this scenario created, for the UI lookup and API read-back. */
  flatPriceRowId?: number;
  /** Its effective date (ISO) — the only rendered column that identifies it in the grid. */
  flatPriceEffectiveDate?: string;

  // --- subm domain (manual invoice submission, UC-SUBM-001..004) ---
  /** Invoice number minted for this scenario — unique per run so parallel scenarios never collide. */
  invoiceNumber?: string;
  /** Boom number minted for this scenario (the invoice's required source-document reference). */
  boomNumber?: string;
  /** DB id of the invoice the scenario created, from the post-save URL. Also the cleanup key. */
  invoiceId?: string;
  /** The clients this scenario resolved at run time, for the API read-back assertion. */
  submitterClient?: { clientNumber: string; clientName: string; clientLocnCode: string };
  otherClient?: { clientNumber: string; clientName: string; clientLocnCode: string };
  /** Invoice date the scenario entered, for the API read-back assertion. */
  invoiceDate?: string;

  // --- example domain (delete/rename; one group per real domain) ---
  /** Id of an <your-resource> created via the UI or API-seeded, for read-back + cleanup. */
  exampleId?: string;
  /** Values a scenario changed via the UI, for the API read-back assertion. */
  expected?: Record<string, unknown>;
};

export type GlobalFixtures = {
  world: World;
  /**
   * The Invoice details screen (`/invoice`, `/invoice/:id`).
   *
   * Cross-domain by nature, which is why it lives here rather than in a domain file: SUBM drives it
   * to create/itemise/submit an invoice, and INBOX lands on it to review and approve one. Declaring
   * it once here is also what stops two domain files each declaring an `invoicePage` fixture, which
   * `mergeTests` would treat as a conflict.
   */
  invoicePage: InvoicePage;
};

export const globalTest = base.extend<GlobalFixtures>({
  world: async ({}, use) => {
    await use({});
  },

  invoicePage: async ({ page }, use) => {
    await use(new InvoicePage(page));
  },

  /**
   * Every scenario records the toasts the app shows, so toast assertions are not racing the app's
   * own 5-second auto-dismiss. Installed by overriding `page` because the recorder is an init
   * script: it has to be registered before the first navigation, and doing it here means no step or
   * page object can forget to. See `pages/common/toastRecorder.ts` for why this is necessary.
   */
  page: async ({ page }, use) => {
    await installToastRecorder(page);
    await use(page);
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
