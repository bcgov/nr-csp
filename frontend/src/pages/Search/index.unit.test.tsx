import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';
import { SearchPage } from './index';

// ── Service mocks ─────────────────────────────────────────────────────────────

vi.mock('@/services/search.service', () => ({
  useSearchQuery: vi.fn(),
  getClientsByName: vi.fn().mockResolvedValue([]),
}));

vi.mock('@/services/lookup.service', () => ({
  useInvoiceStatusesQuery: vi.fn(),
  useInvoiceTypesQuery: vi.fn(),
  useMaturityCodesQuery: vi.fn(),
}));

import { useSearchQuery } from '@/services/search.service';
import { useInvoiceStatusesQuery, useInvoiceTypesQuery, useMaturityCodesQuery } from '@/services/lookup.service';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const mockStatusItems = [
  { code: 'APP', description: 'Approved' },
  { code: 'REJ', description: 'Rejected' },
];

const mockTypeItems = [
  { code: 'SAL', description: 'Sales' },
  { code: 'PUR', description: 'Purchase' },
];

const mockMaturityItems = [
  { code: 'O', description: 'Old Growth' },
  { code: 'S', description: 'Second Growth' },
];

const mockSearchResult = {
  coastalLogSaleId: 200456,
  cspSubmissionId: 100123,
  invoiceStatus: 'Approved',
  invoiceNumber: 'WFP521046',
  invoiceDate: '2024-01-31',
  type: 'Sales',
  clientNumber: '014963285',
  clientName: 'ACME LOGGING LTD',
  maturity: 'Old Growth',
  submissionType: 'Electronic',
};

// ── Render helper ─────────────────────────────────────────────────────────────

function renderSearchPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <PageTitleProvider>
          <SearchPage />
        </PageTitleProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('SearchPage', () => {
  beforeEach(() => {
    vi.mocked(useSearchQuery).mockReturnValue({
      data: { content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 },
      isLoading: false,
      isError: false,
    } as any);
    vi.mocked(useInvoiceStatusesQuery).mockReturnValue({
      data: mockStatusItems,
      isLoading: false,
    } as any);
    vi.mocked(useInvoiceTypesQuery).mockReturnValue({
      data: mockTypeItems,
      isLoading: false,
    } as any);
    vi.mocked(useMaturityCodesQuery).mockReturnValue({
      data: mockMaturityItems,
      isLoading: false,
    } as any);
  });

  it('renders the page title', () => {
    renderSearchPage();
    expect(screen.getByRole('heading', { name: /invoice search/i })).toBeInTheDocument();
  });

  it('renders all filter inputs', () => {
    renderSearchPage();
    expect(screen.getByLabelText(/invoice date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/invoice number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/submitter client number/i)).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /status/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /type/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /maturity/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /seller submission/i })).toBeInTheDocument();
  });

  it('renders the Search button', () => {
    renderSearchPage();
    // Use exact string to avoid matching the keyword search bar's "Clear search input" button
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument();
  });

  it('renders the Clear filters button', () => {
    renderSearchPage();
    expect(screen.getByRole('button', { name: /clear filters/i })).toBeInTheDocument();
  });

  it('renders all table column headers', () => {
    renderSearchPage();
    const expectedHeaders = [
      'Invoice status',
      'Invoice number',
      'Invoice date',
      'Type',
      'Client number',
      'Client name',
      'Maturity',
      'Submission type',
    ];
    const columnHeaders = screen.getAllByRole('columnheader');
    for (const label of expectedHeaders) {
      expect(columnHeaders.some((h) => h.textContent?.includes(label))).toBe(true);
    }
  });

  it('shows no-search-performed empty state before first search', () => {
    renderSearchPage();
    expect(screen.getByText(/no search performed/i)).toBeInTheDocument();
  });

  it('shows loading skeleton while search is in progress', () => {
    vi.mocked(useSearchQuery).mockReturnValue({
      data: [],
      isLoading: true,
      isError: false,
    } as any);
    renderSearchPage();
    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByRole('table')).toBeInTheDocument();
  });

  it('shows invoice results after a search', () => {
    vi.mocked(useSearchQuery).mockReturnValue({
      data: { content: [mockSearchResult], totalElements: 1, totalPages: 1, size: 10, number: 0 },
      isLoading: false,
      isError: false,
    } as any);
    renderSearchPage();
    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByText('WFP521046')).toBeInTheDocument();
    // Verify the new client name column actually renders the value
    expect(screen.getByText('ACME LOGGING LTD')).toBeInTheDocument();
  });

  it('shows no-results empty state when search returns empty', () => {
    vi.mocked(useSearchQuery).mockReturnValue({
      data: { content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 },
      isLoading: false,
      isError: false,
    } as any);
    renderSearchPage();
    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByText(/no results found/i)).toBeInTheDocument();
  });

  it('shows error message when search fails', () => {
    vi.mocked(useSearchQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
    } as any);
    renderSearchPage();
    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByText(/failed to load results/i)).toBeInTheDocument();
  });

  it('status dropdown shows descriptions from the API', () => {
    renderSearchPage();
    // Open the status dropdown to reveal items
    fireEvent.click(screen.getByRole('combobox', { name: /status/i }));
    expect(screen.getByText('Approved')).toBeInTheDocument();
    expect(screen.getByText('Rejected')).toBeInTheDocument();
  });

  it('type dropdown shows descriptions from the API', () => {
    renderSearchPage();
    fireEvent.click(screen.getByRole('combobox', { name: /type/i }));
    expect(screen.getByText('Sales')).toBeInTheDocument();
    expect(screen.getByText('Purchase')).toBeInTheDocument();
  });

  it('maturity dropdown shows descriptions from the API', () => {
    renderSearchPage();
    fireEvent.click(screen.getByRole('combobox', { name: /maturity/i }));
    expect(screen.getByText('Old Growth')).toBeInTheDocument();
    expect(screen.getByText('Second Growth')).toBeInTheDocument();
  });

  it('disables status dropdown while lookup data is loading', () => {
    vi.mocked(useInvoiceStatusesQuery).mockReturnValue({
      data: [],
      isLoading: true,
    } as any);
    renderSearchPage();
    expect(screen.getByRole('combobox', { name: /status/i })).toBeDisabled();
  });

  it('shows pagination after results are returned', () => {
    vi.mocked(useSearchQuery).mockReturnValue({
      data: { content: [mockSearchResult], totalElements: 1, totalPages: 1, size: 10, number: 0 },
      isLoading: false,
      isError: false,
    } as any);
    renderSearchPage();
    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByText(/1\s*[–-]\s*1 of 1 items/i)).toBeInTheDocument();
  });
});
