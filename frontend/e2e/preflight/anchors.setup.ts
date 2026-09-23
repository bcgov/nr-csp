import { test, expect, type APIRequestContext } from '@playwright/test';

import {
  busiestSubmission,
  seededDateWindow,
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

async function jsonOrThrow(req: APIRequestContext, url: string, hint: string): Promise<unknown> {
  const res = await req.get(url);
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

test('preflight: the seeded Inbox slice still resolves', async ({ request }) => {
  const body = (await jsonOrThrow(
    request,
    `/api/inbox?page=0&size=100&startDate=${seededDateWindow.start}&endDate=${seededDateWindow.end}`,
    'seeded Inbox slice',
  )) as { content: { submissionId: string | null }[] };

  expect(
    body.content.length,
    `[preflight] the Inbox returned ${body.content.length} rows for the seeded window ` +
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
