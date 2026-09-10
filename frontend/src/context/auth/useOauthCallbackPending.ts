import { useEffect, useState } from 'react';

/**
 * How long to wait for Amplify to finish the code exchange before treating the
 * callback as failed. The happy path needs a fraction of this: completeOAuthFlow
 * strips the params itself, and does so before it dispatches the signedIn event.
 */
export const OAUTH_CALLBACK_TIMEOUT_MS = 15_000;

function hasCallbackParams(): boolean {
  const params = new URLSearchParams(window.location.search);
  return params.has('code') && params.has('state');
}

/**
 * True while Cognito's `?code=…&state=…` are still on the URL and Amplify may
 * still be exchanging them — so a caller should wait rather than act on
 * `isAuthenticated: false`, which would abandon the sign-in in flight.
 *
 * The wait is bounded. Only Amplify's success path strips those params, so a
 * failed exchange — a replayed or expired code, a token request that never
 * answered — would otherwise leave a caller waiting for good, and still
 * waiting after a reload. Once the wait expires the params are dropped and
 * this returns false, so callers fall through to whatever they do without a
 * session.
 *
 * This lives here rather than at each call site because both the welcome
 * screen and ProtectedRoute need the same rule, and Cognito has more than one
 * way to come back — a denied authorize returns `?error=…` with no code at all,
 * which Amplify reads but neither caller does. One copy is one place to change.
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
