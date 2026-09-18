import { expect, test } from '@playwright/test';

// Smoke-level e2e against the deployed environment. Everything past the public
// welcome route redirects into the OAuth sign-in, so these tests pin what an
// anonymous visitor can prove: the SPA is served (title from index.html), the
// welcome screen at '/' offers the sign-in choice, and the fixed post-sign-out
// landing at /logout redirects onto it.
test.describe('anonymous smoke', () => {
  test('serves the SPA with the expected title', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle('NR CSP');
  });

  test('renders the public welcome screen', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Welcome to CSP' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Log in with IDIR' })).toBeVisible();
  });

  test('redirects the post-sign-out landing to the welcome screen', async ({ page }) => {
    await page.goto('/logout');
    await expect(page.getByRole('heading', { name: 'Welcome to CSP' })).toBeVisible();
    await expect(page).toHaveURL(/\/$/);
  });
});
