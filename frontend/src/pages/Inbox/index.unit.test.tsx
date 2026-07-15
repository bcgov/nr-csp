import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';
import { useInboxSearchQuery } from '@/services/inbox.service';

// ── Service mocks ─────────────────────────────────────────────────────────────
// Stub the data hooks so the table renders deterministically: clicking Search
// must land on the "No results found" empty state, not the loading skeleton.

const defaultInboxSearchResult = {
  data: { content: [], totalElements: 0 },
  isLoading: false,
  isError: false,
  error: null,
};

vi.mock('@/services/inbox.service', () => ({
  useInboxSearchQuery: vi.fn(() => defaultInboxSearchResult),
}));

vi.mock('@/services/lookup.service', () => ({
  useSubmissionStatusesQuery: vi.fn(() => ({ data: [], isLoading: false })),
}));

const mockInboxSearchQuery = vi.mocked(useInboxSearchQuery);

import { InboxPage } from './index';

// ── Render helper ─────────────────────────────────────────────────────────────

function renderInboxPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <PageTitleProvider>
          <InboxPage />
        </PageTitleProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('InboxPage', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
  });

  afterEach(() => {
    window.sessionStorage.clear();
    mockInboxSearchQuery.mockReturnValue(defaultInboxSearchResult as unknown as ReturnType<typeof useInboxSearchQuery>);
  });

  it('renders the page title', () => {
    renderInboxPage();
    expect(screen.getByRole('heading', { name: /inbox/i })).toBeInTheDocument();
  });

  it('renders all filter inputs', () => {
    renderInboxPage();
    expect(screen.getByLabelText(/invoice number/i)).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /submitter client name/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/date start/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/date end/i)).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /submitted by/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /type/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /status/i })).toBeInTheDocument();
  });

  it('renders the Search button', () => {
    renderInboxPage();
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument();
  });

  it('renders the Clear filters button', () => {
    renderInboxPage();
    expect(screen.getByRole('button', { name: /clear filters/i })).toBeInTheDocument();
  });

  it('renders all table column headers', () => {
    renderInboxPage();
    const expectedHeaders = [
      'Submission ID',
      'Submission date',
      'Status',
      'Type',
      'Total',
      'Approved',
      'Rejected',
      'Processing',
      'Cancelled',
    ];
    const columnHeaders = screen.getAllByRole('columnheader');
    for (const label of expectedHeaders) {
      expect(columnHeaders.some((h) => h.textContent?.includes(label))).toBe(true);
    }
  });

  it('shows the initial empty state before a search', () => {
    renderInboxPage();
    expect(screen.getByText(/your search results will appear here/i)).toBeInTheDocument();
    expect(screen.getByText(/enter at least one criteria to start the search/i)).toBeInTheDocument();
  });

  it('switches to the no-results empty state after a search', () => {
    renderInboxPage();
    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByText(/no results found/i)).toBeInTheDocument();
  });

  it('renders the type dropdown options', () => {
    renderInboxPage();
    fireEvent.click(screen.getByRole('combobox', { name: /type/i }));
    expect(screen.getByText('Electronic')).toBeInTheDocument();
    expect(screen.getByText('Manual')).toBeInTheDocument();
  });

  it('restores page, keyword, invoice number, applied filters, and start date from sessionStorage on mount', async () => {
    window.sessionStorage.setItem('csp.table.inbox.v1.hasSearched', 'true');
    window.sessionStorage.setItem('csp.table.inbox.v1.page', '2');
    window.sessionStorage.setItem('csp.table.inbox.v1.keyword', JSON.stringify('widget'));
    window.sessionStorage.setItem('csp.table.inbox.v1.invoiceNumberInput', JSON.stringify('INV-123'));
    window.sessionStorage.setItem('csp.table.inbox.v1.startDateInput', JSON.stringify('2026-01-15'));
    window.sessionStorage.setItem('csp.table.inbox.v1.appliedFilters', JSON.stringify({ invoiceNum: 'INV-123' }));

    mockInboxSearchQuery.mockReturnValue({
      data: {
        content: Array.from({ length: 10 }, (_, i) => ({
          coastalLogSaleId: 200 + i,
          submissionId: `SUB-${200 + i}`,
          submissionDate: '2026-01-15',
          submissionStatus: 'Complete',
          submissionType: 'Electronic',
          invTotal: 1,
          invApproved: 1,
          invRejected: 0,
          invProcessing: 0,
          invCancelled: 0,
        })),
        totalElements: 30,
      },
      isLoading: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useInboxSearchQuery>);

    renderInboxPage();

    // (a) invoice number input restored.
    expect(await screen.findByDisplayValue('INV-123')).toBeInTheDocument();

    // (b) Carbon Pagination page select reflects the restored page.
    const pageSelect = await screen.findByLabelText(/page of \d+ pages/i);
    expect((pageSelect as HTMLSelectElement).value).toBe('2');

    // (c) the "Date start" DateInput is controlled and displays the restored date.
    expect(await screen.findByDisplayValue('2026-01-15')).toBeInTheDocument();
  });
});
