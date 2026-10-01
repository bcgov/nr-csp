// Template for frontend/public/amplify-config.js.
//
//   cp frontend/public/amplify-config.example.js frontend/public/amplify-config.js
//
// The real file is git-ignored (frontend/.gitignore) because it holds
// environment-specific values. Nothing here is baked into the build: the app
// reads window.amplifyConfig at runtime (see frontend/src/env.ts), so changing
// a value needs only a page reload locally, and in OpenShift the file is
// replaced wholesale by a ConfigMap mount (common/openshift.init.yml).
//
// As shipped, this template is set up for LOCAL MOCK AUTH — no Cognito login.
// Pair it with AUTH_MOCK_ENABLED=true in .env; see "Authentication" in README.md.

window.amplifyConfig = {
  // Environment label shown in the UI header: "development" | "test" | "prod".
  appEnv: 'development',

  // ── Cognito / FAM ───────────────────────────────────────────────────────
  // Only needed when mockUser is false. Values come from the FAM team / DevOps
  // and must match the FAM application registration for your environment.
  idpName: 'DEV-IDIR',
  region: 'ca-central-1',
  userPoolId: 'REPLACE_ME',
  userPoolClientId: 'REPLACE_ME',
  cognitoDomain: 'REPLACE_ME.auth.ca-central-1.amazoncognito.com',
  oauthScopes: ['openid', 'profile', 'email'],
  redirectSignIn: 'http://localhost:3000/',
  redirectSignOut: 'http://localhost:3000/logout',

  // ── Federated sign-out chain ────────────────────────────────────────────
  // Drives SiteMinder -> Keycloak -> Cognito logout (frontend/src/utils/logoutChain.ts).
  // If these are omitted the app falls back to a Cognito-only Amplify sign-out,
  // which leaves the upstream IDIR sessions alive. Test/prod use the
  // test.loginproxy / loginproxy and logon7 hosts — see the LOGOUT_* GitHub variables.
  logoutSiteminderUrl: 'https://logontest7.gov.bc.ca/clp-cgi/logoff.cgi',
  logoutKeycloakUrl:
    'https://dev.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/logout',
  logoutKeycloakClientId: 'REPLACE_ME',

  // ── Local mock auth ─────────────────────────────────────────────────────
  // true bypasses Cognito/FAM entirely and signs in a fake user. Honoured ONLY
  // when the app is served from localhost (env.ts re-checks the hostname), so
  // shipping true here cannot weaken a deployed environment.
  // This is only half of mock mode — the backend half is AUTH_MOCK_ENABLED in .env.
  mockUser: true,

  // Cognito group prefix used to map groups to CSP roles, e.g. famClientId "CSP"
  // means group CSP_ADMIN grants role ADMIN. Roles are ADMIN, APPROVE, VIEW.
  famClientId: 'CSP',

  // Optional. Minutes of inactivity before the idle-timeout watcher signs the
  // user out; defaults to 30 when omitted. Ignored in mock mode.
  // idleTimeoutMinutes: 30,
};
