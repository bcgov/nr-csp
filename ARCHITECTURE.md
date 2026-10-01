# Architecture

How CSP is put together, and why. For setup and day-to-day commands see [README.md](README.md);
for end-to-end testing see [`frontend/e2e/README.md`](frontend/e2e/README.md).

- [System overview](#system-overview)
- [Tech stack](#tech-stack)
- [Backend](#backend)
- [Data access: why there is no JPA](#data-access-why-there-is-no-jpa)
- [Invoice domain](#invoice-domain)
- [Reporting pipeline](#reporting-pipeline)
- [Caching and cold start](#caching-and-cold-start)
- [Frontend](#frontend)
- [Authentication and authorization](#authentication-and-authorization)
- [Runtime configuration](#runtime-configuration)
- [Deployment topology](#deployment-topology)
- [Testing strategy](#testing-strategy)
- [Notable constraints](#notable-constraints)

## System overview

```
                 ┌─────────────────────────────────────────────┐
  IDIR user ───► │  Caddy (frontend pod)                       │
                 │   • serves the built React SPA              │
                 │   • reverse-proxies /api/* to the backend   │
                 │   • sets CSP / HSTS / frame-ancestors       │
                 └──────────────────┬──────────────────────────┘
                                    │ /api/*
                 ┌──────────────────▼──────────────────────────┐
                 │  Spring Boot 4 (backend pod)                │
                 │   • stateless JWT auth (Cognito/FAM)        │
                 │   • action-level @PreAuthorize              │
                 │   • invoice domain: ESF XML + manual        │
                 │   • JasperReports → PDF / CSV / XLS         │
                 │   • Caffeine reference-data caches          │
                 └──────────────────┬──────────────────────────┘
                                    │ JDBC (TCPS + Oracle wallet)
                 ┌──────────────────▼──────────────────────────┐
                 │  Oracle                                     │
                 │   • tables + PL/SQL packages (REF CURSORs)  │
                 │   • ESF submission queue                    │
                 └─────────────────────────────────────────────┘
```

The backend is **never exposed directly**. In OpenShift and in local compose alike, all traffic
enters through the frontend, which proxies `/api/*`. There is one consequence worth knowing:
TLS terminates at the OpenShift edge router, so the backend only ever sees plain HTTP and its
Spring HSTS config never fires. HSTS is therefore set at the Caddy layer instead, covering both
static responses and proxied API responses.

## Tech stack

**Backend**

| Component | Choice | Notes |
|---|---|---|
| Language | Java 21 | Builds on JDK 21, runs on a JRE 25 base image |
| Framework | Spring Boot 4.1 | Web MVC, Security, Validation, Cache, JDBC |
| Build | Maven | Surefire (unit) + Failsafe (`*IT.java`) + JaCoCo |
| Database | Oracle | `ojdbc11`, HikariCP, TCPS with an Oracle wallet |
| Data access | Spring `JdbcTemplate` | **No JPA/Hibernate** — see below |
| Logging | Log4j2 | Replaces Spring's default Logback starter throughout |
| API docs | springdoc-openapi | Swagger UI at `/api/swagger-ui.html` |
| Auth | jjwt + `jwks-rsa` | Manual JWT validation against Cognito JWKS |
| Caching | Caffeine | 11 reference-data caches, 12-hour TTL |
| Mapping | MapStruct | DTO ↔ domain mapping |
| Reporting | JasperReports | `.jrxml` designs; OpenPDF for PDF, Apache POI for XLS |
| XML | JAXB + XSD | ESF submission parsing and schema validation |
| Boilerplate | Lombok | |

**Frontend**

| Component | Choice | Notes |
|---|---|---|
| Language | TypeScript 6 | |
| Framework | React 19 | |
| Build | Vite 8 | Dev server proxies `/api` to `:8080` |
| UI | IBM Carbon + `@bcgov-nr/nr-theme` | BC Gov design system on Carbon |
| Routing | React Router 8 | |
| Server state | TanStack React Query 5 | |
| HTTP | Axios | |
| Auth | AWS Amplify 6 | Cognito hosted UI |
| Styling | Sass | |
| PWA | `vite-plugin-pwa` | Service worker, auto-update |
| Testing | Vitest 5 | Two projects: happy-dom and real Chromium |
| E2E | Playwright + playwright-bdd | Nested project in `frontend/e2e/` |
| Serving | Caddy 2 | Production static server |

## Backend

Layered, with the invoice domain pulled out of the generic layers:

```
controller/   REST controllers; `api/` holds the OpenAPI-annotated interfaces they implement
service/      Business logic, reporting, MapStruct mappers, domain models
repository/   JdbcTemplate data access + row mappers
invoice/      The invoice domain (see below)
config/       Security, cache, datasource, Jackson, Jasper, OpenAPI, clock
filter/       JwtRequestFilter and MockRequestFilter
exception/    Domain exceptions + the global handler
util/         Constants (roles, permissions) and validation helpers
```

Controllers are split into an interface (`controller/api/*Api.java`) carrying the OpenAPI
annotations and an implementation (`controller/*Controller.java`) carrying the logic and the
`@PreAuthorize` rules. This keeps the generated API docs readable and the controllers short.

`ClockConfig` injects a `Clock` bean rather than letting code call `LocalDate.now()` directly,
so date-sensitive business rules are testable without freezing system time.

## Data access: why there is no JPA

This is the single most surprising thing about the backend, and the thing most likely to trip
up someone arriving from a typical Spring project.

**There are no JPA entities, no Hibernate, and no Spring Data repositories.** The
`repository/` classes are hand-written and wrap `NamedParameterJdbcTemplate`, issuing SQL
directly against the `THE` schema or calling PL/SQL packages.

The reason is the legacy database. CSP sits on a long-established Ministry of Forests Oracle
schema whose business logic lives in PL/SQL packages that return **REF CURSORs**. Those are the
contract — reimplementing them as ORM mappings would mean duplicating logic that already exists
and is relied on elsewhere. So the application calls them and maps the results.

Two custom classes exist purely to make JasperReports cooperate with that decision:

- **`PlsqlQueryExecuterFactory`** — JasperReports' compile-time verifier checks every `$P{...}`
  used in a query against an allow-list of 19 primitive/date/String types. `java.sql.ResultSet`
  isn't on it, so a report declaring a `REPORT_CURSOR` parameter — the standard shape for an
  Oracle stored procedure returning a REF CURSOR — fails to compile with "Parameter type not
  supported in query". This factory relaxes only that check.
- **`RefCursorProcedureCallHandlerFactory`** — relaxing the compile-time check isn't enough.
  JasperReports' own `OracleProcedureCallHandlerFactory`, despite the name, is dead code in the
  open-source jar: it reflectively loads a class that doesn't exist and silently returns `null`.
  This supplies the runtime REF CURSOR binding it was supposed to provide.

Both are registered for the `plsql` query language in `jasperreports.properties`, because the
R06–R13 designs were authored for JasperReports **Server**, which bundles `plsql` support as a
commercial extension absent from the open-source library.

`ValidatingDataSource` wraps the pool to fail fast on a misconfigured datasource rather than
surfacing the problem on first query.

## Invoice domain

Invoices arrive through two channels, deliberately kept as separate orchestrators:

```
invoice/
  submission/        Electronic (ESF XML from the Oracle queue)
    structural/      Parsing and XSD schema validation
      parser/        SubmissionXmlParser
      schema/        SchemaValidator (schemas/csp/mof-csp.xsd)
    business/        Business-rule validation
      rule/
        submission/  Submission-level rules
        invoice/     Invoice-level rules
        line/        Line-item-level rules
      referencedata/ Reference data the rules need
      support/       Shared rule helpers
  manual/            Manually keyed invoices (CRUD through the UI)
  shared/            Channel-agnostic models and rules
```

Validation runs in two stages: **structural** first (does this parse, and does it satisfy the
XSD?), then **business** (do the values make sense against reference data and the rulebook?).
The submission hierarchy is fixed by the schema — one submission, one submitter client, one or
more invoices, each with one or more line items.

The validation pipeline is written to be application-agnostic; everything CSP-specific is
configuration, declared under `csp.submission.validation` in `application.yml` (schema paths,
JAXB context path, envelope and body namespaces and root elements) and defaulted in
`SubmissionValidationProperties`.

> **Known duplication.** Some invariant invoice rules are currently implemented twice — once
> on the electronic path, once on the manual path — and have drifted. A planned refactor to
> extract a shared pure rule core is documented in
> [`docs/refactor-shared-invoice-rules.md`](docs/refactor-shared-invoice-rules.md). The full
> rulebook ported from the legacy app is in
> [`docs/submission-validation-business-rules.md`](docs/submission-validation-business-rules.md).

## Reporting pipeline

Seven reports — R06, R07, R08, R10, R11, R12, R13 (there is no R09) — each with a `.jrxml` design in
`backend/src/main/resources/reports/`, plus a `_CSV` variant and subreports where needed. Each
has a controller (`R06Api`…`R13Api`), a service, and a frontend page.

A report request flows: controller → service → PL/SQL call returning a REF CURSOR → Jasper
fills the design → export as **PDF** (OpenPDF), **CSV** (the `_CSV` design variant), or **XLS**
(Apache POI).

Font handling is explicit: `fonts.xml` and `jasperreports_extension.properties` register the
font extensions Jasper needs, and ICU4J backs text layout. Without these, PDF output falls back
to substituted fonts and the layouts shift.

## Caching and cold start

Reference data — species, grades, sort codes, invoice and submission statuses, maturity codes,
FOB codes, modelling codes, species/grade combinations — is small, changes rarely, and was
being queried on demand. `CacheConfig` defines 11 Caffeine caches with a 12-hour TTL and a
1,000-entry ceiling.

Caching alone doesn't help the *first* request into a fresh pod, so
`ReferenceDataWarmupService` implements `ApplicationRunner` and populates every cache at
startup, including the per-species grade lists so the first filtered dropdown is warm too.

The reasoning and measurements are in
[`docs/investigations/reference-data-cold-start.md`](docs/investigations/reference-data-cold-start.md).

## Frontend

```
src/
  pages/        One folder per route — 17 pages
  components/
    Form/       Carbon-based inputs: autocompletes, multi-selects, editable tables
    Layout/     Header, side nav, profile panel, theme toggle, mock role selector
    core/       Modals, tables, tags, empty/loading states, file drop zone
  context/      auth, theme, layout, notification, pageTitle providers
  config/       API client, FAM config, React Query defaults, test setup
  hooks/        Shared hooks
  utils/        logoutChain and other helpers
```

Routes map to the domain: `Search`, `SubmissionHistory` (list and detail),
`UploadSubmission`, `Inbox`, `Invoice` (list and detail), `SortCode`, `FlatPriceConversion`,
the seven report pages, plus `Welcome`, `NotFound`, and a `/logout` route that immediately
redirects to the welcome screen.

**Server state is React Query's job**; component state is not used as a cache. The auth
provider is chosen at runtime — `AuthProvider` returns a mock provider or the real Amplify one
depending on `env.mockUser` — so no mock code paths run in a deployed environment.

The PWA service worker uses a `navigateFallbackDenylist` for `/^\/api\//`. Without it, Workbox
applies the SPA navigation fallback to API navigations and serves the React shell when you open
Swagger UI.

## Authentication and authorization

**Sign-in.** Amplify drives the Cognito hosted UI against a FAM-managed user pool federated to
IDIR. The SPA sends the resulting JWT as a bearer token on every `/api` call.

**Token validation.** The backend is stateless — no sessions, no CSRF token (there is no
browser-automatable credential to exploit, since the credential is an `Authorization` header on
a stateless request). `JwtRequestFilter` validates the token against the Cognito JWKS using
`jwks-rsa` and jjwt. `/api/health` and the Swagger/OpenAPI paths are public; everything else
requires authentication.

**Mock auth.** `MockRequestFilter` is registered only when `auth.mock.enabled` is true and
installs a synthetic authentication with the roles from `AUTH_MOCK_ROLES`. It is an
`Optional<MockRequestFilter>` injection, so when mock auth is off the filter does not exist in
the context at all.

**Authorization is action-level, not role-level.** Roles are `ADMIN`, `APPROVE`, `VIEW`,
derived from Cognito groups (`CSP_ADMIN` → `ADMIN`; both plain and FAM-prefixed forms are
accepted). Controllers declare a specific action string from the FAM permission matrix:

```java
@PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Approve')")
```

`PermissionConstants.ROLE_PERMISSIONS` maps each role to its set of permitted actions, and
`PermissionService` evaluates the SpEL. The frontend mirrors the same catalogue in
`context/auth/permissions.ts` to decide which buttons to render — **the two must be kept in
sync**; the backend is the enforcement point and the frontend is only a UX affordance.

**Sign-out** is a federated chain: SiteMinder → Keycloak → Cognito → back to `/logout`. It is
built at runtime from the `logout*` config values (`utils/logoutChain.ts`). Clearing only
Cognito would leave the upstream IDIR sessions alive and silently sign the user straight back
in. A small popup against loginproxy's `idir` broker realm handles a fourth session the
redirect chain cannot reach, because that realm's logout endpoint rejects redirect chaining and
its pages are not frameable.

## Runtime configuration

Nothing environment-specific is baked into the frontend build. The SPA reads
`window.amplifyConfig` from `public/amplify-config.js` at runtime:

- **Locally** — copy `amplify-config.example.js` to `amplify-config.js`. The real file is
  git-ignored.
- **In OpenShift** — a ConfigMap defined in `common/openshift.init.yml` is mounted over
  `/usr/share/caddy/amplify-config.js`. Changing Cognito values needs a rollout, not a rebuild.

The ConfigMap is created in the **init** step rather than the deploy template, because that is
where environment-scoped GitHub variables resolve correctly.

The backend is configured by Spring profiles in `application.yml`: `local` (empty fallbacks,
mock auth on, DEBUG logging) and `prod` (every value required, injected as env vars). Hikari
pool settings are tuned for the OpenShift→Oracle network path — `keepalive-time` pings idle
connections so firewalls can't silently drop them, and `max-lifetime` recycles well inside any
server-side idle limit.

## Deployment topology

Two deployments per zone (a zone being a PR number, `test`, or `prod`):

**Frontend pod** — Caddy on port 3000, serving static files and proxying `/api/*`. The
`caddy:2-alpine` image ships `CAP_NET_BIND_SERVICE` as a file capability on the binary;
OpenShift's restricted SCC refuses to exec a binary with file capabilities when
`allowPrivilegeEscalation=false`, so the Dockerfile strips it. The app listens on 3000, so the
capability was never needed.

**Backend pod** — an **init container** (`ghcr.io/bcgov/nr-forest-client/common:prod`) runs
first, connecting to Oracle on port 1543 (TCPS) with `KEYSTORE_SECRET` to fetch an Oracle
wallet and write it into a shared `/cert` volume. The main container mounts that volume and its
entrypoint merges the wallet certificate into the JVM trust store before the app starts. This
is why the JDBC URL must use the TCPS descriptor form: the short form bypasses the wallet.

Image rollout differs by zone. PR deploys pass the immutable head SHA as the image tag, so
every push changes the pod spec and forces a rollout. Test and prod use the mutable PR-number
tag, where `oc apply` would see an unchanged Deployment and keep serving a stale image despite
`imagePullPolicy: Always` — so a commit stamp on the pod template forces the rollout instead.

## Testing strategy

| Layer | Tool | Scope |
|---|---|---|
| Backend unit | JUnit 5 + Mockito (Surefire) | Services, rules, mappers |
| Backend integration | Failsafe + Testcontainers (`oracle-free`) | `*IT.java` — controllers and reports against a real Oracle |
| Frontend unit | Vitest, happy-dom | `*.unit.test.{ts,tsx}` |
| Frontend component | Vitest, real Chromium | `*.browser.test.{ts,tsx}` — opt-in via `VITEST_BROWSER_ENABLED` |
| API integration | Node suite in `common/tests/integration` | Run against a deployed zone in CI |
| End-to-end | Playwright + playwright-bdd | `frontend/e2e/` — Gherkin features against the full stack |

Coverage is reported to SonarCloud per component. The backend merges unit and integration
coverage into a single JaCoCo report so integration-only code paths aren't reported as
uncovered.

The e2e suite deserves a specific note: `.feature` files are the executable specification,
re-grounded from each use case's legacy Gherkin onto the current app's routes, fields, and
seeded data. Scenarios tagged `@discovered-divergence` track **confirmed application defects**
and fail on purpose — they are excluded from the default `npm test` so its exit status stays
meaningful, and run separately via `npm run test:divergences`. See
[`frontend/e2e/README.md`](frontend/e2e/README.md).

## Notable constraints

Things that look like mistakes but are deliberate:

- **No JPA.** Hand-written `JdbcTemplate` repositories over PL/SQL REF CURSORs. See above.
- **Two custom Jasper classes** exist to work around dead code and an over-strict verifier in
  the open-source JasperReports jar.
- **Log4j2 replaces Logback**, with the default starter excluded from every Spring dependency.
- **CSRF is disabled** — correct for a stateless bearer-token API with no session cookie.
- **HSTS is set at Caddy, not Spring**, because TLS terminates at the edge router.
- **`@playwright/test` is pinned exactly** in the e2e project; the reason is in its README.
- **The Vitest browser project is opt-in**, because Vitest initialises the Playwright provider
  at startup otherwise, triggering an unwanted Chromium download.
- **The Vite dev server ignores `e2e/` subpaths** when watching, or the nested e2e project's
  artifacts trigger full page reloads and a rescan of thousands of irrelevant files.
