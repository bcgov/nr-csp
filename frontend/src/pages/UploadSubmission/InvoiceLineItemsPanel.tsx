import { type FC } from 'react';

import DataPreviewTable, { type DataPreviewColumn, type RowIssues } from '@/components/core/DataPreviewTable';
import { type ParsedLineItem } from '@/services/cspSubmission.service';
import { formatCurrency, formatNumber } from '@/utils/format';

export type LineItemRow = ParsedLineItem & { id: string };

export type InvoiceLineItemsPanelProps = {
  /** Invoice number shown in the panel's title band. */
  invoiceNumber: string;
  /** Line items belonging to this invoice (already filtered by invoice index). */
  lineItems: LineItemRow[];
  /**
   * Validation issues keyed by line-item row `id`. The full page-level map can be
   * passed here — only the ids of this invoice's rows are matched.
   */
  issuesByRowId: Record<string, RowIssues>;
};

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
 * Renders the expanded content of a UploadSubmission invoice row: that invoice's
 * line items, in a bordered card with a grey "Line items for …" title band. The
 * table keeps its per-cell validation error/warning markers.
 *
 * @param {InvoiceLineItemsPanelProps} props - The invoice number and its line items.
 * @returns {JSX.Element} The expanded line-items panel.
 */
const InvoiceLineItemsPanel: FC<InvoiceLineItemsPanelProps> = ({ invoiceNumber, lineItems, issuesByRowId }) => (
  <div className="upload-submission-page__line-items-panel">
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

export default InvoiceLineItemsPanel;
