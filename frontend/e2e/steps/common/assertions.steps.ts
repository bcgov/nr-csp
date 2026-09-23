import { Then, expect } from '../fixtures';

/**
 * Cross-domain assertion steps — no domain vocabulary, reusable by any feature. Domain-specific
 * assertions (banners, read-backs, field gating) live under steps/<domain>/.
 */

Then('I should see the error {string}', async ({ page }, message) => {
  // .first(): some forms surface the same message in BOTH the error banner and the field's inline
  // text (2 matches) — asserting the message is visible somewhere is the intent, so avoid a strict-mode
  // violation on the legitimate duplicate.
  await expect(page.getByText(message).first()).toBeVisible();
});

Then('I should be returned to {string}', async ({ page }, target) => {
  const re = new RegExp(`${target.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`);
  await expect(page).toHaveURL(re);
});
