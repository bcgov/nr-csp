import { defineConfig, devices } from '@playwright/test';

/**
 * E2E tests run against an already-deployed environment (never a local dev
 * server): reusable-tests.yml points E2E_BASE_URL at the PR/TEST OpenShift
 * route after the deploy job completes and passes --project/--reporter on the
 * CLI. Locally: E2E_BASE_URL=https://<env-host>/ npx playwright test
 */
export default defineConfig({
  testDir: './e2e',
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
