# Security Policy

## Supported Versions

Only the current `main` branch, which is what runs in the **test** and **prod** OpenShift environments, gets security fixes. Older images and PR environments are not supported. PR environments are removed when the PR closes, or after a week.

## Reporting a Vulnerability

**DO NOT open public GitHub issues for security vulnerabilities.**

If you believe you have found a security vulnerability in this project, report it privately through GitHub's private vulnerability reporting:

1. Go to the repository's **Security** tab.
2. Click **Report a vulnerability** (under **Advisories** in the sidebar).
3. Fill in the advisory form. Only the repository maintainers can see it.

Please include the affected URL or component, steps to reproduce and the impact you expect. Do not access, modify or delete data that isn't yours while you investigate.

## Security Controls in This Repository

For maintainers, here is a summary of the automated checks and the design choices that matter for security.

**Automated scanning**

| Control | Where | When |
|---|---|---|
| Trivy (vulnerabilities, secrets, IaC misconfiguration; CRITICAL/HIGH, fixable only) | `.github/workflows/analysis.yml`. Results go to the GitHub Security tab | Every PR, every push to `main`, and weekly |
| SonarCloud (backend and frontend) | `.github/workflows/analysis.yml` | Every PR and push to `main` |
| Frontend dependency and supply-chain scan | `bcgov/action-test-and-analyse` (`dep_scan: warn`, `supply_scan: true`) | Every PR touching `frontend/` |
| OWASP ZAP full scan against the **test** environment | `.github/workflows/scheduled.yml`. It opens or updates a "ZAP Security Report" issue | Weekly (Saturdays) |
| gitleaks secret scanning | `.pre-commit-config.yaml` (local pre-commit hook) | On commit, once `pre-commit install` has been run |
| Dependency updates | Renovate (`renovate.json`, bcgov presets) | Continuous |

Accepted Trivy exceptions, each with its reason, are listed in `.github/.trivyignore`. Add a justification comment whenever you add one.

**Authentication and authorization**

- Users sign in with IDIR through AWS Cognito, federated by FAM. The frontend uses Amplify and sends the Cognito **ID token** as a Bearer token.
- The backend validates each JWT's signature against the Cognito JWKS endpoint, and checks issuer and audience when they are configured (`JWT_JWKS_URI`, `JWT_ISSUER`, `JWT_AUDIENCE`; always set both in OpenShift). Roles (`VIEW`, `APPROVE`, `ADMIN`) come from the `cognito:groups` claim. A group matches a role when it equals the role name or ends with `_<ROLE>`, e.g. `CSP_ADMIN`. Write endpoints are guarded with `@PreAuthorize` through `PermissionService`. Every other `/api/**` endpoint requires an authenticated user, except health and the OpenAPI docs.
- Signing out runs the full federated logout chain (SiteMinder → Keycloak → Cognito), so upstream sessions are cleared too, not just the Cognito session.
- Idle sessions are signed out after 30 minutes by default (`idleTimeoutMinutes`).
- **Mock auth is for local development only.** OpenShift deploys `AUTH_MOCK_ENABLED=false`, the generated `amplify-config.js` sets `mockUser: false`, and the frontend honours `mockUser` only when served from a local host. The backend `prod` profile also sets `auth.mock.enabled: false`, but the `AUTH_MOCK_ENABLED` environment variable overrides it, so never set it to `true` in a deployed environment.

**Transport and browser hardening**

- Oracle is reached over TCPS. An init container fetches the Oracle wallet, and the entrypoint merges it into the JVM trust store at startup.
- The backend has no Route. All `/api/*` traffic goes through the frontend's Caddy reverse proxy. NetworkPolicies (`common/openshift.init.yml`) allow backend ingress only from frontend pods in the same zone and from the monitoring namespace.
- Containers run as non-root, with a read-only root filesystem and all capabilities dropped. The service account token is not mounted.
- Caddy sets `Content-Security-Policy` (self-only, with Cognito allowed for `connect-src`, and `frame-ancestors 'none'`), `Strict-Transport-Security`, and `X-Frame-Options: DENY`. See `frontend/Caddyfile`.

**Secrets**

- Credentials live only in GitHub Actions secrets and OpenShift Secrets. They are never committed. `.env`, `frontend/public/amplify-config.js` and `backend/certs/*` are git-ignored.
- Local corporate proxy certificates (`backend/certs/`) exist only in local builds. They never reach CI/CD images.
