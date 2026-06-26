import { useState } from 'react';
import { Grid, Column, IconButton } from '@carbon/react';
import { View } from '@carbon/icons-react';
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

import './index.scss';

type SubmissionRow = {
  id: string;
  cspSubmissionId: number | null;
  submissionDate: string;
  submittedBy: string;
  clientName: string;
  submissionStatus: string;
  comment: string;
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
    comment: r.comment ?? '',
  };
}

export function SubmissionHistoryPage() {
  const navigate = useNavigate();

  const [pageSize, setPageSize] = useState(10);
  const [currentPage, setCurrentPage] = useState(1);
  const [sortParam, setSortParam] = useState<string | undefined>(undefined);

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
    { key: 'comment', header: 'Comments', sortable: false },
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
