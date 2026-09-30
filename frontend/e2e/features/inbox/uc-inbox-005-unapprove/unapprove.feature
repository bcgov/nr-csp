# Re-grounded from UC-INBOX-005-S01 (csp-bmad/_bmad-output/implementation-artifacts/tests/
#   UC-INBOX-005/gherkin/UC-INBOX-005-S01.feature) — Successful Unapprove of Approved Invoice.
#
# Unapprove reverses an approval so corrections can be made. It completes the reviewer decision set
# alongside Approve (UC-INBOX-003) and Reject (UC-INBOX-004).
#
# HOW THE APPROVED INVOICE IS OBTAINED. The seeded borrowable pool holds UNAPPROVED invoices, so
# the scenario approves one through the API as an ARRANGE step and then unapproves it through the
# UI. Approving via the UI first would make this a second copy of UC-INBOX-003 and blur which
# action is under test. The arrange passes no reviewer comment, so the invoice's seeded note is
# left untouched — which is what lets the scenario prove that UNAPPROVING is what replaced it.
#
# WHAT CHANGED vs the legacy Gherkin, and why (it was authored against the JSF app):
#   * route      'invoiceDetails.xhtml'   -> '/invoice/:id'
#   * comment    Reviewer Comment textarea -> '#reviewer-comment'
#   * status     "[id$='invStatusTxt']"    -> the status pill beside the page title
#   * message    growl                     -> a bottom-right Carbon toast
#   * auth       WebADE + 'invoiceDetails/Unapprove' -> CSP mock auth as CSP_ADMIN
#
# THREE RE-GROUNDINGS THAT CHANGE AN ASSERTION, all written up in defects.md:
#   * SPEC-001  "the Unapprove button is disabled" becomes "Unapprove is absent". Approve and
#               Unapprove share one slot in the button row, so unapproving SWAPS the control back
#               to Approve rather than disabling it — the exact mirror of UC-INBOX-003.
#   * SPEC-002  The success text differs: a toast reading "Invoice '<number>' unapproved." rather
#               than "The Invoice has been Un-Approved successfully."
#   * SPEC-003  The reviewer comment box is not empty on arrival — every seeded invoice carries a
#               note, so the scenario REPLACES it rather than filling a blank field.
#
# ONE ASYMMETRY FOUND WHILE AUTHORING, worth BA/QA's attention: the UI requires a reviewer comment
# to unapprove, but the BACKEND does not. `validateForChangeStatus` only enforces the comment for
# REJ, so PATCH {"status":"UNA","reviewComments":""} returns 200. Reject enforces it on both sides.
# See defects.md, BUG-002.
#
# ⚠ THIS SCENARIO BORROWS SEEDED DATA AND PUTS IT BACK — status *and* reviewer comment, exactly as
# the reject scenario does. It borrows from the same pinned pool, indexed by parallelIndex so
# concurrent scenarios never take the same row. See fixtures/inbox/review-test-data.ts.
#
# NOT COVERED here (see coverage.md): unapproving with no comment, re-approving after an unapprove,
# and the remaining UC-INBOX-005 slices.

@p0 @UC-INBOX-005 @S01
Feature: Unapprove an invoice — a reviewer reverses an approval

  As an Invoice Reviewer / Approver
  I want to unapprove an APPROVED invoice
  So that corrections can be made and the invoice can go back through the workflow

  Background:
    Given I am signed in as a CSP ADMIN

  Scenario: Unapproving an APPROVED invoice returns it to UNAPPROVED with a reason recorded
    Given an UNAPPROVED invoice is waiting to be reviewed
    And that invoice has already been approved
    And I open that invoice directly
    Then the invoice is shown as APPROVED with Unapprove available
    And the reviewer comment box already holds the existing note

    When I replace the reviewer comment with an unapprove reason
    And I unapprove the invoice
    Then the invoice returns to UNAPPROVED on screen
    And Unapprove is replaced by an enabled Approve
    And Submit becomes available again
    And the unapproval and its reason read back from the API
