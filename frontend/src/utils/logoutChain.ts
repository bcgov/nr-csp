/**
 * Federated logout-chain URL builder, following FAM's canonical pattern
 * (bcgov/nr-forests-access-management, frontend/src/utils/logoutChain.ts).
 *
 * Builds a multi-hop logout chain where each identity layer clears its own
 * session and then redirects to the next, ending back at the app:
 *
 *     app  →  SiteMinder logoff.cgi  →  Keycloak end-session  →  Cognito /logout  →  app
 *
 * Cognito fires LAST: that way Keycloak's `post_logout_redirect_uri` is a
 * single, stable, app-agnostic value (the Cognito `/logout` URL) — the only
 * thing that must live on the shared FAM Keycloak client's allow-list.
 * Per-app / per-environment origins are only ever registered as Cognito
 * sign-out URLs, which FAM controls (oidc_clients_csp.tf).
 *
 * Correctness hinges on encoding each nested URL EXACTLY ONCE with
 * `encodeURIComponent` as it is embedded in the outer layer's query string.
 * This preserves the structural `?` and `&` of an inner URL through the outer
 * layer's query parse. Without it, `logoff.cgi` peels an inner URL's `?...&...`
 * off as its own query params, silently dropping `post_logout_redirect_uri` at
 * the SiteMinder hop — so Keycloak never learns where to go next and the chain
 * breaks.
 */

type AmplifyConfig = typeof window.amplifyConfig;

/**
 * Builds the full federated logout-chain URL, nesting innermost → outermost so
 * each layer embeds the one below it.
 *
 * @returns The SiteMinder logoff URL that drives the whole chain, or `null` if
 *   any required piece of config is missing (caller falls back to Amplify
 *   `signOut()` for a Cognito-only logout).
 */
export function buildFederatedLogoutUrl(config: AmplifyConfig): string | null {
  const siteminderBase = config?.logoutSiteminderUrl?.trim();
  const keycloakBase = config?.logoutKeycloakUrl?.trim();
  const keycloakClientId = config?.logoutKeycloakClientId?.trim();
  const cognitoDomain = config?.cognitoDomain?.trim();
  const cognitoClientId = config?.userPoolClientId?.trim();
  // The app URL the browser lands on once the chain completes; must be
  // registered verbatim as a Cognito sign-out URL on the FAM app client.
  const appReturnUrl = config?.redirectSignOut?.trim();

  if (!siteminderBase || !keycloakBase || !keycloakClientId || !cognitoDomain || !cognitoClientId || !appReturnUrl) {
    return null; // → caller falls back to Amplify signOut()
  }

  // Innermost: Cognito clears its own session cookie, then redirects to the app.
  const cognitoLogout =
    `https://${cognitoDomain}/logout` +
    `?client_id=${encodeURIComponent(cognitoClientId)}` +
    `&logout_uri=${encodeURIComponent(appReturnUrl)}`;

  // Keycloak clears its session, then returns to the Cognito logout URL. This
  // Cognito URL is the ONLY value allow-listed on the shared FAM Keycloak
  // client's "Valid post logout redirect URIs" — never the app URL.
  const keycloakLogout =
    `${keycloakBase}?client_id=${encodeURIComponent(keycloakClientId)}` +
    `&post_logout_redirect_uri=${encodeURIComponent(cognitoLogout)}`;

  // Outermost: SiteMinder logs off the IDIR session, then returns to the
  // Keycloak end-session URL. `retnow=1` forces an immediate return with no
  // interstitial.
  return `${siteminderBase}?retnow=1&returl=${encodeURIComponent(keycloakLogout)}`;
}

/**
 * How long the IDIR-realm logout popup is given to finish before it is closed
 * and the main logout chain proceeds. The popup needs two round-trips (GET the
 * auto-submitting confirm page, POST the confirmation).
 */
export const IDIR_REALM_LOGOUT_GRACE_MS = 3000;

/**
 * Opens a short-lived popup that logs the user out of loginproxy's `idir`
 * realm — a second Keycloak realm that brokers standard-realm IDIR logins to
 * SiteMinder. Its session is the one layer the main logout chain cannot clear:
 * without it, the next sign-in silently completes through the broker and never
 * prompts for credentials.
 *
 * A popup is the only viable vehicle: the realm's end-session endpoint rejects
 * redirect chaining for integrating clients (HTTP 400 — our URLs are not on
 * its allow-list) and its pages send `frame-ancestors 'self'`, so an invisible
 * iframe is blocked. The realm's logout-confirm page auto-submits itself
 * (loginproxy theme), so the popup completes the logout unattended; the caller
 * waits {@link IDIR_REALM_LOGOUT_GRACE_MS} and closes it.
 *
 * @returns The popup window, or `null` when the chain config is absent or the
 *   popup was blocked — the caller proceeds with the main chain either way
 *   (worst case matches the old behaviour: the idir-realm session survives).
 */
export function openIdirRealmLogoutPopup(config: AmplifyConfig): Window | null {
  const keycloakBase = config?.logoutKeycloakUrl?.trim();
  if (!keycloakBase) return null;

  const idirLogoutUrl = keycloakBase.replace('/realms/standard/', '/realms/idir/');
  if (idirLogoutUrl === keycloakBase) return null;

  // A truly invisible logout is not possible: the page refuses to render in
  // an iframe (frame-ancestors 'self') and fetch() cannot complete the
  // confirm POST (the one-time session_code is in an opaque cross-origin
  // response). Best available: the smallest popup browsers allow, tucked
  // into the bottom-right corner, with focus handed straight back to the
  // app. Browsers clamp size/position, so exact geometry varies.
  let popup: Window | null = null;
  try {
    const left = window.screenX + Math.max(0, window.outerWidth - 130);
    const top = window.screenY + Math.max(0, window.outerHeight - 100);
    popup = window.open(idirLogoutUrl, 'csp-idir-realm-logout', `popup,width=120,height=90,left=${left},top=${top}`);
  } catch {
    return null;
  }

  // Ask the browser to put the app's signing-out screen back in front while
  // the popup works. (Window.blur() on the popup would be the complement, but
  // it is deprecated and a no-op in modern browsers.) Best-effort — a failure
  // here must not be mistaken for a blocked popup.
  try {
    window.focus();
  } catch {
    // ignore
  }
  return popup;
}

/**
 * Removes Amplify's cached Cognito tokens from localStorage. The logout chain
 * drives its own redirects (Amplify's `signOut()` is bypassed), so tokens are
 * cleared locally to ensure that when the browser lands back on the app at the
 * end of the chain it bootstraps in a logged-out state. The final Cognito
 * `/logout` hop clears the Cognito session cookie server-side.
 */
export function clearStoredTokens(userPoolClientId: string | undefined): void {
  if (!userPoolClientId) {
    return;
  }

  const prefix = `CognitoIdentityServiceProvider.${userPoolClientId}`;
  const keysToRemove: string[] = [];
  for (let i = 0; i < window.localStorage.length; i++) {
    const key = window.localStorage.key(i);
    if (key?.startsWith(prefix)) {
      keysToRemove.push(key);
    }
  }
  keysToRemove.forEach((key) => window.localStorage.removeItem(key));
}
