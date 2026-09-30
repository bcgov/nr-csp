# The other two scenarios of UC-FPCP-001-S01 (csp-bmad/.../UC-FPCP-001/gherkin/UC-FPCP-001-S01.feature).
#
# The slice file holds three scenarios; the search-and-edit happy path is in
# search-and-edit.feature. These two stand alone — neither edits anything.
#
# THE SECOND ONE INVERTS ITS LEGACY EXPECTATION, deliberately. The slice says the SEARCH FORM's
# Grade dropdown "updates via AJAX" when Species is selected. In this app it does not: the
# filter-row Grade lists all 17 grades regardless of species. It is the ADD/EDIT DIALOG whose Grade
# is narrowed to the species' valid pairs (and disabled until a species is chosen).
#
# So the behaviour the slice describes exists — on a different control. Rather than quietly assert
# the modal and call the slice covered, this asserts BOTH: that the filter is unnarrowed (the app's
# actual behaviour) and that the dialog is narrowed (where the rule really lives). Logged as
# SPEC-003 in defects.md for BA/QA to decide which control was meant.

@p1 @UC-FPCP-001 @S01
Feature: Flat price conversion — search behaviour

  As a Table Maintenance Administrator
  I want to search the production table without filters and see the grade options behave sensibly
  So that I can browse the table and pick only valid combinations

  Background:
    Given I am on the flat price conversion page as a CSP ADMIN

  Scenario: Searching with no filters returns production rows
    Then the results grid is awaiting criteria
    When I search without setting any filter
    Then the grid returns production rows
    And the results grid shows the flat price columns

  Scenario: Grade options are narrowed by species in the edit dialog, but not in the filters
    Given a production flat price row exists for Birch
    When I search the production table for Birch
    Then every returned row is for Birch
    And the filter Grade dropdown is not narrowed by the chosen species
    And the edit dialog offers only the grades valid for Birch
