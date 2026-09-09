export type SignOutReason = 'user' | 'timeout';

/**
 * Why the current sign-out happened, carried across the federated logout chain.
 *
 * The chain hands the browser to SiteMinder → Keycloak → Cognito and back, so
 * the SPA (and every bit of in-memory state) is discarded on the way out. The
 * URL it returns to is fixed — `redirectSignOut` must be registered verbatim as
 * a Cognito sign-out URL, and that registration lives in FAM's Terraform, not
 * here — so an idle timeout and a deliberate sign-out both land on /logout and
 * cannot be told apart from the URL. Stashing the reason before the chain
 * starts is what lets that page pick the right wording on return.
 *
 * sessionStorage rather than localStorage: this is per-tab, and it should not
 * leak into a tab the user is still working in. The key deliberately sits
 * outside the `csp.table.` namespace that clearPersistedTableState() sweeps,
 * and outside the Cognito prefix that clearStoredTokens() sweeps, so it
 * survives both — see performSignOut in RealAuthProvider.
 */
const KEY = 'csp.signOutReason';

export function setSignOutReason(reason: SignOutReason): void {
  try {
    window.sessionStorage.setItem(KEY, reason);
  } catch {
    // Storage unavailable (private mode, blocked cookies) — the logout page
    // falls back to the plain signed-out wording, which is never wrong, only
    // less specific.
  }
}

/**
 * Read the reason and clear it, so a later visit to /logout doesn't repeat a
 * stale "your session expired". Defaults to a deliberate sign-out.
 */
export function takeSignOutReason(): SignOutReason {
  try {
    const stored = window.sessionStorage.getItem(KEY);
    window.sessionStorage.removeItem(KEY);
    return stored === 'timeout' ? 'timeout' : 'user';
  } catch {
    return 'user';
  }
}
