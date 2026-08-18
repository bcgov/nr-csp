declare global {
  var amplifyConfig:
    | {
        appEnv: string;
        idpName: string;
        region: string;
        userPoolId: string;
        userPoolClientId: string;
        cognitoDomain: string;
        oauthScopes?: string[];
        redirectSignIn: string;
        redirectSignOut: string;
        /** SiteMinder logoff.cgi base URL — first hop of the federated logout chain. */
        logoutSiteminderUrl?: string;
        /** Keycloak end-session endpoint — second hop of the federated logout chain. */
        logoutKeycloakUrl?: string;
        /** FAM's shared Keycloak (Cognito-as-OIDC-client) client id. */
        logoutKeycloakClientId?: string;
        mockUser?: boolean;
        famClientId?: string;
        /** Minutes of inactivity before the app signs the user out. Defaults to 30 (matches old CSP app behavior). */
        idleTimeoutMinutes?: number;
      }
    | undefined;
}

// Mock authentication bypasses Cognito entirely (auto-login as ADMIN), so it is
// permitted ONLY on a local machine. It requires BOTH the runtime opt-in
// (mockUser: true in amplify-config.js) AND the app actually being served from
// a local host.
const LOCAL_HOSTNAMES = new Set(['localhost', '127.0.0.1', '0.0.0.0', '::1', '[::1]']);
const isLocalHost =
  globalThis.window !== undefined &&
  (LOCAL_HOSTNAMES.has(globalThis.window.location.hostname) ||
    globalThis.window.location.hostname.endsWith('.localhost'));

export const env = {
  mockUser: globalThis.amplifyConfig?.mockUser === true && isLocalHost,
  appEnv: globalThis.amplifyConfig?.appEnv ?? 'dev',
  isDevelopment: import.meta.env.DEV,
  isProduction: import.meta.env.PROD,
  idleTimeoutMs: (globalThis.amplifyConfig?.idleTimeoutMinutes ?? 30) * 60_000,
} as const;
