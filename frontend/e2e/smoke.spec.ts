import { expect, test } from '@playwright/test';

// Smoke-level e2e against the deployed environment. The app has no public
// landing content — '/' redirects straight into the OAuth sign-in — so these
// tests pin the two things an anonymous visitor can prove: the SPA is served
// (title from index.html) and the public /logout route renders.
test.describe('anonymous smoke', () => {
  test('serves the SPA with the expected title', async ({ page }) => {
    await page.goto('/logout');
    await expect(page).toHaveTitle('NR CSP');
  });

  test('renders the public signed-out page', async ({ page }) => {
    await page.goto('/logout');
    await expect(page.getByRole('heading', { name: 'You have been signed out.' })).toBeVisible();
  });
});
