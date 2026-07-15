import { type CellIssue, type RowIssues } from '@/components/core/DataPreviewTable';
import { type ValidationMessageResponse } from '@/services/invoice.service';

/**
 * Maps a business-validation message key to the invoice-table column it should
 * highlight. Best-effort: keys not listed here attach at the row level instead.
 */
export const INVOICE_KEY_TO_FIELD: Record<string, string> = {
  'invoice.date.required.error': 'invoiceDate',
  'invoice.date.in.future.error': 'invoiceDate',
  'invoice.type.invalid.error': 'invoiceType',
  'invoice.type.not.saleorpurchase.warning': 'invoiceType',
  'invoice.type.invalid.submitter': 'invoiceType',
  'invoice.maturity.invalid.error': 'maturity',
  'invoice.fob.required.error': 'locationFOB',
  'invoice.totalamount.negative.error': 'totalAmount',
  'invoice.totalamount.dismatch.warning': 'totalAmount',
  'invoice.totalvolume.negative.error': 'totalVolume',
  'invoice.totalvolume.dismatch.warning': 'totalVolume',
  'invoice.totalpieces.negative.error': 'totalPieces',
  'invoice.totalpieces.dismatch.warning': 'totalPieces',
  'invoice.seller.client.location.invalid.error': 'sellerClientNumber',
  'invoice.submitter.not.equal.seller.client.number.error': 'sellerClientNumber',
  'invoice.buyer.client.location.invalid.error': 'buyerClientNumber',
  'invoice.replace.invoicenumber.error': 'invoiceNumber',
  'invoice.replace.with.itself.error': 'invoiceNumber',
  'invoice.adjust.invoicenumber.error': 'invoiceNumber',
  'invoice.adjust.with.itself.error': 'invoiceNumber',
  'invoice.both.replace.adjust.invoicenum.error': 'invoiceNumber',
  'invoice.number.duplicate.same.type.warning': 'invoiceNumber',
};

/** Maps a business-validation message key to the line-item-table column. */
export const LINE_KEY_TO_FIELD: Record<string, string> = {
  'invoice.species.grade.combination.error': 'species',
  'invoice.grade.invalid.required.error': 'grade',
  'invoice.grade.z.warning': 'grade',
  'invoice.secondry.sortcode.invalid.error': 'secondarySortCode',
  'invoice.numberof.pieces.negative.or.zero.error': 'numberOfPieces',
  'invoice.volume.negative.value.error': 'volume',
  'invoice.volume.zero.value.warning': 'volume',
  'invoice.price.negative.value.error': 'price',
  'invoice.price.zero.value.warning': 'price',
};

/** Maps a submission-level message key to a metadata field. */
export const SUBMISSION_KEY_TO_FIELD: Record<string, string> = {
  'invoice.month.completed.warning': 'monthComplete',
};

/** The result of attributing every validation message to a row / field / form. */
export interface MappedIssues {
  /** Keyed by 1-based invoice index. */
  invoices: Record<number, RowIssues>;
  /** Keyed by `${invoiceIndex}:${lineIndex}`. */
  lineItems: Record<string, RowIssues>;
  /** Metadata field key -> issues. */
  submissionFields: Record<string, CellIssue[]>;
  /** Submission-level / unlocated messages, shown as a banner list. */
  formIssues: CellIssue[];
  hasErrors: boolean;
}

// Matches the locator prefix the backend prepends to each message, e.g.
// "invoice #2 (INV-001), line 3" or "invoice #2". The optional "(number)" and
// ", line n" are captured so a message can be attributed to an invoice or line.
const LOCATOR_RE = /^invoice #(\d+)(?:\s*\([^)]*\))?(?:,\s*line (\d+))?$/i;

interface Located {
  invoiceIndex?: number;
  lineIndex?: number;
  /** The message with its locator prefix stripped (row context is shown elsewhere). */
  body: string;
}

/**
 * Splits a backend message into its locator (invoice/line index) and body. The
 * backend renders each message as `"<locator>: <text>"`; the locator has no
 * ": " inside it, so the first ": " is the boundary. If the prefix isn't a
 * recognised locator the whole message is treated as the body (unlocated).
 */
const parseLocator = (message: string): Located => {
  const sep = message.indexOf(': ');
  if (sep === -1) return { body: message };
  const prefix = message.slice(0, sep).trim();
  const body = message.slice(sep + 2);
  const match = LOCATOR_RE.exec(prefix);
  if (!match) return { body: message };
  return {
    invoiceIndex: Number(match[1]),
    lineIndex: match[2] ? Number(match[2]) : undefined,
    body,
  };
};

const emptyRowIssues = (): RowIssues => ({ fields: {}, row: [] });

const addToRow = (rowIssues: RowIssues, field: string | undefined, issue: CellIssue): void => {
  if (field) (rowIssues.fields[field] ??= []).push(issue);
  else rowIssues.row.push(issue);
};

/**
 * Attributes each business-validation message to the invoice/line/field it
 * concerns, so the page can render inline errors on the affected rows and cells
 * and a banner for anything submission-level.
 */
export const mapSubmissionIssues = (messages: ValidationMessageResponse[]): MappedIssues => {
  const result: MappedIssues = {
    invoices: {},
    lineItems: {},
    submissionFields: {},
    formIssues: [],
    hasErrors: false,
  };

  for (const m of messages) {
    if (m.type === 'ERROR') result.hasErrors = true;
    const { invoiceIndex, lineIndex, body } = parseLocator(m.message);
    const issue: CellIssue = { message: body, type: m.type };

    if (invoiceIndex != null && lineIndex != null) {
      const key = `${invoiceIndex}:${lineIndex}`;
      const rowIssues = (result.lineItems[key] ??= emptyRowIssues());
      addToRow(rowIssues, LINE_KEY_TO_FIELD[m.messageKey], issue);
    } else if (invoiceIndex != null) {
      const rowIssues = (result.invoices[invoiceIndex] ??= emptyRowIssues());
      addToRow(rowIssues, INVOICE_KEY_TO_FIELD[m.messageKey], issue);
    } else {
      const field = SUBMISSION_KEY_TO_FIELD[m.messageKey];
      if (field) (result.submissionFields[field] ??= []).push(issue);
      else result.formIssues.push(issue);
    }
  }

  return result;
};
