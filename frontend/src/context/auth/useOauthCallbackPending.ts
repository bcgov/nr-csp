import { useEffect, useState } from 'react';

/**
 * How long to wait for an OIDC code exchange before treating the callback as
 * failed. The happy path needs a fraction of this: RealAuthProvider replaces the
 * callback URL before it stops loading.
 */
export const OAUTH_CALLBACK_TIMEOUT_MS = 15_000;

function hasCallbackParams(): boolean {
  const params = new URLSearchParams(window.location.search);
  return params.has('code') && params.has('state');
}

/**
 * True while an authorization response's `?code=…&state=…` is still on the URL
 * and may still be exchanged — so a caller should wait rather than act on
 * `isAuthenticated: false`, which would abandon the sign-in in flight.
 *
 * RealAuthProvider exchanges the code on /authCallback and replaces that URL
 * (with the saved route, or `/` after a failure) before it stops loading. The
 * wait is bounded anyway: params left on any other URL — a stale link, a
 * reload of an old address — are never exchanged, and would otherwise leave a
 * caller waiting for good. Once the wait expires the params are dropped and
 * this returns false, so callers fall through to whatever they do without a
 * session.
 *
 * This lives here rather than at each call site because both the welcome
 * screen and ProtectedRoute need the same rule. One copy is one place to change.
 */
export function useOauthCallbackPending(): boolean {
  const [expired, setExpired] = useState(false);
  const isCallback = hasCallbackParams();

  useEffect(() => {
    if (!isCallback) return;
    const timer = setTimeout(() => {
      window.history.replaceState(null, '', window.location.pathname);
      setExpired(true);
    }, OAUTH_CALLBACK_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [isCallback]);

  return isCallback && !expired;
}
