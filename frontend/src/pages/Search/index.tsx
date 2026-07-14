import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Grid, Column, TextInput, Button, Link } from '@carbon/react';
import { Search as SearchIcon } from '@carbon/icons-react';

import AutoCompleteInput from '@/components/Form/AutoCompleteInput';
import InvoiceStatusTag from '@/components/core/Tags/InvoiceStatusTag';
import PageTitle from '@/components/core/PageTitle';
import DateInput from '@/components/Form/DateInput';
import SingleSelect from '@/components/Form/SingleSelect';
import ResultsTable, { type ResultsTableColumn } from '@/components/Form/ResultsTable';
import { formatIsoDate } from '@/utils/format';
import {
  type SearchResultResponse,
  type ClientLocationResponse,
  type SearchParams,
  getClientsByName,
  useSearchQuery,
} from '@/services/search.service';
import {
  type LookupItemResponse,
  useInvoiceStatusesQuery,
  useInvoiceTypesQuery,
  useMaturityCodesQuery,
} from '@/services/lookup.service';

import './index.scss';

type InvoiceRow = {
  id: string;
  invoiceStatus: string;
  invoiceNumber: string;
  invoiceDate: string;
  type: string;
  clientNumber: string;
  clientName: string;
  maturity: string;
  submissionType: string;
};

type SellerSubmitterItem = { id: string; label: string };

const sellerSubmitterItems: SellerSubmitterItem[] = [
  { id: 'true', label: 'Yes' },
  { id: 'false', label: 'No' },
];

function toInvoiceRow(r: SearchResultResponse): InvoiceRow {
  return {
    id: r.coastalLogSaleId.toString(),
    invoiceStatus: r.invoiceStatus,
    invoiceNumber: r.invoiceNumber,
    invoiceDate: r.invoiceDate,
    type: r.type,
    clientNumber: r.clientNumber,
    clientName: r.clientName,
    maturity: r.maturity,
    submissionType: r.submissionType,
  };
}

export function SearchPage() {
  const navigate = useNavigate();
  const [hasSearched, setHasSearched] = useState(false);
  const [pageSize, setPageSize] = useState(20);
  const [currentPage, setCurrentPage] = useState(1);

  const invoiceColumns: ResultsTableColumn<InvoiceRow>[] = [
    {
      key: 'invoiceStatus',
      header: 'Invoice status',
      renderCell: (row) => <InvoiceStatusTag status={row.invoiceStatus} />,
    },
    {
      key: 'invoiceNumber',
      header: 'Invoice number',
      renderCell: (row) => (
        <Link
          href="#"
          onClick={(e) => {
            e.preventDefault();
            navigate(`/invoice/${row.id}`, { state: { fromSearch: true } });
          }}
        >
          {row.invoiceNumber}
        </Link>
      ),
    },
    { key: 'invoiceDate', header: 'Invoice date' },
    { key: 'type', header: 'Type' },
    { key: 'clientNumber', header: 'Client number' },
    { key: 'clientName', header: 'Client name' },
    {
      key: 'maturity',
      header: 'Maturity',
      renderCell: (row) => (row.maturity === 'Cants / Export' ? 'Cants' : row.maturity),
    },
    { key: 'submissionType', header: 'Submission type' },
  ];

  // Lookup data
  const { data: statusItems = [], isLoading: statusLoading } = useInvoiceStatusesQuery();
  const { data: typeItems = [], isLoading: typeLoading } = useInvoiceTypesQuery();
  const { data: maturityItemsRaw = [], isLoading: maturityLoading } = useMaturityCodesQuery();
  const maturityItems = maturityItemsRaw
    .filter((item) => item.description !== 'Export')
    .map((item) => (item.description === 'Cants / Export' ? { ...item, description: 'Cants' } : item));

  // Row 1 filter inputs
  const [invoiceDateInput, setInvoiceDateInput] = useState('');
  const [startDateInput, setStartDateInput] = useState('');
  const [endDateInput, setEndDateInput] = useState('');
  const [invoiceNumberInput, setInvoiceNumberInput] = useState('');
  const [selectedStatus, setSelectedStatus] = useState<LookupItemResponse | null>(null);
  const [selectedType, setSelectedType] = useState<LookupItemResponse | null>(null);

  // Row 2 filter inputs
  const [submitterClientNumInput, setSubmitterClientNumInput] = useState('');
  const [sellerBuyerClientNum, setSellerBuyerClientNum] = useState('');
  const [sellerBuyerLocNum, setSellerBuyerLocNum] = useState('');
  const [sellerSubmitterInput, setSellerSubmitterInput] = useState<SellerSubmitterItem | null>(null);
  const [maturityInput, setMaturityInput] = useState<LookupItemResponse | null>(null);
  const [autoCompleteKey, setAutoCompleteKey] = useState(0);
  const [dateKey, setDateKey] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [dateRangeError, setDateRangeError] = useState<string | null>(null);

  // Snapshot of filters at the moment Search is clicked (the criteria part — page/size/sort/keyword
  // are tracked separately so changing them re-queries without re-snapshotting filter inputs).
  const [appliedFilters, setAppliedFilters] = useState<SearchParams>({});

  // Spring-style "field,direction" sort string, or undefined when unsorted.
  const [sortParam, setSortParam] = useState<string | undefined>(undefined);

  const queryParams: SearchParams = {
    ...appliedFilters,
    page: currentPage - 1, // Spring is 0-indexed; Carbon pagination is 1-indexed.
    size: pageSize,
    sort: sortParam,
    keyword: keyword || undefined,
  };

  const { data, isLoading, isError } = useSearchQuery(queryParams, hasSearched);

  const rows: InvoiceRow[] = (data?.content ?? []).map(toInvoiceRow);
  const totalElements = data?.totalElements ?? 0;

  const buildSearchParams = (): SearchParams => ({
    invDate: invoiceDateInput || undefined,
    startDate: startDateInput || undefined,
    endDate: endDateInput || undefined,
    invNumber: invoiceNumberInput.trim() || undefined,
    invStatus: selectedStatus?.code || undefined,
    invType: selectedType?.code || undefined,
    submitterClientNum: submitterClientNumInput.trim() || undefined,
    sellerBuyerClientNum: sellerBuyerClientNum || undefined,
    sellerBuyerLocNum: sellerBuyerLocNum || undefined,
    sellerSubmitter:
      sellerSubmitterInput?.id === 'true' ? true : sellerSubmitterInput?.id === 'false' ? false : undefined,
    maturity: maturityInput?.code || undefined,
  });

  const validateDateRange = (start: string, end: string) => {
    if (start && end && start > end) {
      setDateRangeError('Start date must be before or equal to End date.');
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
    setInvoiceDateInput('');
    setStartDateInput('');
    setEndDateInput('');
    setInvoiceNumberInput('');
    setSelectedStatus(null);
    setSelectedType(null);
    setSubmitterClientNumInput('');
    setSellerBuyerClientNum('');
    setSellerBuyerLocNum('');
    setSellerSubmitterInput(null);
    setMaturityInput(null);
    setDateRangeError(null);
    setAutoCompleteKey((k) => k + 1);
    setDateKey((k) => k + 1);
  };

  return (
    <div className="search-page">
      <Grid fullWidth>
        <PageTitle title="Invoice search" breadCrumbs={[{ name: 'Invoice search', path: '/search' }]} />

        {/* Row 1: 6 equal-width filters */}
        <Column lg={16} md={8} sm={4}>
          <div className="search-page__filter-row">
            <div className="search-page__filter-item">
              <DateInput
                key={`invoice-date-${dateKey}`}
                id="invoice-date"
                labelText="Invoice date"
                onChange={(dates) => setInvoiceDateInput(dates[0] ? formatIsoDate(dates[0]) : '')}
              />
            </div>
            <div className="search-page__filter-item">
              <DateInput
                key={`start-date-${dateKey}`}
                id="start-date"
                labelText="Start date (invoice)"
                onChange={(dates) => {
                  const val = dates[0] ? formatIsoDate(dates[0]) : '';
                  setStartDateInput(val);
                  validateDateRange(val, endDateInput);
                }}
              />
            </div>
            <div className="search-page__filter-item">
              <DateInput
                key={`end-date-${dateKey}`}
                id="end-date"
                labelText="End date (invoice)"
                invalid={!!dateRangeError}
                invalidText={dateRangeError ?? undefined}
                onChange={(dates) => {
                  const val = dates[0] ? formatIsoDate(dates[0]) : '';
                  setEndDateInput(val);
                  validateDateRange(startDateInput, val);
                }}
              />
            </div>
            <div className="search-page__filter-item">
              <TextInput
                id="invoice-number"
                labelText="Invoice number"
                placeholder="Invoice number"
                helperText="Use * or % (any) and ? (single) as wildcards"
                value={invoiceNumberInput}
                onChange={(e) => setInvoiceNumberInput(e.target.value)}
              />
            </div>
            <div className="search-page__filter-item">
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
            <div className="search-page__filter-item">
              <SingleSelect
                id="type-filter"
                titleText="Type"
                items={typeItems}
                selectedItem={selectedType}
                disabled={typeLoading}
                itemToString={(item) => (item ? item.description : '')}
                onChange={({ selectedItem }) => setSelectedType(selectedItem ?? null)}
              />
            </div>
          </div>
        </Column>

        {/* Row 2: equal-width inputs, seller/buyer double, button aligned to input level */}
        <Column lg={16} md={8} sm={4}>
          <div className="search-page__filter-row search-page__filter-row--with-btn">
            <div className="search-page__filter-item">
              <TextInput
                id="submitter-client-number"
                labelText="Submitter client number"
                placeholder="Submitter client number"
                value={submitterClientNumInput}
                onChange={(e) => setSubmitterClientNumInput(e.target.value.replace(/\D/g, '').slice(0, 8))}
                onBlur={(e) => {
                  const val = e.target.value.trim();
                  if (val && /^\d+$/.test(val)) {
                    setSubmitterClientNumInput(val.padStart(8, '0'));
                  }
                }}
              />
            </div>
            <div className="search-page__filter-item search-page__filter-item--double">
              <AutoCompleteInput<ClientLocationResponse>
                key={autoCompleteKey}
                id="seller-buyer-name"
                titleText="Seller or buyer name"
                placeholder="Search by name..."
                onAutoCompleteChange={(value) => getClientsByName(value)}
                extractItems={(raw) => raw as ClientLocationResponse[]}
                itemToString={(item) =>
                  item ? `${item.clientName}${item.clientLocnName ? ` – ${item.clientLocnName}` : ''}` : ''
                }
                onSelect={(item) => {
                  setSellerBuyerClientNum(item?.clientNumber ?? '');
                  setSellerBuyerLocNum(item?.clientLocnCode ?? '');
                }}
              />
            </div>
            <div className="search-page__filter-item">
              <SingleSelect
                id="seller-submission"
                titleText="Seller submission"
                items={sellerSubmitterItems}
                selectedItem={sellerSubmitterInput}
                itemToString={(item) => (item ? item.label : '')}
                onChange={({ selectedItem }) => setSellerSubmitterInput(selectedItem ?? null)}
              />
            </div>
            <div className="search-page__filter-item">
              <SingleSelect
                id="maturity-filter"
                titleText="Maturity"
                items={maturityItems}
                selectedItem={maturityInput}
                disabled={maturityLoading}
                itemToString={(item) => (item ? item.description : '')}
                onChange={({ selectedItem }) => setMaturityInput(selectedItem ?? null)}
              />
            </div>
            <div className="search-page__filter-item">
              <Button kind="primary" size="md" renderIcon={SearchIcon} iconDescription="Search" onClick={executeSearch}>
                Search
              </Button>
            </div>
          </div>
        </Column>

        {/* Clear filters — always visible */}
        <Column lg={16} md={8} sm={4} className="search-page__clear-col">
          <Button kind="ghost" size="sm" onClick={handleClearFilters}>
            Clear filters
          </Button>
        </Column>

        {isError && (
          <Column lg={16} md={8} sm={4} className="search-page__error-col">
            <p className="search-page__error">Failed to load results. Please try again.</p>
          </Column>
        )}

        <Column lg={16} md={8} sm={4} className="search-page__table-col">
          <ResultsTable
            rows={rows}
            columns={invoiceColumns}
            isSortable
            serverSide
            hasSearched={hasSearched}
            isLoading={isLoading}
            page={currentPage}
            pageSize={pageSize}
            onSortChange={(sortKey, sortDir) => {
              setSortParam(sortKey && sortDir !== 'NONE' ? `${sortKey},${sortDir.toLowerCase()}` : undefined);
              setCurrentPage(1);
            }}
            searchKeyword={keyword}
            onSearchKeywordChange={
              hasSearched || rows.length > 0
                ? (kw) => {
                    setKeyword(kw);
                    setCurrentPage(1);
                  }
                : undefined
            }
            totalItems={totalElements}
            paginationItemsPerPageText="Invoices per page:"
            onPaginationChange={({ page, pageSize: newPageSize }) => {
              setCurrentPage(page);
              setPageSize(newPageSize);
            }}
          />
        </Column>
      </Grid>
    </div>
  );
}
