# Re-grounded from two legacy happy-path slices (csp-bmad/_bmad-output/implementation-artifacts/tests/):
#   UC-SRCH-001/gherkin/UC-SRCH-001-S01.feature  Search Invoices by Multiple Criteria
#   UC-SRCH-002/gherkin/UC-SRCH-002-S01.feature  View Invoice Details — Non-NEW Invoice with Line Items
#
# WHY ONE SCENARIO. UC-SRCH-002's Background is "I have performed a search and the results show a
# non-NEW invoice" — it begins where UC-SRCH-001 ends. Running the search for real and clicking
# through is the point: it also proves the link carries `state: { fromSearch: true }`, which is
# what gives the invoice page its "Invoice search" breadcrumb.
#
# This also closes the gap logged as Coverage gap #3 in
# features/inbox/uc-inbox-004-reject/defects.md — the Search -> invoice path had no coverage; the
# review journey covers the Inbox -> submission -> invoice path instead.
#
# WHAT CHANGED vs the legacy Gherkin, and why (it was authored against the JSF app):
#   * routes    '/faces/search.xhtml' -> '/search';  'invoiceDetails.xhtml?invoiceID=' -> '/invoice/:id'
#   * filters   "[id$='id-startdate_input']" / "[id$='id-enddate_input']" / "[id$='InvoiceType']"
#                                            -> '#start-date' / '#end-date' / '#type-filter'
#   * the Type dropdown lists DESCRIPTIONS -> the option reads "Purchase", not "PUR"
#   * table     '#invoicesTable'             -> the Carbon DataTable, addressed by ROLE
#   * paginator 'Showing <count>'            -> not asserted; the row count is asserted directly,
#                                               and every row is checked against the filter
#   * fieldsets "[id$='Invoice_Address_Information']" etc. -> the Carbon accordion sections
#   * auth      WebADE + 'search/Search', 'search/Details' -> CSP mock auth as CSP_ADMIN
#
# THREE RE-GROUNDINGS THAT CHANGE AN ASSERTION, all written up in defects.md:
#   * SPEC-001  There is no read-only "view from search" screen. The legacy slice expects the page
#               to carry NO Save / Approve / Reject / Submit / Delete / Cancel buttons; this app
#               has ONE invoice screen for viewing and acting, so the buttons are present and
#               gated by status. The scenario asserts they are DISABLED — stronger than absence,
#               since it would catch an action being re-enabled on an approved record.
#   * SPEC-002  There is no Back button. Arriving from search sets `fromSearch`, which adds an
#               "Invoice search" breadcrumb; that is the route back, and asserting it also proves
#               the navigation carried its state rather than the test jumping to the URL.
#   * SPEC-003  "Submission ID" is blank — the pinned record is a manual invoice, and that field
#               shows the BUSINESS submission number, which only an ESF submission has.
#
# ⚠ THE PINNED RECORD FAILS TODAY'S VALIDATOR, and the scenario asserts that rather than ignoring
# it. Opening it paints two error banners, one of which is "The Invoice submitted by Seller cannot
# be type PUR." — a real, APPROVED, historical record doing exactly what the legacy
# UC-SUBM-001-S01 happy path tells you to do, and what the current validator forbids. See
# defects.md.
#
# READ-ONLY, and immune to the rest of the suite by construction: the filter is Purchase-only
# (every borrowable review invoice is SAL or ADJ) and restricted to 2013 (the SUBM journey's
# Purchase invoices are dated today). That is what makes an EXACT row count assertable here.

@p0 @UC-SRCH-001 @UC-SRCH-002 @S01 @journey
Feature: Invoice search — find an invoice by several criteria and view its details

  As an Invoice Searcher
  I want to filter invoices and open one from the results
  So that I can review its header, line items and totals for audit or reference

  Background:
    Given I am on the invoice search screen as a CSP ADMIN

  Scenario: Searching by date range and type finds an invoice, which opens with its full details
    Then the search grid is awaiting criteria
    And the results grid shows the search columns

    # --- UC-SRCH-001-S01 — search by multiple criteria ----------------------
    When I search for Purchase invoices in the 2013 date range
    Then the grid returns only the matching invoices
    And the rendered rows match the search API

    # --- UC-SRCH-002-S01 — view the invoice -----------------------------------
    When I open the target invoice from the results
    Then the invoice header matches the record I picked
    And the invoice shows a Back route to the search results
    And the group summary and totals match the record
    And the record reads back from the API with its line items
    And the actions an APPROVED invoice withholds are all disabled
    And the page reports the validation problems with this legacy record
