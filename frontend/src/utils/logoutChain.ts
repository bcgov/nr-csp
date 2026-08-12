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
