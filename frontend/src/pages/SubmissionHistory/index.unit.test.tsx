import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';

// ── Service mocks ─────────────────────────────────────────────────────────────
// Stub the data hooks so the table renders deterministically: clicking Search
// lands on the "No results found" empty state, not the loading skeleton.

vi.mock('@/services/submissionHistory.service', () => ({
  useSubmissionHistorySearchQuery: vi.fn(() => ({
    data: { content: [], totalElements: 0 },
    isLoading: false,
    isError: false,
    error: null,
  })),
}));

vi.mock('@/services/lookup.service', () => ({
  useSubmissionStatusesQuery: vi.fn(() => ({ data: [], isLoading: false })),
}));

import { SubmissionHistoryPage } from './index';

// ── Render helper ─────────────────────────────────────────────────────────────

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <PageTitleProvider>
          <SubmissionHistoryPage />
        </PageTitleProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('SubmissionHistoryPage', () => {
  it('renders the page title', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /submission history/i })).toBeInTheDocument();
  });

  it('renders all filter inputs', () => {
    renderPage();
    expect(screen.getByLabelText(/date start/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/date end/i)).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /submitter client name/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /type/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /status/i })).toBeInTheDocument();
  });

  it('renders the Search button', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Search', exact: true })).toBeInTheDocument();
  });

  it('renders the Clear filters button', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /clear filters/i })).toBeInTheDocument();
  });

  it('renders all table column headers', () => {
    renderPage();
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
    renderPage();
    expect(screen.getByText(/your search results will appear here/i)).toBeInTheDocument();
    expect(screen.getByText(/enter at least one criteria to start the search/i)).toBeInTheDocument();
  });

  it('switches to the no-results empty state after a search', () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByText(/no results found/i)).toBeInTheDocument();
  });
});
