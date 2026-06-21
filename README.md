# NRS CSP App

A starting point for NR application teams. Spring Boot 4 backend, React 19 frontend, Oracle DB.

## Prerequisites

- Docker Compose or Podman Compose
- VPN (Cisco Secure Client) — required to reach the DB and Jasper server
- `hosts` file entries for the DB and Jasper server — ask your team for the values

## Getting started

**1. Configure your environment**

```bash
cp .env.example .env
```

Open `.env` and fill in the required values:
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — DB credentials
- `JASPER_SERVER_USERNAME`, `JASPER_SERVER_PASSWORD` — Jasper server credentials
- `JASPER_IP` — IP address for the Jasper server from your hosts file

> Use single quotes if a value contains special characters: `PASSWORD='p@ss!'`

**2. Connect VPN, then start**

```bash
# Docker
docker compose up --build

# Podman
podman compose up --build
```

| Service  | URL                                          |
|----------|----------------------------------------------|
| Frontend | http://localhost:3000                        |
| Swagger  | http://localhost:3000/api/swagger-ui/index.html |

> The backend is not exposed directly. All `/api/*` traffic is proxied through Caddy (the frontend container) to the backend.

**Common commands**

```bash
# Stop and remove containers
docker compose down          # or: podman compose down

# View logs (all services)
docker compose logs -f       # or: podman compose logs -f

# View logs for a specific service
docker compose logs -f backend   # or: podman compose logs -f backend

# Rebuild a single service without restarting others
docker compose up --build backend   # or: podman compose up --build backend

# Stop without removing containers
docker compose stop          # or: podman compose stop
```


## Corporate certificates (Zscaler / proxy) — local dev only

If your machine routes traffic through a corporate TLS proxy (e.g. Zscaler), the JVM inside the backend container needs to trust the proxy's root certificate or outbound HTTPS calls will fail with `SSLHandshakeException`.

This is a **local development concern only.** Certificate files are git-ignored (`/backend/certs/*` in `.gitignore`), so they are never committed and never present in CI/CD builds. In OpenShift the images are built by GitHub Actions on clean runners where no corporate proxy intercepts traffic, so no extra certificates are needed.

**How it works**

The backend Dockerfile has a dedicated build stage that imports every certificate file found in `backend/certs/` into the JRE's trust store before the final image is assembled. Supported extensions: `.cer`, `.pem`, `.crt`. Empty or missing files are silently skipped — on a fresh clone the directory contains only `.gitkeep` and the import step is a no-op.

**Adding a certificate (local only)**

1. Export the corporate root certificate from your browser or certificate manager (DER/PEM format).
2. Drop the file into `backend/certs/` — e.g. `backend/certs/zscaler.cer`.
3. Rebuild the backend image:

```bash
docker compose up --build backend   # or: podman compose up --build backend
```

The filename (without extension) becomes the alias in the trust store. Multiple certificates are supported — add one file per certificate.

> Only the `.gitkeep` placeholder is tracked in git, which keeps the directory present on a fresh clone so the Docker bind mount never fails. Do **not** force-add cert files to git.

## OpenShift deployment

Deployments to OpenShift are fully automated through GitHub Actions — no manual steps are needed once the namespace secrets are configured.

**Pipeline overview**

| Event | What happens |
|---|---|
| PR opened | Images built, pushed to GHCR, deployed to a PR-specific environment |
| Merged to `main` | Images deployed to **test**, integration tests run, then deployed to **prod** |
| After prod deploy | Sysdig monitoring alerts synced; images tagged `prod` in GHCR |


**Required secrets** (configured in the GitHub repository settings)

| Secret | Description |
|---|---|
| `oc_namespace` | OpenShift namespace to deploy into |
| `oc_token` | OpenShift service account token |
| `db_password` | Database password injected at deploy time |
| `SYSDIG_API_TOKEN` | Sysdig monitoring token (optional — skipped if unset) |

The OpenShift server URL is stored as a repository variable (`oc_server`).

## Authentication

By default, authentication is mocked — no Cognito login is required. This is controlled by `VITE_MOCK_USER=true` in your `.env`.

To run with real Cognito auth, set the following in `.env`:

```env
VITE_MOCK_USER=false
AUTH_MOCK_ENABLED=false
JWT_JWKS_URI=https://cognito-idp.ca-central-1.amazonaws.com/{userPoolId}/.well-known/jwks.json
COGNITO_USER_POOL_ID=
COGNITO_USER_POOL_CLIENT_ID=
COGNITO_DOMAIN=
```