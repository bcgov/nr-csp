# Re-grounded from four legacy happy-path slices (csp-bmad/_bmad-output/implementation-artifacts/tests/):
#   UC-SUBM-001/gherkin/UC-SUBM-001-S01.feature  Create Purchase Invoice
#   UC-SUBM-002/gherkin/UC-SUBM-002-S01.feature  Add Single Line Item
#   UC-SUBM-003/gherkin/UC-SUBM-003-S01.feature  Successfully Save Draft Invoice
#   UC-SUBM-004/gherkin/UC-SUBM-004-S01.feature  Submit Valid DRAFT Invoice
#
# WHY ONE SCENARIO. The four slices are one continuous journey in this app, not four independent
# states: a line item cannot be added until the invoice has been saved (`canAddLineItem` requires
# `isExisting`), and Submit is only offered on a saved DFT/UNA invoice. Splitting them would mean
# three scenarios that API-seed the very thing the previous scenario just proved through the UI.
#
# WHAT CHANGED vs the legacy Gherkin, and why (it was authored against the JSF app):
#   * route        '/faces/invoiceDetails.xhtml'    -> '/invoice' (new) and '/invoice/:id' (saved)
#   * fields       "[id$='Invoice-Number']" etc.    -> short React ids: #inv-number, #inv-type,
#                                                      #inv-date, #submitted-by, #maturity, #fob-code,
#                                                      #boom-numbers (see pages/subm/InvoicePage.ts)
#   * buttons      'Save Invoice'                   -> 'Save';  'New Line Item' -> 'Add new item'
#   * add line     inline "[id$='newLineItem']" panel -> a MODAL that POSTs to
#                                                      /api/invoices/{id}/line-items immediately
#                                                      (the legacy panel deferred to the next Save)
#   * messages     "[id$='msgsW']" growl            -> a Carbon InlineNotification banner for backend
#                                                      warnings + a bottom-right toast for the action
#   * status       "[id$='invStatusTxt']"           -> the status pill beside the page title
#   * auth         'WebADE' + per-action provisioning -> CSP mock auth as CSP_ADMIN, which carries every
#                                                      invoiceDetails/* permission (permissions.ts)
#
# THREE RE-GROUNDINGS THAT CHANGE AN ASSERTION, not just a selector. Each is written up in
# defects.md beside this file:
#   * SPEC-001  Purchase is submitted by the BUYER, not the Seller. The legacy happy path pairs
#               "Purchase" with "Seller", which its own UC-SUBM-001-S06 exception slice says must
#               FAIL — and the backend agrees (`checkSenderBuyerForInvoiceType`). The legacy
#               Gherkin contradicts itself; this follows the rule, not the happy path.
#   * SPEC-002  Submit lands the invoice in PRO (Processing), not UNA (Unapproved). The legacy
#               Gherkin asserts UNAPPROVED, but UC-SUBM-004-detailed.md step 5 says the status goes
#               to "PRO (PROCESSING) in transit" and only later reaches UNA. PRO is the observable
#               immediately after the click, and is what this app writes.
#   * SPEC-003  "Submission ID" is blank on a manual invoice. The legacy Gherkin asserts
#               "[id$='cspSubmissionId'] is not empty"; here that field shows the BUSINESS
#               submission number, which only an ESF submission has.
#
# ONE DISCOVERED DEFECT sits alongside this journey rather than inside it: DIV-001, the
# submit-reminder warning that a FIRST save never shows. See submit-reminder.feature.
#
# NOT COVERED by this journey (deliberately — see coverage.md): every exception and alternative
# slice of all four UCs, e.g. the missing-required-field, invalid-type-for-date, same-client and
# duplicate-number paths. This is the happy path only, as scoped.

@p0 @UC-SUBM-001 @UC-SUBM-002 @UC-SUBM-003 @UC-SUBM-004 @S01 @journey
Feature: Manual invoice — create, itemise, save as draft, and submit for review

  As an Invoice Submitter entering an invoice by hand
  I want to create a Purchase invoice, add a line item, keep it as a draft, and submit it
  So that it leaves my hands in Processing and enters the review workflow

  Background:
    Given I am signed in to the Invoice screen as a CSP ADMIN

  Scenario: A manual Purchase invoice is created, itemised, re-saved as a draft, and submitted
    Given I start a new invoice

    # --- UC-SUBM-001-S01 — Create Purchase Invoice --------------------------
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
    And the Submission ID stays blank because the invoice is manual
    And the invoice reads back from the API in "DFT" status
    # NOTE: UC-SUBM-001-S01 also expects the submit-reminder warning here. This app raises it on the
    # POST but then discards it during the post-create redirect, so it is never shown on a FIRST
    # save. That is DIV-001, tracked by the deliberately-failing scenario in
    # submit-reminder.feature — not asserted here, so this journey stays a true happy path.

    # --- UC-SUBM-002-S01 — Add Single Line Item -----------------------------
    When I add the seeded line item
    Then the group summary shows one group for the added line item
    And the invoice totals reflect the added line item
    And the line item reads back from the API with its calculated amount

    # --- UC-SUBM-003-S01 — Successfully Save Draft Invoice ------------------
    When I save the invoice
    Then the invoice stays in DFT status
    And the save updated the existing invoice instead of creating another
    And the submit-reminder warning is displayed
    And the invoice reads back from the API in "DFT" status

    # --- UC-SUBM-004-S01 — Submit Valid DRAFT Invoice -----------------------
    When I submit the invoice
    Then the invoice moves to "PRO" status on screen
    And Submit is no longer offered
    And the invoice reads back from the API in "PRO" status
