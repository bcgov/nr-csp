# NRS CSP App
A starting point for NR application teams. Spring Boot 4 backend, React 19 frontend, Oracle DB.

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
- VPN (Cisco Secure Client) — required to reach the DB and Jasper server
- `hosts` file entries for the DB and Jasper server — ask your team for the values

## Running locally with Docker Compose

**1. Configure your environment**

```bash
cp .env.example .env
```

Open `.env` and fill in the required values:

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | Oracle JDBC URL (TCPS descriptor form — see `.env.example`) |
| `SPRING_DATASOURCE_USERNAME` | Oracle username |
| `SPRING_DATASOURCE_PASSWORD` | Oracle password |
| `JASPER_SERVER_LOGIN_URL` | Jasper server login URL (ask the team) |
| `JASPER_SERVER_FETCH_URL` | Jasper server fetch URL |
| `JASPER_SERVER_PUT_URL` | Jasper server put URL |
| `JASPER_SERVER_REPORT_URI_BASE` | Jasper report URI base path |
| `JASPER_SERVER_USERNAME` | Jasper server username |
| `JASPER_SERVER_PASSWORD` | Jasper server password |
| `AUTH_MOCK_ENABLED` | Set to `true` for local development — enables backend mock auth (see [Authentication](#authentication)) |

> Use single quotes if a value contains special characters: `PASSWORD='p@ss!'`

`.env.example` defaults to `SPRING_PROFILES_ACTIVE=prod`, which mirrors OpenShift: all of the variables above are **required** (the Jasper URLs have no defaults). If you don't have DB or Jasper credentials yet, set `SPRING_PROFILES_ACTIVE=local` instead — the `local` profile boots with empty fallbacks for Jasper/Cognito, mock auth enabled, and DEBUG logging, so the app starts without any integrations configured.

**2. Connect VPN, then start**

By default, `docker compose up` runs the frontend as a **Vite dev server** with hot-module reloading (via `docker-compose.override.yml`). The source tree is bind-mounted so edits on the host reload live.

```bash
# Docker
docker compose up --build

# Podman
podman compose up --build
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
| Frontend | `"mockUser": true` in `frontend/public/amplify-config.js` (only honoured when served from localhost) | on |
| Backend | `AUTH_MOCK_ENABLED=true` in `.env` | on with the `local` profile; **off** with the `prod` profile — you must set it explicitly |

With the backend half off (and no JWT vars configured), the frontend will show a fake logged-in user but every API call will be rejected with 401. The mock user's roles are controlled by `AUTH_MOCK_ROLES` (default `ADMIN`).

**Running with real Cognito auth locally**

1. Set `AUTH_MOCK_ENABLED=false` in `.env` and fill in the JWT vars.
2. Edit `frontend/public/amplify-config.js`, set `"mockUser": false`, and replace the `REPLACE_ME` placeholders with your Cognito values:

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
  "mockUser": false,
  "famClientId": "CSP"
};
```

> In OpenShift, Cognito config is injected at runtime via a ConfigMap — the `amplify-config.js` placeholder file in the image is overwritten by a volume mount. No rebuild is required when Cognito values change.

## Corporate certificates (Zscaler / proxy) — local dev only

If your machine routes traffic through a corporate TLS proxy (e.g. Zscaler), the JVM inside the backend container needs to trust the proxy's root certificate or outbound HTTPS calls will fail with `SSLHandshakeException`.

This is a **local development concern only.** Certificate files are git-ignored (`/backend/certs/*`), so they are never committed and never present in CI/CD builds.

**How it works**

The backend Dockerfile has a dedicated build stage that imports every certificate file found in `backend/certs/` into the JRE's trust store before the final image is assembled. Supported extensions: `.cer`, `.pem`, `.crt`. Empty or missing files are silently skipped.

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
| PR opened | Images built, pushed to GHCR, deployed to a PR-specific environment |
| Merged to `main` | Images deployed to **test**, integration tests run, then deployed to **prod** |
| After prod deploy | Sysdig monitoring alerts synced; images tagged `prod` in GHCR |

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
| `JASPER_SERVER_LOGIN_URL` | Jasper server login URL |
| `JASPER_SERVER_FETCH_URL` | Jasper server fetch URL |
| `JASPER_SERVER_PUT_URL` | Jasper server put URL |
| `JASPER_SERVER_REPORT_URI_BASE` | Jasper report URI base path |
| `JASPER_SERVER_USERNAME` | Jasper server username |
| `JASPER_SERVER_PASSWORD` | Jasper server password |
| `OC_NAMESPACE` | OpenShift namespace to deploy into |
| `OC_TOKEN` | OpenShift service account token (repo-level fallback) |
| `SONAR_TOKEN_BACKEND` | SonarCloud token for backend analysis |
| `SONAR_TOKEN_FRONTEND` | SonarCloud token for frontend analysis |
| `SYSDIG_API_TOKEN` | Sysdig monitoring token |

**Environment secrets** (scoped to `test` and `prod` — override the repo-level `OC_TOKEN`)

| Secret | Description |
|---|---|
| `OC_TOKEN` | Environment-specific OpenShift service account token |

**Repository variables**

| Variable | Description |
|---|---|
| `OC_SERVER` | OpenShift API server URL |
| `COGNITO_REGION` | AWS region for Cognito (e.g. `ca-central-1`) |
| `COGNITO_USER_POOL_ID` | Cognito User Pool ID |
| `COGNITO_USER_POOL_CLIENT_ID` | Cognito App Client ID |
| `COGNITO_DOMAIN` | Cognito hosted UI domain |
| `COGNITO_OAUTH_SCOPES` | OAuth scopes, comma-separated (e.g. `openid,profile,email`) |
| `APP_ENV` | Application environment label injected into the frontend config (`dev`, `test`, `prod`) |
| `COGNITO_IDP_NAME` | Cognito identity provider name (`DEV-IDIR`, `TEST-IDIR`, `PROD-IDIR`) |
| `FAM_CLIENT_ID` | FAM application client ID for role-group mapping (injected at runtime via `amplify-config.js`) |
