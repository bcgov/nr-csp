/**
 * INBOX domain — data for the review/approve journey (UC-INBOX-001 / 002 / 003).
 *
 * Kept separate from `inbox-test-data.ts` (which pins the search/smoke anchors) so the two concerns
 * can drift independently.
 *
 * Seed image: ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23
 *
 * ---------------------------------------------------------------------------
 * WHY THIS POOL EXISTS — the constraint that shapes the whole approve slice
 * ---------------------------------------------------------------------------
 * `InvoiceValidator.validateForChangeStatus` refuses an approval when the approver IS the invoice's
 * entry user:
 *
 *     if (APPROVED.equals(newStatus) && Objects.equals(details.entryUserID(), userID))
 *         addError("invoice.entry.user.cannot.approve.it.error", null);
 *
 * Locally, every request is authenticated as ONE fixed username — `auth.mock.username`, default
 * `local-dev-user` (`MockRequestFilter`), with no per-request override. So an invoice this suite
 * creates is always entered by the same user that would approve it, and **can never be approved**.
 *
 * The approve scenario therefore cannot own a row it created. It borrows one of the seeded UNA
 * invoices instead — all of which were entered by legacy IDIR accounts — approves it, and RESTORES
 * it to UNA on teardown.
 *
 * Provenance (run against the seeded DB):
 *   SELECT i.coastal_log_sale_id, i.entry_userid, s.submission_id, s.csp_submission_status_code
 *   FROM the.coastal_log_sale i
 *   JOIN the.csp_submission s ON s.csp_submission_id = i.csp_submission_id
 *   WHERE i.log_sale_entry_status_code = 'UNA'
 *   ORDER BY i.coastal_log_sale_id;
 *   -> 18 rows, every one entered by a legacy IDIR account (never `local-dev-user`)
 *
 * The entry users' actual IDIRs are NOT recorded here. This repository is public and those are real
 * people's login identifiers; all the scenario needs to know is that the account is not the one the
 * suite authenticates as, which `enteredByLegacyUser` states without naming anyone.
 *
 * WHY MUTATING THESE IS SAFE, verified end-to-end before the scenario was written:
 *   * `InvoiceRepository.updateStatus` writes ONLY log_sale_entry_status_code, revision_count,
 *     update_userid and update_timestamp. It does not touch approved_userid or reviewer_notes, so
 *     a PATCH back to UNA is a genuine restore of the only functional field.
 *   * The submission status does NOT cascade. `applySubmissionStatusOnStatusChange` returns early
 *     while any PROCESSING invoice remains in the submission, and both submissions below keep
 *     plenty (57547 has 32 PRO, 119351 has 11). Measured round trip on invoice 2005425:
 *       UNA -> APP -> UNA, submission "Lobby" throughout, invApproved 0 -> 1 -> 0, inbox total 50.
 *   * Residue after a run is therefore audit columns only.
 *
 * IF A RESTORE EVER FAILS, the scenario fails loud, and `preflight/anchors.setup.ts` additionally
 * asserts every pool member is UNA at the start of the next run — so a left-behind APP surfaces
 * immediately instead of silently changing what later runs see. Recover with
 * `./scripts/reset-db.sh`, or PATCH the row back to UNA.
 */

/**
 * ---------------------------------------------------------------------------
 * A SECOND INVARIANT THE POOL MUST HOLD — every row needs a reviewer comment
 * ---------------------------------------------------------------------------
 * REJECTING writes the reviewer comment (`InvoiceService.changeStatus` calls `updateReviewerNotes`
 * whenever `reviewComments` is non-null), so a reject scenario OVERWRITES the seeded note and the
 * teardown has to put the original back.
 *
 * That only works if the original is non-null: the service SKIPS the write when `reviewComments` is
 * null, so a null-noted row could never have a test's comment removed again. All twelve rows below
 * currently carry a note, the fixture refuses to borrow one that does not, and the preflight
 * asserts the invariant for the whole pool — together those stop a future pool addition from
 * quietly becoming unrestorable.
 *
 * Provenance: GET /api/invoices/{id} for each pinned id — every one carries a non-empty note left
 * by a legacy user. The note text is not recorded here; the teardown reads each row's own value at
 * run time and writes exactly that back.
 */

/** One borrowable UNA invoice. */
export type BorrowableInvoice = {
  /** coastal_log_sale_id — the `/invoice/:id` route param and the ViewSubmission link target. */
  invoiceId: number;
  /** Business submission number — the `/submission-history/:submissionId` route param. */
  submissionId: string;
  /**
   * True when the row's `entry_userid` is a legacy account rather than the suite's own mock user —
   * which is what makes an approval permissible (`validateForChangeStatus` refuses an approval by
   * the invoice's own entry user). The identifier itself is deliberately not recorded.
   */
  enteredByLegacyUser: boolean;
};

/**
 * The borrowable pool, in a fixed order. Twelve entries so a scenario can be run concurrently with
 * itself — see `allocateBorrowableInvoice`.
 *
 * ⚠ INVOICE NUMBERS ARE DUPLICATED inside a submission — four of the rows below share one number,
 * and several others share another. Never locate one of these by number; address it by
 * `invoiceId`, which is what the ViewSubmission table's Invoice # link carries in its href. The app
 * itself keys that table on the id "because invoice numbers can be blank or duplicated within a
 * submission" (`pages/ViewSubmission/index.tsx`). The numbers themselves are client-supplied data
 * and are not recorded here.
 */
export const borrowableInvoicePool: readonly BorrowableInvoice[] = [
  { invoiceId: 2002836, submissionId: '57547', enteredByLegacyUser: true },
  { invoiceId: 2005171, submissionId: '57547', enteredByLegacyUser: true },
  { invoiceId: 2005172, submissionId: '57547', enteredByLegacyUser: true },
  { invoiceId: 2005173, submissionId: '57547', enteredByLegacyUser: true },
  { invoiceId: 2005367, submissionId: '119351', enteredByLegacyUser: true },
  { invoiceId: 2005369, submissionId: '119351', enteredByLegacyUser: true },
  { invoiceId: 2005404, submissionId: '119351', enteredByLegacyUser: true },
  { invoiceId: 2005405, submissionId: '119351', enteredByLegacyUser: true },
  { invoiceId: 2005406, submissionId: '119351', enteredByLegacyUser: true },
  { invoiceId: 2005407, submissionId: '119351', enteredByLegacyUser: true },
  { invoiceId: 2005408, submissionId: '119351', enteredByLegacyUser: true },
  { invoiceId: 2005425, submissionId: '119351', enteredByLegacyUser: true },
] as const;

/**
 * Pick this worker's invoice from the pool.
 *
 * The suite runs `fullyParallel`, and a flake check runs the same scenario several times at once
 * (`--repeat-each=5`). Every concurrent copy must borrow a DIFFERENT row — two approving the same
 * invoice would race, and the second would find the Approve button already gone.
 *
 * Playwright guarantees `parallelIndex` is unique across the workers running at any one moment
 * (0..workers-1), so indexing the pool by it hands each concurrent copy its own row. The pool is
 * comfortably larger than the worker count this suite runs with; the modulo keeps it correct rather
 * than out-of-range if that ever stops being true, and `poolExhausted` below lets the caller say so
 * plainly instead of silently double-booking.
 */
export const allocateBorrowableInvoice = (parallelIndex: number): BorrowableInvoice =>
  borrowableInvoicePool[parallelIndex % borrowableInvoicePool.length];

/** True when more workers are running than the pool can give distinct rows to. */
export const poolExhausted = (workerCount: number): boolean => workerCount > borrowableInvoicePool.length;

/**
 * The submissions the pool's invoices belong to, and how the Inbox renders them.
 *
 * Both fall inside `seededDateWindow` (inbox-test-data.ts), so the journey's date-range search
 * returns them. Note the submission STATUS differs from the invoice status: an invoice can be UNA
 * while its submission sits in Lobby, because the submission only moves once no PROCESSING invoices
 * remain in it.
 *
 * Provenance: GET /api/inbox?page=0&size=100
 *   57547  -> submissionDate 2016-04-22, status "Inbox",  invTotal 35, invProcessing 32
 *   119351 -> submissionDate 2018-09-13, status "Lobby",  invTotal 11, invProcessing 11
 *
 * ⚠ TWO DATE FORMS, and the grid shows the second one. The API returns ISO (`2016-04-22`), but the
 * Inbox renders every date through `formatDisplayDate` — `toLocaleDateString('en-CA', { month:
 * 'long', … })` — so the CELL reads "April 22, 2016". Asserting the ISO string against the grid
 * silently fails; keep both and use the right one for the surface being asserted.
 */
export const reviewSubmissions: Record<
  string,
  { submissionDate: string; submissionDateRendered: string; submissionStatus: string }
> = {
  '57547': {
    submissionDate: '2016-04-22',
    submissionDateRendered: 'April 22, 2016',
    submissionStatus: 'Inbox',
  },
  '119351': {
    submissionDate: '2018-09-13',
    submissionDateRendered: 'September 13, 2018',
    submissionStatus: 'Lobby',
  },
};

/**
 * Invoice status in the TWO vocabularies the app shows it in.
 *
 * ⚠ THE SAME INVOICE STATUS READS DIFFERENTLY ON THE TWO SCREENS this journey passes through, and
 * asserting the wrong one fails in a way that looks like wrong data:
 *
 *   * `/invoice/:id`                  -> "UNA" / "APP"        (the CODE)
 *   * `/submission-history/:id`       -> "Unapproved" / "Approved"  (the DESCRIPTION)
 *
 * Both are rendered by the same `InvoiceStatusTag`, which prints whatever string it is handed and
 * maps either form to a colour. They differ because the two endpoints return different fields:
 * `InvoiceResponse.invStatus` carries the code, while the submission-detail response carries the
 * resolved description. Recorded as an observation in defects.md (Verified-not-a-defect #2) rather
 * than a fault — but it is exactly the kind of thing that makes a re-ground look broken.
 */
export const invoiceStatus = {
  /** As `/invoice/:id` renders it. */
  unapproved: 'UNA',
  approved: 'APP',
  rejected: 'REJ',
  /** As the submission detail page's "Decision" column renders it. */
  unapprovedLabel: 'Unapproved',
  approvedLabel: 'Approved',
  rejectedLabel: 'Rejected',
} as const;

/**
 * A fresh rejection reason, unique per scenario so a leaked one is identifiable in the seeded DB
 * and two concurrent rejects can never assert each other's text.
 *
 * The reviewer comment is REQUIRED to reject — both client-side (`runStatusChange('REJ', true)`
 * refuses an empty box with "Reviewer comment is required for this action.") and server-side
 * (`validateForChangeStatus` -> `invoice.reject.need.reviewer.comment.error`).
 *
 * Max length 4000 (`ChangeStatusRequest.reviewComments` / the `reviewComments` column).
 */
export const rejectionReason = (): string => {
  const stamp = String(Date.now()).slice(-8);
  const salt = String(Math.floor(Math.random() * 1000)).padStart(3, '0');
  return `E2E rejection reason ${stamp}${salt}`;
};

/**
 * A fresh unapprove reason, unique per scenario (same rationale as `rejectionReason`).
 *
 * ⚠ THE COMMENT REQUIREMENT FOR UNAPPROVE IS CLIENT-SIDE ONLY, unlike reject. The UI refuses an
 * empty box (`runStatusChange('UNA', true)` → "Reviewer comment is required for this action."), but
 * the backend accepts it: `validateForChangeStatus` only rejects a blank comment for REJ, so
 * `PATCH {"status":"UNA","reviewComments":""}` returns 200. Verified against the running backend.
 * Recorded in defects.md as an inconsistency for BA/QA rather than a fault.
 */
export const unapproveReason = (): string => {
  const stamp = String(Date.now()).slice(-8);
  const salt = String(Math.floor(Math.random() * 1000)).padStart(3, '0');
  return `E2E unapprove reason ${stamp}${salt}`;
};

/**
 * The server-side error when a reject is attempted with no reviewer comment.
 *
 * Verified against the running backend, not copied from the bundle — note the space before the
 * comma, which is in the real message:
 *   PATCH /api/invoices/{id}/status {"status":"REJ","reviewComments":""}
 *   -> 400 invoice.reject.need.reviewer.comment.error
 *      "For rejecting an invoice , reviewer comment is required."
 *
 * Unused by the S01 happy path; pinned here for the exception slice (see defects.md, Coverage gap).
 */
export const rejectWithoutCommentError = 'For rejecting an invoice , reviewer comment is required.';
