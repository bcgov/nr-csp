# NRS CSP App

The **Coast Selling Price (CSP) System** for the BC Ministry of Forests. Users submit, review
and approve coastal log sale invoices, maintain the sort-code and flat-price-conversion
reference tables, and run the R06–R13 reports. It replaces the legacy CSP system (Oracle Forms
+ JasperReports Server).

Invoices reach CSP two ways: **electronically**, as ESF XML submissions pulled from an Oracle
queue and run through a schema + business-rule validation pipeline; or **manually**, keyed in
through the UI. Both land in the same inbox for review, approval, or rejection. On top of that
sit seven operational reports (R06–R13; there is no R09), each renderable as PDF, CSV, or XLS.

Spring Boot 4 (Java 21) backend · React 19 + IBM Carbon frontend · Oracle · deployed to
OpenShift. See [ARCHITECTURE.md](ARCHITECTURE.md) for the full stack and system design.

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


## Contents

- [Prerequisites](#prerequisites)
- [Project structure](#project-structure)
- [Running locally with Docker Compose](#running-locally-with-docker-compose)
- [Choosing a database: VPN or local seeded Oracle](#choosing-a-database-vpn-or-local-seeded-oracle)
- [Authentication](#authentication)
- [Roles and permissions](#roles-and-permissions)
- [Testing](#testing)
- [Working outside containers](#working-outside-containers)
- [Corporate certificates (Zscaler / proxy)](#corporate-certificates-zscaler--proxy--local-dev-only)
- [Troubleshooting](#troubleshooting)
- [CI/CD pipelines](#cicd-pipelines)
- [OpenShift deployment](#openshift-deployment)
- [Further documentation](#further-documentation)
- [Contributing](#contributing)

## Prerequisites

**To run the stack in containers** (the common case):

- Docker Compose or Podman Compose
- A database — either VPN access to the shared Oracle instance, or the local seeded Oracle
  image. See [Choosing a database](#choosing-a-database-vpn-or-local-seeded-oracle).

**Only if you build or test outside the containers** — see
[Working outside containers](#working-outside-containers):

| Tool | Version | Used for |
|---|---|---|
| JDK | 21 | Backend compile/test (`<java.version>21</java.version>`) |
| Maven | 3.9+ | Backend build (or use the containerized build) |
| Node.js | ≥ 24 | Frontend and e2e suite (`engines` in `package.json`) |
| npm | ≥ 10 | Frontend and e2e suite |

> The backend image builds on JDK 21 and runs on a JRE 25 base; bytecode targets 21.

## Project structure

```
backend/                  Spring Boot 4 API (Java 21)
  src/main/java/.../csp/backend/
    controller/           REST controllers + DTOs (16 API interfaces)
    service/              Business services, Jasper reporting, mappers
    repository/           JdbcTemplate data access (no JPA — see ARCHITECTURE.md)
    invoice/              Invoice domain: submission (ESF/XML) + manual pipelines
    config/               Security, caching, datasource, Jasper, OpenAPI
    security/ filter/     JWT and mock-auth request filters
  src/main/resources/
    reports/              JasperReports .jrxml designs (R06–R13, PDF + CSV variants)
    schemas/csp/          ESF submission XSDs
  certs/                  Local-only corporate CA certs (git-ignored)

frontend/                 React 19 + Carbon SPA (Vite 8, TypeScript)
  src/pages/              One folder per route (17 pages)
  src/components/         Form, Layout, and core shared components
  src/context/            Auth, theme, layout, notifications, page title
  src/config/             API client, FAM, React Query, test setup
  e2e/                    BDD end-to-end suite — see frontend/e2e/README.md
  Caddyfile               Production static server + /api reverse proxy + CSP headers

common/openshift.init.yml One-time OpenShift objects, incl. the runtime config ConfigMap
monitoring/alerts/        Sysdig alert definitions synced after prod deploys
docs/                     Business rules, refactor plans, investigations
.github/workflows/        CI/CD — see CI/CD pipelines below
```

## Running locally with Docker Compose

**1. Configure your environment**

```bash
cp .env.example .env
```

Open `.env` and fill in the values:

| Variable | Required | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | `prod` (mirrors OpenShift, all vars required) or `local` (empty fallbacks, mock auth on, DEBUG logging) |
| `SPRING_DATASOURCE_URL` | yes | Oracle JDBC URL — **TCPS descriptor form**, see note below |
| `SPRING_DATASOURCE_USERNAME` | yes | Oracle username |
| `SPRING_DATASOURCE_PASSWORD` | yes | Oracle password |
| `CSP_OIDC_ISSUER_URI` | real auth | CSS environment issuer |
| `CSP_OIDC_CLIENT_ID` | real auth | Public CSS client ID |
| `AUTH_MOCK_ENABLED` | for local dev | `true` enables backend mock auth — see [Authentication](#authentication) |
| `AUTH_MOCK_IDENTITY_PROVIDER` | no | Mock user's identity provider: `idir` (default) or `bceidbusiness` |
| `AUTH_MOCK_ROLES` | no | Mock user's FAM codes, comma-separated (`ADMIN`, `APPROVE`, `VIEW` are shorthand for `CSP_*`). Default `ADMIN` |
| `API_DOCS_ENABLED` | for local dev | `true` serves Swagger UI and the OpenAPI document. Off by default, and never set in deployed environments |
| `JAVA_OPTS` | no | JVM flags for the backend container; defaults to container-aware heap sizing |

> Use single quotes if a value contains special characters: `PASSWORD='p@ss!'`

> **For the shared Oracle instance, the JDBC URL must use the TCPS descriptor form.** The
> short `@//host:port/service` form bypasses the Oracle wallet and fails with PKIX certificate
> errors. See `.env.example` for the exact shape. The local seeded image is the exception —
> it serves plain TCP, so the short form is correct there.

`.env.example` defaults to `SPRING_PROFILES_ACTIVE=prod`, which mirrors OpenShift. Setting
`local` instead turns on mock auth by default and raises logging to DEBUG for
`ca.bc.gov.nrs.csp` and Spring Security.

> **Neither profile starts without integrations.** This is the most common first-run failure,
> and the `local` profile does not exempt you from it:
>
> - **Database.** `ValidatingDataSource` runs `SELECT 1 FROM DUAL` during startup and the app
>   refuses to boot if the DB is unreachable. You need a working database either way — VPN and
>   credentials, or the local seeded image.
> - **SSO.** Real authentication requires the CSS issuer/client; local mock authentication does not.
>
> The frontend container waits on the backend's health check, so a backend that fails to start
> takes the frontend down with it — the symptom you see is the frontend never coming up.

**2. Choose frontend authentication**

On localhost the frontend uses mock authentication by default. Deployed environments mount
the public CSS runtime configuration over `frontend/public/config.js`. For real DEV sign-in,
copy `frontend/.env.example` to the git-ignored `frontend/.env`, set the DEV issuer and client
ID, and set `AUTH_MOCK_ENABLED=false` and `CSP_OIDC_*` for the backend.

**3. Connect to a database, then start**

Connect VPN (or start the local seeded Oracle image — see
[Choosing a database](#choosing-a-database-vpn-or-local-seeded-oracle)).

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
| Swagger UI (with `API_DOCS_ENABLED=true`, as a staff user) | http://localhost:3000/api/swagger-ui/index.html |

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

## Choosing a database: VPN or local seeded Oracle

There are two ways to get a database, and which you pick drives your `.env`.

**Option A — shared Oracle over VPN.** The production-shaped option, and the only one with
current data.

- VPN (Cisco Secure Client) connected
- `hosts` file entries for the DB — ask your team for the values
- `SPRING_DATASOURCE_*` pointed at the shared instance, TCPS descriptor form

**Option B — local seeded Oracle in Docker.** No VPN required. A published image carries a
real-data snapshot (50 submissions / 2,273 invoices / 15,122 line items), which is enough for
most feature work and is what the e2e suite runs against.

```bash
docker login ghcr.io -u <your-github-username>    # PAT with read:packages (private package)
docker run -d --name real-data-seeded-csp-db -p 1525:1521 \
  ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:latest
```

Service `DBDOCK_01`, user/password `THE`/`default`. Allow **~40 seconds** — `docker ps` reports
`Up` well before Oracle accepts connections, and the listener transiently returns `ORA-12514`
while starting. Poll for a real connection rather than trusting container status.

Then in `.env`:

```bash
SPRING_PROFILES_ACTIVE=local
SPRING_DATASOURCE_URL='jdbc:oracle:thin:@//host.docker.internal:1525/DBDOCK_01'
SPRING_DATASOURCE_USERNAME=THE
SPRING_DATASOURCE_PASSWORD=default
```

> The short `@//host:port/service` URL form is correct **here** — the local image serves plain
> TCP, not TCPS, so there is no wallet to apply.

Full details on the seeded image — how it is built, how to refresh it, how to reset to the
snapshot, and how to tell which database you are actually connected to — are in
**[`frontend/e2e/README.md`](frontend/e2e/README.md)**.

## Authentication

Browser authentication uses modern FAM / BC Gov CSS OpenID Connect with PKCE. API calls carry
access tokens; the backend validates this environment's issuer and client, token type and
identity. The GitHub variables and the CSS redirect URIs to register are listed under
**GitHub Secrets & Variables** below.

Local mock mode requires a localhost origin in the frontend and `AUTH_MOCK_ENABLED=true` in
the backend. On localhost the frontend uses mock mode unless `VITE_OIDC_ISSUER_URI` and
`VITE_OIDC_CLIENT_ID` are set in the git-ignored `frontend/.env`; an explicit `mockUser` in
`config.js` overrides that default. Mock mode does not need SSO configuration. Real mode
requires `CSP_OIDC_ISSUER_URI` and `CSP_OIDC_CLIENT_ID` in the backend and the same
issuer/client in the frontend configuration.

## Roles and permissions

Nobody gets into CSP without signing in with IDIR or Business BCeID **and** holding a CSP role
for that identity provider. Everything else is denied, by the API and, to match it, the UI.

| Identity provider | FAM code in the token | Internal role | Can do |
| --- | --- | --- | --- |
| IDIR / IDIR MFA | `CSP_ADMIN` | `ADMIN` | Existing admin permissions |
| IDIR / IDIR MFA | `CSP_APPROVE` | `APPROVE` | Existing entry and review permissions |
| IDIR / IDIR MFA | `CSP_VIEW` | `VIEW` | Existing read/export permissions |
| Business BCeID | `CSP_SUBMITTER_FOREST_CLIENT-<8 digits>`, one per client | `SUBMITTER` | Upload XML submissions and see submission history, for those clients only |

- Role codes must match exactly (case-insensitive). FAM `FAM:` bookkeeping roles, bare or
  foreign-app roles, and any other identity provider grant nothing.
- Staff codes never apply to a Business BCeID token, and `CSP_SUBMITTER` never applies to an IDIR
  token.
- `CSP_SUBMITTER` must exist in FAM as a **forest-client-scoped** role. FAM then issues
  `CSP_SUBMITTER_FOREST_CLIENT-00012345` for each client granted, never the bare code. CSP parses
  it as FAM's `CssRoleNaming.parse` does (suffixes stripped from the right), and accepts only that
  exact base, a single forest-client scope and an 8-digit client number. A Business BCeID user
  without one gets **No CSP access**.
- A submitter may call only the four `/api/submissions/*` endpoints and the three
  `/api/submission-history` reads (`SecurityConfig`); every other endpoint and page stays staff-only.
  Validating or submitting XML returns 403 (`CLIENT_NOT_AUTHORIZED`), before any business rule
  runs, when its submission client or any invoice's submitting party is a client they do not
  hold. History lists every submission of all their clients together,
  as legacy ESF showed a business all its submissions; another client's submission is not found.
  `ClientScope` has no empty form, so a submitter whose clients are missing is denied rather
  than shown everything.

Authorization is **action-level, not role-level**. Controllers declare the specific action they
need and a role→action map decides:

```java
@PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Approve')")
```

Action strings use the exact names from the FAM permission matrix — a page (`invoiceDetails`)
or a page sub-action (`invoiceDetails/Approve`). `submissionUpload` and `submissionHistory` are
CSP's own, because legacy industry submitted through ESF. The catalogue and the role→action map
live in `backend/.../util/constants/PermissionConstants.java`, and the frontend keeps a mirrored
copy in `permissions.ts` to drive routes, navigation and button visibility. **The two must be kept
in sync.** The FAM-code mapping is mirrored the same way: `CspAuthorities.java` in the backend and
`famRoles.ts` in the frontend. Each route names its page action in `routes/access.ts`.

Locally, `AUTH_MOCK_IDENTITY_PROVIDER` and `AUTH_MOCK_ROLES` set the backend's default mock user;
FAM codes are mapped exactly as a real token's are, and `ADMIN`, `APPROVE` and `VIEW` are
shorthand for the staff codes. The UI's mock role selector (`components/Layout/MockRoleSelector`)
switches between the staff roles and `SUBMITTER (BCeID)` without a restart, and sends the chosen
identity to the backend with each request, so the API enforces what the UI shows. A mock
submitter holds forest client `00000001`; set `localStorage['csp.mockForestClients']`
(comma-separated) for others. Real Business BCeID sign-in needs Business BCeID enabled on the CSS
client.

## Testing

Three layers, each run independently.

### Backend — unit and integration

```bash
cd backend

mvn test                 # unit tests (surefire)
mvn verify               # unit + integration tests (failsafe, *IT.java)
mvn verify -DskipITs     # unit tests only, still produces coverage
```

Integration tests (`*IT.java`) spin up a real Oracle via **Testcontainers**
(`testcontainers-oracle-free`), so Docker must be running and the first run pulls a sizeable
image. They do not need VPN.

JaCoCo writes three reports under `backend/target/site/`:

| Report | Covers |
|---|---|
| `jacoco/` | Unit tests |
| `jacoco-it/` | Integration tests |
| `jacoco-merged/` | Both combined — this is what SonarCloud consumes |

### Frontend — unit and component

```bash
cd frontend

npm test                 # watch mode, node project
npm run test:unit        # *.unit.test.{ts,tsx} in happy-dom
npm run test:browser     # *.browser.test.{ts,tsx} in real Chromium
npm run test:coverage    # node project with v8 coverage -> frontend/coverage/
npm run test:ci          # single run, CI reporter
```

Vitest is split into two projects. The **node** project runs in happy-dom and is the default.
The **browser** project runs in real Chromium via Playwright and is **opt-in** behind
`VITEST_BROWSER_ENABLED=true` — without that guard Vitest initialises the Playwright provider
at startup even when `--project node` is passed, which triggers a Chromium download. The
`test:browser` script sets it for you.

> On a machine with no bundled Chromium (e.g. a WSL/Ubuntu release Playwright doesn't support
> yet), set `PLAYWRIGHT_CHANNEL=chrome` to fall back to a system-installed browser.

### End-to-end — BDD (Gherkin + Playwright)

The e2e suite lives in `frontend/e2e/` as a **nested npm project with its own
`package.json`**, and is a BDD suite: `.feature` files are the executable spec, `playwright-bdd`
compiles them into Playwright tests, and a step layer implements the Gherkin against page
objects.

```bash
cd frontend/e2e
npm install
npm test                 # chromium, excluding @discovered-divergence
npm run test:headed      # watch it run
npm run test:ui          # Playwright UI mode
npm run test:divergences # scenarios tracking confirmed app defects — red on purpose
npm run report           # open the last HTML report
```

It needs the **full stack running** — seeded Oracle, backend on `:8080`, frontend on `:3000` —
and `AUTH_MOCK_ENABLED=true` on that backend, or every `/api` call 401s while `/api/health`
still returns 200.

> **📖 Read [`frontend/e2e/README.md`](frontend/e2e/README.md) before running the e2e suite.**
> It is the authoritative guide and covers everything this section does not: the exact
> stack-up commands, the one-time `NODE_EXTRA_CA_CERTS` step for `playwright install` behind
> the corporate TLS proxy, running the e2e stack alongside your delivery stack on a second
> port, resetting the database to its snapshot, confirming which database you are on, scenario
> tags, the suite layout by domain and use case, why `@playwright/test` is pinned exactly, and
> a "four things that will bite you" section. Companion docs sit beside it:
> [`coverage-guide.md`](frontend/e2e/coverage-guide.md) and
> [`defects-guide.md`](frontend/e2e/defects-guide.md), plus per-use-case `coverage.md` and
> `defects.md` files under `frontend/e2e/features/`.

## Working outside containers

Compose is the supported path, but both halves run natively.

```bash
# Backend — needs JDK 21 + Maven, and .env values exported into your shell
cd backend && mvn spring-boot:run

# Frontend — needs Node >= 24
cd frontend && npm install && npm start      # Vite dev server on :3000
```

The Vite dev server proxies `/api` to `http://localhost:8080` by default; override with
`REACT_APP_API_TARGET`. Other local-only frontend env vars: `VITE_PUBLIC_URL` (base path) and
`VITE_USE_POLLING` (filesystem polling, needed inside Docker/WSL).

Linting and formatting:

```bash
cd frontend
npm run lint             # ESLint over src/**/*.{ts,tsx}
npm run format           # Prettier write
```

`backend/google_checks.xml` holds Google's Checkstyle ruleset for IDE import — it is **not**
wired into the Maven build, so there is no `mvn checkstyle:check` step.

`.pre-commit-config.yaml` configures a single [gitleaks](https://github.com/gitleaks/gitleaks)
hook that scans staged changes for committed secrets. To enable it:

```bash
pre-commit install
```

## Corporate certificates (Zscaler / proxy) — local dev only

If your machine routes traffic through a corporate TLS proxy (e.g. Zscaler), the JVM inside the backend container needs to trust the proxy's root certificate or outbound HTTPS calls will fail with `SSLHandshakeException`.

This is a **local development concern only.** Certificate files are git-ignored (`/backend/certs/*`), so they are never committed and never present in CI/CD builds.

**How it works**

The backend Dockerfile handles certificates in **two separate stages**, and they accept
different things:

| Stage | Reads | Fixes |
|---|---|---|
| Maven build | `backend/certs/*.pem` **only** | Dependency downloads failing behind the proxy during `mvn package` |
| JRE trust store | `backend/certs/*.cer`, `*.pem`, `*.crt` | The running app's outbound HTTPS calls |

Empty or missing files are silently skipped in both. **If your Maven build is the thing
failing, the certificate must be a `.pem`** — a `.cer` or `.crt` is picked up for the runtime
trust store but never reaches the build stage.

**Adding a certificate**

1. Export the corporate root certificate from your browser or certificate manager (DER/PEM format).
2. Drop the file into `backend/certs/` — e.g. `backend/certs/zscaler.cer`.
3. Rebuild the backend image:

```bash
docker compose up --build backend
```

The filename (without extension) becomes the alias in the trust store. Multiple certificates are supported — add one file per certificate.

> Only the `.gitkeep` placeholder is tracked in git. Do **not** force-add cert files to git.

**The Node half — `npx playwright install` fails**

Node does **not** use the system trust store; it ships its own CA list. So `npm` and `git`
work behind the proxy while Playwright's browser download fails with
`UNABLE_TO_GET_ISSUER_CERT_LOCALLY`, which does not read like a certificate problem:

```bash
NODE_EXTRA_CA_CERTS=/etc/ssl/certs/ca-certificates.crt npx playwright install chromium
```

Export `NODE_EXTRA_CA_CERTS` in your shell profile to avoid rediscovering this. Note that
`frontend/.npmrc` sets `playwright_skip_browser_download=1`, so `npm install` never fetches
browsers — you must run the install above explicitly.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| UI loads, user looks signed in, but every API call returns 401 | Backend mock auth off — only the frontend half is on | Set `AUTH_MOCK_ENABLED=true` in `.env` and restart the backend |
| Pod is "healthy" but every request fails | `/api/health` returns a hardcoded `UP` and never touches the DB — and it is the liveness probe | Check the backend logs; don't trust health for DB state |
| `PKIX path building failed` / certificate errors from Oracle | JDBC URL uses the short `@//host:port/service` form, bypassing the wallet | Use the TCPS descriptor form — see `.env.example` |
| `ORA-12514` right after starting the seeded DB container | Oracle listener still coming up; `docker ps` reports `Up` early | Wait ~40 s and poll for a real connection |
| Backend fails to start under the `prod` profile | A required var is unset — `prod` has no fallbacks | Fill in the table in [step 1](#running-locally-with-docker-compose), or switch to `SPRING_PROFILES_ACTIVE=local` |
| `SSLHandshakeException` on backend outbound HTTPS | JVM doesn't trust the corporate proxy CA | [Add the certificate](#corporate-certificates-zscaler--proxy--local-dev-only) to `backend/certs/` |
| `UNABLE_TO_GET_ISSUER_CERT_LOCALLY` from `playwright install` | Node ignores the system trust store | Set `NODE_EXTRA_CA_CERTS` — see above |
| Host edits don't trigger a reload in the compose dev server | FS events don't cross the Docker/WSL boundary | Already handled by `VITE_USE_POLLING=true` in the override file — confirm it is set |
| First user after a deploy sees slow lookups | Reference-data caches cold | Expected and mitigated by startup warm-up — see `docs/investigations/reference-data-cold-start.md` |

## CI/CD pipelines

All workflows live in `.github/workflows/`.

| Workflow | Trigger | What it does |
|---|---|---|
| `pr-open.yml` | PR opened/updated | Builds backend + frontend images to GHCR, tests the release scripts, deploys a PR-specific OpenShift environment, runs the reusable test suite |
| `pr-validate.yml` | PR opened/updated | Validates the PR title against Conventional Commits |
| `analysis.yml` | Push, PR, schedule | Backend tests (Maven) and frontend tests, SonarCloud analysis for both, Trivy security scan with results uploaded as CodeQL SARIF |
| `reusable-tests.yml` | Called by PR/merge | Backend integration tests, API integration tests (`common/tests/integration`), and the Playwright e2e suite, uploading reports as artifacts |
| `reusable-deploy.yml` | Called by PR/merge | Deploys backend and frontend to a given OpenShift zone |
| `merge.yml` | Push to `main` | Deploys **TEST**, runs tests, and records the tested image digests; does not deploy PROD |
| `release-prod.yml` | Manual, tested tag | Deploys PROD by immutable image digest with the vanity route |
| `pr-close.yml` | PR closed | Tears down the PR environment and promotes images |
| `scheduled.yml` | Cron / manual | Closes stale branches and PRs, purges old PR deployments, regenerates SchemaSpy docs, runs OWASP ZAP full scans |

Sysdig alert definitions are version-controlled in `monitoring/alerts/` (`replicas.json`,
`crashloop.json`, `restarts.json`) and synced automatically after a prod deploy.

## OpenShift deployment

PR/TEST deployments run through GitHub Actions. PROD requires a manual tested-tag release.

**Pipeline overview**

| Event | What happens |
|---|---|
| PR opened | Images built, pushed to GHCR, deployed to a PR-specific environment — zone is the **PR number mod 50**, running lite (one replica, no HPA or PDB) |
| Merged to `main` | Images deployed to **TEST**, acceptance tests run, and immutable release-candidate digests recorded |
| Manual Release PROD | Tested images deployed by digest through the vanity route; Sysdig alerts synced |

**PROD releases**

PR images are built once and promoted by digest. Before merging a PR after `main` has moved,
sync it with `main` and wait for its checks, so its images include every earlier merge.

1. Wait for the commit's push-triggered **Merge** run to succeed, including TEST tests and
   **Record tested release candidate**. If it fails, use **Re-run all jobs**; a partial rerun
   cannot record a candidate, because TEST may have moved to another commit.
2. Tag that exact commit, for example `v1.0.0`; the Merge run's summary shows the command. Tagging
   does not deploy.
3. Run **Actions → Release PROD → Run workflow** on branch `main` with the tag. The `prod`
   environment's deployment branch rule allows only `main`.
4. Verify `https://<VANITY_HOST>`: certificate chain, HTTP-to-HTTPS redirect, sign-in, API
   calls and full sign-out.

To roll back, run **Release PROD** with an older tested tag. This works only while that tag's Merge
run still has its release-candidate artifact (the repository's artifact retention period); a
missing, expired or mismatched artifact stops the release. The older tag deploys its own templates
through the current workflow, which ignores parameters those templates do not declare. A tag
from before the FAM migration cannot be released this way: its templates expect Cognito settings
the workflow no longer passes.

**PROD vanity route**

Release PROD serves the frontend only at `https://<VANITY_HOST>`, through the edge Route in
`frontend/openshift.vanity-route.yml`; it creates no Gold-hostname PROD route. PR and TEST deploys
keep the generated Gold hostname.
Release PROD fails before deploying if `VANITY_HOST` or any `VANITY_TLS_*` secret is missing.
To renew the certificate, replace the three `VANITY_TLS_*` secrets and rerun the latest
**Release PROD** run (its tag's release-candidate artifact must still exist). Never commit
certificate material.

Before the first release, set `CSP_OIDC_ISSUER_URI` and `CSP_OIDC_CLIENT_ID` in the `prod`
environment and register these on the CSS PROD client:

| Setting | Exact URL |
|---|---|
| Redirect URI | `https://<VANITY_HOST>/authCallback` |
| Post-logout redirect URI | `https://<VANITY_HOST>/` |

**Oracle init container**

The backend pod uses an init container (`ghcr.io/bcgov/nr-forest-client/common:prod`) that runs before the app starts. It connects to the Oracle host on port 1543 (TCPS) using `KEYSTORE_SECRET` to fetch and write an Oracle wallet into a shared volume (`/cert`). The main container mounts this volume and the entrypoint script merges the wallet cert into the JVM trust store at startup.

**GitHub Secrets & Variables**

Deployment settings live in the `dev`, `test` and `prod` GitHub Environments. PR deploys, PR
cleanup and the scheduled PR purge use `dev`. The optional SonarCloud tokens are the only
repository secrets, because the analysis jobs attach no environment. Every deploy job fails unless
`OC_NAMESPACE` ends in `-<environment>`, and each environment's issuer must be its own SSO realm.

**Repository secrets** (optional; without them the SonarCloud scans are skipped)

| Secret | Description |
|---|---|
| `SONAR_TOKEN_BACKEND` | SonarCloud token for backend analysis |
| `SONAR_TOKEN_FRONTEND` | SonarCloud token for frontend analysis |

**Environment secrets** (set in each of `dev`, `test` and `prod`)

| Secret | Description |
|---|---|
| `OC_NAMESPACE` | That environment's namespace, ending in `-dev`, `-test` or `-prod` |
| `OC_TOKEN` | Service account token for that namespace |
| `SPRING_DATASOURCE_URL` | Full JDBC URL (TCPS descriptor form) |
| `SPRING_DATASOURCE_USERNAME` | Oracle username |
| `SPRING_DATASOURCE_PASSWORD` | Oracle password |
| `KEYSTORE_SECRET` | Used by the init container to authenticate against the Oracle keystore |
| `DATABASE_HOST` | Oracle DB hostname |
| `DATABASE_SERVICE_NAME` | Oracle service name |
| `SYSDIG_API_TOKEN` | `prod` only, optional — Sysdig alert sync |
| `VANITY_TLS_CERTIFICATE` | `prod` only — PEM leaf certificate for `VANITY_HOST` |
| `VANITY_TLS_KEY` | `prod` only — matching unencrypted PEM private key |
| `VANITY_TLS_CA_CERTIFICATE` | `prod` only — PEM issuer chain, in issuer order |

**Environment variables** (set in each of `dev`, `test` and `prod` unless noted)

| Variable | Description |
|---|---|
| `OC_SERVER` | OpenShift API server URL |
| `CSP_OIDC_ISSUER_URI` | CSS realm issuer: `https://dev.loginproxy.gov.bc.ca/auth/realms/standard` in `dev`, `test.loginproxy` in `test`, `loginproxy.gov.bc.ca` in `prod` |
| `CSP_OIDC_CLIENT_ID` | The CSS public client ID (`resource` in the installation JSON) |
| `VANITY_HOST` | `prod` only — PROD hostname without scheme or path, e.g. `csp.nrs.gov.bc.ca` |
| `CSP_OIDC_SITEMINDER_LOGOUT_URL` | Optional — SiteMinder `logoff.cgi` URL; defaults to `logontest7` outside PROD and `logon7` in PROD |
| `CSP_OIDC_IDIR_HINT`, `CSP_OIDC_BCEID_HINT` | Optional — identity provider hints (defaults `azureidir`, `bceidbusiness`) |
| `OPENSHIFT_APPS_DOMAIN` | Optional — cluster apps domain; defaults to `apps.gold.devops.gov.bc.ca` |
| `frontend_log_level` | Optional — Caddy log level (default `INFO`) |

The backend log levels default to `INFO`, `INFO`, `INFO` and `WARN`. The deploy job reads
`backend_log_level`, `backend_root_log_level`, `backend_spring_log_level` and
`backend_spring_security_log_level` inside its `matrix`, which GitHub evaluates before the
environment is attached, so only repository variables of those names would change them. For the
same reason the frontend `config.js` ConfigMap is built in `common/openshift.init.yml`, whose job
has the environment, rather than in the frontend deploy template.

> The sign-in URLs are **not** variables. The SPA uses `https://<host>/authCallback` as its
> redirect URI and `https://<host>/` as its post-logout redirect URI, where the host is
> `VANITY_HOST` in PROD and `<repo>-<zone>.<domain>` for PR and TEST deploys. Both must be
> registered on the environment's CSS client or sign-in/sign-out will fail.

> The `CSP_OIDC_*` values are written into the frontend `config.js`, so the init job fails before
> deploying unless each matches a strict pattern.

## Further documentation

| Document | What's in it |
|---|---|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Tech stack, system design, data access, auth flow, reporting pipeline |
| [`frontend/e2e/README.md`](frontend/e2e/README.md) | **End-to-end testing** — stack setup, seeded DB, tags, layout, pitfalls |
| [`frontend/e2e/coverage-guide.md`](frontend/e2e/coverage-guide.md) | How per-use-case `coverage.md` files are written |
| [`frontend/e2e/defects-guide.md`](frontend/e2e/defects-guide.md) | How discovered defects are recorded and tracked |
| [`docs/submission-validation-business-rules.md`](docs/submission-validation-business-rules.md) | Every business rule applied to an electronic ESF submission, ported from the legacy app |
| [`docs/refactor-shared-invoice-rules.md`](docs/refactor-shared-invoice-rules.md) | Planned refactor to share invoice rules between the electronic and manual paths |
| [`docs/investigations/reference-data-cold-start.md`](docs/investigations/reference-data-cold-start.md) | Why reference-data caching and startup warm-up exist |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Branching strategy and commit conventions |
| [`SECURITY.md`](SECURITY.md) | How to report a vulnerability |
| [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) | Contributor code of conduct |

API documentation is generated from the code and served locally at
`http://localhost:3000/api/swagger-ui/index.html` (OpenAPI JSON at `/api/v3/api-docs`) when
`API_DOCS_ENABLED=true`. Like the rest of `/api` it needs a staff role, which the default mock user
has. Deployed environments don't serve it at all: `/api/health` is the only public endpoint.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the full workflow. In short: branch off a fresh
`main` with a type prefix (`feat/`, `fix/`, `chore/`, `docs/`), use
[Conventional Commits](https://www.conventionalcommits.org/) — `pr-validate.yml` enforces this
on the PR title — and open a PR back to `main`.
