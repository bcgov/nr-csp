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

common/openshift.init.yml One-time OpenShift objects, incl. the amplify-config ConfigMap
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
| `JWT_JWKS_URI` | **always** | Cognito JWKS endpoint. Must be a **well-formed URL even in mock mode** — the backend parses it at startup and fails to boot on an empty value. With mock auth any placeholder works, e.g. `https://mock-auth.invalid/.well-known/jwks.json` |
| `JWT_ISSUER` | real auth | JWT issuer claim |
| `JWT_AUDIENCE` | real auth | JWT audience claim (the Cognito app client ID) |
| `AUTH_MOCK_ENABLED` | for local dev | `true` enables backend mock auth — see [Authentication](#authentication) |
| `AUTH_MOCK_ROLES` | no | Roles for the mock user: `ADMIN`, `APPROVE`, `VIEW` (comma-separated). Default `ADMIN` |
| `JAVA_OPTS` | no | JVM flags for the backend container; defaults to container-aware heap sizing |
| `COGNITO_DOMAIN` | no | Only feeds the Caddy `Content-Security-Policy` header when running the **production** image. Leave unset for the dev server — compose will warn that it is unset |

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
> - **JWKS URI.** `JwtService` parses `JWT_JWKS_URI` into a `URL` in `@PostConstruct`, so an
>   empty value throws at startup even with mock auth on. Any syntactically valid URL will do.
>
> The frontend container waits on the backend's health check, so a backend that fails to start
> takes the frontend down with it — the symptom you see is the frontend never coming up.

**2. Create your frontend auth config**

`frontend/public/amplify-config.js` is git-ignored and is **not** present in a fresh clone.
The app will not boot without it — `getAmplifyConfig()` throws if `window.amplifyConfig` is
unset. Two ways to get one:

```bash
# Quickest — a committed template, already pointed at localhost with mock auth on
cp frontend/public/amplify-config.example.js frontend/public/amplify-config.js
```

Or **grab the live file from a deployed environment**, which is what the template was made
from and the better route if the Cognito values have moved on:

```bash
# From a running environment
curl -s https://nr-csp-test.apps.gold.devops.gov.bc.ca/amplify-config.js \
  > frontend/public/amplify-config.js

# Or straight from the ConfigMap, if you have cluster access
oc extract configmap/nr-csp-frontend-amplify-config-test --to=frontend/public/ --confirm
```

Then make the two local edits:

1. Point `redirectSignIn` / `redirectSignOut` at `http://localhost:3000/` and
   `http://localhost:3000/logout`.
2. Add `"mockUser": true` to skip Cognito — deployments omit it or set it false.

See [Authentication](#authentication) for running against real Cognito instead.

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

For local development, use **mock mode** — no Cognito login required. Mock mode has two halves,
and **both must be on** or you get a half-broken session:

| Half | Switch | Where |
|---|---|---|
| Frontend | `mockUser: true` | `frontend/public/amplify-config.js` — on in the example template |
| Backend | `AUTH_MOCK_ENABLED=true` | `.env` — on in `.env.example` |

**Frontend half.** `mockUser` is only honoured when the app is served from localhost
(`frontend/src/env.ts` re-checks the hostname), so it cannot weaken a deployed environment.
It is read as `mockUser === true`: if `amplify-config.js` is missing the key — or the file
itself is absent, as in a fresh clone — mock mode is **off** and you get a real Cognito login
screen. A config copied from a deployment won't have it either — deployments set it false — so
add it by hand, or start from the template, which has it on. See
[step 2](#running-locally-with-docker-compose).

**Backend half.** `AUTH_MOCK_ENABLED` defaults to on under the `local` profile. Under the
`prod` profile `application.yml` sets it off, but the environment variable overrides that
(env vars outrank profile YAML in Spring Boot), so setting it in `.env` works under either
profile.

With the backend half off, the frontend shows a fake logged-in user but every API call is
rejected with 401 — `/api/health` still returns 200, so the backend looks healthy. The mock
user's roles come from `AUTH_MOCK_ROLES` (default `ADMIN`); see
[Roles and permissions](#roles-and-permissions).

**Running with real Cognito auth locally**

1. Set `AUTH_MOCK_ENABLED=false` in `.env` and fill in `JWT_JWKS_URI`, `JWT_ISSUER`, and
   `JWT_AUDIENCE`. They must point at the same Cognito User Pool the frontend uses.
2. In `frontend/public/amplify-config.js`, set `"mockUser": false` (or remove the key) and make
   sure the Cognito values are current. The simplest way to be sure is to take them from a
   deployed environment — see [step 2](#running-locally-with-docker-compose).

Keep `redirectSignIn` / `redirectSignOut` pointed at localhost, and note that both must be
registered with FAM for local sign-in to work at all. Cognito values (User Pool ID, client ID,
domain, IDP name) are owned by the FAM team / DevOps; the canonical rendering of the whole set
is the ConfigMap template in `common/openshift.init.yml`.

> **How sign-out works.** Signing out drives a federated logout chain — SiteMinder → Keycloak → Cognito → back to the app at `/logout`, which immediately redirects to the welcome screen — built at runtime from the `logout*` values above (see `frontend/src/utils/logoutChain.ts`; same pattern as FAM's own console). This clears all three upstream sessions, not just Cognito's. Two registration constraints, both owned by FAM (`oidc_clients_csp.tf` in [nr-forests-access-management](https://github.com/bcgov/nr-forests-access-management)): `redirectSignOut` must be registered verbatim as a Cognito sign-out URL, and the Cognito domain must be on the shared Keycloak client's post-logout allow-list. If the `logout*` values are missing, the app falls back to a Cognito-only Amplify sign-out. Test/prod use `test.loginproxy`/`loginproxy` and (prod) `logon7` hosts — see the `LOGOUT_*` GitHub variables. Sign-out also briefly opens a small popup against loginproxy's `idir` broker realm: that realm holds a fourth session the chain cannot reach (its logout endpoint rejects redirect chaining and its pages are not frameable), and without clearing it the next sign-in silently logs the user back in without prompting for credentials.

> In OpenShift, Cognito config is injected at runtime via a ConfigMap. The file is git-ignored,
> so CI builds an image without it; the ConfigMap defined in `common/openshift.init.yml` is
> mounted over `/usr/share/caddy/amplify-config.js` by `frontend/openshift.deploy.yml`. No
> rebuild is required when Cognito values change — only a rollout.

## Roles and permissions

CSP has three roles, granted through Cognito groups assigned by FAM. The group name is the
`famClientId` prefix plus the role, so with `famClientId: "CSP"`:

| Cognito group | Role | Scope |
|---|---|---|
| `CSP_ADMIN` | `ADMIN` | Everything, including ESF submission upload |
| `CSP_APPROVE` | `APPROVE` | Review actions — approve, reject, unapprove, cancel |
| `CSP_VIEW` | `VIEW` | Read-only: search, view, generate reports |

**The match is by suffix, not by exact name.** A group grants a role when it *equals* the role
name or *ends with* `_<ROLE>`, compared case-insensitively — so `ADMIN`, `CSP_ADMIN` and
`NRS_CSP_ADMIN` all grant `ADMIN`. The group names above are just the conventional FAM form.
Backend and frontend apply the same rule (`JwtService.matchesRole`, `RealAuthProvider`).

Authorization is **action-level, not role-level**. Controllers declare the specific action they
need and a role→action map decides:

```java
@PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Approve')")
```

Action strings use the exact names from the FAM permission matrix — a page (`invoiceDetails`)
or a page sub-action (`invoiceDetails/Approve`). The catalogue and the role→action map live in
`backend/.../util/constants/PermissionConstants.java`, and the frontend keeps a mirrored copy
in `permissions.ts` to drive button visibility. **The two must be kept in sync.**

Locally, `AUTH_MOCK_ROLES` sets the mock user's roles, and the UI's mock role selector
(`components/Layout/MockRoleSelector`) switches between them without a restart.

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
| App shows a Cognito login screen instead of a mock user | `frontend/public/amplify-config.js` missing or has no `mockUser` key | `cp frontend/public/amplify-config.example.js frontend/public/amplify-config.js` |
| UI loads, user looks signed in, but every API call returns 401 | Backend mock auth off — only the frontend half is on | Set `AUTH_MOCK_ENABLED=true` in `.env` and restart the backend |
| Backend won't start; frontend never comes up either | DB unreachable, or `JWT_JWKS_URI` empty — both fail at startup on **any** profile, and the frontend waits on the backend's health check | Point at a working DB and give `JWT_JWKS_URI` a valid URL (any placeholder under mock auth) |
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
| `pr-open.yml` | PR opened/updated | Builds backend + frontend images to GHCR, deploys a PR-specific OpenShift environment, runs the reusable test suite |
| `pr-validate.yml` | PR opened/updated | Validates the PR title against Conventional Commits |
| `analysis.yml` | Push, PR, schedule | Backend tests (Maven) and frontend tests, SonarCloud analysis for both, Trivy security scan with results uploaded as CodeQL SARIF |
| `reusable-tests.yml` | Called by PR/merge | Backend integration tests, API integration tests (`common/tests/integration`), and the Playwright e2e suite, uploading reports as artifacts |
| `reusable-deploy.yml` | Called by PR/merge | Deploys backend and frontend to a given OpenShift zone |
| `merge.yml` | Push to `main` | Deploys to **test** → runs tests → deploys to **prod** → syncs Sysdig alerts → promotes images to the `prod` tag |
| `pr-close.yml` | PR closed | Tears down the PR environment and promotes images |
| `scheduled.yml` | Cron / manual | Closes stale branches and PRs, purges old PR deployments, regenerates SchemaSpy docs, runs OWASP ZAP full scans |

Sysdig alert definitions are version-controlled in `monitoring/alerts/` (`replicas.json`,
`crashloop.json`, `restarts.json`) and synced automatically after a prod deploy.

## OpenShift deployment

Deployments to OpenShift are fully automated through GitHub Actions — no manual steps are needed once the secrets and variables are configured.

**Pipeline overview**

| Event | What happens |
|---|---|
| PR opened | Images built, pushed to GHCR, deployed to a PR-specific environment — zone is the **PR number mod 50**, running lite (one replica, no HPA or PDB) |
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

GitHub variable names are **lowercase**; the uppercase names you see in the workflows are
OpenShift template parameters, not variables.

Scope depends on **which job reads the variable**, and this is a real trap:

- The **init** job has `environment:` attached (`reusable-deploy.yml:12`), so variables it
  reads may be scoped per environment, with a repo-level value as the fallback for PR deploys.
- The **deploy** job reads its variables inside a `matrix`, which GitHub evaluates *before*
  the environment is attached. An environment-scoped value is **silently ignored** there, so
  those variables must be set at repo level.

This is why the `amplify-config` ConfigMap is built in `common/openshift.init.yml` rather than
in the frontend deploy template — see the note at `common/openshift.init.yml:53`.

| Variable | Read by | Description |
|---|---|---|
| `OC_SERVER` | both | OpenShift API server URL |
| `OPENSHIFT_APPS_DOMAIN` | both | Optional — cluster apps domain; defaults to `apps.gold.devops.gov.bc.ca` |
| `app_env` | init | Environment label injected into the frontend config (`dev`, `test`, `prod`) |
| `cognito_idp_name` | init | Cognito identity provider name (`DEV-IDIR`, `TEST-IDIR`, `PROD-IDIR`) |
| `cognito_region` | init | AWS region for Cognito (e.g. `ca-central-1`) |
| `cognito_user_pool_id` | init | Cognito User Pool ID |
| `cognito_user_pool_client_id` | init | Cognito App Client ID — differs per environment |
| `cognito_domain` | init | Cognito hosted UI domain; also used in the Caddy CSP header |
| `cognito_oauth_scopes` | init | OAuth scopes, comma-separated (e.g. `openid,profile,email`) |
| `fam_client_id` | init | FAM application client ID for role-group mapping |
| `logout_siteminder_url` | init | SiteMinder `logoff.cgi` URL — first hop of the sign-out chain |
| `logout_keycloak_url` | init | Keycloak end-session endpoint — second hop of the sign-out chain |
| `logout_keycloak_client_id` | init | FAM's shared Keycloak client ID |
| `frontend_log_level` | init | Optional — Caddy log level (default `INFO`) |
| `backend_log_level`, `backend_root_log_level`, `backend_spring_log_level`, `backend_spring_security_log_level` | **deploy (matrix)** | Optional — backend log levels (defaults `INFO`, `INFO`, `INFO`, `WARN`). Must be repo-level |

> `JWT_JWKS_URI` is declared optional in `reusable-deploy.yml`, but the backend will not start
> without it.

> The Cognito redirect URLs are **not** variables. `reusable-deploy.yml` derives them from the
> repository name and apps domain (`https://<repo>-<zone>.<domain>` and `.../logout`), so each
> PR, test, and prod environment gets its own pair. Both must be registered with FAM for the
> environment or sign-in/sign-out will fail.

> Every variable in this table feeds the `amplify-config` ConfigMap template in
> `common/openshift.init.yml`. A missing one renders as an empty string rather than failing
> the deploy, which usually surfaces later as a broken sign-in or sign-out.

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

API documentation is generated from the code and served by the running app at
`http://localhost:3000/api/swagger-ui/index.html` (OpenAPI JSON at `/api/v3/api-docs`).

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the full workflow. In short: branch off a fresh
`main` with a type prefix (`feat/`, `fix/`, `chore/`, `docs/`), use
[Conventional Commits](https://www.conventionalcommits.org/) — `pr-validate.yml` enforces this
on the PR title — and open a PR back to `main`.
