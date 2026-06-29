import { useState } from 'react';
import { Grid, Column, IconButton, Link } from '@carbon/react';
import { View, Chat } from '@carbon/icons-react';
import { useNavigate } from 'react-router-dom';

import SubmissionStatusTag from '@/components/core/Tags/SubmissionStatusTag';
import PageTitle from '@/components/core/PageTitle';
import ResultsTable, { type ResultsTableColumn } from '@/components/Form/ResultsTable';
import { ROUTES } from '@/routes/routePaths';
import { formatShortDate } from '@/utils/format';
import {
  type SubmissionHistoryListParams,
  type SubmissionHistoryRowResponse,
  useSubmissionHistoryListQuery,
} from '@/services/submissionHistory.service';

import InvoiceCommentsPanel from './InvoiceCommentsPanel';
import './index.scss';

type SubmissionRow = {
  id: string;
  cspSubmissionId: number | null;
  submissionDate: string;
  submittedBy: string;
  clientName: string;
  submissionStatus: string;
  invoiceCount: number;
  commentedInvoiceCount: number;
};

function formatClientName(name: string | null, number: string | null): string {
  if (!name) return number ?? '—';
  return number ? `${name} (${number})` : name;
}

function toSubmissionRow(r: SubmissionHistoryRowResponse, index: number): SubmissionRow {
  return {
    id: r.cspSubmissionId?.toString() ?? `row-${index}`,
    cspSubmissionId: r.cspSubmissionId,
    submissionDate: formatShortDate(r.submissionDate),
    submittedBy: r.submittedBy ?? '—',
    clientName: formatClientName(r.clientName, r.clientNumber),
    submissionStatus: r.submissionStatus,
    invoiceCount: r.invoiceCount ?? 0,
    commentedInvoiceCount: r.commentedInvoiceCount ?? 0,
  };
}

export function SubmissionHistoryPage() {
  const navigate = useNavigate();

  const [pageSize, setPageSize] = useState(10);
  const [currentPage, setCurrentPage] = useState(1);
  const [sortParam, setSortParam] = useState<string | undefined>(undefined);
  const [expandedRowIds, setExpandedRowIds] = useState<Set<string>>(new Set());

  const queryParams: SubmissionHistoryListParams = {
    page: currentPage - 1, // Spring is 0-indexed; Carbon pagination is 1-indexed.
    size: pageSize,
    sort: sortParam,
  };

  const { data, isLoading, isError, error } = useSubmissionHistoryListQuery(queryParams);

  const apiErrorMessage = (() => {
    if (!isError) return null;
    const axiosError = error as { response?: { data?: { message?: string } } };
    return axiosError?.response?.data?.message ?? 'Failed to load submissions. Please try again.';
  })();

  const rows: SubmissionRow[] = (data?.content ?? []).map(toSubmissionRow);
  const totalElements = data?.totalElements ?? 0;

  const columns: ResultsTableColumn<SubmissionRow>[] = [
    { key: 'submissionDate', header: 'Submission Date' },
    { key: 'submittedBy', header: 'Submitted By' },
    { key: 'clientName', header: 'Client Name' },
    {
      key: 'submissionStatus',
      header: 'Status',
      renderCell: (row) => <SubmissionStatusTag status={row.submissionStatus} />,
    },
    {
      key: 'invoiceCount',
      header: 'Invoices',
      sortable: false,
      renderCell: (row) => (
        <span className="submission-history-page__invoices-cell">
          {row.cspSubmissionId != null ? (
            <Link
              href={`${ROUTES.SUBMISSION_HISTORY}/${row.cspSubmissionId}`}
              onClick={(e) => {
                e.preventDefault();
                navigate(`${ROUTES.SUBMISSION_HISTORY}/${row.cspSubmissionId}`);
              }}
            >
              {row.invoiceCount} {row.invoiceCount === 1 ? 'invoice' : 'invoices'}
            </Link>
          ) : (
            `${row.invoiceCount} ${row.invoiceCount === 1 ? 'invoice' : 'invoices'}`
          )}
          {row.commentedInvoiceCount > 0 && (
            <span
              className="submission-history-page__comment-badge"
              title={`${row.commentedInvoiceCount} invoice(s) with comments`}
            >
              <Chat size={16} />
              {row.commentedInvoiceCount}
            </span>
          )}
        </span>
      ),
    },
    {
      key: 'id',
      header: 'Actions',
      sortable: false,
      headerAlign: 'center',
      cellAlign: 'center',
      renderCell: (row) =>
        row.cspSubmissionId != null ? (
          <IconButton
            kind="ghost"
            size="sm"
            label="View submission"
            align="left"
            onClick={() => navigate(`${ROUTES.SUBMISSION_HISTORY}/${row.cspSubmissionId}`)}
          >
            <View />
          </IconButton>
        ) : null,
    },
  ];

  return (
    <div className="submission-history-page">
      <Grid fullWidth>
        <PageTitle
          title="Submission History"
          subtitle="View your previous CSP submissions."
          breadCrumbs={[{ name: 'Submission history', path: ROUTES.SUBMISSION_HISTORY }]}
        />

        {isError && (
          <Column lg={16} md={8} sm={4} className="submission-history-page__error-col">
            <p className="submission-history-page__error">{apiErrorMessage}</p>
          </Column>
        )}

        <Column lg={16} md={8} sm={4} className="submission-history-page__table-col">
          <ResultsTable
            rows={rows}
            columns={columns}
            isSortable
            serverSide
            hasSearched
            expandable
            expandedRowIds={expandedRowIds}
            onExpandedRowIdsChange={setExpandedRowIds}
            renderExpandedContent={(row) => (
              <InvoiceCommentsPanel submissionId={row.cspSubmissionId} enabled={expandedRowIds.has(row.id)} />
            )}
            isLoading={isLoading}
            page={currentPage}
            pageSize={pageSize}
            totalItems={totalElements}
            pageSizes={[10, 20, 30, 40, 50]}
            paginationItemsPerPageText="Submissions per page:"
            paginationItemRangeText={(min, max, total) => `${min} – ${max} of ${total} submissions`}
            onSortChange={(sortKey, sortDir) => {
              setSortParam(sortKey && sortDir !== 'NONE' ? `${sortKey},${sortDir.toLowerCase()}` : undefined);
              setCurrentPage(1);
            }}
            onPaginationChange={({ page, pageSize: newPageSize }) => {
              setCurrentPage(page);
              setPageSize(newPageSize);
            }}
            emptyTitle="No submissions found"
            emptyDescription="You have no previous CSP submissions yet."
          />
        </Column>
      </Grid>
    </div>
  );
}
