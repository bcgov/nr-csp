/**
 * INBOX domain — known-good data pinned from the SEEDED database.
 *
 * Every value here was discovered against the real seeded DB, never invented. If the DB image is
 * rebuilt from a fresh extract these may move: `preflight/anchors.setup.ts` fails fast with one
 * clear message so you re-ground HERE rather than chasing mid-suite reds.
 *
 * Seed image: ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23
 *   50 submissions / 2,273 invoices / 15,122 line items (AUTO seed = busiest 50 submissions).
 */

/**
 * The busiest Electronic submission in the seed — the most stable, most visible Inbox row.
 *
 * Provenance (run against the seeded DB):
 *   SELECT s.submission_id, s.csp_submission_id, COUNT(i.coastal_log_sale_id), sc.description
 *   FROM the.csp_submission s
 *   JOIN the.coastal_log_sale i ON i.csp_submission_id = s.csp_submission_id
 *   JOIN the.csp_submission_status_code sc ON sc.csp_submission_status_code = s.csp_submission_status_code
 *   WHERE s.submission_id IS NOT NULL
 *   GROUP BY s.submission_id, s.csp_submission_id, sc.description, s.entry_timestamp
 *   ORDER BY COUNT(i.coastal_log_sale_id) DESC, s.submission_id
 *   FETCH FIRST 3 ROWS ONLY;
 *   -> submission_id=56835  csp_submission_id=100010003  invoices=147  status=Inbox  entry=2016-02-26
 */
export const busiestSubmission = {
  /** Business submission number — the "Submission ID" column, and what the detail route keys on. */
  submissionId: '56835',
  /** Surrogate PK on csp_submission (not shown in the UI; useful for DB-side checks). */
  cspSubmissionId: 100_010_003,
  /** csp_submission_status_code.description as the Inbox renders it. */
  submissionStatus: 'Inbox',
  submissionType: 'Electronic',
  /** entry_timestamp, ISO — the API's submissionDate. */
  submissionDate: '2016-02-26',
  /**
   * NOTE: the API's `invTotal` is csp_submission.NUMBER_INVOICES_SUBMITTED (the DECLARED count = 150),
   * which does NOT equal the 147 coastal_log_sale rows actually present. That gap exists in the
   * SOURCE data (fortmp1), not in the extract — the extract pulls every invoice whose
   * csp_submission_id matches. Don't "fix" it; assert against the declared value the app shows.
   */
  declaredInvoiceTotal: 150,
  actualInvoiceRows: 147,
} as const;

/**
 * Total rows the Inbox returns with no filters applied — every seeded submission that has at least
 * one invoice (the Inbox INNER JOINs coastal_log_sale, so an invoice-less submission never appears).
 *
 * Provenance:
 *   SELECT COUNT(*) FROM (SELECT s.csp_submission_id FROM the.csp_submission s
 *     JOIN the.coastal_log_sale i ON i.csp_submission_id = s.csp_submission_id
 *     GROUP BY s.csp_submission_id);              -> 50
 */
export const unfilteredInboxRowCount = 50;

/**
 * A date range that spans every seeded submission, for the unfiltered-equivalent search.
 *
 * The Inbox requires at least one criterion before it queries (until then the grid shows only its
 * empty-state row), so "show me everything" is expressed as a range wide enough to cover the seed.
 *
 * Provenance:
 *   SELECT TO_CHAR(MIN(entry_timestamp),'YYYY-MM-DD'), TO_CHAR(MAX(entry_timestamp),'YYYY-MM-DD')
 *   FROM the.csp_submission;                     -> 2015-10-01 .. 2018-09-13
 * Padded by a year on each side so a re-extract that shifts the window slightly still matches.
 */
export const seededDateWindow = {
  start: '2015-01-01',
  end: '2019-12-31',
} as const;

/** Column headers the Inbox grid renders, exactly as the DOM shows them (note the lowercase "date"). */
export const inboxColumnHeaders = [
  'Submission ID',
  'Submission date',
  'Status',
  'Type',
  'Total',
  'Approved',
  'Rejected',
  'Processing',
  'Cancelled',
] as const;
