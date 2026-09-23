# Coverage — UC-INBOX-001 Search Submission Inbox

> New to these files? See [`coverage-guide.md`](../../../coverage-guide.md) at the e2e root for the column + status-flag legend.

Sources reconciled: `UC-INBOX-001-S01..S10.feature` (csp-bmad
`_bmad-output/implementation-artifacts/tests/UC-INBOX-001/gherkin/`) against the app's read path
(`pages/Inbox/index.tsx` → `services/inbox.service` → `InboxController` → `InboxRepository`).

Test data (real, 2026-09-23): pinned in `fixtures/inbox/inbox-test-data.ts` (finding queries in the
comments there). Seed image `ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23`.

**SCOPE OF THIS PASS — skeleton only.** Exactly one slice is authored, deliberately: a connection
smoke scenario that proves the stack serves real seeded data. The other nine slices of UC-INBOX-001
are **not yet authored** and are listed below as coverage gaps so nothing looks silently covered.

| Source item | Source citation | App enforcement point | Scenario (tags) | Status | Gap/defect |
|---|---|---|---|---|---|
| Inbox renders results table with Submission ID / date / status / type / invoice counts | S01 `UC-INBOX-001-S01.feature:28-31` | `pages/Inbox/index.tsx:125-…` (inboxColumns), `ResultsTable` | `smoke.feature` `@S01 @p0` | covered | — |
| Search returns matching submissions for a date range | S01 `:24-27` | `InboxRepository.search` + `/api/inbox` | `smoke.feature` `@S01 @p0` | covered (range only; boundary/no-match not exercised) | Coverage gap #1 |
| Grid shows an empty state until criteria are supplied | not in legacy Gherkin (new-app behaviour) | `pages/Inbox/index.tsx` (ResultsTable empty state) | `smoke.feature` `@S01 @p0` | covered (+ spec-gap) | Spec gap #1 |
| Clear search filters | S02 | `handleClearFilters` (`pages/Inbox/index.tsx:315`) | — | deferred | Coverage gap #2 |
| Remaining slices S03–S10 (filters, pagination, sorting, no-match, validation, navigation) | S03–S10 | various | — | deferred | Coverage gap #2 |
| "Authenticated via WebADE" + "provisioned for inbox/Search" | S01 Background | Cognito groups / `usePageAccess` | — | not-applicable | Coverage gap #3 |
| Paginator "Showing <count>" | S01 `:30` | `ResultsTable` pagination | — | deferred | Coverage gap #2 |

**Symmetry check:** not applicable to this pass — no mirror matrix is exercised by a single smoke
slice. It must be re-run when S03–S10 are authored (filter arms, sort directions, match/no-match).

**Role / permission coverage:** blocked by mock auth — `MockAuthProvider` issues exactly one role
(`CSP_<role>` from `localStorage['csp.mockRole']`, default `ADMIN`). Role-differentiated scenarios
are authorable by seeding a different role, but none are authored in this pass.
