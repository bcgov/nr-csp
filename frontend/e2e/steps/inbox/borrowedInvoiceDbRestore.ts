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
    `[cleanup] no local sqlplus and no running Docker container named "${DB_CONTAINER}", so the ` +
      `borrowed invoice cannot be restored. Start the seeded-DB container (or set DB_CONTAINER in .env).`,
  );
}

// Hex-encoded so no note text (quotes, &, blank lines) can break sqlplus parsing.
function noteExpression(note: string | null): string {
  if (!note) return 'NULL';
  const hexLines = Buffer.from(note, 'utf8').toString('hex').match(/.{1,1000}/g) ?? [];
  return `UTL_I18N.RAW_TO_CHAR(HEXTORAW(\n    ${hexLines.map((hex) => `'${hex}'`).join('\n    || ')}), 'AL32UTF8')`;
}

function restoreFailed(invoiceId: number, detail: string): Error {
  return new Error(
    `[cleanup] FAILED to restore borrowed invoice ${invoiceId}. The seeded DB is now drifted: restore ` +
      `it by hand or run ./scripts/reset-db.sh. sqlplus said:\n${detail.trim()}`,
  );
}

export type BorrowedInvoiceState = { invStatus: string; reviewComments: string | null };

// Writes only if the row still holds expected (what the API showed), so a wrong DB or a later edit is left alone.
// FOR UPDATE WAIT bounds a held lock in Oracle, because a timeout kill does not stop sqlplus in the container.
export function restoreBorrowedInvoice(
  invoiceId: number,
  expected: BorrowedInvoiceState,
  restored: BorrowedInvoiceState,
): void {
  for (const status of [expected.invStatus, restored.invStatus]) {
    if (!Number.isInteger(invoiceId) || !/^[A-Z]{3}$/.test(status)) {
      throw new Error(`[cleanup] refusing to restore invoice ${invoiceId} with unexpected status "${status}".`);
    }
  }
  const script = `WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
DECLARE
  expected_note VARCHAR2(32767) := ${noteExpression(expected.reviewComments)};
  restored_note VARCHAR2(32767) := ${noteExpression(restored.reviewComments)};
  locked_id NUMBER;
BEGIN
  SELECT coastal_log_sale_id INTO locked_id FROM THE.coastal_log_sale
   WHERE coastal_log_sale_id = ${invoiceId} FOR UPDATE WAIT 10;
  UPDATE THE.coastal_log_sale SET
      log_sale_entry_status_code = '${restored.invStatus}',
      reviewer_notes = restored_note,
      revision_count = revision_count + 1,
      update_timestamp = SYSDATE
  WHERE coastal_log_sale_id = ${invoiceId}
    AND log_sale_entry_status_code = '${expected.invStatus}'
    AND DECODE(reviewer_notes, expected_note, 1, 0) = 1;
  IF SQL%ROWCOUNT <> 1 THEN
    RAISE_APPLICATION_ERROR(-20001, 'Invoice ${invoiceId} in this DB does not match what the app shows, '
      || 'so nothing was restored. Check that ORACLE_DSN is the DB the backend uses.');
  END IF;
  COMMIT;
END;
/
EXIT
`;
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
    const failure = err as { status?: number | null; code?: string; signal?: string; stdout?: Buffer; stderr?: Buffer };
    throw restoreFailed(
      invoiceId,
      `exit ${failure.status ?? failure.code ?? failure.signal}\n${failure.stdout ?? ''}\n${failure.stderr ?? ''}`,
    );
  }
  // sqlplus can exit 0 after an ORA- error, so check its output too.
  if (/ORA-\d{5}/.test(output)) throw restoreFailed(invoiceId, output);
}
