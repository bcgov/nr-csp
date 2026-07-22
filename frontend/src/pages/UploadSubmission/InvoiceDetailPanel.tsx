import { type FC, type ReactNode } from 'react';

import DataPreviewTable, {
  type CellIssue,
  type DataPreviewColumn,
  type RowIssues,
} from '@/components/core/DataPreviewTable';
import DetailSection, { type DetailItem } from '@/components/core/DetailSection';
import { type ParsedInvoice, type ParsedLineItem } from '@/services/cspSubmission.service';
import { formatCurrency, formatNumber } from '@/utils/format';

import { FieldIssueMarker, InvoiceIssueList } from './InvoiceIssues';
import { type InvoiceIssue } from './submissionErrors';

export type LineItemRow = ParsedLineItem & { id: string };

export type InvoiceDetailPanelProps = {
  /** The invoice, for the "Invoice details" field grid. */
  invoice: ParsedInvoice;
  /** Invoice number shown in the panel's title bands. */
  invoiceNumber: string;
  /** Line items belonging to this invoice (already filtered by invoice index). */
  lineItems: LineItemRow[];
  /**
   * Validation issues keyed by line-item row `id`. The full page-level map can be
   * passed here — only the ids of this invoice's rows are matched.
   */
  issuesByRowId: Record<string, RowIssues>;
  /** This invoice's issues (its own + its line items), for the local issue list. */
  issues: InvoiceIssue[];
  /**
   * This invoice's per-field issues, keyed by the ParsedInvoice field name, used
   * to mark the matching field in the details grid with an error/warning icon.
   */
  fieldIssues: Record<string, CellIssue[]>;
};

/** Renders an italic "(empty)" placeholder for blank fields, mirroring the ViewSubmission page. */
const orEmpty = (value: string | null | undefined): ReactNode =>
  value && value.trim().length > 0 ? value : <em className="upload-submission-page__detail-empty">(empty)</em>;

// The nested line-item table drops the redundant "Invoice #" column (the panel
// title already names the invoice) and starts at Species, matching the
// ViewSubmission page's expanded line-items table.
const lineItemColumns: DataPreviewColumn<LineItemRow>[] = [
  { key: 'species', header: 'Species', renderCell: (r) => r.species ?? '—' },
  { key: 'grade', header: 'Grade', renderCell: (r) => r.grade ?? '—' },
  { key: 'secondarySortCode', header: 'Sort Code', renderCell: (r) => r.secondarySortCode ?? '—' },
  { key: 'clientSecondarySortCode', header: 'Client Sort Code', renderCell: (r) => r.clientSecondarySortCode ?? '—' },
  { key: 'numberOfPieces', header: '# Pieces', align: 'right', renderCell: (r) => formatNumber(r.numberOfPieces) },
  { key: 'volume', header: 'Volume', align: 'right', renderCell: (r) => formatNumber(r.volume, 3) },
  { key: 'price', header: 'Price', align: 'right', renderCell: (r) => formatCurrency(r.price) },
];

/**
 * Renders the expanded content of a UploadSubmission invoice row: the local
 * validation-issue list, the "Invoice details" field grid (the supplementary
 * invoice fields, matching the ViewSubmission page), and the invoice's
 * line-items table. Tables keep their per-cell validation markers.
 *
 * @param {InvoiceDetailPanelProps} props - The invoice, its line items and issues.
 * @returns {JSX.Element} The expanded invoice detail panel.
 */
const InvoiceDetailPanel: FC<InvoiceDetailPanelProps> = ({
  invoice,
  invoiceNumber,
  lineItems,
  issuesByRowId,
  issues,
  fieldIssues,
}) => {
  // Pair a field's value with its error/warning icon when that field carries an
  // issue, so the marker sits directly beside the value it concerns.
  const withMarker = (fieldKey: keyof ParsedInvoice, value: ReactNode): ReactNode => {
    const fieldIssueList = fieldIssues[fieldKey];
    if (!fieldIssueList?.length) return value;
    return (
      <span className="upload-submission-page__detail-value">
        {value}
        <FieldIssueMarker issues={fieldIssueList} />
      </span>
    );
  };

  const detailItems: DetailItem[] = [
    { label: 'Replaces Invoice Numbers', value: withMarker('replacesInvoiceNumbers', orEmpty(invoice.replacesInvoiceNumbers)) },
    { label: 'Adjusts Invoice Numbers', value: withMarker('adjustsInvoiceNumbers', orEmpty(invoice.adjustsInvoiceNumbers)) },
    { label: 'Seller Client Location Code', value: withMarker('sellerClientLocnCode', orEmpty(invoice.sellerClientLocnCode)) },
    { label: 'Buyer Client Location Code', value: withMarker('buyerClientLocnCode', orEmpty(invoice.buyerClientLocnCode)) },
    { label: 'Other Party Name', value: withMarker('otherPartyName', orEmpty(invoice.otherPartyName)) },
    { label: 'Other Party City', value: withMarker('otherPartyCity', orEmpty(invoice.otherPartyCity)) },
    { label: 'Other Party Prov/State', value: withMarker('otherPartyProvState', orEmpty(invoice.otherPartyProvState)) },
    { label: 'Primary Sort Code', value: withMarker('primarySortCode', orEmpty(invoice.primarySortCode)) },
    { label: 'Client Primary Sort Code', value: withMarker('clientPrimarySortCode', orEmpty(invoice.clientPrimarySortCode)) },
    { label: 'Boom Numbers', value: withMarker('boomNumbers', orEmpty(invoice.boomNumbers)) },
    { label: 'Timber Marks', value: withMarker('timberMarks', orEmpty(invoice.timberMarks)) },
    { label: 'Weigh Slip Numbers', value: withMarker('weighSlipNumbers', orEmpty(invoice.weighSlipNumbers)) },
    { label: 'Submitter Notes', value: withMarker('submitterNotes', orEmpty(invoice.submitterNotes)), fullWidth: true },
  ];

  return (
    <div className="upload-submission-page__line-items-panel">
      <InvoiceIssueList invoiceNumber={invoiceNumber} issues={issues} />

      <DetailSection
        className="upload-submission-page__invoice-details"
        title={`Invoice details for ${invoiceNumber}`}
        items={detailItems}
      />

      <div className="upload-submission-page__line-items">
        <p className="upload-submission-page__line-items-title">
          Line items for {invoiceNumber} ({lineItems.length})
        </p>
        <DataPreviewTable
          rows={lineItems}
          columns={lineItemColumns}
          size="sm"
          emptyMessage="This invoice has no line items."
          issuesByRowId={issuesByRowId}
        />
      </div>
    </div>
  );
};

export default InvoiceDetailPanel;
