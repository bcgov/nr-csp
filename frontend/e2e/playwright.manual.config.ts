import { defineConfig, devices } from '@playwright/test';

// Captures the user manual screenshots; see manual/manual.screenshots.spec.ts.
// No server: the spec serves the built app (frontend/build) and the API fixtures itself.
export default defineConfig({
  testDir: './manual',
  testMatch: /manual\.screenshots\.spec\.ts/,
  outputDir: './test-results/manual',
  timeout: 120_000,
  reporter: [['list']],
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
