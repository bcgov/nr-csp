# Re-grounded from three legacy happy-path slices (csp-bmad/_bmad-output/implementation-artifacts/tests/):
#   UC-INBOX-001/gherkin/UC-INBOX-001-S01.feature  Search Inbox with Date Range and Find Matching Results
#   UC-INBOX-002/gherkin/UC-INBOX-002-S01.feature  View UNAPPROVED Invoice Details with Approve and Reject Available
#   UC-INBOX-003/gherkin/UC-INBOX-003-S01.feature  Successful Approval of UNAPPROVED Invoice
#
# WHY ONE SCENARIO. The three slices are one reviewer's path through three screens —
# /inbox -> /submission-history/:submissionId -> /invoice/:id -> approve. Each screen is reached by
# following the previous screen's own link, which is the part worth testing: navigating straight to
# a URL would skip the linkage these slices exist to cover.
#
# WHAT CHANGED vs the legacy Gherkin, and why (it was authored against the JSF app):
#   * routes        '/faces/inbox.xhtml' + 'invoiceDetails.xhtml'  -> '/inbox',
#                                                  '/submission-history/:submissionId', '/invoice/:id'
#   * an extra screen: legacy went inbox -> invoice directly. This app puts the submission detail
#                      page between them, so the journey passes through it (and asserts it).
#   * date fields   "[id$='CalDate1'/'CalDate2']"  -> '#date-start' / '#date-end'
#   * results table "[id$='submissionsTable']"     -> the Carbon DataTable, addressed by ROLE
#   * status        "[id$='invStatusTxt']"         -> the status pill beside the invoice page title
#   * panels        "[id$='Invoice_Group_Summary']", "[id$='Boom_Numbers']" etc.
#                                                  -> the "Invoice group summary" table and the
#                                                     #boom-numbers / #timber-marks / #weigh-slips fields
#   * growl         "Invoice_Form:msgsW"           -> a bottom-right Carbon toast
#   * auth          'WebADE' + provisioning        -> CSP mock auth as CSP_ADMIN, which carries
#                                                     invoiceDetails/Approve and /Reject
#
# TWO RE-GROUNDINGS THAT CHANGE AN ASSERTION, not just a selector. Both are in defects.md:
#   * SPEC-001  "Unapprove is disabled" becomes "Unapprove is absent". Approve and Unapprove share
#               one slot in the button row, so an UNA invoice has no Unapprove button at all, and
#               approving REPLACES Approve with Unapprove rather than disabling it.
#   * SPEC-002  The success text differs — a toast reading "Invoice '<number>' approved." rather
#               than the legacy growl's "The Invoice has been Approved successfully."
#
# ⚠ THIS SCENARIO BORROWS SEEDED DATA AND PUTS IT BACK.
# The backend refuses an approval by the invoice's own entry user, and local mock auth is a single
# fixed username — so the suite can never approve an invoice it created. It borrows one of the
# seeded UNAPPROVED invoices (all entered by legacy IDIR accounts), approves it, and restores it to
# UNA in a fail-loud teardown. Each parallel worker borrows a DIFFERENT invoice from a pinned pool,
# so the scenario is safe to run concurrently with itself. The full reasoning and the evidence that
# the round trip leaves nothing behind are in fixtures/inbox/review-test-data.ts.
#
# NOT COVERED by this journey (see coverage.md): the reject and cancel paths, the
# entry-user-cannot-approve rule, approval with missing price-conversion factors, and every other
# slice of the three UCs. This is the happy path only, as scoped.

@p0 @UC-INBOX-001 @UC-INBOX-002 @UC-INBOX-003 @S01 @journey
Feature: Invoice review — find a submission in the inbox, open its invoice, and approve it

  As an Invoice Reviewer / Approver
  I want to find a submission awaiting review and approve one of its invoices
  So that the invoice leaves the review queue as APPROVED

  Background:
    Given I am signed in as a CSP ADMIN

  Scenario: A reviewer searches the inbox, opens an UNAPPROVED invoice, and approves it
    Given an UNAPPROVED invoice is waiting to be reviewed

    # --- UC-INBOX-001-S01 — Search the inbox -------------------------------
    When I open the Submission Inbox
    Then the Inbox is awaiting search criteria
    When I search the full seeded date range
    Then the submissions table is displayed
    And the results grid shows the Inbox columns
    And the submission holding that invoice is listed in the inbox results

    # --- UC-INBOX-002-S01 — Open the submission, then the invoice ----------
    When I open that submission from the inbox
    Then the submission detail page lists its invoices
    And that invoice is shown as UNAPPROVED in the submission
    When I open that invoice from the submission
    Then the invoice details page shows it as UNAPPROVED
    And the invoice shows its line items and source documents
    And Approve and Reject are offered and Unapprove is not

    # --- UC-INBOX-003-S01 — Approve ----------------------------------------
    When I approve the invoice
    Then the invoice becomes APPROVED on screen
    And Approve is replaced by Unapprove
    And the approval reads back from the API
    And the submission is not knocked out of the inbox by this approval
