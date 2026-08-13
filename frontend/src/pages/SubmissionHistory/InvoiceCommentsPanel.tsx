import { type FC } from 'react';
import { Loading } from '@carbon/react';

import InvoiceStatusTag from '@/components/core/Tags/InvoiceStatusTag';
import { useSubmissionInvoiceCommentsQuery } from '@/services/submissionHistory.service';

export type InvoiceCommentsPanelProps = {
  submissionId: number | null;
  /** Gates the network request so collapsed rows never fetch. */
  enabled: boolean;
};

const hasComment = (comment: string | null): boolean => !!comment && comment.trim().length > 0;

const isRejected = (status: string | null): boolean => (status ?? '').toLowerCase().includes('reject');

/**
 * Renders the expanded Submission History row's "Invoice comments" sub-table:
 * each invoice with its status pill and reviewer comment. Comment data is lazily
 * fetched the first time the row is expanded.
 *
 * @param {InvoiceCommentsPanelProps} props - The submission id and expansion flag.
 * @returns {JSX.Element} The invoice-comments panel.
 */
const InvoiceCommentsPanel: FC<InvoiceCommentsPanelProps> = ({ submissionId, enabled }) => {
  const { data, isLoading, isError } = useSubmissionInvoiceCommentsQuery(submissionId, enabled);

  const testId = `invoice-comments-panel-${submissionId}`;

  if (isLoading) {
    return (
      <div className="invoice-comments-panel invoice-comments-panel--status" data-testid={testId}>
        <Loading withOverlay={false} small description="Loading invoice comments" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="invoice-comments-panel invoice-comments-panel--status" data-testid={testId}>
        <p className="invoice-comments-panel__error">Failed to load invoice comments. Please try again.</p>
      </div>
    );
  }

  const invoices = data ?? [];

  if (invoices.length === 0) {
    return (
      <div className="invoice-comments-panel invoice-comments-panel--status" data-testid={testId}>
        <p className="invoice-comments-panel__empty">This submission has no invoices.</p>
      </div>
    );
  }

  const commentedCount = invoices.filter((inv) => hasComment(inv.comment)).length;

  return (
    <div className="invoice-comments-panel" data-testid={testId}>
      <p className="invoice-comments-panel__header">
        Invoice comments ({commentedCount} of {invoices.length} have comments)
      </p>
      <div className="invoice-comments-panel__table-wrap">
        <table className="invoice-comments-panel__table">
          <thead>
            <tr>
              <th scope="col">Invoice #</th>
              <th scope="col">Status</th>
              <th scope="col">Comment</th>
            </tr>
          </thead>
          <tbody>
            {invoices.map((inv, i) => (
              <tr key={`${inv.invoiceNumber ?? 'inv'}-${i}`}>
                <td>{inv.invoiceNumber ?? '—'}</td>
                <td>{inv.status ? <InvoiceStatusTag status={inv.status} /> : '—'}</td>
                <td
                  className={
                    hasComment(inv.comment) && isRejected(inv.status)
                      ? 'invoice-comments-panel__comment invoice-comments-panel__comment--rejected'
                      : 'invoice-comments-panel__comment'
                  }
                >
                  {hasComment(inv.comment) ? inv.comment : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default InvoiceCommentsPanel;
