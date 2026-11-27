import { existsSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';

import { expect, test, type Browser, type BrowserContext, type Locator, type Page } from '@playwright/test';

import {
  resolveManualApi,
  SAMPLE_INVOICE_ID,
  SAMPLE_SUBMISSION_ID,
  UPLOAD_FILE_NAME,
  type ManualScenario,
} from './manual-fixtures';

// Both editions come from a synthetic session: the built app, signed in with a sample token,
// every API call answered from manual-fixtures.ts. Add a SHOTS entry and its fixture data.

type Edition = 'industry' | 'ministry';

type Shot = {
  edition: Edition;
  /** Image name without extension; the two-digit prefix is the chapter. */
  file: string;
  /** 'loggedOut' installs no session. */
  scenario: ManualScenario | 'loggedOut';
  route: string;
  /** Visible once the screen has its data. */
  ready: (page: Page) => Locator;
  /** Clicks, typing and panel changes after the screen is ready. */
  steps?: (page: Page) => Promise<void>;
  /** Screenshot one viewport scrolled to this element instead of the top of the page. */
  focus?: (page: Page) => Locator;
  /** Capture just this part of the page. */
  area?: (page: Page) => Locator;
  /** Tallest capture from the top of the page, in CSS pixels. */
  maxHeight?: number;
  /** Widest capture from the left of the page, in CSS pixels. */
  maxWidth?: number;
};

const BUILD_DIR = path.join(import.meta.dirname, '..', '..', 'build');
const OUT_DIR = path.join(import.meta.dirname, '..', '..', '..', 'docs', 'manual');
const ORIGIN = 'http://localhost:3000';
const ISSUER = 'https://login.example.com/realms/standard';
const CLIENT_ID = 'csp-manual';
const VIEWPORT = { width: 1440, height: 900 };
// Every screen shows the same "today", so dates in the images never drift.
const FIXED_NOW = new Date('2026-11-25T18:00:00Z');
const HEADER_OFFSET_PX = 64;
const SETTLE_TIMEOUT_MS = 20_000;

const SESSIONS: Record<ManualScenario, Record<string, unknown>> = {
  industry: {
    identity_provider: 'bceidbusiness',
    bceid_username: 'JSAMPLE',
    display_name: 'Jordan Sample',
    email: 'jordan.sample@example.com',
    client_roles: ['CSP_SUBMITTER_FOREST_CLIENT-00000001'],
  },
  ministry: {
    identity_provider: 'idir',
    idir_username: 'PEXAMPLE',
    display_name: 'Pat Example',
    email: 'pat.example@example.com',
    client_roles: ['CSP_ADMIN'],
  },
};

const CONTENT_TYPES: Record<string, string> = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.webp': 'image/webp',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.json': 'application/json',
  '.webmanifest': 'application/manifest+json',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
  '.ttf': 'font/ttf',
};

const heading = (page: Page, name: string | RegExp) => page.getByRole('heading', { level: 1, name });
const button = (page: Page, name: string) => page.getByRole('button', { name, exact: true }).first();

const openProfilePanel = async (page: Page) => {
  await page.getByRole('button', { name: 'User settings' }).click();
  await expect(page.getByRole('button', { name: 'Log out' })).toBeVisible();
};

const openMenuGroups = async (page: Page, ...names: string[]) => {
  const menu = page.getByRole('navigation', { name: 'Side navigation' });
  for (const name of names) {
    const group = menu.getByRole('button', { name, exact: true });
    if ((await group.getAttribute('aria-expanded')) !== 'true') await group.click();
  }
};

const runSearch = async (page: Page, resultText: string) => {
  await button(page, 'Search').click();
  await expect(page.getByText(resultText, { exact: true }).first()).toBeVisible();
};

const expandRow = async (page: Page, rowText: string, readyText: string | RegExp) => {
  await page.getByRole('row').filter({ hasText: rowText }).first().getByRole('button').first().click();
  await expect(page.getByText(readyText).first()).toBeVisible();
};

const uploadSampleFile = async (page: Page) => {
  await page.locator('input[type="file"]').setInputFiles({
    name: UPLOAD_FILE_NAME,
    mimeType: 'text/xml',
    buffer: Buffer.from('<?xml version="1.0" encoding="UTF-8"?><sample/>'),
  });
  await expect(page.getByText(/1 warning found/).first()).toBeVisible();
};

// The sign-in column only; the photo beside it adds megabytes and nothing to read.
const welcomeShot = (edition: Edition): Shot => ({
  edition,
  file: '01-welcome',
  scenario: 'loggedOut',
  route: '/',
  ready: (page) => button(page, 'Log in with Business BCeID'),
  maxWidth: 720,
  maxHeight: 480,
});

const SHOTS: Shot[] = [
  // Industry edition
  welcomeShot('industry'),
  {
    edition: 'industry',
    file: '01-home',
    scenario: 'industry',
    route: '/upload-submission',
    ready: (page) => heading(page, 'CSP Submission'),
    steps: (page) => openMenuGroups(page, 'Support'),
  },
  {
    edition: 'industry',
    file: '01-profile',
    scenario: 'industry',
    route: '/upload-submission',
    ready: (page) => heading(page, 'CSP Submission'),
    steps: openProfilePanel,
  },
  {
    edition: 'industry',
    file: '02-upload-submission',
    scenario: 'industry',
    route: '/upload-submission',
    ready: (page) => heading(page, 'CSP Submission'),
    steps: uploadSampleFile,
    maxHeight: 1000,
  },
  {
    edition: 'industry',
    file: '02-upload-invoices',
    scenario: 'industry',
    route: '/upload-submission',
    ready: (page) => heading(page, 'CSP Submission'),
    steps: async (page) => {
      await uploadSampleFile(page);
      await page.getByText('Collapse all', { exact: true }).first().click();
      await expandRow(page, 'EX-24108', 'Warnings for EX-24108');
    },
    focus: (page) => page.getByText('Invoice Details', { exact: true }).first(),
  },
  {
    edition: 'industry',
    file: '02-submission-history',
    scenario: 'industry',
    route: '/submission-history',
    ready: (page) => page.getByText('EXAMPLE FORESTRY LTD. (00000001)').first(),
    steps: (page) => expandRow(page, 'Rejected', /Invoice comments/),
  },
  {
    edition: 'industry',
    file: '02-view-submission',
    scenario: 'industry',
    route: `/submission-history/${SAMPLE_SUBMISSION_ID}`,
    ready: (page) => heading(page, `Submission ${SAMPLE_SUBMISSION_ID}`),
    steps: (page) => expandRow(page, 'EX-24109', 'Staff Comment:'),
    focus: (page) => page.getByRole('row').filter({ hasText: 'EX-24109' }).first(),
  },

  // Ministry edition
  welcomeShot('ministry'),
  {
    edition: 'ministry',
    file: '01-home',
    scenario: 'ministry',
    route: '/search',
    ready: (page) => heading(page, 'Invoice search'),
    steps: (page) => openMenuGroups(page, 'Table maintenance', 'Submissions', 'Support'),
  },
  {
    edition: 'ministry',
    file: '01-profile',
    scenario: 'ministry',
    route: '/search',
    ready: (page) => heading(page, 'Invoice search'),
    steps: openProfilePanel,
  },
  {
    edition: 'ministry',
    file: '02-search',
    scenario: 'ministry',
    route: '/search',
    ready: (page) => heading(page, 'Invoice search'),
    steps: (page) => runSearch(page, 'EX-24107'),
    maxHeight: 1000,
  },
  {
    edition: 'ministry',
    file: '03-inbox',
    scenario: 'ministry',
    route: '/inbox',
    ready: (page) => heading(page, 'Inbox'),
    steps: (page) => runSearch(page, String(SAMPLE_SUBMISSION_ID)),
    maxHeight: 1000,
  },
  {
    edition: 'ministry',
    file: '03-submission',
    scenario: 'ministry',
    route: '/inbox',
    ready: (page) => heading(page, 'Inbox'),
    steps: async (page) => {
      await runSearch(page, String(SAMPLE_SUBMISSION_ID));
      await page.getByRole('link', { name: String(SAMPLE_SUBMISSION_ID), exact: true }).click();
      await expect(heading(page, `Submission ${SAMPLE_SUBMISSION_ID}`)).toBeVisible();
    },
    maxHeight: 1000,
  },
  {
    edition: 'ministry',
    file: '04-invoice',
    scenario: 'ministry',
    route: `/invoice/${SAMPLE_INVOICE_ID}`,
    ready: (page) => page.getByText('Invoice address information').first(),
    maxHeight: 1100,
  },
  {
    edition: 'ministry',
    file: '04-invoice-groups',
    scenario: 'ministry',
    route: `/invoice/${SAMPLE_INVOICE_ID}`,
    ready: (page) => page.getByText('Invoice group summary').first(),
    focus: (page) => page.getByText('Invoice group summary').first(),
  },
  {
    edition: 'ministry',
    file: '04-add-line-item',
    scenario: 'ministry',
    route: `/invoice/${SAMPLE_INVOICE_ID}`,
    ready: (page) => page.getByText('Invoice group summary').first(),
    steps: async (page) => {
      await button(page, 'Add new item').click();
      await expect(page.getByRole('dialog').first()).toBeVisible();
    },
    area: (page) => page.locator('.cds--modal.is-visible .cds--modal-container').first(),
  },
  {
    edition: 'ministry',
    file: '05-flat-price-conversion',
    scenario: 'ministry',
    route: '/table-maintenance/flat-price-conversion',
    ready: (page) => heading(page, 'Flat price conversion'),
    steps: (page) => runSearch(page, 'Hemlock'),
  },
  {
    edition: 'ministry',
    file: '05-sort-code',
    scenario: 'ministry',
    route: '/sort-code',
    ready: (page) => page.getByText('Gang sawlog').first(),
  },
  {
    edition: 'ministry',
    file: '06-report-r07',
    scenario: 'ministry',
    route: '/reports/r07-reconciliation',
    ready: (page) => heading(page, 'R07 - Reconciliation report'),
    maxHeight: 1100,
  },
  {
    edition: 'ministry',
    file: '07-upload-submission',
    scenario: 'ministry',
    route: '/upload-submission',
    ready: (page) => heading(page, 'CSP Submission'),
    steps: uploadSampleFile,
    maxHeight: 1000,
  },
];

type CaptureIssues = string[];

const base64Url = (value: unknown) => Buffer.from(JSON.stringify(value)).toString('base64url');

/** An unsigned token with the claims CSP reads; the app decodes it but never verifies it. */
const sampleToken = (claims: Record<string, unknown>) =>
  `${base64Url({ alg: 'none', typ: 'JWT' })}.${base64Url(claims)}.manual`;

const installSession = async (page: Page, scenario: ManualScenario) => {
  const issuedAt = Math.floor(FIXED_NOW.getTime() / 1000);
  const expiresAt = issuedAt + 8 * 3600;
  const claims = {
    iss: ISSUER,
    azp: CLIENT_ID,
    typ: 'Bearer',
    sub: `${String(SESSIONS[scenario].bceid_username ?? SESSIONS[scenario].idir_username).toLowerCase()}@example`,
    iat: issuedAt,
    exp: expiresAt,
    ...SESSIONS[scenario],
  };
  const token = sampleToken(claims);
  const stored = JSON.stringify({
    id_token: token,
    session_state: null,
    access_token: token,
    refresh_token: 'manual-refresh-token',
    token_type: 'Bearer',
    scope: 'openid profile email',
    profile: { sub: claims.sub, iss: ISSUER, aud: CLIENT_ID, exp: expiresAt, iat: issuedAt },
    expires_at: expiresAt,
  });
  await page.addInitScript(([key, value]) => window.sessionStorage.setItem(key, value), [
    `oidc.user:${ISSUER}:${CLIENT_ID}`,
    stored,
  ] as const);
};

/** Serves the built app, a runtime config that signs in through the sample issuer, and the fixtures. */
const serveApp = async (page: Page, shot: Shot, issues: CaptureIssues) => {
  const scenario = shot.scenario === 'loggedOut' ? 'ministry' : shot.scenario;
  // Nothing reaches a real service: any request the routes below don't serve is refused and reported.
  await page.route('**/*', (route) => {
    issues.push(`${shot.edition}/${shot.file}: unexpected request to ${route.request().url()}`);
    return route.abort();
  });
  await page.route(`${ISSUER}/**`, (route) => route.abort());
  await page.route(`${ORIGIN}/**`, async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === '/config.js') {
      const config = { mockUser: false, appEnv: 'prod', oidcIssuerUri: ISSUER, oidcClientId: CLIENT_ID };
      return route.fulfill({
        contentType: 'text/javascript',
        body: `window.appConfig = ${JSON.stringify(config)};`,
      });
    }
    if (url.pathname.startsWith('/api/')) {
      const apiPath = url.pathname.slice('/api'.length);
      const resolved = resolveManualApi(scenario, request.method(), apiPath, url.searchParams, request.postData());
      if (!resolved) {
        issues.push(`${shot.edition}/${shot.file}: no fixture for ${request.method()} ${apiPath}${url.search}`);
        return route.fulfill({ status: 501, contentType: 'application/json', body: '{}' });
      }
      return route.fulfill({
        status: resolved.status ?? 200,
        contentType: resolved.contentType ?? 'application/json',
        headers: { date: FIXED_NOW.toUTCString(), ...resolved.headers },
        body: typeof resolved.body === 'string' ? resolved.body : JSON.stringify(resolved.body),
      });
    }
    const file = path.join(BUILD_DIR, decodeURIComponent(url.pathname));
    const servable = file.startsWith(BUILD_DIR) && existsSync(file) && statSync(file).isFile();
    const target = servable ? file : path.join(BUILD_DIR, 'index.html');
    return route.fulfill({
      contentType: CONTENT_TYPES[path.extname(target)] ?? 'application/octet-stream',
      body: readFileSync(target),
    });
  });
};

const newChapterContext = async (browser: Browser) => {
  const context = await browser.newContext({
    viewport: VIEWPORT,
    deviceScaleFactor: 1.5,
    colorScheme: 'light',
    serviceWorkers: 'block',
    locale: 'en-CA',
    timezoneId: 'America/Vancouver',
  });
  await context.clock.setFixedTime(FIXED_NOW);
  return context;
};

/** Waits for skeletons, spinners and loading text to go, then for transitions to settle. */
const settle = async (page: Page, shot: Shot, issues: CaptureIssues) => {
  const busy = page
    .locator('.cds--skeleton, .cds--skeleton__text, .cds--data-table--skeleton, .cds--loading')
    .filter({ visible: true });
  try {
    await expect(busy).toHaveCount(0, { timeout: SETTLE_TIMEOUT_MS });
  } catch {
    issues.push(`${shot.edition}/${shot.file}: still loading after ${SETTLE_TIMEOUT_MS / 1000}s`);
  }
  await page.waitForTimeout(500);
};

const snap = async (page: Page, shot: Shot, issues: CaptureIssues) => {
  await settle(page, shot, issues);
  // No hover state or focus ring in the image.
  await page.mouse.move(VIEWPORT.width / 2, VIEWPORT.height - 2);
  await page.evaluate(() => (document.activeElement as HTMLElement | null)?.blur());
  const output = path.join(OUT_DIR, shot.edition, 'images', `${shot.file}.png`);
  if (shot.area) {
    await shot.area(page).screenshot({ path: output, animations: 'disabled' });
    return;
  }
  if (shot.focus) {
    await shot.focus(page).evaluate((element, offset) => {
      window.scrollTo(0, element.getBoundingClientRect().top + window.scrollY - offset);
    }, HEADER_OFFSET_PX);
    await page.waitForTimeout(300);
    await page.screenshot({ path: output, animations: 'disabled' });
    return;
  }
  await page.evaluate(() => window.scrollTo(0, 0));
  const height = await page.evaluate(() => document.documentElement.scrollHeight);
  await page.screenshot({
    path: output,
    fullPage: true,
    clip: {
      x: 0,
      y: 0,
      width: shot.maxWidth ?? VIEWPORT.width,
      height: Math.min(height, shot.maxHeight ?? VIEWPORT.height),
    },
    animations: 'disabled',
  });
};

// One page per shot, so each has its own session; they share the chapter's context and clock.
const capture = async (context: BrowserContext, shot: Shot, issues: CaptureIssues) => {
  const page = await context.newPage();
  try {
    await serveApp(page, shot, issues);
    if (shot.scenario !== 'loggedOut') await installSession(page, shot.scenario);
    await page.goto(`${ORIGIN}${shot.route}`);
    await expect(shot.ready(page)).toBeVisible({ timeout: 30_000 });
    await settle(page, shot, issues);
    await shot.steps?.(page);
    await snap(page, shot, issues);
  } finally {
    await page.close();
  }
};

test.beforeAll(() => {
  if (!existsSync(path.join(BUILD_DIR, 'index.html'))) {
    throw new Error('Build the app first: cd frontend && npm run build');
  }
});

const chapters = new Map<string, Shot[]>();
for (const shot of SHOTS) {
  const key = `${shot.edition} ${shot.file.slice(0, 2)}`;
  chapters.set(key, [...(chapters.get(key) ?? []), shot]);
}

for (const [chapter, shots] of chapters) {
  test(`sample data: ${chapter}`, async ({ browser }) => {
    const issues: CaptureIssues = [];
    const context = await newChapterContext(browser);
    try {
      for (const shot of shots) {
        await test.step(`${shot.edition}/${shot.file}`, () => capture(context, shot, issues));
      }
    } finally {
      await context.close();
    }
    for (const issue of issues) {
      test.info().annotations.push({ type: 'manual screenshot gap', description: issue });
    }
    expect(issues, 'Every screenshot must load fully from fixtures').toEqual([]);
  });
}
