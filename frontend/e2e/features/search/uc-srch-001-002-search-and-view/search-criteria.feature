# The other two scenarios of UC-SRCH-001-S01 (csp-bmad/.../UC-SRCH-001/gherkin/UC-SRCH-001-S01.feature).
#
# That slice file holds THREE scenarios, not one. The first — a date range plus a type — is the
# journey's entry point in journey.feature, because UC-SRCH-002 continues from it. These two stand
# alone: each exercises a single filter, so neither needs to drill into an invoice.
#
# WHAT CHANGED vs the legacy Gherkin:
#   * the status scenario searches for "APR", which is not a status this app has. The real codes
#     are PRO / UNA / APP / CAN / DFT / DVF / REJ / VER, and the dropdown lists their DESCRIPTIONS
#     — so the option reads "Approved". Logged as SPEC-004 in defects.md.
#   * the invoice-number scenario searches for "INV-2024-001", an invented value. Re-grounded to a
#     real seeded number that returns exactly one row.
#   * both legacy scenarios settle for "at least one row is displayed". That passes even if the
#     filter is ignored entirely, so each is strengthened: the number search asserts the row IS the
#     expected record, field by field, and the status search asserts EVERY returned row carries the
#     status asked for.
#
# WHY THE TWO ASSERT DIFFERENTLY. The invoice-number search hits a record nothing else touches, so
# its result is exactly one row. A status search spans the whole seed, and the review scenarios
# move borrowed invoices in and out of APPROVED while they run — so the total is legitimately
# variable and only the per-row property is safe to assert. Pinning a count there would be a flake
# waiting for a parallel run.

@p1 @UC-SRCH-001 @S01
Feature: Invoice search — single-criterion searches

  As an Invoice Searcher
  I want to filter invoices by one criterion at a time
  So that I can find records by whichever detail I happen to know

  Background:
    Given I am on the invoice search screen as a CSP ADMIN

  Scenario: Searching by invoice number returns just that invoice
    When I search by the target invoice number
    Then exactly the target invoice is returned

  Scenario: Searching by status returns only invoices with that status
    When I search by the status "Approved"
    Then every returned invoice has the status "Approved"
