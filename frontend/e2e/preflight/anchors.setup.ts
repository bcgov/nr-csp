import { test, expect, type APIRequestContext } from '@playwright/test';

import {
  busiestSubmission,
  seededDateWindow,
  snapshotFingerprint,
  unfilteredInboxRowCount,
} from '../fixtures/inbox/inbox-test-data';
import { submAnchors } from '../fixtures/subm/invoice-test-data';
import { borrowableInvoicePool, invoiceStatus } from '../fixtures/inbox/review-test-data';
import { purchase2013Filter, viewTargetInvoice } from '../fixtures/search/search-test-data';
import { birchGrades, birchSpecies, fpcpAnchors, ownRow } from '../fixtures/fpcp/flat-price-test-data';

/**
 * Suite PREFLIGHT — runs once (a Playwright `setup` project the `chromium` project depends on)
 * before any scenario. It asserts the anchors the fixtures pin still resolve in the loaded DB, so a
 * stale DB — or a backend that booted before its DB and is serving warm-but-empty reference data —
 * fails HERE with one clear message instead of as a dozen confusing mid-suite reds.
 *
 * Every check goes through the app's own API, so no Oracle client is needed at runtime. Nothing is
 * written, so preflight leaves the DB exactly as found.
 *
 * CSP has no seed patches yet (real-test-data-patches/ is empty): the published image already
 * carries everything these scenarios need. Add an applied-seed presence check here the moment a
 * patch is introduced — that is the class of failure a preflight exists to catch.
 */

const REGROUND =
  'Re-ground: confirm the seeded DB container is running on the port in .env (ORACLE_DSN) and that ' +
  'the backend points at it, then re-verify the pinned values in fixtures/inbox/inbox-test-data.ts. ' +
  'If the image was rebuilt from a fresh extract, the pinned ids will have moved.';

/**
 * A 401/403 from a protected endpoint has exactly one cause worth naming, and it is NOT stale data:
 * the backend is enforcing real JWT auth while the suite is presenting a mock session. Reported
 * separately from REGROUND because the fix is completely different.
 */
const MOCK_AUTH_HINT =
  'The backend rejected an unauthenticated request, which means it is NOT running with mock auth. ' +
  'Start it with AUTH_MOCK_ENABLED=true (and AUTH_MOCK_ROLES=ADMIN) — see the CSP quick start in ' +
  'e2e/README.md. Note this is the BACKEND flag; the frontend side needs no setup, because the ' +
  'suite injects mock auth into its own browser.';

async function jsonOrThrow(req: APIRequestContext, url: string, hint: string): Promise<unknown> {
  const res = await req.get(url);
  if (res.status() === 401 || res.status() === 403) {
    throw new Error(`[preflight] ${hint} — GET ${url} returned HTTP ${res.status()}. ${MOCK_AUTH_HINT}`);
  }
  expect(
    res.ok(),
    `[preflight] ${hint} — GET ${url} returned HTTP ${res.status()}. ${REGROUND}`,
  ).toBeTruthy();
  return res.json();
}

test('preflight: backend is up and reachable through the dev-server proxy', async ({ request }) => {
  const body = (await jsonOrThrow(request, '/api/health', 'backend health')) as { status?: string };
  expect(
    body.status,
    `[preflight] /api/health did not report UP (got ${JSON.stringify(body)}). The backend is ` +
      'reachable but unhealthy — check it started AFTER the database. ' +
      REGROUND,
  ).toBe('UP');
});

test('preflight: the backend is running with mock auth enabled', async ({ request }) => {
  // /api/health is PUBLIC on both auth modes, so it cannot detect this — a backend enforcing real
  // JWT looks perfectly healthy right up until the first protected call. Probe a PROTECTED endpoint
  // unauthenticated instead: 200 means mock auth is on, 401 means it is not.
  //
  // Without this check the failure surfaces as a confusing red much later (or as an empty grid in a
  // scenario), which is what a reviewer hit setting the suite up for the first time.
  const res = await request.get('/api/inbox?page=0&size=1');
  expect(
    res.status(),
    `[preflight] ${MOCK_AUTH_HINT}\n\n` +
      `(GET /api/inbox returned HTTP ${res.status()}; expected 200. A 401/403 here is the signature ` +
      `of AUTH_MOCK_ENABLED=false.)`,
  ).not.toBe(401);
  expect(
    res.status(),
    `[preflight] ${MOCK_AUTH_HINT}\n\n(GET /api/inbox returned HTTP ${res.status()}; expected 200.)`,
  ).not.toBe(403);
});

test('preflight: the seeded Inbox slice still resolves', async ({ request }) => {
  const body = (await jsonOrThrow(
    request,
    // Param names must match InboxApi (submissionDateFrom / submissionDateTo). Spring IGNORES
    // unknown params, so a typo silently queries the UNFILTERED page — the preflight would then
    // report success against data it never actually filtered.
    `/api/inbox?page=0&size=100&submissionDateFrom=${seededDateWindow.start}` +
      `&submissionDateTo=${seededDateWindow.end}`,
    'seeded Inbox slice',
  )) as { content: { submissionId: string | null }[]; totalElements?: number };

  // Assert on totalElements, NOT content.length: the latter is capped by the page `size`, so a
  // database with far MORE rows (e.g. a backend pointed at delivery, which has ~20,500) reports
  // exactly the page size and the message reads as a plausible small number instead of "wrong
  // database". Observed for real: a colleague saw "returned 100, expected 50" when the true count
  // was 20,528.
  expect(
    body.totalElements,
    `[preflight] the Inbox reported ${body.totalElements} total rows for the seeded window ` +
      `${seededDateWindow.start}..${seededDateWindow.end}, expected ${unfilteredInboxRowCount}. ` +
      REGROUND,
  ).toBe(unfilteredInboxRowCount);

  const ids = body.content.map((r) => r.submissionId);
  expect(
    ids,
    `[preflight] pinned submission ${busiestSubmission.submissionId} is absent from the Inbox. ` +
      REGROUND,
  ).toContain(busiestSubmission.submissionId);
});

test('preflight: reference data is loaded (lookups are not empty)', async ({ request }) => {
  // A backend whose reference-data cache warmed before the data load serves EMPTY code lists while
  // looking perfectly healthy — every dropdown renders blank and creates fail. Cheapest guard there is.
  const maturity = (await jsonOrThrow(
    request,
    '/api/lookup/maturity',
    'maturity lookup',
  )) as unknown[];
  expect(
    maturity.length,
    '[preflight] the maturity lookup came back empty. The backend most likely warmed its ' +
      'reference-data cache before the DB was seeded — restart the backend (or evict its cache). ' +
      REGROUND,
  ).toBeGreaterThan(0);
});

/**
 * SUBM anchors — the clients and reference codes the manual-invoice journey writes with.
 *
 * The reference-table COUNTS are already fingerprinted below, but a count cannot tell you whether
 * the specific code a fixture pins is still in the table, and it says nothing about the two client
 * records. Without this, a re-extracted DB surfaces as a mid-journey red — an empty autocomplete,
 * or an opaque "The combination of the submitter Client Number ... cannot be found in CSP" from the
 * save — which reads like a broken test rather than moved data.
 */
test('preflight: the SUBM invoice anchors still resolve', async ({ request }) => {
  const missing: string[] = [];

  // The journey RESOLVES its clients at run time rather than pinning any (this repo is public, and
  // client names and numbers are production records). So the anchor is not a particular client —
  // it is that the lookup still yields two DISTINCT ones for the journey to use as submitter and
  // other party, which `isSameSellerAndBuyer` requires them to be.
  const clientRows = (await jsonOrThrow(
    request,
    `/api/clients?name=${encodeURIComponent(submAnchors.clientSearchTerm)}`,
    `client lookup "${submAnchors.clientSearchTerm}"`,
  )) as { clientNumber: string; clientLocnCode: string }[];
  const distinctClients = new Set(clientRows.map((r) => r.clientNumber));
  if (distinctClients.size < 2) {
    missing.push(
      `the client lookup for "${submAnchors.clientSearchTerm}" yields ${distinctClients.size} ` +
        `distinct client(s); the invoice journey needs two, since its submitter and other party ` +
        `must differ`,
    );
  }

  const hasCode = async (lookup: string, code: string) => {
    const rows = (await jsonOrThrow(request, `/api/lookup/${lookup}`, `lookup ${lookup}`)) as {
      code: string;
    }[];
    if (!rows.some((r) => r.code === code)) missing.push(`lookup/${lookup} no longer contains "${code}"`);
  };

  await hasCode('type', submAnchors.invoiceTypeCode);
  await hasCode('maturity', submAnchors.maturityCode);
  await hasCode('fob', submAnchors.fobCode);
  await hasCode('sort-code', submAnchors.sortCode);

  const combos = (await jsonOrThrow(
    request,
    '/api/lookup/species-grade-combinations',
    'species/grade combinations',
  )) as { species: string; grade: string }[];
  const { species, grade } = submAnchors.speciesGradePair;
  if (!combos.some((c) => c.species === species && c.grade === grade)) {
    missing.push(`species/grade pair ${species}/${grade} is no longer an active combination`);
  }

  expect(
    missing,
    `[preflight] the manual-invoice (SUBM) anchors no longer resolve:\n  - ${missing.join('\n  - ')}\n\n` +
      `Re-ground fixtures/subm/invoice-test-data.ts — each value there carries the command that ` +
      `found it. ${REGROUND}`,
  ).toEqual([]);
});

/**
 * BORROWED-INVOICE POOL — the drift guard for the one scenario that mutates seeded data.
 *
 * The review journey borrows a seeded UNAPPROVED invoice, approves it, and restores it to UNA on
 * teardown (it cannot create its own: the backend refuses an approval by the invoice's own entry
 * user, and local mock auth is a single fixed username). If a restore ever fails — a crashed
 * worker, a killed run — the row is left APPROVED, and NOTHING else would notice: the snapshot
 * fingerprint below counts Inbox rows and lookup sizes, neither of which moves when one invoice
 * changes status.
 *
 * So the pool is checked explicitly, here, before any scenario runs. A left-behind APPROVED row
 * fails the suite in about a second with the exact id and the command to put it back, instead of
 * surfacing later as a missing Approve button that reads like a UI bug.
 */
test('preflight: the borrowable UNAPPROVED invoice pool is intact', async ({ request }) => {
  const wrong: string[] = [];

  for (const invoice of borrowableInvoicePool) {
    const res = await request.get(`/api/invoices/${invoice.invoiceId}`);
    if (!res.ok()) {
      wrong.push(`invoice ${invoice.invoiceId}: GET returned HTTP ${res.status()}`);
      continue;
    }
    const body = (await res.json()) as { invStatus: string; reviewComments: string | null };
    if (body.invStatus !== invoiceStatus.unapproved) {
      wrong.push(
        `invoice ${invoice.invoiceId} (submission ${invoice.submissionId}) is ` +
          `"${body.invStatus}", expected "${invoiceStatus.unapproved}"`,
      );
    }
    // A pool row MUST carry a reviewer note. Rejecting overwrites it, and the API cannot write
    // NULL back (the service skips the update when reviewComments is null) — so a null-noted row
    // could never be restored after a reject scenario borrowed it.
    if (body.reviewComments === null) {
      wrong.push(
        `invoice ${invoice.invoiceId} has a NULL reviewer comment, which a reject scenario could ` +
          `overwrite and never restore`,
      );
    }
  }

  expect(
    wrong,
    `[preflight] the borrowable UNAPPROVED invoice pool has drifted:\n  - ${wrong.join('\n  - ')}\n\n` +
      `Most likely a previous run of the review journey failed to restore a borrowed invoice (its ` +
      `teardown fails loud, so check that run's output). Put each one back with:\n` +
      `  curl -X PATCH "$BASE_URL/api/invoices/<id>/status" -H 'Content-Type: application/json' \\\n` +
      `       -d '{"status":"UNA","reviewComments":null}'\n` +
      `or reset the whole DB with ./scripts/reset-db.sh. If the seed image was rebuilt from a fresh ` +
      `extract, re-ground fixtures/inbox/review-test-data.ts instead — the finding query is in the ` +
      `comment there.`,
  ).toEqual([]);
});

/**
 * SEARCH anchors — the fixed slice of data the search/view scenarios assert an EXACT count on.
 *
 * That count is only safe because the filter (Purchase, in 2013) excludes every invoice the
 * suite's writing scenarios touch. If a re-extract moves these rows, or if something new starts
 * creating historical Purchase invoices, the scenario would fail mid-run on a row count — which
 * reads like a search bug. This names the real cause in about a second instead.
 */
test('preflight: the SEARCH anchors still resolve', async ({ request }) => {
  const wrong: string[] = [];

  const res = (await jsonOrThrow(
    request,
    `/api/search?invType=${purchase2013Filter.typeCode}` +
      `&startDate=${purchase2013Filter.startDate}&endDate=${purchase2013Filter.endDate}&page=0&size=100`,
    'the pinned search filter',
  )) as { content: { coastalLogSaleId: number }[]; totalElements: number };

  if (res.totalElements !== purchase2013Filter.expectedRowCount) {
    wrong.push(
      `the pinned filter returned ${res.totalElements} rows, expected ` +
        `${purchase2013Filter.expectedRowCount}`,
    );
  }
  if (!res.content.some((r) => r.coastalLogSaleId === viewTargetInvoice.invoiceId)) {
    wrong.push(`the view target ${viewTargetInvoice.invoiceId} is not in the pinned filter's results`);
  }

  const invoice = (await jsonOrThrow(
    request,
    `/api/invoices/${viewTargetInvoice.invoiceId}`,
    'the search view target',
  )) as { invStatus: string; lineItems: unknown[] };
  if (invoice.invStatus !== viewTargetInvoice.status) {
    wrong.push(`the view target is "${invoice.invStatus}", expected "${viewTargetInvoice.status}"`);
  }
  if (invoice.lineItems.length !== viewTargetInvoice.lineItemCount) {
    wrong.push(
      `the view target has ${invoice.lineItems.length} line items, expected ` +
        `${viewTargetInvoice.lineItemCount}`,
    );
  }

  expect(
    wrong,
    `[preflight] the SEARCH anchors have drifted:\n  - ${wrong.join('\n  - ')}\n\n` +
      `Re-ground fixtures/search/search-test-data.ts — the finding queries are in the comments ` +
      `there. If the row count moved but the target is still present, check whether something in ` +
      `the suite has started creating Purchase invoices with historical dates, which would break ` +
      `the immunity argument the exact count relies on. ` + REGROUND,
  ).toEqual([]);
});

/**
 * FPCP anchors — the reference codes the flat-price scenarios CREATE a production row from.
 *
 * `update` and `create` both re-validate species/grade, sort code and maturity against the row's
 * effective date, so a re-extract that retires any of them makes the create fail with a 422 that
 * reads like a broken test. This names the real cause up front.
 *
 * It also asserts no row is left behind by a previous run: a leak would collide with the next
 * create's duplicate key, and these are live pricing rows that invoice submission reads.
 */
test('preflight: the FPCP flat-price anchors still resolve', async ({ request }) => {
  const wrong: string[] = [];

  const species = (await jsonOrThrow(request, '/api/lookup/species', 'species lookup')) as {
    code: string;
    description: string;
  }[];
  const birch = species.find((s) => s.code === birchSpecies.code);
  if (!birch) wrong.push(`species ${birchSpecies.code} is gone from the lookup`);
  else if (birch.description !== birchSpecies.label) {
    wrong.push(`species ${birchSpecies.code} now reads "${birch.description}", expected "${birchSpecies.label}"`);
  }

  const grades = (await jsonOrThrow(
    request,
    `/api/lookup/grade-by-species/${birchSpecies.code}`,
    'grades for the pinned species',
  )) as { code: string }[];
  const gradeCodes = grades.map((g) => g.code).sort();
  if (gradeCodes.join(',') !== [...birchGrades].sort().join(',')) {
    wrong.push(
      `grades for ${birchSpecies.code} are now [${gradeCodes.join(', ')}], expected ` +
        `[${[...birchGrades].sort().join(', ')}]`,
    );
  }

  const rows = (await jsonOrThrow(
    request,
    `/api/flat-price-conversions?modellingCode=${fpcpAnchors.modellingCode}&species=${birchSpecies.code}`,
    'the pinned production flat-price slice',
  )) as { id: number; entryUserid: string }[];
  if (rows.length !== birchSpecies.seededRowCount) {
    wrong.push(
      `the production table holds ${rows.length} ${birchSpecies.label} rows, expected ` +
        `${birchSpecies.seededRowCount}`,
    );
  }
  const leaked = rows.filter((r) => r.entryUserid === 'local-dev-user').map((r) => r.id);
  if (leaked.length > 0) {
    wrong.push(
      `rows ${leaked.join(', ')} were created by the suite and never deleted — they are live ` +
        `pricing rows and will collide with the next run's create`,
    );
  }

  expect(
    wrong,
    `[preflight] the FPCP flat-price anchors have drifted:\n  - ${wrong.join('\n  - ')}\n\n` +
      `If a row was leaked, delete it (DELETE /api/flat-price-conversions/{id}) — the scenario's ` +
      `cleanup fails loud, so check the run that left it. Otherwise re-ground ` +
      `fixtures/fpcp/flat-price-test-data.ts; note the created row also needs maturity ` +
      `${ownRow.maturityCode} and sort code ${ownRow.sortCode} active on its effective date. ` + REGROUND,
  ).toEqual([]);
});

/**
 * SNAPSHOT FINGERPRINT — the drift guard.
 *
 * The suite cannot tell which database the backend is pointed at; it only speaks HTTP to :3000. So
 * this asserts the DB still LOOKS like the published snapshot before any scenario runs. It catches
 * three things that would otherwise produce a confusing mid-suite red, or worse a meaningless green:
 *
 *   1. the backend is pointed at delivery (fortmp1) rather than the local seeded image;
 *   2. a previous run's cleanup failed and left residue;
 *   3. the image was rebuilt from a fresh extract, so every pinned value needs re-grounding.
 *
 * It is deliberately cheap — one request per reference table — and runs once per suite, not per test.
 */
test('preflight: the database still matches the published snapshot', async ({ request }) => {
  const drift: string[] = [];

  const inbox = (await jsonOrThrow(request, '/api/inbox?page=0&size=1', 'inbox row count')) as {
    totalElements?: number;
  };
  if (inbox.totalElements !== snapshotFingerprint.inboxRows) {
    drift.push(
      `inbox rows: expected ${snapshotFingerprint.inboxRows}, got ${inbox.totalElements}`,
    );
  }

  for (const [name, expected] of Object.entries(snapshotFingerprint.lookups)) {
    const rows = (await jsonOrThrow(request, `/api/lookup/${name}`, `lookup ${name}`)) as unknown[];
    if (rows.length !== expected) {
      drift.push(`lookup/${name}: expected ${expected}, got ${rows.length}`);
    }
  }

  expect(
    drift,
    `[preflight] the database does not match the published snapshot:\n  - ${drift.join('\n  - ')}\n\n` +
      `Most likely one of:\n` +
      `  * the backend is pointed at a DIFFERENT database (delivery/fortmp1 rather than the local\n` +
      `    seeded image) — check its SPRING_DATASOURCE_URL;\n` +
      `  * a previous run left residue — reset with ./scripts/reset-db.sh, then restart the backend;\n` +
      `  * the seed image was rebuilt from a fresh extract — re-measure snapshotFingerprint in\n` +
      `    fixtures/inbox/inbox-test-data.ts (a re-ground event).`,
  ).toEqual([]);
});
