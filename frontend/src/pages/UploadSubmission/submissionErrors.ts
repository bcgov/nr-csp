import { type CellIssue, type RowIssues } from '@/components/core/DataPreviewTable';
import { type ValidationMessageResponse } from '@/services/invoice.service';

/** A structural (schema) error flattened into a row for the issues table. */
export interface StructuralIssueRow {
  id: string;
  /** Human category of the problem (Issue column). */
  issue: string;
  /** "line N, col N" source location, or '' when the error has no location. */
  location: string;
  /** The specific problem text (Detail column). */
  detail: string;
}

// Maps a structural error code to a human "Issue" category. Schema/JAXB content
// problems dominate; the rest are format / envelope / IO failures.
const STRUCTURAL_ISSUE_LABEL: Record<string, string> = {
  XSD: 'XML content error',
  JAXB: 'XML content error',
  XML_PARSE: 'XML parse error',
  XML_READ: 'File read error',
  FORMAT_UNRECOGNIZED: 'Unrecognized format',
  ENVELOPE_PARSE_ERROR: 'Envelope error',
  ENVELOPE_UNRECOGNIZED: 'Envelope error',
  ENVELOPE_MISSING_CONTENT: 'Envelope error',
  ENVELOPE_NO_BODY: 'Envelope error',
  ENVELOPE_EXTRACTION_FAILED: 'Envelope error',
};

// Structural messages are rendered by the backend as "line N, col N: <detail>"
// (schema errors) or just "<detail>" (format/envelope errors). Split the
// leading location off so it can sit in its own column.
const LOCATION_RE = /^(line \d+, col \d+): ?([\s\S]*)$/;

// Xerces schema messages are prefixed with an error code like "cvc-type.3.1.3: "
// or "cvc-fractionDigits-valid: ". Drop it so the Detail column reads as plain
// prose — the Issue column already says it is an XML content error.
const SCHEMA_CODE_RE = /^cvc-[\w.-]+:\s*/;

/** Flattens structural validation errors into rows for the issues table. */
export const toStructuralIssueRows = (errors: ValidationMessageResponse[]): StructuralIssueRow[] =>
  errors.map((e, i) => {
    const match = LOCATION_RE.exec(e.message);
    const detail = (match ? match[2] : e.message).replace(SCHEMA_CODE_RE, '');
    return {
      id: `structural-${i}`,
      issue: STRUCTURAL_ISSUE_LABEL[e.messageKey] ?? 'XML content error',
      location: match ? match[1] : '',
      detail,
    };
  });

const csvCell = (value: string): string => (/[",\n\r]/.test(value) ? `"${value.replaceAll('"', '""')}"` : value);

/** Serialises the issues table to CSV (Issue, File location, Detail). */
export const structuralIssuesToCsv = (rows: StructuralIssueRow[]): string =>
  [['Issue', 'File location', 'Detail'], ...rows.map((r) => [r.issue, r.location, r.detail])]
    .map((cols) => cols.map(csvCell).join(','))
    .join('\r\n');

/**
 * Maps a business-validation message key to the invoice field it concerns. The
 * field name is used to mark the matching field in the expanded "Invoice
 * details" card with an inline error/warning icon. Keys not listed here attach
 * at the row level instead. (The reference/location rules deliberately target
 * the supplementary detail fields — replaces/adjusts numbers, client location
 * codes — since those are the fields shown in the details card.)
 */
export const INVOICE_KEY_TO_FIELD: Record<string, string> = {
  // ── Summary fields (shown in the invoice row) ──────────────────────────────
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
  'invoice.submitter.not.equal.seller.client.number.error': 'sellerClientNumber',
  'invoice.number.duplicate.same.type.warning': 'invoiceNumber',

  // ── Detail-card fields (shown in the expanded "Invoice details" card) ──────
  // Replaces / Adjusts invoice numbers
  'invoice.replace.invoicenumber.error': 'replacesInvoiceNumbers',
  'invoice.replace.with.itself.error': 'replacesInvoiceNumbers',
  'invoice.morethanmax.replace.invoicenum.error': 'replacesInvoiceNumbers',
  'invoice.both.replace.adjust.invoicenum.error': 'replacesInvoiceNumbers',
  'invoice.adjust.invoicenumber.error': 'adjustsInvoiceNumbers',
  'invoice.adjust.with.itself.error': 'adjustsInvoiceNumbers',
  'invoice.morethanmax.adjust.invoicenum.error': 'adjustsInvoiceNumbers',
  // Client location codes
  'invoice.seller.client.location.invalid.error': 'sellerClientLocnCode',
  'invoice.submitter.not.equal.seller.client.location.error': 'sellerClientLocnCode',
  'invoice.buyer.client.location.invalid.error': 'buyerClientLocnCode',
  // Other party
  'invoice.otherparty.seller.name.required.error': 'otherPartyName',
  'invoice.otherparty.buyer.name.required.error': 'otherPartyName',
  'invoice.manual.other.party.name.error': 'otherPartyName',
  'invoice.otherparty.error': 'otherPartyName',
  'invoice.otherparty.seller.city.required.error': 'otherPartyCity',
  'invoice.otherparty.buyer.city.required.error': 'otherPartyCity',
  'invoice.otherparty.seller.province.required.error': 'otherPartyProvState',
  'invoice.otherparty.buyer.province.required.error': 'otherPartyProvState',
  // Primary sort code
  'invoice.primary.sortcode.invalid.error': 'primarySortCode',
  // Source-document references (boom / timber / weigh)
  'invoice.boomnumber.duplicate.warning': 'boomNumbers',
  'invoice.morethan.Max.boomnumbers.error': 'boomNumbers',
  'invoice.oneofthe.boom.timber.wiegh.requiered.error': 'boomNumbers',
  'invoice.morethan.Max.timbermarks.error': 'timberMarks',
  'invoice.morethan.Max.weighslips.error': 'weighSlipNumbers',
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

/**
 * Maps a submission-level message key to the metadata field(s) it should
 * highlight. A key may target more than one field (e.g. the submitter
 * client-number + location combination is one message about two fields).
 */
export const SUBMISSION_KEY_TO_FIELD: Record<string, string | string[]> = {
  'invoice.month.completed.warning': 'monthComplete',
  'invoice.submitter.client.location.invalid.error': ['submissionClientNumber', 'submissionClientLocnCode'],
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
  if (!match) {
    // A submission-level message carries a bare "submission" locator (no invoice
    // index). Strip it so the field/banner text reads cleanly; it stays
    // unlocated so it routes via SUBMISSION_KEY_TO_FIELD.
    if (prefix.toLowerCase() === 'submission') return { body };
    return { body: message };
  }
  return {
    invoiceIndex: Number(match[1]),
    lineIndex: match[2] ? Number(match[2]) : undefined,
    body,
  };
};

/** A validation issue flattened for the top-of-page banner list. */
export interface IssueBanner {
  key: string;
  /** Row context, e.g. "Invoice #2 (INV-001)" or "Invoice #2, line 3". */
  label: string;
  message: string;
  type: 'ERROR' | 'WARNING';
}

/**
 * Flattens the mapped invoice- and line-item issues into a labelled list so
 * every inline error/warning is also surfaced as a banner at the top of the
 * page. `invoiceNumberByIndex` supplies the invoice number for a friendlier
 * label. Submission-level (form) issues are rendered separately.
 */
export const collectIssueBanners = (
  issues: MappedIssues,
  invoiceNumberByIndex: Record<number, string | null | undefined>,
): IssueBanner[] => {
  const banners: IssueBanner[] = [];

  const invoiceLabel = (index: number): string => {
    const number = invoiceNumberByIndex[index];
    return number ? `Invoice #${index} (${number})` : `Invoice #${index}`;
  };

  const pushRow = (label: string, keyPrefix: string, rowIssues: RowIssues): void => {
    [...Object.values(rowIssues.fields).flat(), ...rowIssues.row].forEach((issue, i) =>
      banners.push({ key: `${keyPrefix}-${i}`, label, message: issue.message, type: issue.type }),
    );
  };

  for (const index of Object.keys(issues.invoices)
    .map(Number)
    .sort((a, b) => a - b)) {
    pushRow(invoiceLabel(index), `inv-${index}`, issues.invoices[index]);
  }

  for (const key of Object.keys(issues.lineItems)) {
    const [invoiceIndex, lineIndex] = key.split(':').map(Number);
    pushRow(`${invoiceLabel(invoiceIndex)}, line ${lineIndex}`, `li-${key}`, issues.lineItems[key]);
  }

  return banners;
};

const emptyRowIssues = (): RowIssues => ({ fields: {}, row: [] });

const addToRow = (rowIssues: RowIssues, field: string | undefined, issue: CellIssue): void => {
  if (field) {
    rowIssues.fields[field] ??= [];
    rowIssues.fields[field].push(issue);
  } else {
    rowIssues.row.push(issue);
  }
};

/** Routes a submission-level message to its mapped field(s), or the banner list. */
const addSubmissionIssue = (result: MappedIssues, messageKey: string, issue: CellIssue): void => {
  const mapped = SUBMISSION_KEY_TO_FIELD[messageKey];
  if (!mapped) {
    result.formIssues.push(issue);
    return;
  }
  const fields = Array.isArray(mapped) ? mapped : [mapped];
  for (const field of fields) {
    result.submissionFields[field] ??= [];
    result.submissionFields[field].push(issue);
  }
};

/** Attributes a single message to the invoice/line/field/form it concerns. */
const attributeMessage = (result: MappedIssues, m: ValidationMessageResponse): void => {
  if (m.type === 'ERROR') result.hasErrors = true;
  const { invoiceIndex, lineIndex, body } = parseLocator(m.message);
  const issue: CellIssue = { message: body, type: m.type };

  if (invoiceIndex != null && lineIndex != null) {
    const rowIssues = (result.lineItems[`${invoiceIndex}:${lineIndex}`] ??= emptyRowIssues());
    addToRow(rowIssues, LINE_KEY_TO_FIELD[m.messageKey], issue);
  } else if (invoiceIndex != null) {
    const rowIssues = (result.invoices[invoiceIndex] ??= emptyRowIssues());
    addToRow(rowIssues, INVOICE_KEY_TO_FIELD[m.messageKey], issue);
  } else {
    addSubmissionIssue(result, m.messageKey, issue);
  }
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

  for (const m of messages) attributeMessage(result, m);

  return result;
};

/**
 * True when the currently-mapped issues contain any ERROR (a "hard" error), as
 * opposed to warnings. Computed live from `issues` rather than the map-time
 * `hasErrors` flag so it reacts as the user clears errors on editable metadata
 * fields — errors on read-only invoice/line data stay and keep it true.
 */
export const hasHardErrors = (issues: MappedIssues | null): boolean => {
  if (!issues) return false;
  const anyError = (list: CellIssue[]): boolean => list.some((i) => i.type === 'ERROR');
  const rowHasError = (r: RowIssues): boolean => anyError(r.row) || Object.values(r.fields).some(anyError);
  return (
    anyError(issues.formIssues) ||
    Object.values(issues.submissionFields).some(anyError) ||
    Object.values(issues.invoices).some(rowHasError) ||
    Object.values(issues.lineItems).some(rowHasError)
  );
};

/** One flattened issue belonging to a single invoice (its own or one of its lines). */
export interface InvoiceIssue {
  type: 'ERROR' | 'WARNING';
  /** 1-based line index when the issue belongs to a line item; absent for invoice-level issues. */
  lineIndex?: number;
  message: string;
}

/** ERROR outranks WARNING outranks clean, for a per-invoice roll-up. */
export type InvoiceSeverity = 'error' | 'warning' | 'none';

/**
 * Flattens one invoice's issues — its own invoice-level issues plus every issue
 * on its line items — into a single list, errors first (invoice-level before
 * line-level within each severity). Drives the per-invoice badge and the local
 * issue list in the expanded panel.
 */
export const collectInvoiceIssues = (issues: MappedIssues | null, invoiceIndex: number): InvoiceIssue[] => {
  if (!issues) return [];

  // Flatten a row's field + row-level issues into InvoiceIssue records, tagging
  // each with its line index when the issue belongs to a line item.
  const flatten = (rowIssues: RowIssues, lineIndex?: number): InvoiceIssue[] =>
    [...Object.values(rowIssues.fields).flat(), ...rowIssues.row].map((i) => ({
      type: i.type,
      lineIndex,
      message: i.message,
    }));

  const invoice = issues.invoices[invoiceIndex];
  const invoiceIssues = invoice ? flatten(invoice) : [];

  const lineIssues = Object.entries(issues.lineItems).flatMap(([key, rowIssues]) => {
    const [idx, lineIndex] = key.split(':').map(Number);
    return idx === invoiceIndex ? flatten(rowIssues, lineIndex) : [];
  });

  // Errors before warnings; Array.prototype.sort is stable, so insertion order
  // (invoice-level before line-level) is preserved within each severity.
  const rank = (type: InvoiceIssue['type']): number => (type === 'ERROR' ? 0 : 1);
  return [...invoiceIssues, ...lineIssues].sort((a, b) => rank(a.type) - rank(b.type));
};

/** The worst severity present in a flattened invoice-issue list. */
export const invoiceSeverity = (issues: InvoiceIssue[]): InvoiceSeverity => {
  if (issues.some((i) => i.type === 'ERROR')) return 'error';
  if (issues.length > 0) return 'warning';
  return 'none';
};

/**
 * The worst severity across the whole mapped submission — every form, field,
 * invoice and line-item issue combined. Drives the page-level status banner:
 * 'error' when any ERROR is present, 'warning' when only warnings remain,
 * 'none' when clean. Computed live from `issues` like {@link hasHardErrors},
 * so it downgrades as the user clears errors on editable metadata fields.
 */
export const submissionSeverity = (issues: MappedIssues | null): InvoiceSeverity => {
  if (!issues) return 'none';
  if (hasHardErrors(issues)) return 'error';
  const rowHasIssue = (r: RowIssues): boolean =>
    r.row.length > 0 || Object.values(r.fields).some((list) => list.length > 0);
  const hasAny =
    issues.formIssues.length > 0 ||
    Object.values(issues.submissionFields).some((list) => list.length > 0) ||
    Object.values(issues.invoices).some(rowHasIssue) ||
    Object.values(issues.lineItems).some(rowHasIssue);
  return hasAny ? 'warning' : 'none';
};
