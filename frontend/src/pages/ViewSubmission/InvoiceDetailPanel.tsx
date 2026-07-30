import { type FC, type ReactNode } from 'react';

import DetailSection, { type DetailItem } from '@/components/core/DetailSection';
import ResultsTable, { type ResultsTableColumn } from '@/components/Form/ResultsTable';
import { type SubmissionInvoiceResponse, type SubmissionLineItemResponse } from '@/services/submissionHistory.service';
import { formatNumber } from '@/utils/format';

export type InvoiceDetailPanelProps = {
  invoice: SubmissionInvoiceResponse;
  /** Line items belonging to this invoice (already filtered by coastal log sale id). */
  lineItems: SubmissionLineItemResponse[];
};

type LineItemRow = SubmissionLineItemResponse & { id: string };

const hasComment = (comment: string | null): boolean => !!comment && comment.trim().length > 0;

/**
 * Wraps a formatted numeric value in a fixed-width, right-aligned, tabular-figure
 * box. The box is left-positioned in its (left-aligned) column so the numbers
 * sit right after the text columns for even spacing across the table, while the
 * right-aligned content + fixed decimals keeps every row's decimal point (and
 * ones/tens/…) lined up down the column. Both the header and the cells use this
 * same box (same `rem` width) so the header lines up directly above the values.
 *
 * Width is in `rem` (NOT `ch`) on purpose: the header is bold and the cells are
 * regular weight, and a `ch` is font-relative, so a bold `ch` is wider than a
 * regular one — a `rem` width renders identically in both.
 *
 * @param value - The pre-formatted string (e.g. from `formatNumber`).
 * @param rem - Box width in `rem`; must be >= the column's widest value (in the
 *   BOLD header weight too, since the header uses the same box).
 */
const numeric = (value: string, rem: number): ReactNode => (
  <span
    style={{
      display: 'inline-block',
      width: `${rem}rem`,
      textAlign: 'right',
      fontVariantNumeric: 'tabular-nums',
    }}
  >
    {value}
  </span>
);

/** Renders an italic "(empty)" placeholder for blank fields, mirroring the design. */
const orEmpty = (value: string | null | undefined): ReactNode =>
  value && value.trim().length > 0 ? value : <em className="invoice-detail-panel__empty">(empty)</em>;

const lineItemColumns: ResultsTableColumn<LineItemRow>[] = [
  { key: 'species', header: 'Species', renderCell: (r) => r.species ?? '—' },
  { key: 'grade', header: 'Grade', renderCell: (r) => r.grade ?? '—' },
  { key: 'sortCode', header: 'Sort Code', renderCell: (r) => r.sortCode ?? '—' },
  { key: 'clientSortCode', header: 'Client Sort Code', renderCell: (r) => r.clientSortCode ?? '—' },
  {
    key: 'pieces',
    header: '# Pieces',
    // Numeric columns: header + each value share the same fixed-width, right-
    // aligned box (see `numeric`), left-positioned in the column — so the
    // columns stay evenly spaced, the header sits directly above the values, and
    // the decimals line up down each column. Header + cell must share the same
    // `rem` width.
    renderHeader: () => numeric('# Pieces', 5),
    renderCell: (r) => numeric(formatNumber(r.pieces), 5),
  },
  {
    key: 'volume',
    header: 'Volume',
    renderHeader: () => numeric('Volume', 6),
    renderCell: (r) => numeric(formatNumber(r.volume == null ? null : Number(r.volume), 3), 6),
  },
  {
    key: 'price',
    header: 'Price',
    renderHeader: () => numeric('Price', 7),
    renderCell: (r) => numeric(formatNumber(r.price == null ? null : Number(r.price), 2), 7),
  },
];

/**
 * Renders the expanded ViewSubmission invoice row: the per-invoice "Invoice
 * details" field grid, the staff comment line, and a nested line-items table.
 *
 * @param {InvoiceDetailPanelProps} props - The invoice and its line items.
 * @returns {JSX.Element} The expanded invoice detail panel.
 */
const InvoiceDetailPanel: FC<InvoiceDetailPanelProps> = ({ invoice, lineItems }) => {
  const invoiceNumber = invoice.invoiceNumber ?? '—';

  const detailItems: DetailItem[] = [
    { label: 'Replaces Invoice Numbers', value: orEmpty(invoice.replacesInvoiceNumbers) },
    { label: 'Adjusts Invoice Numbers', value: orEmpty(invoice.adjustsInvoiceNumbers) },
    { label: 'Seller Client Location Code', value: orEmpty(invoice.sellerClientLocnCode) },
    { label: 'Buyer Client Location Code', value: orEmpty(invoice.buyerClientLocnCode) },
    { label: 'Other Party Name', value: orEmpty(invoice.otherPartyName) },
    { label: 'Other Party City', value: orEmpty(invoice.otherPartyCity) },
    { label: 'Other Party Prov/State', value: orEmpty(invoice.otherPartyProvState) },
    { label: 'Primary Sort Code', value: orEmpty(invoice.primarySortCode) },
    { label: 'Client Primary Sort Code', value: orEmpty(invoice.clientPrimarySortCode) },
    { label: 'Boom Numbers', value: orEmpty(invoice.boomNumbers) },
    { label: 'Timber Marks', value: orEmpty(invoice.timberMarks) },
    { label: 'Weigh Slip Numbers', value: orEmpty(invoice.weighSlips) },
    { label: 'Submitter Notes', value: orEmpty(invoice.submitterNotes), fullWidth: true },
  ];

  const lineItemRows: LineItemRow[] = lineItems.map((li, i) => ({
    ...li,
    id: `${invoiceNumber}-li-${i}`,
  }));

  return (
    <div className="invoice-detail-panel">
      <DetailSection
        className="invoice-detail-panel__details"
        title={`Invoice details for ${invoiceNumber}`}
        items={detailItems}
      />

      <p className="invoice-detail-panel__staff-comment">
        <strong>Staff Comment:</strong>{' '}
        {hasComment(invoice.staffComment) ? (
          invoice.staffComment
        ) : (
          <span className="invoice-detail-panel__muted">No comment provided (optional for approved invoices)</span>
        )}
      </p>

      <div className="invoice-detail-panel__line-items">
        <p className="invoice-detail-panel__line-items-title">
          Line items for {invoiceNumber} ({lineItemRows.length})
        </p>
        <ResultsTable
          rows={lineItemRows}
          columns={lineItemColumns}
          size="sm"
          hasSearched
          withZebraStyles={false}
          emptyTitle="No line items"
          emptyDescription="This invoice has no line items."
        />
      </div>
    </div>
  );
};

export default InvoiceDetailPanel;
