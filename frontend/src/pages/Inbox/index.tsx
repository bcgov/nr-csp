import { useState } from 'react';
import { Grid, Column, TextInput, Button } from '@carbon/react';
import { Search as SearchIcon } from '@carbon/icons-react';

import SubmissionStatusTag from '@/components/core/Tags/SubmissionStatusTag';
import PageTitle from '@/components/core/PageTitle';
import ClientAutocomplete, { type ClientLocationResponse } from '@/components/Form/ClientAutocomplete';
import DateInput from '@/components/Form/DateInput';
import SingleSelect from '@/components/Form/SingleSelect';
import ResultsTable, { type ResultsTableColumn } from '@/components/Form/ResultsTable';
import { formatDisplayDate, formatIsoDate } from '@/utils/format';
import { type LookupItemResponse, useSubmissionStatusesQuery } from '@/services/lookup.service';
import { type InboxSearchParams, type InboxRowResponse, useInboxSearchQuery } from '@/services/inbox.service';
import { usePersistentState } from '@/hooks/usePersistentState';

import './index.scss';

type InboxRow = {
  id: string;
  submissionId: string;
  submissionDate: string;
  submissionStatus: string;
  submissionType: string;
  invTotal: number;
  invApproved: number;
  invRejected: number;
  invProcessing: number;
  invCancelled: number;
};

type SelectItem = { id: string; label: string };

const submittedByItems: SelectItem[] = [
  { id: 'Buyer', label: 'Buyer' },
  { id: 'Seller', label: 'Seller' },
];

const typeItems: SelectItem[] = [
  { id: 'Electronic', label: 'Electronic' },
  { id: 'Manual', label: 'Manual' },
];

function toInboxRow(r: InboxRowResponse, index: number): InboxRow {
  return {
    id: r.coastalLogSaleId?.toString() ?? `row-${index}`,
    submissionId: r.submissionId ?? '—',
    submissionDate: formatDisplayDate(r.submissionDate),
    submissionStatus: r.submissionStatus,
    submissionType: r.submissionType,
    invTotal: r.invTotal,
    invApproved: r.invApproved,
    invRejected: r.invRejected,
    invProcessing: r.invProcessing,
    invCancelled: r.invCancelled,
  };
}

const NS = 'csp.table.inbox.v1';

export function InboxPage() {
  const [hasSearched, setHasSearched] = usePersistentState(NS, 'hasSearched', false);
  const [pageSize, setPageSize] = usePersistentState(NS, 'pageSize', 10);
  const [currentPage, setCurrentPage] = usePersistentState(NS, 'page', 1);
  const [sortParam, setSortParam] = usePersistentState<string | undefined>(NS, 'sort', undefined);
  const [keyword, setKeyword] = usePersistentState(NS, 'keyword', '');

  // Filter inputs
  const [invoiceNumberInput, setInvoiceNumberInput] = usePersistentState(NS, 'invoiceNumberInput', '');
  const [submitterClient, setSubmitterClient] = usePersistentState<ClientLocationResponse | null>(
    NS,
    'submitterClient',
    null,
  );
  const [startDateInput, setStartDateInput] = usePersistentState(NS, 'startDateInput', '');
  const [endDateInput, setEndDateInput] = usePersistentState(NS, 'endDateInput', '');
  const [submittedBy, setSubmittedBy] = usePersistentState<SelectItem | null>(NS, 'submittedBy', null);
  const [selectedType, setSelectedType] = usePersistentState<SelectItem | null>(NS, 'selectedType', null);
  const [selectedStatus, setSelectedStatus] = usePersistentState<LookupItemResponse | null>(NS, 'selectedStatus', null);
  const [dateKey, setDateKey] = useState(0);

  // Snapshot of filter criteria at the moment Search is clicked.
  const [appliedFilters, setAppliedFilters] = usePersistentState<InboxSearchParams>(NS, 'appliedFilters', {});

  const { data: statusItems = [], isLoading: statusLoading } = useSubmissionStatusesQuery();

  const queryParams: InboxSearchParams = {
    ...appliedFilters,
    page: currentPage - 1, // Spring is 0-indexed; Carbon pagination is 1-indexed.
    size: pageSize,
    sort: sortParam,
    keyword: keyword || undefined,
  };

  const [dateRangeError, setDateRangeError] = useState<string | null>(null);

  const { data, isLoading, isError, error } = useInboxSearchQuery(queryParams, hasSearched);

  // Extract the most specific message from a backend 400 validation response.
  const apiErrorMessage = (() => {
    if (!isError) return null;
    const axiosError = error as { response?: { data?: { errors?: { message?: string }[]; message?: string } } };
    const firstError = axiosError?.response?.data?.errors?.[0]?.message;
    return firstError ?? axiosError?.response?.data?.message ?? 'Failed to load results. Please try again.';
  })();

  const rows: InboxRow[] = (data?.content ?? []).map(toInboxRow);
  const totalElements = data?.totalElements ?? 0;

  const inboxColumns: ResultsTableColumn<InboxRow>[] = [
    { key: 'submissionId', header: 'Submission ID' },
    { key: 'submissionDate', header: 'Submission date' },
    {
      key: 'submissionStatus',
      header: 'Status',
      renderCell: (row) => <SubmissionStatusTag status={row.submissionStatus} />,
    },
    { key: 'submissionType', header: 'Type' },
    { key: 'invTotal', header: 'Total' },
    { key: 'invApproved', header: 'Approved' },
    { key: 'invRejected', header: 'Rejected' },
    { key: 'invProcessing', header: 'Processing' },
    { key: 'invCancelled', header: 'Cancelled' },
  ];

  const buildSearchParams = (): InboxSearchParams => ({
    invoiceNum: invoiceNumberInput.trim() || undefined,
    submitterClientNum: submitterClient?.clientNumber || undefined,
    submitterLocNum: submitterClient?.clientLocnCode || undefined,
    submissionDateFrom: startDateInput || undefined,
    submissionDateTo: endDateInput || undefined,
    submittedBy: submittedBy?.id || undefined,
    submissionType: selectedType?.id || undefined,
    submissionStatus: selectedStatus?.code || undefined,
  });

  const validateDateRange = (start: string, end: string) => {
    if (start && end && start > end) {
      setDateRangeError('Submission Date Start must be before or equal to Submission Date End.');
    } else {
      setDateRangeError(null);
    }
  };

  const executeSearch = () => {
    if (dateRangeError) return;
    setAppliedFilters(buildSearchParams());
    setHasSearched(true);
    setCurrentPage(1);
  };

  const handleClearFilters = () => {
    setInvoiceNumberInput('');
    setSubmitterClient(null);
    setStartDateInput('');
    setEndDateInput('');
    setSubmittedBy(null);
    setSelectedType(null);
    setSelectedStatus(null);
    setDateRangeError(null);
    setDateKey((k) => k + 1);
  };

  return (
    <div className="inbox-page">
      <Grid fullWidth>
        <PageTitle title="Inbox" breadCrumbs={[{ name: 'Inbox search', path: '/inbox' }]} />

        {/* Row 1: invoice number + submitter client name */}
        <Column lg={16} md={8} sm={4}>
          <div className="inbox-page__filter-row">
            <div className="inbox-page__filter-item">
              <TextInput
                id="invoice-number"
                labelText="Invoice number"
                placeholder="Invoice number"
                helperText="Use * or % (any) and ? (single) as wildcards"
                value={invoiceNumberInput}
                maxLength={15}
                onChange={(e) => setInvoiceNumberInput(e.target.value)}
              />
            </div>
            <div className="inbox-page__filter-item">
              <ClientAutocomplete
                id="submitter-client-name"
                titleText="Submitter client name"
                size="md"
                selectedClient={submitterClient}
                onSelect={setSubmitterClient}
              />
            </div>
          </div>
        </Column>

        {/* Row 2: dates, selects, and the Search button aligned to input level */}
        <Column lg={16} md={8} sm={4}>
          <div className="inbox-page__filter-row inbox-page__filter-row--with-btn">
            <div className="inbox-page__filter-item">
              <DateInput
                key={`start-date-${dateKey}`}
                id="date-start"
                labelText="Date start"
                value={startDateInput || undefined}
                onChange={(dates) => {
                  const val = dates[0] ? formatIsoDate(dates[0]) : '';
                  setStartDateInput(val);
                  validateDateRange(val, endDateInput);
                }}
              />
            </div>
            <div className="inbox-page__filter-item">
              <DateInput
                key={`end-date-${dateKey}`}
                id="date-end"
                labelText="Date end"
                value={endDateInput || undefined}
                invalid={!!dateRangeError}
                invalidText={dateRangeError ?? undefined}
                onChange={(dates) => {
                  const val = dates[0] ? formatIsoDate(dates[0]) : '';
                  setEndDateInput(val);
                  validateDateRange(startDateInput, val);
                }}
              />
            </div>
            <div className="inbox-page__filter-item">
              <SingleSelect
                id="submitted-by-filter"
                titleText="Submitted by"
                items={submittedByItems}
                selectedItem={submittedBy}
                itemToString={(item) => (item ? item.label : '')}
                onChange={({ selectedItem }) => setSubmittedBy(selectedItem ?? null)}
              />
            </div>
            <div className="inbox-page__filter-item">
              <SingleSelect
                id="type-filter"
                titleText="Type"
                items={typeItems}
                selectedItem={selectedType}
                itemToString={(item) => (item ? item.label : '')}
                onChange={({ selectedItem }) => setSelectedType(selectedItem ?? null)}
              />
            </div>
            <div className="inbox-page__filter-item">
              <SingleSelect
                id="status-filter"
                titleText="Status"
                items={statusItems}
                selectedItem={selectedStatus}
                disabled={statusLoading}
                itemToString={(item) => (item ? item.description : '')}
                onChange={({ selectedItem }) => setSelectedStatus(selectedItem ?? null)}
              />
            </div>
            <div className="inbox-page__filter-item">
              <span className="inbox-page__search-btn-spacer" aria-hidden="true">
                &nbsp;
              </span>
              <Button
                kind="primary"
                size="md"
                renderIcon={SearchIcon}
                iconDescription="Search"
                onClick={executeSearch}
                disabled={!!dateRangeError}
              >
                Search
              </Button>
            </div>
          </div>
        </Column>

        {/* Clear filters — always visible */}
        <Column lg={16} md={8} sm={4} className="inbox-page__clear-col">
          <Button kind="ghost" size="sm" onClick={handleClearFilters}>
            Clear filters
          </Button>
        </Column>

        {isError && (
          <Column lg={16} md={8} sm={4} className="inbox-page__error-col">
            <p className="inbox-page__error">{apiErrorMessage}</p>
          </Column>
        )}

        <Column lg={16} md={8} sm={4} className="inbox-page__table-col">
          <ResultsTable
            rows={rows}
            columns={inboxColumns}
            isSortable
            serverSide
            hasSearched={hasSearched}
            isLoading={isLoading}
            page={currentPage}
            pageSize={pageSize}
            totalItems={totalElements}
            pageSizes={[10, 20, 30, 40, 50]}
            paginationItemsPerPageText="Invoice per page:"
            paginationItemRangeText={(min, max, total) => `${min} – ${max} of ${total} invoices`}
            searchKeyword={keyword}
            onSearchKeywordChange={
              hasSearched || rows.length > 0
                ? (kw) => {
                    setKeyword(kw);
                    setCurrentPage(1);
                  }
                : undefined
            }
            onSortChange={(sortKey, sortDir) => {
              setSortParam(sortKey && sortDir !== 'NONE' ? `${sortKey},${sortDir.toLowerCase()}` : undefined);
              setCurrentPage(1);
            }}
            onPaginationChange={({ page, pageSize: newPageSize }) => {
              setCurrentPage(page);
              setPageSize(newPageSize);
            }}
            emptyTitle={hasSearched ? undefined : 'Your search results will appear here!'}
            emptyDescription={hasSearched ? undefined : 'Enter at least one criteria to start the search.'}
          />
        </Column>
      </Grid>
    </div>
  );
}
