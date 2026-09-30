# Re-grounded from UC-FPCP-001-S01 (csp-bmad/_bmad-output/implementation-artifacts/tests/
#   UC-FPCP-001/gherkin/UC-FPCP-001-S01.feature) — Search and Edit Production Table Row.
#
# That slice file holds THREE scenarios. This is the first and the substantial one; the other two
# are in search-variants.feature.
#
# WHAT CHANGED vs the legacy Gherkin, and why (it was authored against the JSF app):
#   * route     'prodFlatPriceConv.xhtml'          -> '/table-maintenance/flat-price-conversion'
#                                                     (the page hardcodes modellingCode 'P')
#   * filters   "[id=\"formSearch:species\"]" etc. -> '#species-filter' / '#maturity-filter' /
#                                                     '#sort-code-filter' / '#grade-filter'
#   * search    "[id=\"formSearch:search\"]"        -> the Search button in the filter row
#   * table     "[id=\"formFPCTable:tbl\"]"         -> the Carbon DataTable, addressed by ROLE
#   * dropdowns show DESCRIPTIONS, not codes -> the Species option reads "Birch", Maturity
#               "Cants / Export", Sort code "Deciduous"; only Grade shows its bare code
#   * auth      'prodFlatPriceConv/Edit' + '/Save' -> CSP mock auth as CSP_ADMIN, which holds them
#
# TWO RE-GROUNDINGS THAT CHANGE AN ASSERTION, both in defects.md:
#   * SPEC-001  EDITING IS A DIALOG, NOT AN INLINE ROW. The legacy screen edited in place — edit
#               icon, editable cells, then a save icon in the row, triggering `saveRowEditAction`.
#               This app opens an "Edit row" modal; no part of the row becomes editable. So "click
#               the inline save icon" becomes "save the dialog", and the dialog closing is itself
#               the proof the update was accepted (it closes only on success).
#   * SPEC-002  The confirmation text differs — a toast reading "Row updated successfully." rather
#               than the growl's "Record has been updated successfully." (which the slice's own
#               TODO admits was never confirmed).
#
# ⚠ THIS SCENARIO CREATES ITS OWN ROW AND DELETES IT. These are LIVE PRICING rows — invoice
# submission reads them — so nothing seeded is edited. Each worker creates its row on a distinct
# effective date, because the backend rejects a duplicate on
# modellingCode + sortCode + species/grade + maturity + effectiveDate and every scenario uses the
# same first four. It works in Birch, while the SUBM journey's invoices use Balsam, so the two
# writing domains never touch the same pricing data.
#
# NOT COVERED here (see coverage.md): adding a row through the dialog (S02), deleting one (S03),
# and every validation slice (S04-S11) — including the relative-price range and the duplicate
# checks, which the edit path enforces and which are the highest-value things to author next.

@p0 @UC-FPCP-001 @S01
Feature: Flat price conversion — search the production table and edit a row

  As a Table Maintenance Administrator
  I want to find a production flat price conversion row and change its relative price
  So that live pricing calculations use the corrected parameter

  Background:
    Given I am on the flat price conversion page as a CSP ADMIN

  Scenario: Searching by species finds a row, which can be edited to a new relative price
    Given a production flat price row exists for Birch
    Then the results grid is awaiting criteria
    And the results grid shows the flat price columns

    When I search the production table for Birch
    Then every returned row is for Birch
    And my row appears with its original relative price

    When I edit my row to a new relative price
    Then the row update is confirmed on screen
    And the grid shows my row at the new relative price
    And the edit reads back from the API
