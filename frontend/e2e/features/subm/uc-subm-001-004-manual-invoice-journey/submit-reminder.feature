# DIV-001 — the submit-reminder warning is never shown after a FIRST save.
#
# THIS SCENARIO IS EXPECTED TO FAIL, and it is tagged @discovered-divergence so it can be filtered
# out of a "is anything newly broken?" run:
#
#     npm run bddgen && npx playwright test --project=chromium --grep-invert @discovered-divergence
#
# It is deliberately NOT skipped, weakened or inverted. A red here IS the tracking signal, and
# Playwright isolates tests, so this one failing costs nothing elsewhere. When the app is fixed this
# turns green on its own and the defects.md entry can be closed.
#
# WHAT THE SPEC SAYS. UC-SUBM-001-S01 expects the submit-reminder warning immediately after the
# invoice is saved, and UC-SUBM-003-S01 expects the same text in the growl on a draft save. The
# backend agrees: `InvoiceValidator.isSubmitProcessRequiered` raises
# `invoice.submit.saved.warning` on EVERY manual save.
#
# WHAT ACTUALLY HAPPENS. Confirmed against the running stack (localhost:8080):
#   POST /api/invoices        -> warnings: ["If you want the Invoice to be Submitted for ...Submit button"]
#   GET  /api/invoices/{id}   -> warnings: []        (the GET validates with ActionType.OTHER)
#   PUT  /api/invoices/{id}   -> warnings: ["If you want the Invoice to be Submitted for ...Submit button"]
# The warning is therefore RETURNED on create but never rendered: `handleSave` stores it, then
# navigates to `/invoice/{id}`, which changes `invoiceId` and fires the reset effect that clears
# `warnings`; the page then re-hydrates from the GET, which carries no reminder. A second save (a
# PUT, with no navigation) shows it correctly — which is why the main journey asserts it there.
#
# IMPACT (plain language): the first time someone enters an invoice by hand, the app does not tell
# them it still needs submitting. Their invoice sits in Draft, unsubmitted, with nothing on screen
# to say so. The reminder only appears if they happen to press Save a second time.

@p1 @UC-SUBM-001 @S01 @discovered-divergence
Feature: Manual invoice — submit reminder after the first save

  As an Invoice Submitter entering an invoice by hand
  I want to be reminded that saving is not submitting
  So that my invoice does not sit in Draft without me realising

  Background:
    Given I am signed in to the Invoice screen as a CSP ADMIN

  Scenario: Saving a brand-new invoice reminds the submitter that it still needs submitting
    Given I start a new invoice
    When I enter a unique invoice number
    And I select "Purchase" as the invoice type
    And I select "Buyer" as the submitted-by party
    And I choose the seeded submitting client
    And I choose the seeded other party
    And I select "Old Growth" as the maturity
    And I enter the seeded FOB code
    And I add a boom number as the source document reference
    And I enter yesterday's date as the invoice date
    And I save the invoice
    Then the invoice is created and opened in DFT status
    # FAILS TODAY — the backend returned this warning on the POST, but the post-create redirect
    # cleared it before it could be rendered.
    And the submit-reminder warning is displayed
