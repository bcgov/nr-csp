import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

/**
 * Puts a borrowed invoice's status and reviewer note back with one SQL update on the local seeded DB.
 * The API cannot do it: the status endpoint only allows the moves the UI offers, so a rejected invoice
 * never leaves REJ, and an unapproved one cannot be set to UNA again just to restore its note.
 *
 * sqlplus is chosen and called the way scripts/apply-patches.sh does it ($SQLPLUS, else a local
 * sqlplus, else the Docker wrapper), with the same defaults. playwright.config.ts loads .env.
 */

const DOCKER_SQLPLUS = fileURLToPath(new URL('../../scripts/docker-sqlplus.sh', import.meta.url));
const ORACLE_DSN = process.env.ORACLE_DSN ?? 'THE/default@localhost:1525/DBDOCK_01';
const DB_CONTAINER = process.env.DB_CONTAINER ?? 'real-data-seeded-csp-db';

export type BorrowedInvoiceState = { invStatus: string; reviewComments: string | null; version: string };

function outputOf(command: string, args: string[]): string | null {
  try {
    return execFileSync(command, args, { stdio: 'pipe' }).toString();
  } catch {
    return null;
  }
}

function sqlplusCommand(): string[] {
  if (process.env.SQLPLUS) return [process.env.SQLPLUS];
  if (outputOf('which', ['sqlplus']) !== null) return ['sqlplus'];
  const runningContainers = outputOf('docker', ['ps', '--format', '{{.Names}}'])?.split('\n') ?? [];
  if (runningContainers.includes(DB_CONTAINER)) return ['bash', DOCKER_SQLPLUS];
  throw new Error(
    `[borrowedInvoice] no local sqlplus and no running Docker container named "${DB_CONTAINER}", so a ` +
      `borrowed invoice could not be restored. Start the seeded-DB container (or set DB_CONTAINER in .env).`,
  );
}

function runSqlplus(script: string, failure: string): void {
  const [command, ...args] = sqlplusCommand();
  let output: string;
  try {
    output = execFileSync(command, [...args, '-S', '-L', ORACLE_DSN], {
      input: script,
      stdio: 'pipe',
      env: { ...process.env, DB_CONTAINER },
      timeout: 60_000,
    }).toString();
  } catch (err) {
    // Not err.message: it repeats the command line, which carries the DSN.
    const e = err as { status?: number | null; code?: string; signal?: string; stdout?: Buffer; stderr?: Buffer };
    throw new Error(`${failure} sqlplus said:\nexit ${e.status ?? e.code ?? e.signal}\n${e.stdout ?? ''}\n${e.stderr ?? ''}`.trim());
  }
  // sqlplus can exit 0 after an ORA- error, so check its output too.
  if (/ORA-\d{5}/.test(output)) throw new Error(`${failure} sqlplus said:\n${output.trim()}`);
}

// The row's revision and update time, as the first half of InvoiceRepository.INVOICE_VERSION_SQL.
// Matching it ties the SQL to the exact row write the API reported, so another DB or a later edit never matches.
function rowIsAtVersion(invoiceId: number, version: string): string {
  const [revision, updated] = version.split('.');
  if (!Number.isInteger(invoiceId) || !/^\d+$/.test(revision ?? '') || !/^\d{14}$/.test(updated ?? '')) {
    throw new Error(`[borrowedInvoice] invoice ${invoiceId} has an unexpected id or version "${version}".`);
  }
  return `coastal_log_sale_id = ${invoiceId}
     AND revision_count || '.' || TO_CHAR(update_timestamp, 'YYYYMMDDHH24MISS') = '${revision}.${updated}'`;
}

const notTheAppsRow = (invoiceId: number) =>
  `RAISE_APPLICATION_ERROR(-20001, 'Invoice ${invoiceId} in this DB is not at the version the app shows. '
      || 'Check that ORACLE_DSN is the DB the backend uses.')`;

// Hex-encoded so no note text (quotes, &, blank lines) can break sqlplus parsing.
function noteExpression(note: string | null): string {
  if (!note) return 'NULL';
  const hexLines = Buffer.from(note, 'utf8').toString('hex').match(/.{1,1000}/g) ?? [];
  return `UTL_I18N.RAW_TO_CHAR(HEXTORAW(\n    ${hexLines.map((hex) => `'${hex}'`).join('\n    || ')}), 'AL32UTF8')`;
}

// Run before the scenario, so a broken sqlplus setup or wrong DB fails while the row is still untouched.
export function checkBorrowedInvoiceRestorable(invoiceId: number, asFound: BorrowedInvoiceState): void {
  runSqlplus(
    `WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
DECLARE
  matching NUMBER;
BEGIN
  SELECT COUNT(*) INTO matching FROM THE.coastal_log_sale
   WHERE ${rowIsAtVersion(invoiceId, asFound.version)};
  IF matching <> 1 THEN
    ${notTheAppsRow(invoiceId)};
  END IF;
END;
/
EXIT
`,
    `[borrowedInvoice] restoring invoice ${invoiceId} would fail, so the scenario did not run and the row is untouched.`,
  );
}

// FOR UPDATE WAIT bounds a held lock in Oracle, because a timeout kill does not stop sqlplus in the container.
export function restoreBorrowedInvoice(
  invoiceId: number,
  afterScenario: BorrowedInvoiceState,
  asFound: BorrowedInvoiceState,
): void {
  const rowMatches = rowIsAtVersion(invoiceId, afterScenario.version);
  if (!/^[A-Z]{3}$/.test(asFound.invStatus)) {
    throw new Error(`[cleanup] refusing to restore invoice ${invoiceId} to unexpected status "${asFound.invStatus}".`);
  }
  runSqlplus(
    `WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
DECLARE
  restored_note VARCHAR2(32767) := ${noteExpression(asFound.reviewComments)};
  locked_id NUMBER;
BEGIN
  SELECT coastal_log_sale_id INTO locked_id FROM THE.coastal_log_sale
   WHERE coastal_log_sale_id = ${invoiceId} FOR UPDATE WAIT 10;
  UPDATE THE.coastal_log_sale SET
      log_sale_entry_status_code = '${asFound.invStatus}',
      reviewer_notes = restored_note,
      revision_count = revision_count + 1,
      update_timestamp = SYSDATE
   WHERE ${rowMatches};
  IF SQL%ROWCOUNT <> 1 THEN
    ${notTheAppsRow(invoiceId)};
  END IF;
  COMMIT;
END;
/
EXIT
`,
    `[cleanup] FAILED to restore borrowed invoice ${invoiceId}. The seeded DB is now drifted: restore it by ` +
      `hand or run ./scripts/reset-db.sh.`,
  );
}
