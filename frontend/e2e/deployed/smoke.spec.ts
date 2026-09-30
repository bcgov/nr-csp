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

/**
 * Security response headers — the automated check for the two fixable findings from the
 * 2026-09-30 ZAP scan of the test environment.
 *
 * ---------------------------------------------------------------------------
 * WHY THESE LIVE IN THE **DEPLOYED** PROJECT AND NOT THE LOCAL SUITE
 * ---------------------------------------------------------------------------
 * Both findings are properties of the response as it leaves the real edge, and neither reproduces
 * locally. The duplicate-header finding in particular could not be reproduced against a local
 * Caddy + backend at all — every local measurement returned exactly one of each header, with
 * Caddy's value, while the scan saw two of each in the deployed environment. Only the deployed
 * route exercises the actual chain (OpenShift edge router terminating TLS -> Caddy -> Spring), so
 * this is the only place an assertion about it means anything.
 *
 * Anonymous by design, so no credentials are needed: `/api/health` is `permitAll` in
 * SecurityConfig, and `/` is the public welcome document.
 *
 * `headersArray()` is used rather than `headers()` because the whole point is counting DUPLICATES —
 * `headers()` merges repeated headers into one comma-joined value and would silently hide the very
 * defect being tested.
 */
test.describe('security response headers', () => {
  const countHeader = (headers: { name: string; value: string }[], name: string) =>
    headers.filter((h) => h.name.toLowerCase() === name.toLowerCase());

  // ZAP finding 1: "Strict-Transport-Security Multiple Header Entries". RFC 6797 requires exactly
  // one. Two layers were emitting it — Caddy (frontend/Caddyfile) and Spring Security, the latter
  // because `forward-headers-strategy: framework` makes a forwarded request look secure to it.
  //
  // Fixed by disabling it in the backend, leaving Caddy the single owner. That fix is
  // deployment-INDEPENDENT: it removes the second source outright rather than relying on Caddy
  // collapsing it, which is why this assertion is safe to pin at exactly one. Stripping it in
  // Caddy instead was measured and rejected — a `header_down -Strict-Transport-Security` removes
  // Caddy's own value too and leaves /api/* with no security headers at all.
  for (const path of ['/api/health', '/']) {
    test(`sends exactly one Strict-Transport-Security header on ${path}`, async ({ request }) => {
      const res = await request.get(path);
      expect(res.ok()).toBeTruthy();

      const hsts = countHeader(res.headersArray(), 'strict-transport-security');
      // The received array is in the failure output, so a regression shows both values and makes
      // it obvious which layer started emitting one again (Spring's renders with a space before
      // the semicolon: `max-age=31536000 ; includeSubDomains`).
      expect(hsts.map((h) => h.value)).toHaveLength(1);
    });
  }

  // ZAP finding 2: "CSP: Failure to Define Directive with No Fallback". form-action does NOT fall
  // back to default-src, so without it the policy places no restriction on where a form may submit.
  for (const path of ['/api/health', '/']) {
    test(`CSP defines form-action on ${path}`, async ({ request }) => {
      const res = await request.get(path);
      expect(res.ok()).toBeTruthy();

      const csp = countHeader(res.headersArray(), 'content-security-policy');
      // Exactly one, and it is the policy that carries form-action — i.e. Caddy's, not the
      // backend's shorter one. Asserting the count here too is what proves the surviving policy is
      // the complete one rather than whichever copy happened to win.
      expect(csp).toHaveLength(1);
      expect(csp[0].value).toContain("form-action 'self'");
    });
  }

  // ZAP finding 1 covered only HSTS, but the same scan output showed CSP, X-Frame-Options and
  // X-Content-Type-Options doubled too, from the same cause. All four are now stripped from the
  // upstream copy in the Caddyfile's /api/* proxy, so all four are pinned here.
  //
  // Reproduced locally before fixing (Caddy -> the real backend, status 200): CSP:2, XFO:2, XCTO:2.
  // If this regresses, check the Caddyfile's `header_down` lines first.
  for (const header of ['content-security-policy', 'x-frame-options', 'x-content-type-options']) {
    test(`sends exactly one ${header} header on /api/health`, async ({ request }) => {
      const res = await request.get('/api/health');
      expect(res.ok()).toBeTruthy();
      expect(countHeader(res.headersArray(), header).map((h) => h.value)).toHaveLength(1);
    });
  }
});
