import { ArrowLeft } from '@carbon/icons-react';
import { Grid, Column, Link, Loading } from '@carbon/react';
import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import DetailSection, { type DetailItem } from '@/components/core/DetailSection';
import PageTitle from '@/components/core/PageTitle';
import InvoiceStatusTag from '@/components/core/Tags/InvoiceStatusTag';
import SubmissionStatusTag from '@/components/core/Tags/SubmissionStatusTag';
import ResultsTable, { type ResultsTableColumn } from '@/components/Form/ResultsTable';
import { ROUTES } from '@/routes/routePaths';
import {
  type SubmissionInvoiceResponse,
  type SubmissionLineItemResponse,
  useSubmissionDetailQuery,
} from '@/services/submissionHistory.service';
import { formatCurrency, formatNumber, formatShortDate } from '@/utils/format';

import InvoiceDetailPanel from './InvoiceDetailPanel';
import './index.scss';

type InvoiceRow = SubmissionInvoiceResponse & { id: string; lineItemCount: number };

/** Strips a leading "mailto:" so the email displays as a plain address. */
const cleanEmail = (email: string | null): string | null => (email ? email.replace(/^mailto:/i, '') : email);

/** "n thing" / "n things" — keeps the summary line grammatical. */
const pluralize = (count: number, noun: string): string => `${count} ${noun}${count === 1 ? '' : 's'}`;

export function ViewSubmissionPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data, isLoading, isError, error } = useSubmissionDetailQuery(id);

  const [expandedRowIds, setExpandedRowIds] = useState<Set<string>>(new Set());

  const apiErrorMessage = (() => {
    if (!isError) return null;
    const axiosError = error as { response?: { status?: number; data?: { message?: string } } };
    if (axiosError?.response?.status === 404) return 'Submission not found.';
    return axiosError?.response?.data?.message ?? 'Failed to load the submission. Please try again.';
  })();

  const metadataItems: DetailItem[] = data
    ? [
        { label: 'Email Address', value: cleanEmail(data.email) },
        { label: 'Telephone Number', value: data.telephone },
      ]
    : [];

  const detailItems: DetailItem[] = data
    ? [
        { label: 'Month Complete', value: data.monthComplete },
        { label: 'Seller Submission', value: data.sellerSubmission },
      ]
    : [];

  const submitterItems: DetailItem[] = data
    ? [
        { label: 'Submission Client Number', value: data.clientNumber },
        { label: 'Submission Client Location Code', value: data.clientLocnCode },
      ]
    : [];

  // Line items grouped by their parent invoice number, so each expanded row
  // can render only its own lines and the table can show a per-invoice count.
  const lineItemsByInvoice = useMemo(() => {
    const map = new Map<string, SubmissionLineItemResponse[]>();
    (data?.lineItems ?? []).forEach((li) => {
      const key = li.invoiceNumber ?? '';
      const group = map.get(key) ?? [];
      group.push(li);
      map.set(key, group);
    });
    return map;
  }, [data]);

  const invoiceRows: InvoiceRow[] = (data?.invoices ?? []).map((inv, i) => ({
    ...inv,
    id: inv.coastalLogSaleId?.toString() ?? `inv-${i}`,
    lineItemCount: (lineItemsByInvoice.get(inv.invoiceNumber ?? '') ?? []).length,
  }));

  // Summary line totals across the submission's invoices.
  const totalLineItems = data?.lineItems?.length ?? 0;
  const totalAmount = (data?.invoices ?? []).reduce((sum, inv) => sum + (inv.totalAmount ?? 0), 0);
  const totalVolume = (data?.invoices ?? []).reduce((sum, inv) => sum + (inv.totalVolume ?? 0), 0);
  const totalPieces = (data?.invoices ?? []).reduce((sum, inv) => sum + (inv.totalPieces ?? 0), 0);
  const invoicesSummary = [
    pluralize(invoiceRows.length, 'invoice'),
    pluralize(totalLineItems, 'line item'),
    `Total ${formatCurrency(totalAmount)}`,
    `${formatNumber(totalVolume, 3)} m³`,
    pluralize(totalPieces, 'piece'),
  ].join(' · ');

  const expandAll = () => setExpandedRowIds(new Set(invoiceRows.map((row) => row.id)));
  const collapseAll = () => setExpandedRowIds(new Set());

  const renderContent = () => {
    if (isLoading) {
      return (
        <Column lg={16} md={8} sm={4} className="view-submission-page__loading">
          <Loading withOverlay={false} description="Loading submission" />
        </Column>
      );
    }

    if (isError) {
      return (
        <Column lg={16} md={8} sm={4} className="view-submission-page__error-col">
          <p className="view-submission-page__error">{apiErrorMessage}</p>
        </Column>
      );
    }

    if (!data) {
      return null;
    }

    return (
      <>
        <Column lg={16} md={8} sm={4} className="view-submission-page__summary">
          <SubmissionStatusTag status={data.submissionStatus} />
          <span className="view-submission-page__summary-text">
            Submission by <strong>{data.submittedBy ?? '—'}</strong> — Client Name{' '}
            <strong>
              {data.clientName ?? '—'}
              {data.clientNumber ? ` (${data.clientNumber})` : ''}
            </strong>
            {' · '}
            {formatShortDate(data.submissionDate)}
          </span>
        </Column>

        <Column lg={16} md={8} sm={4} className="view-submission-page__section">
          <DetailSection title="Submission Metadata" items={metadataItems} />
        </Column>
        <Column lg={16} md={8} sm={4} className="view-submission-page__section">
          <DetailSection title="Submission Details" items={detailItems} />
        </Column>
        <Column lg={16} md={8} sm={4} className="view-submission-page__section">
          <DetailSection title="Submitter Information" items={submitterItems} />
        </Column>

        <Column lg={16} md={8} sm={4} className="view-submission-page__section">
          <div className="view-submission-page__invoices-card">
            <div className="view-submission-page__invoices-header">
              <div>
                <h2 className="view-submission-page__section-title">Invoices</h2>
                <p className="view-submission-page__section-count">{invoicesSummary}</p>
              </div>
              {invoiceRows.length > 0 && (
                <div className="view-submission-page__expand-actions">
                  <Link
                    href="#"
                    onClick={(e) => {
                      e.preventDefault();
                      expandAll();
                    }}
                  >
                    Expand all
                  </Link>
                  <span aria-hidden="true">|</span>
                  <Link
                    href="#"
                    onClick={(e) => {
                      e.preventDefault();
                      collapseAll();
                    }}
                  >
                    Collapse all
                  </Link>
                </div>
              )}
            </div>
            <div className="view-submission-page__invoices-body">
              <ResultsTable
                rows={invoiceRows}
                columns={invoiceColumns}
                hasSearched
                expandable
                expandedRowIds={expandedRowIds}
                onExpandedRowIdsChange={setExpandedRowIds}
                renderExpandedContent={(row) => (
                  <InvoiceDetailPanel invoice={row} lineItems={lineItemsByInvoice.get(row.invoiceNumber ?? '') ?? []} />
                )}
                emptyTitle="No invoices"
                emptyDescription="This submission has no invoices."
              />
            </div>
          </div>
        </Column>
      </>
    );
  };

  const invoiceColumns: ResultsTableColumn<InvoiceRow>[] = [
    {
      key: 'invoiceNumber',
      header: 'Invoice #',
      renderCell: (row) =>
        row.coastalLogSaleId == null ? (
          (row.invoiceNumber ?? '—')
        ) : (
          <Link
            href={`${ROUTES.INVOICE}/${row.coastalLogSaleId}`}
            onClick={(e) => {
              e.preventDefault();
              navigate(`${ROUTES.INVOICE}/${row.coastalLogSaleId}`);
            }}
          >
            {row.invoiceNumber ?? '—'}
          </Link>
        ),
    },
    {
      key: 'status',
      header: 'Decision',
      renderCell: (row) => (row.status ? <InvoiceStatusTag status={row.status} /> : '—'),
    },
    { key: 'invoiceDate', header: 'Date', renderCell: (r) => r.invoiceDate ?? '—' },
    { key: 'type', header: 'Type', renderCell: (r) => r.type ?? '—' },
    { key: 'sellerClient', header: 'Seller Client #', renderCell: (r) => r.sellerClient ?? '—' },
    { key: 'buyerClient', header: 'Buyer Client #', renderCell: (r) => r.buyerClient ?? '—' },
    { key: 'maturity', header: 'Maturity', renderCell: (r) => r.maturity ?? '—' },
    { key: 'fobLocation', header: 'FOB Location', renderCell: (r) => r.fobLocation ?? '—' },
    {
      key: 'totalAmount',
      header: 'Total Amount',
      headerAlign: 'right',
      cellAlign: 'right',
      renderCell: (r) => formatNumber(r.totalAmount, 2),
    },
    {
      key: 'totalVolume',
      header: 'Total Volume',
      headerAlign: 'right',
      cellAlign: 'right',
      renderCell: (r) => formatNumber(r.totalVolume, 3),
    },
    {
      key: 'totalPieces',
      header: 'Total Pieces',
      headerAlign: 'right',
      cellAlign: 'right',
      renderCell: (r) => formatNumber(r.totalPieces),
    },
    {
      key: 'lineItemCount',
      header: 'Line Items',
      headerAlign: 'right',
      cellAlign: 'right',
      renderCell: (r) => formatNumber(r.lineItemCount),
    },
  ];

  return (
    <div className="view-submission-page">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4} className="view-submission-page__back-col">
          <Link
            href={ROUTES.SUBMISSION_HISTORY}
            className="view-submission-page__back-link"
            onClick={(e) => {
              e.preventDefault();
              navigate(ROUTES.SUBMISSION_HISTORY);
            }}
          >
            <ArrowLeft />
            Back to Submission History
          </Link>
        </Column>

        <PageTitle title="View Submission" />

        {renderContent()}
      </Grid>
    </div>
  );
}
