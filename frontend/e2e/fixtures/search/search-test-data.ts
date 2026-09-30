/**
 * SEARCH domain (UC-SRCH-001 / 002) — anchors pinned from the SEEDED database, discovered through
 * the app's own `/api/search` and `/api/invoices/{id}` endpoints.
 *
 * Seed image: ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23
 *
 * ---------------------------------------------------------------------------
 * WHY THIS PARTICULAR SLICE OF DATA — it is immune to every other scenario
 * ---------------------------------------------------------------------------
 * These scenarios are READ-ONLY, but they assert exact result counts, so they must not be
 * disturbed by the suite's two writing families. The filter below excludes both, by construction:
 *
 *   * the INBOX review scenarios borrow and mutate seeded UNAPPROVED invoices — every one of those
 *     twelve is type SAL or ADJ, so filtering on **Purchase** excludes the entire pool;
 *   * the SUBM journey creates and deletes its own Purchase invoices — but always dated
 *     `daysAgo(1)`, i.e. today-ish, so restricting to **2013** excludes them too.
 *
 * Nothing else in the suite writes invoices. Hence "Purchase, in 2013" is a fixed set of exactly
 * three rows and can be asserted by count, not merely by "at least one".
 *
 * Provenance:
 *   curl -s 'http://localhost:8080/api/search?invType=PUR&startDate=2013-01-01&endDate=2013-12-31'
 *   -> totalElements 3, all Approved, all dated 2013-06-28. Their client names, client numbers and
 *      client-supplied invoice numbers are deliberately NOT recorded here — see the note below.
 */

/**
 * The multi-criteria filter UC-SRCH-001-S01 exercises: a date range plus an invoice type.
 *
 * ⚠ The Type dropdown lists DESCRIPTIONS, not codes — the option reads "Purchase", and the
 * results grid's Type column renders "Purchase" too, though the API takes `invType=PUR`.
 */
export const purchase2013Filter = {
  startDate: '2013-01-01',
  endDate: '2013-12-31',
  /** The option label in the Type dropdown (`itemToString` = description). */
  typeLabel: 'Purchase',
  /** What `/api/search` takes, for the API cross-check. */
  typeCode: 'PUR',
  /** Exactly this many rows — see the immunity argument above. */
  expectedRowCount: 3,
  /** Every row in the set carries these, so they can be asserted per row. */
  everyRowStatus: 'Approved',
  everyRowType: 'Purchase',
  /** As the grid renders it (`formatDisplayDate`, en-CA long month) — all three share a date. */
  everyRowDateRendered: 'June 28, 2013',
} as const;

/**
 * ---------------------------------------------------------------------------
 * NO CLIENT DATA IS COMMITTED HERE
 * ---------------------------------------------------------------------------
 * This repository is PUBLIC, and client names, client numbers and client-supplied invoice numbers
 * are production records. Only INTERNAL surrogate keys are pinned below; everything
 * client-identifying is read from the API at run time and asserted against what was resolved.
 *
 * That makes the assertions slightly stronger as well as safer: the grid's Client number, Client
 * name and Invoice number cells are compared with what `/api/invoices/{id}` and `/api/search`
 * return for the same record, so they cannot pass by agreeing with a stale literal.
 */

/**
 * The invoice UC-SRCH-002-S01 opens from the results.
 *
 * Chosen from the three in the pinned filter because it has the most line items (7, in 3 groups).
 * Its invoice number is also unique across the seed, which is what lets the search-by-number
 * scenario resolve it at run time and still expect exactly one row.
 *
 * Provenance: GET /api/invoices/200388
 *   status APP · type PUR · date 2013-06-28 · maturity O · manual (no submission number)
 *   totals 92 pieces / 191.57 m3 / $11,462.06 · 7 line items
 *
 * ⚠ NO SOURCE DOCUMENTS. This invoice has no boom numbers, timber marks or weigh slips — unlike
 * the UNAPPROVED pool rows, which all carry a boom number. That is why the view assertions here
 * check the group summary and totals rather than the source-document panels.
 */
export const viewTargetInvoice = {
  /** coastal_log_sale_id — an internal surrogate key, and the `/invoice/:id` route param. */
  invoiceId: 200388,
  status: 'APP',
  statusLabel: 'Approved',
  invoiceDate: '2013-06-28',
  type: 'PUR',
  typeLabel: 'Purchase',
  maturityLabel: 'Old Growth',
  lineItemCount: 7,
  /**
   * Rows the group summary renders. NOT the line-item count — the table groups by
   * (species, secondSort, EXACT price), and grade is deliberately not part of the key
   * (`groupLineItems` in pages/Invoice/index.tsx).
   *
   * Derivation from the seven lines (all the same secondary sort): one group at the first price
   * for one species, then two groups for the other species split only by price. That last pair is
   * what makes this record a good check that price really is part of the grouping key.
   */
  groupCount: 3,
  totals: { pieces: 92, volume: 191.57, amount: 11462.06 },
  /** As the invoice page renders them (utils/format.ts). */
  rendered: {
    pieces: '92',
    volume: '191.570',
    amount: '$11,462.06',
  },
} as const;

/**
 * ⚠ THIS SEEDED INVOICE FAILS TODAY'S VALIDATOR, and the page says so on load.
 *
 * `GET /api/invoices/{id}` re-runs the validator with `ActionType.OTHER` and returns whatever it
 * finds in `errors[]`, so opening this record paints two error banners. Both are real: the record
 * is historical data that predates rules the new app enforces.
 *
 * Verified: GET /api/invoices/200388 -> errors[]
 *   invoice.type.invalid.submitter                  "The Invoice submitted by Seller cannot be type PUR."
 *   invoice.oneofthe.boom.timber.wiegh.requiered.error
 *                                                   "One of Boom Number, Timber Mark or Weigh Slip must have a value."
 * and warnings[]
 *   invoice.month.completed.warning                 "... is for a month that is already \"Completed\"."
 *                                                   (the full text interpolates the invoice number)
 *
 * The first is the very rule that makes the legacy UC-SUBM-001-S01 happy path contradictory (a
 * Purchase invoice submitted by the Seller) — here is a real, APPROVED record that does exactly
 * that. See this UC's defects.md.
 *
 * The scenario asserts these messages rather than ignoring them: they are the honest state of the
 * screen, and asserting them stops a future change that suppresses validation on read from passing
 * unnoticed.
 *
 * ⚠ ONE OF THESE USED TO BE INVISIBLE, and this record is what guards the fix.
 * `routeServerErrors` splits errors by `MESSAGE_KEY_TO_FIELD` (pages/Invoice/messageKeyMap.ts): a
 * mapped key becomes an inline error on its field, anything else falls through to the page banner.
 *   * `invoice.oneofthe.boom.timber.wiegh.requiered.error` -> unmapped -> page banner
 *   * `invoice.type.invalid.submitter` -> mapped to `invType`
 *
 * Every header field is DISABLED on a locked invoice (`canEdit` is false outside DFT/PRO/UNA), and
 * Carbon renders nothing for a disabled field's `invalid`/`invalidText` — so the second error was
 * computed, routed to its field, and shown to nobody. Measured at the time: the dropdown button was
 * `disabled` with no invalid class and no `aria-invalid`, and the page held ZERO
 * `.cds--form-requirement` elements, while the banners rendered fine.
 *
 * `applyServerErrors` now routes field-mapped errors to the banner when the header is not
 * editable, so both appear. Written up as BUG-001 in defects.md.
 *
 * Warnings do not go through that split at all — every warning renders as a banner, even one whose
 * key appears in the map (the month-complete key maps to `invDate`, yet it banners correctly).
 */
export const viewTargetValidationMessages = {
  /**
   * Both of the record's errors, in the page-level banner.
   *
   * The second one used to be invisible: it is mapped to the Invoice type field, and Carbon renders
   * nothing for a DISABLED field's `invalidText`, which every header field is on a locked invoice.
   * `applyServerErrors` now sends field-mapped errors to the banner when the header is not
   * editable, so both are shown. Asserting both is what keeps that fix from regressing.
   */
  pageErrors: [
    'One of Boom Number, Timber Mark or Weigh Slip must have a value.',
    'The Invoice submitted by Seller cannot be type PUR.',
  ],
  /**
   * Matched as a FRAGMENT, not the whole message. The full text interpolates the client-supplied
   * invoice number, which is not committed here — this is the stable part of the sentence.
   */
  warningFragment: 'is for a month that is already "Completed"',
} as const;

/**
 * Which action buttons an APPROVED invoice offers, and which it withholds.
 *
 * UC-SRCH-002-S01 expects the view-from-search screen to carry NO action buttons at all. This app
 * has one Invoice screen for viewing and acting, so the buttons are present but gated — see the
 * spec gap in defects.md. Pinned here as the re-grounded equivalent of that assertion.
 *
 * Derived from `pages/Invoice/index.tsx` and confirmed on screen:
 *   Save       disabled — the record has field errors, and APP is not in EDITABLE_STATUSES
 *   Submit     disabled — APP is not in SUBMITTABLE_STATUSES
 *   Reject     disabled — APP is not in STATUS_CHANGEABLE ({PRO, UNA})
 *   Cancel     disabled — as Reject
 *   Duplicate  disabled — APP is not in DUPLICABLE_STATUSES
 *   Delete     disabled — APP is not in DELETABLE_STATUSES
 *   Unapprove  ENABLED  — offered in APP, and the ONLY decision button with no permission check
 *                         (see UC-INBOX-005 defects.md, BUG-001)
 */
export const approvedInvoiceDisabledActions = ['Save', 'Submit', 'Reject', 'Cancel', 'Duplicate', 'Delete'] as const;

/** Column headers the search results grid renders, exactly as the DOM shows them. */
export const searchColumnHeaders = [
  'Invoice status',
  'Invoice number',
  'Invoice date',
  'Type',
  'Client number',
  'Client name',
  'Maturity',
  'Submission type',
] as const;

/**
 * Anchors the preflight re-checks, so a re-extracted DB fails fast here rather than mid-scenario.
 */
export const searchAnchors = {
  filter: purchase2013Filter,
  target: viewTargetInvoice,
} as const;
