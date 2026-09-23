import { defineConfig, devices } from '@playwright/test';

/**
 * E2E tests run against an already-deployed environment (never a local dev
 * server): reusable-tests.yml points E2E_BASE_URL at the PR/TEST OpenShift
 * route after the deploy job completes and passes --project/--reporter on the
 * CLI. Locally: E2E_BASE_URL=https://<env-host>/ npx playwright test
 *
 * SCOPE: this config owns ONLY the deployed-environment smoke specs that sit
 * directly in ./e2e (currently smoke.spec.ts). The BDD suite that also lives
 * under ./e2e is a SEPARATE, self-contained npm project with its own
 * playwright.config.ts, run from ./e2e via `npm test` against a LOCAL stack.
 * The testIgnore below keeps the two apart -- without it, `bddgen` output in
 * .features-gen/ (real *.spec.js files) would be collected by THIS config and
 * run without playwright-bdd's fixtures, failing for reasons that look like
 * app bugs. Keep these patterns in sync if the BDD project moves.
 */
export default defineConfig({
  testDir: './e2e',
  testIgnore: [
    '**/.features-gen/**',
    '**/features/**',
    '**/steps/**',
    '**/pages/**',
    '**/fixtures/**',
    '**/preflight/**',
    '**/node_modules/**',
  ],
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:3000/',
    // OpenShift PR routes can lag on cert provisioning; the workflow's health
    // check already uses curl -k for the same reason.
    ignoreHTTPSErrors: true,
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
