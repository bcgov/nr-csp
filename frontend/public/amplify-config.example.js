// Template for frontend/public/amplify-config.js (git-ignored, required to boot).
//
//   cp frontend/public/amplify-config.example.js frontend/public/amplify-config.js
//
// This mirrors what a deployed environment actually serves — the ConfigMap in
// common/openshift.init.yml renders exactly this shape, which is why oauthScopes
// is a .split(',') rather than a literal array. You can equally grab the live
// file from a deployment and edit it; see "Authentication" in README.md.
//
// Values below are from TEST. They are public client identifiers — the deployed
// SPA serves this file to every browser — not secrets.
//
// Two edits turn a deployment copy into a local one:
//   1. point redirectSignIn / redirectSignOut at localhost (already done here)
//   2. add "mockUser": true to skip Cognito entirely (already done here)

window.amplifyConfig = {
  "appEnv": "test",
  "idpName": "TEST-IDIR",
  "region": "ca-central-1",
  "userPoolId": "ca-central-1_UpeAqsYt4",
  "userPoolClientId": "5ndpemk6sp9u4kb117nsdt3jjh",
  "cognitoDomain": "lza-prod-fam-user-pool-domain.auth.ca-central-1.amazoncognito.com",
  "oauthScopes": "openid,profile,email".split(','),
  "redirectSignIn": "http://localhost:3000/",
  "redirectSignOut": "http://localhost:3000/logout",
  "logoutSiteminderUrl": "https://logontest7.gov.bc.ca/clp-cgi/logoff.cgi",
  "logoutKeycloakUrl": "https://test.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/logout",
  "logoutKeycloakClientId": "fsa-cognito-idir-dev-4088",
  "famClientId": "CSP",

  // Local-only: bypasses Cognito/FAM and signs in a fake user. Read as
  // `mockUser === true` AND only honoured when served from localhost
  // (frontend/src/env.ts), so it cannot weaken a deployed environment.
  // Deployments omit it / set it false. Pair with AUTH_MOCK_ENABLED=true in .env.
  // Set to false to exercise the real Cognito sign-in flow locally.
  "mockUser": true

  // Optional: "idleTimeoutMinutes": 30 — inactivity before auto sign-out.
};
