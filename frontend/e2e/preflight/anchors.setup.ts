import { test, expect, type APIRequestContext } from '@playwright/test';

import {
  busiestSubmission,
  seededDateWindow,
  snapshotFingerprint,
  unfilteredInboxRowCount,
} from '../fixtures/inbox/inbox-test-data';

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
