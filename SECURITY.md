# Security Policy

## Supported Versions

Only the current `main` branch — what runs in the **test** and **prod** OpenShift environments
— receives security fixes. Older images and PR environments are not supported. PR environments
are removed when the PR closes, or purged after a week by `scheduled.yml`.

## Reporting a Vulnerability

**DO NOT open public GitHub issues for security vulnerabilities.**

Report privately through GitHub's private vulnerability reporting:

1. Go to the repository's **Security** tab.
2. Under **Advisories** in the sidebar, click **Report a vulnerability**.
3. Fill in the advisory form. Only the repository maintainers can see it.

Please include the affected URL or component, steps to reproduce, and the impact you expect.
While investigating, do not access, modify, or delete data that isn't yours.

## Security Controls

A summary for maintainers of the automated checks and the design decisions that carry security
weight. See [ARCHITECTURE.md](ARCHITECTURE.md) for the full system design.

### Automated scanning

| Control | Where | When |
|---|---|---|
| Trivy — vulnerabilities, secrets, and IaC misconfiguration (`CRITICAL,HIGH`, fixable only) | `analysis.yml`; results upload to the GitHub Security tab as SARIF | Every PR, every push to `main`, and weekly on Sundays |
| SonarCloud, backend and frontend | `analysis.yml` | Every PR and push to `main` |
| Frontend dependency and supply-chain scan | `bcgov/action-test-and-analyse` | Every PR touching `frontend/` |
| OWASP ZAP full scan against **test** | `scheduled.yml` | Weekly, Saturdays (`0 11 * * 6`) |
| gitleaks secret scanning | `.pre-commit-config.yaml` | On commit, once `pre-commit install` has been run |
| Dependency updates | Renovate (`renovate.json`, bcgov shared presets) | Continuous |

Accepted Trivy exceptions live in `.github/.trivyignore`. Add a justification comment whenever
you add one.

### Authentication and authorization

- Users sign in with IDIR through AWS Cognito, federated by FAM. The frontend uses Amplify and
  sends the Cognito **ID token** as a bearer token on every `/api` call.
- The backend validates each JWT's signature against the Cognito JWKS endpoint (key selected by
  `kid`), and checks issuer and audience when configured — `JWT_JWKS_URI`, `JWT_ISSUER`,
  `JWT_AUDIENCE`. **Always set all three in a deployed environment.**
- Roles (`VIEW`, `APPROVE`, `ADMIN`) come from the `cognito:groups` claim. A group grants a role
  when it equals the role name or ends with `_<ROLE>` — so `CSP_ADMIN` grants `ADMIN`. Note this
  is a **suffix match**, so any group ending `_ADMIN` from any issuer in the pool would also
  grant it.
- Write endpoints are guarded with `@PreAuthorize` through `PermissionService`. Every other
  `/api/**` endpoint requires an authenticated user, except `/api/health` and the OpenAPI docs,
  which are public. Read, report, search, and lookup endpoints are **authentication-only** —
  they do not check a role.
- Sessions are stateless: no cookie, no server-side session. CSRF protection is disabled
  deliberately, which is correct for bearer-token auth with no browser-automatable credential.
- Signing out runs the full federated chain (SiteMinder → Keycloak → Cognito), clearing the
  upstream IDIR sessions rather than only the Cognito one.
- Idle sessions sign out after `idleTimeoutMinutes` (default 30).

### Mock authentication — local development only

Mock auth bypasses Cognito entirely and authenticates every request as `local-dev-user` with
the roles in `AUTH_MOCK_ROLES`. Four independent things keep it out of deployed environments:

1. `MockRequestFilter` is annotated `@ConditionalOnProperty(name = "auth.mock.enabled",
   havingValue = "true")`, so the filter bean does not exist unless explicitly enabled.
2. The backend `prod` profile sets `auth.mock.enabled: false`.
3. OpenShift deploys `AUTH_MOCK_ENABLED=false`, and the generated `amplify-config.js` sets
   `mockUser: false`.
4. The frontend honours `mockUser` only when served from a local hostname (`localhost`,
   `127.0.0.1`, `0.0.0.0`, `::1`, or `*.localhost`), re-checked at runtime in `env.ts`.

> **The environment variable overrides the profile.** `AUTH_MOCK_ENABLED=true` will enable mock
> auth even under the `prod` profile, because environment variables outrank profile YAML in
> Spring Boot. Never set it to `true` in a deployed environment.

### Transport and network

- Oracle is reached over **TCPS**. An init container fetches the Oracle wallet into a shared
  volume and the entrypoint merges it into the JVM trust store at startup.
- The backend has **no Route**. All `/api/*` traffic goes through the frontend's Caddy reverse
  proxy, on the same origin as the SPA — which is also why no CORS configuration exists.
- NetworkPolicies (`common/openshift.init.yml`) permit backend ingress only from frontend pods
  **in the same zone** and from the cluster monitoring namespace; frontend ingress only from the
  OpenShift router.
- TLS terminates at the OpenShift edge router, so the backend only ever sees plain HTTP and its
  Spring HSTS configuration never fires. HSTS is set at the Caddy layer instead.

### Container hardening

- Containers run as non-root with `readOnlyRootFilesystem: true`, all capabilities dropped
  (`drop: ["ALL"]`), and `automountServiceAccountToken: false`.
- The frontend image strips `CAP_NET_BIND_SERVICE` from the Caddy binary — OpenShift's
  restricted SCC refuses to exec a binary carrying file capabilities when
  `allowPrivilegeEscalation=false`, and the app listens on 3000 so the capability is unneeded.

### Browser hardening

Caddy sets the security headers for both static responses and proxied `/api/*` responses
(`frontend/Caddyfile`):

- `Content-Security-Policy` — `default-src 'self'`, with the Cognito hosted-UI domain and the
  regional `cognito-idp` endpoint allowed for `connect-src` (Amplify's silent token refresh
  needs the latter), `frame-ancestors 'none'`, `object-src 'none'`.
- `Strict-Transport-Security` — one year, `includeSubDomains`.
- `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: strict-origin-when-cross-origin`.

The router's HAProxy balancing cookie is disabled
(`haproxy.router.openshift.io/disable_cookies`) — the frontend is stateless and needs no
session affinity, and the cookie was otherwise flagged by scans as `SameSite=None`.

### Secrets

- Credentials live only in GitHub Actions secrets and OpenShift Secrets, and are never
  committed. `.env`, `frontend/public/amplify-config.js`, and `backend/certs/*` are git-ignored.
- Local corporate-proxy certificates in `backend/certs/` exist only in local builds and never
  reach CI/CD images.
- `frontend/public/amplify-config.example.js` is committed, but holds only **public client
  identifiers** — the same values the deployed SPA serves to every browser. No secret belongs
  in that file.
