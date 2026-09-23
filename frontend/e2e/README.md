# CSP E2E (BDD — Gherkin + Playwright via playwright-bdd)

Browser-automation end-to-end tests for a BC Gov Natural Resources app, driven against a **running
local stack** (Vite/React frontend → Spring backend → Docker Oracle DB). Self-contained on purpose so
it can live in its own folder or be lifted into the app repo.

This is a **BDD suite**: `.feature` files are the executable spec, `playwright-bdd` (`bddgen test`)
compiles them into native Playwright tests, and a reusable step layer implements the Gherkin against
page objects. Scenarios are re-grounded from each use case's **Gherkin**: that Gherkin defines *what*
each screen must do (authored from the legacy app / the spec); here the routes, fields, and input
values are pinned to the *new* app and the *seeded* DB.

---

## CSP quick start (this app's actual coordinates)

Everything below the divider is the skill's generic guidance. This section is what **actually works
for CSP on a CGI-network machine**, discovered by standing the stack up and running the suite. Read
this first; treat the generic sections as background.

### 0. One-time: let `npx playwright install` through the TLS proxy

```bash
npx playwright install chromium            # FAILS: UNABLE_TO_GET_ISSUER_CERT_LOCALLY
NODE_EXTRA_CA_CERTS=/etc/ssl/certs/ca-certificates.crt npx playwright install chromium   # works
```

The CGI network re-signs TLS (CGI gateway + Zscaler CAs). `npm` and `git` work because they use the
system trust store; **Node uses its own bundled CA list and ignores it**, so Playwright's CDN download
fails. `~/.npmrc` also sets `playwright_skip_browser_download=1`, so `npm install` never fetches
browsers — you must run the install above explicitly. Export `NODE_EXTRA_CA_CERTS` in your shell
profile to avoid re-discovering this.

### 1. Seeded database

```bash
docker login ghcr.io -u <your-github-username>      # PAT with read:packages (private package)
docker run -d --name real-data-seeded-csp-db -p 1525:1521 \
  ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:latest
```

Service `DBDOCK_01`, user/password `THE`/`default`. **Allow ~40 s**: `docker ps` reports `Up` well
before Oracle accepts connections, and the listener transiently returns `ORA-12514` while starting.
Poll for a real connection rather than trusting container status.

Contents: 50 submissions / 2,273 invoices / 15,122 line items (the busiest 50 submissions from
`fortmp1`). **No seed patches are needed** — `real-test-data-patches/` is empty because the image
already carries everything these scenarios use.

### 2. Backend on :8080

```bash
docker run -d --name csp-backend-e2e -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SPRING_DATASOURCE_URL='jdbc:oracle:thin:@//host.docker.internal:1525/DBDOCK_01' \
  -e SPRING_DATASOURCE_USERNAME=THE -e SPRING_DATASOURCE_PASSWORD=default \
  -e AUTH_MOCK_ENABLED=true -e AUTH_MOCK_ROLES=ADMIN \
  -e JWT_JWKS_URI='https://mock-auth.invalid/.well-known/jwks.json' \
  -e JWT_ISSUER='https://mock-auth.invalid' -e JWT_AUDIENCE='mock-audience' \
  nr-csp-backend:latest
```

Three things that are easy to get wrong:

- **The JWT vars are mandatory even with mock auth on.** `JwtService.init()` is a `@PostConstruct`
  that runs unconditionally and does `new URL(jwksUri)`; an empty value throws
  `MalformedURLException: no protocol` and the context never starts. The placeholders above are only
  ever *parsed*, never fetched — mock auth bypasses the code path that would dereference them.
- **Don't use `--network host` under Rancher Desktop.** It binds inside the Rancher VM's network, not
  your WSL distro's, so `localhost:8080` from your shell never reaches it. Publish the port and use
  `host.docker.internal` for the DB.
- **Start the DB before the backend.** A backend that boots first leaves the port open but 404s every
  route, and can warm an empty reference-data cache. `preflight/anchors.setup.ts` catches the cache
  case explicitly.

Health check: `curl http://localhost:8080/api/health` → `{"status":"UP",...}`. Note it is
`/api/health`, **not** `/actuator/health` (not exposed).

### 3. Frontend on :3000

```bash
cd frontend && npm start
```

Then enable mock auth by adding `"mockUser": true` to `frontend/public/amplify-config.js`. That file
is **gitignored** (local-only), so this is not an app change.

**CSP's mock auth differs from the scaffold's assumption.** `MockAuthProvider` returns
`isAuthenticated: true` on first paint, with the role from `localStorage['csp.mockRole']` (default
`ADMIN`). There is **no login button to click** and the session is **not** in-memory-only, so
`page.goto()` to a protected route is safe. `pages/common/authNav.ts` has been re-grounded
accordingly — `signInAsMockUser()` seeds the role via `addInitScript` and performs no login dance.

### 4. Run

```bash
cd frontend/e2e
npm test                    # pretest runs bddgen first — ALWAYS use this, not `npx playwright test`
npm test -- --headed
npx playwright test --repeat-each=5     # flake check (after an explicit `npm run bddgen`)
```

`npx playwright test` on its own runs whatever is **stale** in `.features-gen/` — edit a `.feature`,
run that, and you will be testing the previous version.

### This directory holds TWO separate Playwright projects

| | `frontend/playwright.config.ts` | `frontend/e2e/playwright.config.ts` |
|---|---|---|
| Owns | `e2e/smoke.spec.ts` only | the BDD suite (`features/`, `steps/`, `pages/`) |
| Runs against | the **deployed** OpenShift route | a **local** stack |
| Driven by | CI (`reusable-tests.yml`) | `npm test` in this folder |

They are kept apart by a `testIgnore` in the outer config. **Without it the outer config collects
`.features-gen/**/*.spec.js`** — real Playwright specs — and runs them without playwright-bdd's
fixtures, failing in ways that look like app bugs. Keep those patterns in sync if this project moves.

---

## Layout — organized by domain, then use case

Every artifact lives under its **domain** (a short subject code, e.g. `<DOMAIN>`) and then its **use
case**, so the suite stays navigable as it grows to hundreds of scenarios. The Playwright globs are
recursive, so adding a domain or UC needs no config change.

```
e2e/
  playwright.config.ts   # testDir = defineBddConfig({ features, steps }); baseURL :3000; HTML+JUnit
  features/<domain>/<uc-id-slug>/
    <concern>.feature    # e.g. features/<DOMAIN>/uc-<DOMAIN>-005-create/validation.feature
    coverage.md          # per-UC coverage matrix: source item -> app enforcement -> scenario -> status -> gap
                         #   (how to read one: coverage-guide.md at the e2e root)
    defects.md           # per-UC defect log: findings by register (divergence/bug/coverage-gap/spec-gap/verified)
                         #   (how it works: defects-guide.md at the e2e root)
  coverage-guide.md      # plain-language legend for the coverage.md files (columns, status flags) — BA/QA
  defects-guide.md       # plain-language legend for the defects.md files (registers, tags, how-to-read) — BA/QA
  steps/
    fixtures/            # composition root, split per domain (step files import from '../fixtures')
      index.ts           #   mergeTests(globalTest, <domain>Test, …) -> createBdd(test) -> Given/When/Then
      global.ts          #   cross-domain only: world + the `World` union, shared page objects
      <domain>.ts        #   one per domain: page objects + cleanup registry + mutation spy
    common/*.steps.ts    # cross-domain REUSABLE steps (generic asserts) — no domain vocabulary
    <domain>/*.steps.ts  # domain steps, split by concern (navigation/form/verify); NO DOM selectors
  pages/<domain>/*.ts    # Page Objects (selectors + interactions) — called BY steps
  pages/common/*.ts      # cross-domain browser helpers (mock-auth login, client-side nav, Carbon fields)
  fixtures/<domain>/*-test-data.ts   # known-good test data pinned from the seeded DB (with provenance)
  preflight/anchors.setup.ts   # `setup` project the chromium project depends on: asserts anchors +
                         #   applied-seed presence resolve BEFORE any scenario (fail fast, one clear message)
  scripts/               # apply-patches.sh / teardown-patches.sh (seed patches, auto-discovered);
                         #   docker-sqlplus.sh (run sqlplus inside the DB container when no local client)
  real-test-data-patches/   # minimal per-domain seed patches (<name>.sql + <name>.teardown.sql) the
                         #   real extract can't supply — applied via scripts/ (see that folder's README)
  .features-gen/         # generated Playwright tests (git-ignored, disposable — never edit/commit)
```

Naming: UC folder = `uc-<domain>-<nnn>-<verb>` (matches the source `UC-<DOMAIN>-<NNN>`), one `.feature`
per concern within it. A step belongs to `steps/<domain>/` if only that domain uses it, or
`steps/common/` if it is genuinely domain-agnostic.

## Prerequisites — bring up the full stack

The steps below describe the typical **BC Gov NR-stack** bring-up (Vite/React frontend → Spring backend →
Docker Oracle DB, with local mock auth). Treat the specific ports, DSN, container name, cache-evict URL,
and env flags as **defaults to adjust for your app** — override them via `.env` (see below).

1. **Seeded Oracle DB — pre-built Docker image + seed patches.** The suite runs against a Docker image
   that already contains the **real extracted test data** — no repo checkout, `docker compose`, or manual
   load step is needed.

   The image lives in a **private** GitHub Packages registry under the
   [CGI-BC](https://github.com/CGI-BC) org, so steps 1 and 2 are a one-time authorization per machine.
   Skip them and the `docker pull` in step 3 is simply refused — and a registry denial does not always
   read like one, so it is easy to mistake for a bad image tag.

   **Step 1 — create a PAT** for the GitHub account you use for [CGI-BC](https://github.com/CGI-BC). The
   packages themselves are listed at <https://github.com/orgs/CGI-BC/packages>.
   - **Classic token:** tick at least the **`read:packages`** scope.
   - **Fine-grained token:** grant **Packages: Read-only** for the **CGI-BC** organisation.

   Read-only is genuinely all that is needed here — this is a pull, never a push.

   **Step 2 — log in to the GitHub container registry** using that same account:
   ```bash
   docker login ghcr.io -u YOUR_GITHUB_USERNAME
   ```
   When it prompts for a password, paste the **PAT** — not your GitHub account password. Docker stores
   the credential, so this is not repeated on later pulls.

   **Step 3 — pull and run the image** (map the Oracle listener to host port **1525**):
   ```bash
   docker run -d --name real-data-seeded-<APP-NAME>-db -p 1525:1521 \
     ghcr.io/cgi-bc/nr-mof-oracle-<APP-NAME>-real-test-data-seeded:latest
   ```
   - **Oracle service / user / password** — as published by your image (the scaffold defaults assume
     service `DBDOCK_01`, user/password `THE`/`default`; **JDBC URL**
     `jdbc:oracle:thin:@//localhost:1525/DBDOCK_01`).
   - The image tag and container name may change over time. If your container is named something other
     than the scripts' default (`real-data-seeded-db` — name yours `real-data-seeded-<APP-NAME>-db`, e.g.
     `real-data-seeded-ilcr-db`), set `DB_CONTAINER` for the sqlplus
     wrapper (see [`.env.example`](.env.example)).

   **Step 4 — apply the seed patches** (once per fresh container; the image does **not** include them).
   These are the minimal seed rows the real extract can't supply (see
   [`real-test-data-patches/`](real-test-data-patches)). 
   
   From **WSL** or a **Git Bash** terminal:
   ```bash
   ./scripts/apply-patches.sh            # tear down again with ./scripts/teardown-patches.sh
   ```
   - **No configuration** — the script auto-detects the client (local `sqlplus`, else your DB container)
     and prints which. Set `DB_CONTAINER` in `.env` if your container isn't the default.
   - **Applying to an already-running backend?** Evict the app's reference-data cache afterward if it
     has one (SCS example: `POST /api/api/internal/cache/evict`) or restart it.
   - Skip this and the seed-dependent scenarios **fail fast in preflight**, with a message telling you to run it.

   **Fixtures & anchors.** The fixtures pin real anchors discovered from this data (record ids, codes,
   keys — each with provenance in its `fixtures/<domain>/*-test-data.ts`). **Re-verify them if the image
   is rebuilt from a fresh extract**, since real data is non-deterministic. The `preflight/` setup fails
   fast with one clear message if an anchor no longer resolves.
2. **Backend** on `:8080` with local mock auth: `security.jwt.enabled=false`, the app's `LOCAL`
   environment flag, JNDI datasource → `localhost:1525/DBDOCK_01`.
   **After loading data into an already-running backend, evict the app's reference-data cache if it has one**
   (SCS example: `POST /api/api/internal/cache/evict`) or restart it — otherwise a startup-warmed cache serves stale
   code lists and create calls 500. (Also: start the DB *before* the backend — the backend's Spring
   context fails to initialize if the Oracle listener isn't up yet, and then every `/api` route 404s.)
3. **Frontend** on `:3000` with `VITE_MOCK_USER=true` (`npm start`). Mock auth auto-logs-in a single
   admin role — no Cognito/login flow needed. (The one app-specific bit — the Landing page's login
   button test-id and route — is a labeled default at the top of `pages/common/authNav.ts`.)

## Seeded database image — how it's built and refreshed

The DB you run in step 1 is a **pre-built Oracle image preloaded with real data extracted from a dev/test
system** (not a synthetic seed). Developers just pull and run it; a maintainer rebuilds it periodically.
The process:

1. **Extract** real data from a dev/test source DB with your app's extract tooling. The extract is
   typically **one FK hop**, so some referential gaps are expected (a child row whose parent wasn't
   pulled) — a live `INSERT` still enforces FKs, so a create can fail on a *data gap*, not a bug.
   *(SCS extracts via `scs-data-extract` / `toad_extract`.)*
2. **Load** the extract into a base Oracle Free container running locally (the SCS image publishes service
   `DBDOCK_01`, user/password `THE`/`default`).
3. **Snapshot** the loaded container into a tagged image and **push** it to the team packages registry
   **(run the `docker` commands one at a time)**:
   ```bash
   SEEDED_NAME=<your-seeded-local-container-name>
   DEST_IMAGE=ghcr.io/cgi-bc/nr-mof-oracle-<APP-NAME>-real-test-data-seeded

   docker stop -t 120 "$SEEDED_NAME"                          # clean checkpoint BEFORE commit (avoids a fuzzy snapshot)
   docker commit "$SEEDED_NAME" "$DEST_IMAGE:latest"          # may take a few minutes
   docker tag "$DEST_IMAGE:latest" "$DEST_IMAGE:$(date +%F)"  # dated tag pins this known-good snapshot
   docker push "$DEST_IMAGE:latest"
   docker push "$DEST_IMAGE:$(date +%F)"
   ```
   Restart the local container afterward (`docker start "$SEEDED_NAME"`) if you still need it running.
   *(SCS image: `ghcr.io/cgi-bc/nr-mof-oracle-scs-real-test-data-seeded`.)*
4. **Consume**: developers `docker run -p 1525:1521 <image>` (step 1) and apply the seed patches
   per-container (step 2). Patches are **not** baked into the image — re-apply them on each fresh
   container, or re-snapshot *with* them to bake them in.

**After any re-extract the real data changes**, so **re-verify the pinned fixtures and any seed patches** —
treat a re-extract as a re-ground event (sweep `fixtures/**` and any hardcoded ids in the ledgers). The
`preflight/` setup fails fast if a pinned anchor no longer resolves.

**Windows/WSL note:** building or loading a large Oracle image inflates the WSL virtual disk (`.vhdx`).
Set your disks to sparse, so `wsl --shutdown` can reclaim the space back to your `C:` drive.

## Install & run

**`.env` is the single config point.** Copy `.env.example` to `.env` and adjust `BASE_URL` (and `ORACLE_DSN` /
`DB_CONTAINER` if you'll run the seed-patch scripts). It's loaded by `playwright.config.ts` (via dotenv) and
auto-sourced by `scripts/apply-patches.sh` / `teardown-patches.sh`.

> **CSP:** read the [CSP quick start](#csp-quick-start-this-apps-actual-coordinates) above instead —
> the paths and the `playwright install` command below both differ on a CGI-network machine
> (`cd frontend/e2e`, and the browser download needs `NODE_EXTRA_CA_CERTS`).

```bash
cd e2e
cp .env.example .env    # then edit BASE_URL / DB vars for your local stack
npm install
npx playwright install chromium
npm test                # regenerates from features (pretest -> bddgen test), then runs headless
npm run test:headed     # watch it drive the browser (recommended first run)
npm run bddgen          # just regenerate .features-gen/ from features + steps
npm run report          # open the HTML report
```

**A freshly laid-down scaffold has no features yet**, so `npm test` reports "no tests" until you author your
first `.feature` (the placeholder `preflight/anchors.setup.ts` skips itself until you fill it in). You never
configure sqlplus — the seed-patch scripts auto-detect it.

Set `BASE_URL` in `.env` (or inline, `BASE_URL=http://localhost:3001 npm test`) if your ports differ.
Edit `features/` and `steps/` only — `.features-gen/` is regenerated on every `npm test`. An unbound step fails `bddgen`
(before Playwright runs) and names the exact `.feature` line.

### Scenario tags

Every scenario carries two kinds of tag:

- **Priority** — how important the scenario is, used to pick what to run when you can't run the whole suite:

  | Tag | Meaning | Examples |
  |---|---|---|
  | `@p0` | **Critical** — the core happy path must work; if it's red the feature is broken. Runs in every smoke check. | create a record and confirm it persisted |
  | `@p1` | **Important** — key validation, error handling, and required-field rules. | required-field errors, numeric-format errors, server-error handling |
  | `@p2` | **Secondary** — UI niceties and lower-risk branches. | field enable/disable gating, Back-button behavior |

- **Traceability** — `@UC-<DOMAIN>-<NNN>` (the use case) and `@S<NN>` (the source slice), so a scenario
  maps straight back to its Gherkin/spec and to the `coverage.md` matrix.

- **Special handling** — a few tags mark scenarios that aren't a normal pass:

  | Tag | Meaning |
  |---|---|
  | `@discovered-divergence` | **Deliberately RED** — reproduces a divergence (app ≠ legacy spec, suspected defect). Never forced green; logged in the UC's `defects.md` for BA/QA → Jira; flips to green when the app is fixed. |
  | `@discovered-bug` | **Deliberately RED** — reproduces a confirmed bug/regression awaiting a fix (has a Jira ticket). |
  | `@skip` | Genuinely can't be automated yet (e.g. blocked by a single mock admin role) — never used to hide a failure. |

  **Never force green:** a suspected-defect divergence / confirmed bug is a genuinely-failing tagged test, never masked with `@skip`, xfail, or a weakened assertion. A failing test does not stop the others (Playwright isolates them). Run a **clean "fresh failures only" pass** — everything except the known reds — with:

  ```bash
  npx playwright test --grep-invert "@discovered-divergence|@discovered-bug"
  ```

Filter with Playwright's `--grep` (args after `--` pass through; `pretest` still regenerates first):

```bash
npm test -- --grep @p0                 # smoke: core happy paths only
npm test -- --grep "@p0|@p1"           # smoke + key validation/error handling
npm test -- --grep @UC-<DOMAIN>-005    # one use case
npm test -- --grep @<domain>           # one domain
```

At scale you can also filter at *generation* time so only matching scenarios compile:
`npm run bddgen -- --tags @p0` then `npx playwright test`.

## Oracle MCP server (data discovery / DB assertions) — optional

An Oracle MCP server (thin-mode `mcp-oracle`, tools `list_tables` / `describe_table` / `run_query`)
lets the test author query the seeded DB directly — to pin known-good anchor values and confirm writes
landed. It is an **authoring/verification** aid, not a test-runtime dependency — the specs stay pure-UI
and portable. Register it project-scoped (e.g. a repo `.mcp.json` for Claude Code) launched portably as
`python -m mcp_oracle`; install once with `python -m pip install mcp-oracle oracledb`, then trust the
project. Point `ORACLE_DSN` / `ORACLE_USER` / `ORACLE_PASSWORD` at your seeded DB (defaults match the
`localhost:1525/DBDOCK_01` `THE`/`default` dev DB — throwaway local creds, not secrets).

> Prereq for the actual queries: the seeded Oracle Docker DB must be up (see step 1 of *Prerequisites*).

## Notes

- **Self-cleaning (fails loud):** a step that creates a record registers its id and the shared fixture
  deletes it on teardown, so each scenario leaves the seeded DB as it found it. Residue **throws**
  rather than silently polluting the shared seed. Two cleanup paths by flow:
  - Resources with a **DELETE endpoint** → registry → `DELETE /api/<your-resource>/{id}`; a `404` is
    treated as already-gone.
  - Resources with **no DELETE endpoint** → delete the row directly at the DB via `sqlplus`
    (`steps/<domain>/*DbCleanup.ts`; DSN + binary env-overridable), then read back through the API to
    prove it's gone (don't trust the delete blindly).
- **Reusable steps:** add a `Given/When/Then` only when no existing phrase fits; repeated behavior is a
  `Scenario Outline` with an `Examples:` table. Selectors live in the page object, never in steps.
- **Tags & coverage:** every scenario is tagged by priority + UC/slice (see *Scenario tags*), and each
  UC folder carries a `coverage.md` matrix mapping every source item to the scenario that covers it (or
  a logged gap) — the durable audit trail behind a green run.
- Permission-gated scenarios (e.g. "no Delete without the delete permission") can't be expressed with a
  single mock admin role yet — a `@skip` scenario documents this; deferred.
- No CI wiring yet (the app + DB aren't containerized for CI here).
