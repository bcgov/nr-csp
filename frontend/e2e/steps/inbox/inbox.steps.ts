import {
  busiestSubmission,
  inboxColumnHeaders,
  seededDateWindow,
  unfilteredInboxRowCount,
} from '../../fixtures/inbox/inbox-test-data';
import { Given, When, Then, expect } from '../fixtures';

/**
 * INBOX domain steps. No DOM selectors here — those belong to pages/inbox/InboxPage.ts. Steps hold
 * the domain vocabulary and the assertions; the page object holds the locators.
 */

Given('I am signed in as a CSP {word}', async ({ inboxPage }, role: string) => {
  await inboxPage.open(role.toUpperCase() as 'ADMIN' | 'APPROVER' | 'SUBMITTER' | 'VIEWER');
});

When('I open the Submission Inbox', async ({ inboxPage }) => {
  await inboxPage.waitForLoaded();
});

Then('the Inbox is awaiting search criteria', async ({ inboxPage }) => {
  // The Inbox issues no query until a criterion is supplied; proving this FIRST is what makes the
  // later row assertions meaningful (otherwise the empty-state row itself satisfies "has a row").
  await expect(inboxPage.emptyState).toHaveCount(1);
  await expect(inboxPage.resultRows).toHaveCount(0);
});

When('I search the full seeded date range', async ({ inboxPage }) => {
  await inboxPage.searchByDateRange(seededDateWindow.start, seededDateWindow.end);
});

Then('the submissions table is displayed', async ({ inboxPage }) => {
  await expect(inboxPage.table).toBeVisible();
});

Then('the submissions table shows the {int} seeded submissions', async ({ inboxPage }, expected: number) => {
  // Pinned from the seed (fixtures/inbox/inbox-test-data.ts). The Inbox INNER JOINs
  // coastal_log_sale, so a submission with no invoices never appears.
  expect(
    expected,
    'Scenario expectation drifted from the pinned fixture — re-ground one or the other.',
  ).toBe(unfilteredInboxRowCount);
  // Poll: the grid repaints when the query resolves, so a single-shot count can race the response.
  await expect.poll(async () => inboxPage.resultRowCount(), { timeout: 30_000 }).toBe(expected);
});

Then('the results grid shows the Inbox columns', async ({ inboxPage }) => {
  for (const header of inboxColumnHeaders) {
    await expect(inboxPage.columnHeader(header)).toBeVisible();
  }
});

Then('the pinned submission from the seeded database is listed', async ({ inboxPage }) => {
  const row = inboxPage.rowBySubmissionId(busiestSubmission.submissionId);
  await expect(
    row,
    `Pinned submission ${busiestSubmission.submissionId} is not in the Inbox results. Either the DB ` +
      `image was rebuilt from a fresh extract (re-ground fixtures/inbox/inbox-test-data.ts), or the ` +
      `backend is pointed at a different database.`,
  ).toHaveCount(1);
  const cells = (await inboxPage.rowCells(row)).join(' | ');
  expect(cells).toContain(busiestSubmission.submissionStatus);
  expect(cells).toContain(busiestSubmission.submissionType);
  expect(cells).toContain(String(busiestSubmission.declaredInvoiceTotal));
});

/**
 * The read-back that makes this a genuine connection proof rather than a "the page rendered" check:
 * the same query is issued straight to the API and compared with what the browser painted. If the
 * UI were serving stubbed, mocked or stale-cached rows, this diverges.
 */
Then('the rendered rows match the Inbox API', async ({ inboxPage, request, world }) => {
  const res = await request.get(
    `/api/inbox?page=0&size=100&startDate=${seededDateWindow.start}&endDate=${seededDateWindow.end}`,
  );
  expect(res.ok(), `GET /api/inbox returned HTTP ${res.status()}`).toBeTruthy();
  const body = (await res.json()) as {
    content: { submissionId: string | null; invTotal: number }[];
    totalElements?: number;
  };
  world.inboxApiRows = body.content;

  expect(body.content.length, 'Inbox API returned no rows for the seeded window').toBeGreaterThan(0);
  const uiFirst = await inboxPage.firstResultSubmissionId();
  expect(
    uiFirst,
    "The first rendered Submission ID does not match the API's first record — the grid is not " +
      'rendering live API data.',
  ).toBe(body.content[0].submissionId ?? '—');
});
