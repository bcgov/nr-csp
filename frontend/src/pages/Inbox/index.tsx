import { useState } from 'react';
import { Grid, Column, TextInput, Button, Link } from '@carbon/react';
import { Search as SearchIcon } from '@carbon/icons-react';

import InvoiceStatusTag from '@/components/core/Tags/InvoiceStatusTag';
import PageTitle from '@/components/core/PageTitle';
import ClientAutocomplete, { type ClientLocationResponse } from '@/components/Form/ClientAutocomplete';
import DateInput from '@/components/Form/DateInput';
import SingleSelect from '@/components/Form/SingleSelect';
import ResultsTable, { type ResultsTableColumn } from '@/components/Form/ResultsTable';

import './index.scss';

type InboxRow = {
  id: string;
  submissionId: string;
  submissionDate: string;
  status: string;
  type: string;
  total: number;
  approved: number;
  rejected: number;
  processing: number;
  cancelled: number;
};

// Static, placeholder option lists. These are template values for the styling
// pass only — they are not yet wired to lookup endpoints.
type SelectItem = { id: string; label: string };

const submittedByItems: SelectItem[] = [
  { id: 'wfp', label: 'WFP' },
  { id: 'tla', label: 'TLA' },
  { id: 'bcts', label: 'BCTS' },
];

const typeItems: SelectItem[] = [
  { id: 'sales', label: 'Sales' },
  { id: 'purchase', label: 'Purchase' },
];

const statusItems: SelectItem[] = [
  { id: 'approved', label: 'Approved' },
  { id: 'rejected', label: 'Rejected' },
  { id: 'processing', label: 'Processing' },
  { id: 'cancelled', label: 'Cancelled' },
  { id: 'draft', label: 'Draft' },
];

export function InboxPage() {
  const [hasSearched, setHasSearched] = useState(false);
  const [pageSize, setPageSize] = useState(10);
  const [currentPage, setCurrentPage] = useState(1);

  // Filter inputs
  const [invoiceNumberInput, setInvoiceNumberInput] = useState('');
  const [submitterClient, setSubmitterClient] = useState<ClientLocationResponse | null>(null);
  const [startDateInput, setStartDateInput] = useState('');
  const [endDateInput, setEndDateInput] = useState('');
  const [submittedBy, setSubmittedBy] = useState<SelectItem | null>(null);
  const [selectedType, setSelectedType] = useState<SelectItem | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<SelectItem | null>(null);
  const [dateKey, setDateKey] = useState(0);

  // No data wiring yet — the table renders its empty state during this styling pass.
  const rows: InboxRow[] = [];

  const inboxColumns: ResultsTableColumn<InboxRow>[] = [
    { key: 'submissionId', header: 'Submission ID' },
    { key: 'submissionDate', header: 'Submission date' },
    {
      key: 'status',
      header: 'Status',
      renderCell: (row) => <InvoiceStatusTag status={row.status} />,
    },
    { key: 'type', header: 'Type' },
    { key: 'total', header: 'Total' },
    { key: 'approved', header: 'Approved' },
    { key: 'rejected', header: 'Rejected' },
    { key: 'processing', header: 'Processing' },
    { key: 'cancelled', header: 'Cancelled' },
  ];

  const executeSearch = () => {
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
                value={invoiceNumberInput}
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
                onChange={(dates) => setStartDateInput(dates[0] ? dates[0].toISOString().slice(0, 10) : '')}
              />
            </div>
            <div className="inbox-page__filter-item">
              <DateInput
                key={`end-date-${dateKey}`}
                id="date-end"
                labelText="Date end"
                onChange={(dates) => setEndDateInput(dates[0] ? dates[0].toISOString().slice(0, 10) : '')}
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
                itemToString={(item) => (item ? item.label : '')}
                onChange={({ selectedItem }) => setSelectedStatus(selectedItem ?? null)}
              />
            </div>
            <div className="inbox-page__filter-item">
              <Button kind="primary" size="md" renderIcon={SearchIcon} iconDescription="Search" onClick={executeSearch}>
                Search
              </Button>
            </div>
          </div>
        </Column>

        {/* Clear filters — always visible */}
        <Column lg={16} md={8} sm={4} className="inbox-page__clear-col">
          <Link
            onClick={(e) => {
              e.preventDefault();
              handleClearFilters();
            }}
            href="#"
          >
            Clear filters
          </Link>
        </Column>

        <Column lg={16} md={8} sm={4} className="inbox-page__table-col">
          <ResultsTable
            rows={rows}
            columns={inboxColumns}
            isSortable
            serverSide
            hasSearched={hasSearched}
            page={currentPage}
            pageSize={pageSize}
            totalItems={rows.length}
            pageSizes={[10, 20, 30, 40, 50]}
            paginationItemsPerPageText="Invoice per page:"
            paginationItemRangeText={(min, max, total) => `${min} – ${max} of ${total} invoices`}
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
