# NRS CSP App
The **Coast Selling Price (CSP) System** for the BC Ministry of Forests. Users submit, review and approve coastal log sale invoices (entered manually or uploaded as XML), maintain the sort-code and flat-price-conversion reference tables, and run the R06–R13 reports.

Spring Boot 4 (Java 21) backend, React 19 + Vite + Carbon frontend, Oracle DB, deployed to OpenShift Gold. See [ARCHITECTURE.md](ARCHITECTURE.md) for how the pieces fit together.

**Frontend**
***
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=alert_status&style=flat)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=security_rating)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=coverage)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=bugs)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=code_smells)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=duplicated_lines_density)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=sqale_index)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_frontend&metric=ncloc)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_frontend)

**Backend**
***
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=alert_status)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=security_rating)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=coverage)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=bugs)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=code_smells)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=duplicated_lines_density)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=sqale_index)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=bcgov-sonarcloud_nr-csp_backend&metric=ncloc)](https://sonarcloud.io/summary/overall?id=bcgov-sonarcloud_nr-csp_backend)


## Prerequisites

- Docker Compose or Podman Compose
- VPN (Cisco Secure Client) — required to reach the DB
- `hosts` file entries for the DB — ask your team for the values

## Running locally with Docker Compose

**1. Configure your environment**

```bash
cp .env.example .env
```

Open `.env` and fill in the required values:

| Variable | Description |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (default in `.env.example`) or `local` — see below |
| `SPRING_DATASOURCE_URL` | Oracle JDBC URL (TCPS descriptor form — see `.env.example`) |
| `SPRING_DATASOURCE_USERNAME` | Oracle username |
| `SPRING_DATASOURCE_PASSWORD` | Oracle password |
| `JWT_JWKS_URI` | Cognito JWKS endpoint. **Must be a well-formed URL even in mock mode** — the backend parses it at startup and fails to boot on an empty value. With mock auth, any placeholder such as `https://mock-auth.invalid/.well-known/jwks.json` works |
| `JWT_ISSUER` / `JWT_AUDIENCE` | Expected JWT issuer and audience (Cognito user pool / app client). Only needed for real Cognito auth |
| `AUTH_MOCK_ENABLED` | Set to `true` for local development — enables backend mock auth (see [Authentication](#authentication)) |
| `AUTH_MOCK_ROLES` | Roles granted to the mock user (default `ADMIN`) |
| `JAVA_OPTS` | JVM flags for the backend container (defaults are fine) |
| `COGNITO_DOMAIN` | Optional. Only feeds the Caddy `Content-Security-Policy` header when running the production image; leave unset for the dev server (compose will warn that it is unset) |

> Use single quotes if a value contains special characters: `PASSWORD='p@ss!'`

`.env.example` defaults to `SPRING_PROFILES_ACTIVE=prod`, which mirrors OpenShift. Setting `SPRING_PROFILES_ACTIVE=local` instead turns on mock auth by default and DEBUG logging for `ca.bc.gov.nrs.csp` and Spring Security. Neither profile starts without integrations:

- **Database.** The backend runs `SELECT 1 FROM DUAL` at startup (`ValidatingDataSource`) and refuses to start if the DB is unreachable. You need VPN and valid DB credentials if connecting to dev db, whichever profile you use.
- **JWKS URI.** `JWT_JWKS_URI` must be a syntactically valid URL (see the table above).

The frontend container waits for the backend health check, so if the backend fails to start, the frontend never comes up either.

**2. Create the frontend runtime config**

The frontend reads all environment config from `frontend/public/amplify-config.js` at runtime. This file is **git-ignored and not in the repo**, so you have to create it. Without it the app crashes on load with `window.amplifyConfig not found`. For mock mode, this is enough:

```javascript
window.amplifyConfig = {
  "appEnv": "dev",
  "mockUser": true,
  "famClientId": "CSP"
};
```

See [Authentication](#authentication) for the full set of keys needed for real Cognito auth.

**3. Connect VPN, then start**

By default, `docker compose up` runs the frontend as a **Vite dev server** with hot-module reloading (via `docker-compose.override.yml`). The source tree is bind-mounted so edits on the host reload live.

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend (dev server) | http://localhost:3000 |
| Swagger UI | http://localhost:3000/api/swagger-ui/index.html |

> The backend is not exposed directly. All `/api/*` traffic is proxied through the frontend container to the backend.

**Running the production image locally**

To run the production Caddy build (static files, no HMR) instead of the dev server:

```bash
docker compose -f docker-compose.yml up --build
```

**Common commands**

```bash
# Stop and remove containers
docker compose down

# View logs (all services)
docker compose logs -f

# View logs for one service
docker compose logs -f backend

# Rebuild a single service
docker compose up --build backend

# Stop without removing containers
docker compose stop
```

## Authentication

For local development, use **mock mode** — no Cognito login required. Mock mode has two halves, and both must be on:

| Half | Switch | Default |
|---|---|---|
| Frontend | `"mockUser": true` in `frontend/public/amplify-config.js` (only honoured when served from `localhost`, `127.0.0.1`, `0.0.0.0`, `::1` or `*.localhost`) | none — you create the file yourself (see step 2 above) |
| Backend | `AUTH_MOCK_ENABLED=true` in `.env` | on with the `local` profile; **off** with the `prod` profile — you must set it explicitly |

If only the frontend half is on, the frontend shows a fake logged-in user but the backend rejects every API call with 401. The mock user (`local-dev-user`) gets the roles in `AUTH_MOCK_ROLES` (default `ADMIN`).

**Running with real Cognito auth locally**

1. Set `AUTH_MOCK_ENABLED=false` in `.env` and fill in the JWT vars.
2. In `frontend/public/amplify-config.js`, set `"mockUser": false` and fill in your Cognito values (get them from the FAM team or DevOps):

```javascript
window.amplifyConfig = {
  "appEnv": "dev",
  "idpName": "DEV-IDIR",
  "region": "ca-central-1",
  "userPoolId": "ca-central-1_XXXXXXXXX",
  "userPoolClientId": "XXXXXXXXXXXXXXXXXXXXXXXXXX",
  "cognitoDomain": "nrs-XXXX.auth.ca-central-1.amazoncognito.com",
  "oauthScopes": ["openid", "profile", "email"],
  "redirectSignIn": "http://localhost:3000/",
  "redirectSignOut": "http://localhost:3000/logout",
  "logoutSiteminderUrl": "https://logontest7.gov.bc.ca/clp-cgi/logoff.cgi",
  "logoutKeycloakUrl": "https://dev.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/logout",
  "logoutKeycloakClientId": "fsa-cognito-idir-dev-4088",
  "mockUser": false,
  "famClientId": "CSP",
  "idleTimeoutMinutes": 30
};
```

`famClientId` identifies the FAM application. The app has three roles, `VIEW`, `APPROVE` and `ADMIN`. A role is granted when a Cognito group in the ID token equals the role name or ends with `_<ROLE>`, e.g. `CSP_ADMIN`. The backend and the frontend apply the same rule. `idleTimeoutMinutes` is optional; after that many minutes of inactivity the app signs the user out (default 30). In dev mode, `redirectSignIn` is replaced with the current origin.

> **How sign-out works.** Signing out drives a federated logout chain — SiteMinder → Keycloak → Cognito → back to the app at `/logout`, which immediately redirects to the welcome screen — built at runtime from the `logout*` values above (see `frontend/src/utils/logoutChain.ts`; same pattern as FAM's own console). This clears all three upstream sessions, not just Cognito's. Two registration constraints, both owned by FAM (`oidc_clients_csp.tf` in [nr-forests-access-management](https://github.com/bcgov/nr-forests-access-management)): `redirectSignOut` must be registered verbatim as a Cognito sign-out URL, and the Cognito domain must be on the shared Keycloak client's post-logout allow-list. If the `logout*` values are missing, the app falls back to a Cognito-only Amplify sign-out. Test/prod use `test.loginproxy`/`loginproxy` and (prod) `logon7` hosts — see the `LOGOUT_*` GitHub variables. Sign-out also briefly opens a small popup against loginproxy's `idir` broker realm: that realm holds a fourth session the chain cannot reach (its logout endpoint rejects redirect chaining and its pages are not frameable), and without clearing it the next sign-in silently logs the user back in without prompting for credentials.

> In OpenShift, Cognito config is injected at runtime via a ConfigMap — the `amplify-config.js` placeholder file in the image is overwritten by a volume mount. No rebuild is required when Cognito values change.

## Corporate certificates (Zscaler / proxy) — local dev only

If your machine routes traffic through a corporate TLS proxy (e.g. Zscaler), the JVM inside the backend container needs to trust the proxy's root certificate or outbound HTTPS calls will fail with `SSLHandshakeException`.

This is a **local development concern only.** Certificate files are git-ignored (`/backend/certs/*`), so they are never committed and never present in CI/CD builds.

**How it works**

The backend Dockerfile has a dedicated build stage that imports every certificate file found in `backend/certs/` into the JRE's trust store before the final image is assembled. Supported extensions: `.cer`, `.pem`, `.crt`. Empty or missing files are silently skipped.

The Maven build stage imports only `*.pem` files. If Maven dependency downloads fail behind the proxy, provide the certificate as a `.pem`.

**Adding a certificate**

1. Export the corporate root certificate from your browser or certificate manager (DER/PEM format).
2. Drop the file into `backend/certs/` — e.g. `backend/certs/zscaler.cer`.
3. Rebuild the backend image:

```bash
docker compose up --build backend
```

The filename (without extension) becomes the alias in the trust store. Multiple certificates are supported — add one file per certificate.

> Only the `.gitkeep` placeholder is tracked in git. Do **not** force-add cert files to git.

## OpenShift deployment

Deployments to OpenShift are fully automated through GitHub Actions — no manual steps are needed once the secrets and variables are configured.

**Pipeline overview**

| Event | What happens |
|---|---|
| PR opened (`pr-open.yml`) | Images built, pushed to GHCR and deployed to a PR-specific environment (`nr-csp-<PR# mod 50>`, one replica, no HPA/PDB). Integration tests then run against it |
| Every PR and push (`analysis.yml`) | Backend unit tests + JaCoCo, frontend lint + Vitest coverage, SonarCloud, and a Trivy scan (vulnerabilities, secrets, misconfiguration). The `Analysis Results` job is the merge gate |
| Merged to `main` (`merge.yml`) | The PR's images (not rebuilt) are deployed to **test**, integration tests run, then the same images go to **prod** |
| After prod deploy | Sysdig monitoring alerts synced from `monitoring/alerts/`; images tagged `prod` in GHCR |
| PR closed (`pr-close.yml`) | PR environment removed |
| Saturdays (`scheduled.yml`) | Stale issues and PRs closed, PR environments older than a week purged, SchemaSpy published, and a ZAP full scan run against **test** |

**Oracle init container**

The backend pod uses an init container (`ghcr.io/bcgov/nr-forest-client/common:prod`) that runs before the app starts. It connects to the Oracle host on port 1543 (TCPS) using `KEYSTORE_SECRET` to fetch and write an Oracle wallet into a shared volume (`/cert`). The main container mounts this volume and the entrypoint script merges the wallet cert into the JVM trust store at startup.

**GitHub Secrets & Variables**

The following secrets and variables must be configured on the repository for the pipeline to work.

**Repository secrets**

| Secret | Description |
|---|---|
| `DATABASE_HOST` | Oracle DB hostname |
| `DATABASE_SERVICE_NAME` | Oracle service name |
| `SPRING_DATASOURCE_URL` | Full JDBC URL (TCPS descriptor form) |
| `SPRING_DATASOURCE_USERNAME` | Oracle username |
| `SPRING_DATASOURCE_PASSWORD` | Oracle password |
| `KEYSTORE_SECRET` | Used by the init container to authenticate against the Oracle keystore |
| `JWT_JWKS_URI` | Cognito JWKS endpoint URL |
| `JWT_ISSUER` | JWT issuer claim |
| `JWT_AUDIENCE` | JWT audience claim |
| `OC_NAMESPACE` | OpenShift namespace to deploy into |
| `OC_TOKEN` | OpenShift service account token (repo-level fallback) |
| `SONAR_TOKEN_BACKEND` | SonarCloud token for backend analysis |
| `SONAR_TOKEN_FRONTEND` | SonarCloud token for frontend analysis |
| `SYSDIG_API_TOKEN` | Sysdig monitoring token |

**Environment secrets** (scoped to `test` and `prod` — override the repo-level `OC_TOKEN`)

| Secret | Description |
|---|---|
| `OC_TOKEN` | Environment-specific OpenShift service account token |

**Variables**

Variables marked *per-env* differ between environments. Set them as environment variables on `test` and `prod`, with a repo-level value as the fallback for PR deployments. The others are repo-level only. Variables used in the deploy job matrix, such as `COGNITO_DOMAIN` and `COGNITO_REGION`, resolve at repo level, because GitHub evaluates the matrix before the environment is attached.

| Variable | Scope | Description |
|---|---|---|
| `OC_SERVER` | repo | OpenShift API server URL |
| `OPENSHIFT_APPS_DOMAIN` | repo | Optional. Apps domain for routes and test targets (default `apps.gold.devops.gov.bc.ca`) |
| `COGNITO_REGION` | repo | AWS region for Cognito (e.g. `ca-central-1`) |
| `COGNITO_USER_POOL_ID` | repo | Cognito User Pool ID |
| `COGNITO_USER_POOL_CLIENT_ID` | repo | Cognito App Client ID |
| `COGNITO_DOMAIN` | repo | Cognito hosted UI domain (also used in the Caddy CSP header) |
| `COGNITO_OAUTH_SCOPES` | repo | OAuth scopes, comma-separated (e.g. `openid,profile,email`) |
| `FAM_CLIENT_ID` | repo | FAM application client ID for role-group mapping (injected at runtime via `amplify-config.js`) |
| `APP_ENV` | per-env | Application environment label injected into the frontend config (`dev`, `test`, `prod`) |
| `COGNITO_IDP_NAME` | per-env | Cognito identity provider name (`DEV-IDIR`, `TEST-IDIR`, `PROD-IDIR`) |
| `LOGOUT_SITEMINDER_URL` | per-env | SiteMinder `logoff.cgi` URL, the first hop of the sign-out chain |
| `LOGOUT_KEYCLOAK_URL` | per-env | Keycloak end-session endpoint, the second hop of the sign-out chain |
| `LOGOUT_KEYCLOAK_CLIENT_ID` | per-env | FAM's shared Keycloak client ID |
| `FRONTEND_LOG_LEVEL` | per-env | Optional. Frontend log level (default `INFO`) |
| `BACKEND_LOG_LEVEL`, `BACKEND_ROOT_LOG_LEVEL`, `BACKEND_SPRING_LOG_LEVEL`, `BACKEND_SPRING_SECURITY_LOG_LEVEL` | repo (matrix) | Optional. Backend log levels (default `INFO`, `INFO`, `INFO`, `WARN`) |

> `JWT_JWKS_URI` is declared optional in `reusable-deploy.yml`, but the backend will not start without it.

## Testing

| What | Command | Notes |
|---|---|---|
| Backend unit tests | `cd backend && mvn -DskipITs verify` | JUnit + JaCoCo coverage |
| Backend integration tests | `cd backend && mvn test-compile failsafe:integration-test failsafe:verify` | `*IT.java`, Oracle via Testcontainers (needs Docker) |
| Frontend lint / unit tests | `cd frontend && npm run lint && npm run test:unit` | `npm run test:coverage` for coverage; `npm run test:browser` for browser-mode tests |
| Frontend E2E | `cd frontend && npm run test:e2e` | Playwright BDD suite. See [frontend/e2e/README.md](frontend/e2e/README.md) for setup |

A [gitleaks](https://github.com/gitleaks/gitleaks) pre-commit hook is configured in `.pre-commit-config.yaml`. Run `pre-commit install` to enable it.

## Contributing notes

- The root `.gitignore` ignores `.github/` and every `*.md` file except `README.md`. New workflow files or documentation, including `ARCHITECTURE.md`, must be added with `git add -f`. Files that are already tracked are unaffected.
- See [ARCHITECTURE.md](ARCHITECTURE.md) for how the pieces fit together, and [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.
