import 'dotenv/config';
import { defineConfig, devices } from '@playwright/test';
import { defineBddConfig } from 'playwright-bdd';

/**
 * E2E — Playwright + BDD config (BC Gov NR stack).
 *
 * Architecture: the `.feature` files under `features/` are the executable spec; `bddgen test`
 * compiles them into native Playwright tests (in the generated `testDir` below) using the step
 * definitions under `steps/`. Everything downstream — reporters, tracing, parallelism, fixtures,
 * page objects — is stock Playwright, unchanged by BDD. Run via `npm test` (the `pretest` hook runs
 * `bddgen test` first) or `npx bddgen test && npx playwright test`.
 *
 * Runs against an ALREADY-RUNNING local system (we do NOT start servers here, because the app is a
 * two-process stack — Vite frontend on :3000 + Spring backend on :8080 — plus a Docker Oracle DB,
 * each with its own env. Bring them up per e2e/README.md, then run the tests).
 *
 * Timeout / artifact standards follow the TEA `playwright-config` guardrails.
 */
const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

/**
 * Where the DEPLOYED-environment smoke project points. CI (reusable-tests.yml) sets E2E_BASE_URL to
 * the PR/TEST OpenShift route after the deploy job; locally you would pass it explicitly.
 */
const DEPLOYED_BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:3000/';

// Compile features/*.feature + steps/*.ts into generated Playwright tests; returns their dir.
const testDir = defineBddConfig({
  features: 'features/**/*.feature',
  steps: 'steps/**/*.ts',
});

export default defineConfig({
  testDir,
  outputDir: './test-results',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  /**
   * Locally capped at 4 — NOT left as Playwright's default (one worker per CPU core, 7 here).
   *
   * The whole suite runs against ONE Vite dev server, ONE Spring backend and ONE Oracle container.
   * Past about four concurrent browsers they queue on the backend rather than on the browser, and
   * the longest scenarios — the multi-screen journeys, which poll API read-backs — cross the test
   * timeout. It surfaces as `Request context disposed` or `Target page, context or browser has
   * been closed` inside an `expect.poll`, which reads like a bug in the test but is just the test
   * timing out mid-poll while the stack thrashes.
   *
   * Measured on a 14-core machine, full suite:
   *   7 workers (the default) -> 3 failed, ~90 s   (the review journey alone took 1.1 min)
   *   4 workers               -> 0 failed, ~87 s
   *   3 workers               -> 0 failed, ~110 s
   * So the cap is FASTER as well as correct: over-subscribing the backend costs more than it buys.
   * Raise it only if the stack behind it gains capacity.
   */
  workers: process.env.CI ? 1 : 4,
  /**
   * 90 s, not 60 s. The journeys legitimately take 30-45 s under load — three screens, several
   * polled read-backs — so 60 s left almost no headroom on a busy machine or a slow CI runner.
   * This is headroom, not a mask: a scenario that genuinely hangs still fails.
   */
  timeout: 90_000,
  expect: { timeout: 10_000 },
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: 'test-results/results.xml' }],
    ['list'],
  ],
  use: {
    baseURL: BASE_URL,
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  projects: [
    {
      // Runs ONCE before the suite (chromium depends on it): asserts the pinned real-data anchors still
      // resolve, so a stale/re-extracted DB fails fast with one clear "re-ground the fixtures" message
      // rather than dozens of confusing mid-suite failures. See preflight/anchors.setup.ts.
      name: 'setup',
      testDir: './preflight',
      testMatch: /.*\.setup\.ts$/,
    },
    {
      /**
       * DEPLOYED-environment smoke — the suite that predates the BDD work and that CI runs after a
       * deploy. It lives HERE, inside the e2e project, rather than under its own
       * frontend/playwright.config.ts, because a spec file sitting inside frontend/e2e/ resolves
       * `@playwright/test` to THIS folder's node_modules; a second runner rooted at frontend/ then
       * fails with "Requiring @playwright/test second time" and silently collects 0 tests. One
       * Playwright install, one config, no clash.
       *
       * Deliberately does NOT depend on the `setup` project: preflight asserts a LOCAL seeded DB,
       * which a deployed environment neither has nor needs. It also carries no BDD steps — these are
       * plain Playwright specs.
       */
      name: 'deployed',
      testDir: './deployed',
      use: {
        ...devices['Desktop Chrome'],
        baseURL: DEPLOYED_BASE_URL,
        // OpenShift PR routes can lag on cert provisioning; the workflow's health check uses curl -k
        // for the same reason.
        ignoreHTTPSErrors: true,
        trace: 'on-first-retry',
      },
    },
    {
      name: 'chromium',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        // Taller than devices['Desktop Chrome']'s own 1280x720 default (must come AFTER the spread —
        // devices['Desktop Chrome'] sets its own viewport, which would otherwise win the merge): at
        // 720px, a Carbon Modal's footer buttons (e.g. a tall Carbon form's modal footer button) sit close
        // enough to the bottom edge that a coordinate-based click lands on the modal's own outer
        // wrapper instead of the button (confirmed via document.elementFromPoint at the button's
        // center) — not an app bug, just an unrealistically short viewport for an 8+ field form.
        // 900px matches a realistic admin desktop.
        viewport: { width: 1280, height: 900 },
      },
    },
  ],
});
