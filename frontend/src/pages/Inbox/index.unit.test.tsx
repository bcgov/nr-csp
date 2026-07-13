import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';

// ── Service mocks ─────────────────────────────────────────────────────────────
// Stub the data hooks so the table renders deterministically: clicking Search
// must land on the "No results found" empty state, not the loading skeleton.

vi.mock('@/services/inbox.service', () => ({
  useInboxSearchQuery: vi.fn(() => ({
    data: { content: [], totalElements: 0 },
    isLoading: false,
    isError: false,
    error: null,
  })),
}));

vi.mock('@/services/lookup.service', () => ({
  useSubmissionStatusesQuery: vi.fn(() => ({ data: [], isLoading: false })),
}));

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
});
