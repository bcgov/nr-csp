# Re-grounded from: UC-INBOX-001-S01 (csp-bmad/_bmad-output/implementation-artifacts/tests/
#   UC-INBOX-001/gherkin/UC-INBOX-001-S01.feature)
#
# WHAT CHANGED vs the legacy Gherkin, and why (the legacy spec was authored against the JSF app):
#   * route         '/faces/inbox.xhtml'           -> '/inbox'  (ROUTES.INBOX)
#   * table         "[id$='submissionsTable']"     -> the Carbon DataTable, addressed by ROLE
#   * date fields   "[id$='CalDate1'/'CalDate2']"  -> '#date-start' / '#date-end' (Carbon DatePicker)
#   * auth          "authenticated via WebADE"     -> CSP mock auth (auto-authenticated as CSP_ADMIN;
#                                                     see pages/common/authNav.ts)
#   * provisioning  "provisioned for inbox/Search" -> not modelled: CSP gates on Cognito groups and
#                                                     the mock provider grants ADMIN, so it is implicit
#   * paginator     'Showing <count>'              -> not asserted here; row count is asserted directly
#
# SCOPE: this is the suite's CONNECTION SMOKE slice. It proves the whole chain is live and serving
# real seeded data — browser -> Vite (:3000) -> Spring backend (:8080) -> Oracle (:1525, the published
# seed image) — so that any later failure is about behaviour, not plumbing.
#
# NOTE the deliberate first assertion: the Inbox issues NO query until a criterion is entered, and
# until then the grid holds a single EMPTY-STATE row. Asserting that empty state FIRST is what stops
# the later "50 rows" check from being satisfied by the placeholder.

@p0 @UC-INBOX-001 @S01 @smoke
Feature: Submission Inbox — connection smoke

  As an Invoice Reviewer / Approver
  I want the Inbox to load real submissions from the database
  So that I can trust the environment before relying on any other test

  Background:
    Given I am signed in as a CSP ADMIN

  Scenario: Searching the Inbox returns the submissions seeded from the real extract
    When I open the Submission Inbox
    Then the submissions table is displayed
    And the results grid shows the Inbox columns
    And the Inbox is awaiting search criteria
    When I search the full seeded date range
    Then the submissions table shows the 50 seeded submissions
    And the pinned submission from the seeded database is listed
    And the rendered rows match the Inbox API
