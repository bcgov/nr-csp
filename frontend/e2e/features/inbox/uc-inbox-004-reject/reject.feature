# Re-grounded from UC-INBOX-004-S01 (csp-bmad/_bmad-output/implementation-artifacts/tests/
#   UC-INBOX-004/gherkin/UC-INBOX-004-S01.feature) — Successful Rejection of UNAPPROVED Invoice.
#
# This is the REJECT arm of the reviewer's decision. Its Approve twin is
# features/inbox/uc-inbox-001-003-review-approve-journey/, and closing this asymmetry was the top
# coverage gap that pass left open.
#
# WHAT CHANGED vs the legacy Gherkin, and why (it was authored against the JSF app):
#   * route       'search.xhtml' -> 'invoiceDetails.xhtml'  -> straight to '/invoice/:id'
#                 The legacy slice reaches the invoice via Invoice Search. Navigation is already
#                 covered end-to-end by the UC-INBOX-001..003 journey, so this scenario opens the
#                 invoice directly and stays about the rejection itself.
#   * comment     "[id$='Reviewer_Comment']" textarea       -> '#reviewer-comment'
#   * status      "[id$='invStatusTxt']"                    -> the status pill beside the page title
#   * growl       "[id$='msgs']"                            -> a bottom-right Carbon toast
#   * auth        WebADE + 'invoiceDetails/Reject'          -> CSP mock auth as CSP_ADMIN
#
# TWO RE-GROUNDINGS THAT CHANGE AN ASSERTION, both written up in defects.md:
#   * SPEC-001  The reviewer comment box is NOT empty to begin with. The slice's precondition says
#               it "has not yet been filled"; every seeded UNAPPROVED invoice arrives with a note
#               already in it. The scenario asserts the note is there and then REPLACES it, which
#               is a stronger check than filling a blank box — it proves the new reason overwrote
#               the old one rather than being appended.
#   * SPEC-002  The success text differs: a toast reading "Invoice '<number>' rejected." rather
#               than the growl's "The Invoice has been Rejected successfully."
#
# ONE ASSERTION RE-GROUNDS UNCHANGED, which is worth noting because its Approve counterpart did
# not: "the Reject button is disabled" is literally true here. REJ is not in STATUS_CHANGEABLE, so
# the button stays on screen and goes disabled — whereas Approve is swapped out for an Unapprove
# button rather than disabled.
#
# ⚠ THIS SCENARIO BORROWS SEEDED DATA AND PUTS IT BACK — status *and* reviewer comment.
# Rejecting overwrites the invoice's reviewer note, so unlike the approve journey the teardown has
# to restore two fields, not one. It borrows from the same pinned pool, indexed by parallelIndex so
# concurrent scenarios never take the same row, and the teardown reads both fields back. See
# fixtures/inbox/review-test-data.ts.
#
# NOT COVERED here (see coverage.md): rejecting with no comment (the exception slice — the rule is
# enforced both client- and server-side and is the highest-value thing to author next), and the
# remaining UC-INBOX-004 slices.

@p0 @UC-INBOX-004 @S01
Feature: Reject an invoice — a reviewer rejects an UNAPPROVED invoice with a reason

  As an Invoice Reviewer / Approver
  I want to reject an UNAPPROVED invoice and record why
  So that it leaves the review queue as REJECTED with the reason on the record

  Background:
    Given I am signed in as a CSP ADMIN

  Scenario: Rejecting an UNAPPROVED invoice records the status change and the reason
    Given an UNAPPROVED invoice is waiting to be reviewed
    And I open that invoice directly
    Then the invoice is shown as UNAPPROVED with Reject available
    And the reviewer comment box already holds the existing note

    When I replace the reviewer comment with a rejection reason
    And I reject the invoice
    Then the invoice becomes REJECTED on screen
    And Reject is no longer offered
    And Approve is no longer offered either
    And the rejection and its reason read back from the API
