import { expect, test } from '@playwright/test';

// Smoke-level e2e against the deployed environment. Everything past the two
// public routes redirects into the OAuth sign-in, so these tests pin what an
// anonymous visitor can prove: the SPA is served (title from index.html), the
// welcome screen at '/' offers the sign-in choice, and /logout renders.
test.describe('anonymous smoke', () => {
  test('serves the SPA with the expected title', async ({ page }) => {
    await page.goto('/logout');
    await expect(page).toHaveTitle('NR CSP');
  });

  test('renders the public welcome screen', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Welcome to CSP' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Log in with IDIR' })).toBeVisible();
  });

  test('renders the public signed-out page', async ({ page }) => {
    await page.goto('/logout');
    await expect(page.getByRole('heading', { name: 'You’ve successfully logged out' })).toBeVisible();
  });
});
