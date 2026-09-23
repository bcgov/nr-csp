import { type Page, expect } from '@playwright/test';

/**
 * Cross-domain browser-interaction helpers — mock identity, client-side navigation, and Carbon
 * date entry. These hold no domain vocabulary, so every page object across every domain reuses them
 * from here rather than re-inlining or keeping a domain-local copy "private".
 */

// ---------------------------------------------------------------------------
// CSP mock auth — GROUNDED for this app (differs from the scaffold default).
//
// The scaffold assumes an NR Landing page with an IDIR button to click and an
// IN-MEMORY session that a full page.goto() would drop. CSP does NOT work that
// way: frontend/src/context/auth/MockAuthProvider.tsx returns
// `isAuthenticated: true` immediately, with the role read from
// localStorage['csp.mockRole'] (default 'ADMIN'). See also frontend/src/env.ts —
// mock auth requires BOTH `mockUser: true` in public/amplify-config.js (that
// file is GITIGNORED: local-only) AND a localhost hostname.
//
// Consequences, and why there is no login dance below:
//   * there is nothing to click — the app is authenticated on first paint;
//   * the session is NOT in-memory-only, so page.goto() to a protected route is
//     safe and clientSideNavigate() is optional (kept for parity/other apps).
// ---------------------------------------------------------------------------

/** localStorage key MockAuthProvider reads the role from (MOCK_ROLE_KEY in MockAuthProvider.tsx). */
const MOCK_ROLE_KEY = 'csp.mockRole';

/** Roles CSP's mock provider accepts; it maps `X` -> group `CSP_X` (see auth/permissions.ts). */
export type MockRole = 'ADMIN' | 'APPROVER' | 'SUBMITTER' | 'VIEWER';

/** The role MockAuthProvider falls back to when localStorage holds nothing valid. */
export const MOCK_USER_ROLE: MockRole = 'ADMIN';

/**
 * Establish the mock identity for the scenario.
 *
 * Seeds localStorage BEFORE any app code runs (addInitScript applies to every document in the
 * context), so the very first render is already authenticated as `role` — no login step, and no
 * race against a redirect to the welcome screen. Call this before the first navigation.
 */
export async function signInAsMockUser(page: Page, role: MockRole = MOCK_USER_ROLE): Promise<void> {
  await page.addInitScript(
    ([key, value]) => {
      try {
        window.localStorage.setItem(key, value);
      } catch {
        // Private mode / blocked storage: MockAuthProvider then falls back to ADMIN, which is the
        // default this helper asks for anyway. Never fail the scenario on a storage write.
      }
    },
    [MOCK_ROLE_KEY, role] as const,
  );
}

/**
 * Kept for cross-app parity: apps whose mock session is in-memory only must navigate client-side so
 * a reload doesn't drop it. CSP does not need this (see the note above), but a step that wants to
 * assert SPA routing rather than a fresh document load can still use it.
 */
/**
 * Client-side navigate to a protected route so the in-memory mock session survives — a full
 * `page.goto()` would reload the app and drop it. React Router reads
 * `location.state` from `history.state.usr`, so a non-empty `state` is seeded there. The
 * screen-specific readiness assertion (which heading/control proves the screen loaded) stays in the
 * calling page object. This is the ONE home for the mock-auth nav dance — every page object across
 * every domain reuses it rather than re-inlining `history.pushState`/`PopStateEvent`.
 */
export async function clientSideNavigate(
  page: Page,
  path: string,
  state: Record<string, unknown> = {},
): Promise<void> {
  await page.evaluate(
    ({ p, usr }) => {
      const hasState = Object.keys(usr).length > 0;
      window.history.pushState(hasState ? { usr } : {}, '', p);
      window.dispatchEvent(new PopStateEvent('popstate'));
    },
    { p: path, usr: state },
  );
}

/**
 * Commit a value into a Carbon DatePicker input: click, type, then Escape (dismiss the calendar
 * overlay) and blur (onBlur reads the typed value). Shared across every domain's date fields.
 */
export async function setDateField(page: Page, id: string, value: string): Promise<void> {
  const input = page.locator(id);
  await input.click();
  await input.fill(value);
  await page.keyboard.press('Escape');
  await input.blur();
}
