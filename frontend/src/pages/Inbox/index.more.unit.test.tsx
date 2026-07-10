import { render, screen, fireEvent, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';

// ── Service mocks ─────────────────────────────────────────────────────────────

vi.mock('@/services/inbox.service', () => ({
  useInboxSearchQuery: vi.fn(),
}));

vi.mock('@/services/lookup.service', () => ({
  useSubmissionStatusesQuery: vi.fn(() => ({
    data: [{ code: 'REC', description: 'Received' }],
    isLoading: false,
  })),
}));

// ClientAutocomplete resolves suggestions through search.service; stub it so
// typing never triggers a real HTTP request.
vi.mock('@/services/search.service', () => ({
  getClientsByName: vi.fn().mockResolvedValue([]),
}));

import { useInboxSearchQuery } from '@/services/inbox.service';
import { InboxPage } from './index';

const mockUseInboxSearchQuery = vi.mocked(useInboxSearchQuery);

// ── Fixtures ──────────────────────────────────────────────────────────────────

const fullRow = {
  coastalLogSaleId: 555,
  submissionId: 'SUB-555',
  submissionDate: '2024-01-15',
  submissionStatus: 'REC',
  submissionType: 'Electronic',
  invTotal: 5,
  invApproved: 2,
  invRejected: 1,
  invProcessing: 1,
  invCancelled: 1,
};

// Exercises the null-coalescing fallbacks in toInboxRow.
const sparseRow = {
  coastalLogSaleId: null,
  submissionId: null,
  submissionDate: null,
  submissionStatus: 'REC',
  submissionType: 'Manual',
  invTotal: 0,
  invApproved: 0,
  invRejected: 0,
  invProcessing: 0,
  invCancelled: 0,
};

const emptyResult = {
  data: { content: [], totalElements: 0 },
  isLoading: false,
  isError: false,
  error: null,
};

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

const lastQueryParams = () =>
  mockUseInboxSearchQuery.mock.calls[mockUseInboxSearchQuery.mock.calls.length - 1][0] as Record<string, unknown>;

const lastQueryEnabled = () =>
  mockUseInboxSearchQuery.mock.calls[mockUseInboxSearchQuery.mock.calls.length - 1][1];

const setDate = (label: RegExp, value: string) => {
  const input = screen.getByLabelText(label);
  fireEvent.input(input, { target: { value } });
  fireEvent.blur(input);
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('InboxPage interactions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseInboxSearchQuery.mockReturnValue(emptyResult as never);
  });

  it('maps API rows into table rows, including fallbacks for missing ids and dates', () => {
    mockUseInboxSearchQuery.mockReturnValue({
      data: { content: [fullRow, sparseRow], totalElements: 2 },
      isLoading: false,
      isError: false,
      error: null,
    } as never);
    renderInboxPage();
    expect(screen.getByText('SUB-555')).toBeInTheDocument();
    expect(screen.getByText('January 15, 2024')).toBeInTheDocument();
    // Sparse row renders em-dash fallbacks for submission id and date.
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(2);
  });

  it('applies the filter inputs to the query when Search is clicked', () => {
    renderInboxPage();
    fireEvent.change(screen.getByLabelText(/invoice number/i), { target: { value: ' INV-9 ' } });
    setDate(/date start/i, '2024-01-01');
    setDate(/date end/i, '2024-02-01');

    fireEvent.click(screen.getByRole('combobox', { name: /submitted by/i }));
    fireEvent.click(screen.getByText('Buyer'));
    fireEvent.click(screen.getByRole('combobox', { name: /^type$/i }));
    fireEvent.click(screen.getByText('Manual'));
    fireEvent.click(screen.getByRole('combobox', { name: /status/i }));
    fireEvent.click(screen.getByText('Received'));

    fireEvent.click(screen.getByRole('button', { name: 'Search' }));

    expect(lastQueryEnabled()).toBe(true);
    expect(lastQueryParams()).toMatchObject({
      invoiceNum: 'INV-9',
      submissionDateFrom: '2024-01-01',
      submissionDateTo: '2024-02-01',
      submittedBy: 'Buyer',
      submissionType: 'Manual',
      submissionStatus: 'REC',
      page: 0,
    });
  });

  it('shows a range error when start date is after end date and blocks the search', () => {
    renderInboxPage();
    setDate(/date start/i, '2024-05-10');
    setDate(/date end/i, '2024-05-01');
    expect(
      screen.getByText('Submission Date Start must be before or equal to Submission Date End.'),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(lastQueryEnabled()).toBe(false);

    // Correcting the end date clears the error and allows searching again.
    setDate(/date end/i, '2024-06-01');
    expect(
      screen.queryByText('Submission Date Start must be before or equal to Submission Date End.'),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(lastQueryEnabled()).toBe(true);
  });

  it('clears every filter input when Clear filters is clicked', () => {
    renderInboxPage();
    fireEvent.change(screen.getByLabelText(/invoice number/i), { target: { value: 'ABC' } });
    setDate(/date start/i, '2024-05-10');
    setDate(/date end/i, '2024-05-01');

    fireEvent.click(screen.getByRole('button', { name: /clear filters/i }));

    expect(screen.getByLabelText(/invoice number/i)).toHaveValue('');
    expect(screen.getByLabelText(/date start/i)).toHaveValue('');
    expect(
      screen.queryByText('Submission Date Start must be before or equal to Submission Date End.'),
    ).not.toBeInTheDocument();
  });

  it('shows the most specific backend validation message on error', () => {
    mockUseInboxSearchQuery.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: { response: { data: { errors: [{ message: 'Date range too large.' }] } } },
    } as never);
    renderInboxPage();
    expect(screen.getByText('Date range too large.')).toBeInTheDocument();
  });

  it('falls back to the top-level backend message on error', () => {
    mockUseInboxSearchQuery.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: { response: { data: { message: 'Bad request.' } } },
    } as never);
    renderInboxPage();
    expect(screen.getByText('Bad request.')).toBeInTheDocument();
  });

  it('falls back to a generic message when the error has no response body', () => {
    mockUseInboxSearchQuery.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error('network down'),
    } as never);
    renderInboxPage();
    expect(screen.getByText('Failed to load results. Please try again.')).toBeInTheDocument();
  });

  it('applies the keyword filter and resets to page 1 on Enter', () => {
    mockUseInboxSearchQuery.mockReturnValue({
      data: { content: [fullRow], totalElements: 1 },
      isLoading: false,
      isError: false,
      error: null,
    } as never);
    renderInboxPage();
    const keywordInput = screen.getByRole('searchbox', { name: /search by keyword/i });
    fireEvent.change(keywordInput, { target: { value: 'cedar' } });
    fireEvent.keyDown(keywordInput, { key: 'Enter' });
    expect(lastQueryParams()).toMatchObject({ keyword: 'cedar', page: 0 });
  });

  it('cycles the sort param asc -> desc -> none when a header is clicked', () => {
    mockUseInboxSearchQuery.mockReturnValue({
      data: { content: [fullRow], totalElements: 1 },
      isLoading: false,
      isError: false,
      error: null,
    } as never);
    renderInboxPage();
    const header = screen.getByRole('columnheader', { name: /submission id/i });
    const sortButton = within(header).getByRole('button');

    fireEvent.click(sortButton);
    expect(lastQueryParams()).toMatchObject({ sort: 'submissionId,asc', page: 0 });
    fireEvent.click(sortButton);
    expect(lastQueryParams()).toMatchObject({ sort: 'submissionId,desc', page: 0 });
    fireEvent.click(sortButton);
    expect(lastQueryParams().sort).toBeUndefined();
  });

  it('updates page and page size through the pagination bar', () => {
    mockUseInboxSearchQuery.mockReturnValue({
      data: { content: [fullRow], totalElements: 45 },
      isLoading: false,
      isError: false,
      error: null,
    } as never);
    renderInboxPage();

    fireEvent.click(screen.getByRole('button', { name: /next page/i }));
    expect(lastQueryParams()).toMatchObject({ page: 1, size: 10 });

    fireEvent.change(screen.getByLabelText('Invoice per page:'), { target: { value: '20' } });
    expect(lastQueryParams()).toMatchObject({ size: 20 });
  });
});
