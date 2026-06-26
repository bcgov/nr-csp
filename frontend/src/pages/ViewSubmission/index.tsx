import { Grid, Column, Link, Loading, InlineNotification } from '@carbon/react';
import { ArrowLeft } from '@carbon/icons-react';
import { useNavigate, useParams } from 'react-router-dom';

import DetailSection, { type DetailItem } from '@/components/core/DetailSection';
import PageTitle from '@/components/core/PageTitle';
import SubmissionStatusTag from '@/components/core/Tags/SubmissionStatusTag';
import ResultsTable, { type ResultsTableColumn } from '@/components/Form/ResultsTable';
import { ROUTES } from '@/routes/routePaths';
import { formatShortDate } from '@/utils/format';
import {
  type SubmissionInvoiceResponse,
  type SubmissionLineItemResponse,
  useSubmissionDetailQuery,
} from '@/services/submissionHistory.service';

import './index.scss';

const fixed = (value: number | null, decimals: number): string =>
  value === null || value === undefined ? '—' : Number(value).toFixed(decimals);

type InvoiceRow = SubmissionInvoiceResponse & { id: string };
type LineItemRow = SubmissionLineItemResponse & { id: string };

export function ViewSubmissionPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data, isLoading, isError, error } = useSubmissionDetailQuery(id);

  const apiErrorMessage = (() => {
    if (!isError) return null;
    const axiosError = error as { response?: { status?: number; data?: { message?: string } } };
    if (axiosError?.response?.status === 404) return 'Submission not found.';
    return axiosError?.response?.data?.message ?? 'Failed to load the submission. Please try again.';
  })();

  const isRejected = (data?.submissionStatus ?? '').toLowerCase().includes('reject');

  const metadataItems: DetailItem[] = data
    ? [
        { label: 'Email Address', value: data.email },
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

  const invoiceRows: InvoiceRow[] = (data?.invoices ?? []).map((inv, i) => ({
    ...inv,
    id: inv.coastalLogSaleId?.toString() ?? `inv-${i}`,
  }));

  const lineItemRows: LineItemRow[] = (data?.lineItems ?? []).map((li, i) => ({
    ...li,
    id: `${li.invoiceNumber ?? 'li'}-${i}`,
  }));

  const invoiceColumns: ResultsTableColumn<InvoiceRow>[] = [
    {
      key: 'invoiceNumber',
      header: 'Invoice #',
      renderCell: (row) =>
        row.coastalLogSaleId != null ? (
          <Link
            href={`${ROUTES.INVOICE}/${row.coastalLogSaleId}`}
            onClick={(e) => {
              e.preventDefault();
              navigate(`${ROUTES.INVOICE}/${row.coastalLogSaleId}`);
            }}
          >
            {row.invoiceNumber ?? '—'}
          </Link>
        ) : (
          (row.invoiceNumber ?? '—')
        ),
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
      renderCell: (r) => fixed(r.totalAmount, 2),
    },
    {
      key: 'totalVolume',
      header: 'Total Volume',
      headerAlign: 'right',
      cellAlign: 'right',
      renderCell: (r) => fixed(r.totalVolume, 3),
    },
    {
      key: 'totalPieces',
      header: 'Total Pieces',
      headerAlign: 'right',
      cellAlign: 'right',
      renderCell: (r) => (r.totalPieces ?? '—').toString(),
    },
  ];

  const lineItemColumns: ResultsTableColumn<LineItemRow>[] = [
    { key: 'invoiceNumber', header: 'Invoice #', renderCell: (r) => r.invoiceNumber ?? '—' },
    { key: 'species', header: 'Species', renderCell: (r) => r.species ?? '—' },
    { key: 'grade', header: 'Grade', renderCell: (r) => r.grade ?? '—' },
    { key: 'sortCode', header: 'Sort Code', renderCell: (r) => r.sortCode ?? '—' },
    { key: 'clientSortCode', header: 'Client Sort Code', renderCell: (r) => r.clientSortCode ?? '—' },
    {
      key: 'pieces',
      header: '# Pieces',
      headerAlign: 'right',
      cellAlign: 'right',
      renderCell: (r) => (r.pieces ?? '—').toString(),
    },
    {
      key: 'volume',
      header: 'Volume',
      headerAlign: 'right',
      cellAlign: 'right',
      renderCell: (r) => fixed(r.volume, 3),
    },
    {
      key: 'price',
      header: 'Price',
      headerAlign: 'right',
      cellAlign: 'right',
      renderCell: (r) => fixed(r.price, 2),
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

        {isLoading ? (
          <Column lg={16} md={8} sm={4} className="view-submission-page__loading">
            <Loading withOverlay={false} description="Loading submission" />
          </Column>
        ) : isError ? (
          <Column lg={16} md={8} sm={4} className="view-submission-page__error-col">
            <p className="view-submission-page__error">{apiErrorMessage}</p>
          </Column>
        ) : data ? (
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

            <Column lg={16} md={8} sm={4} className="view-submission-page__banner-col">
              <InlineNotification
                lowContrast
                hideCloseButton
                kind={isRejected ? 'error' : 'info'}
                title={isRejected ? 'Submission Rejected:' : 'Admin Comment:'}
                subtitle={data.adminComment ?? (isRejected ? '' : 'no comment provided')}
              />
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
              <h2 className="view-submission-page__section-title">Invoice Details</h2>
              <p className="view-submission-page__section-count">{invoiceRows.length} invoice entries</p>
              <ResultsTable
                rows={invoiceRows}
                columns={invoiceColumns}
                hasSearched
                emptyTitle="No invoices"
                emptyDescription="This submission has no invoices."
              />
            </Column>

            <Column lg={16} md={8} sm={4} className="view-submission-page__section">
              <h2 className="view-submission-page__section-title">Invoice Line Items</h2>
              <p className="view-submission-page__section-count">{lineItemRows.length} line item entries</p>
              <ResultsTable
                rows={lineItemRows}
                columns={lineItemColumns}
                hasSearched
                emptyTitle="No line items"
                emptyDescription="This submission has no line items."
              />
            </Column>
          </>
        ) : null}
      </Grid>
    </div>
  );
}
