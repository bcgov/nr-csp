import { render, screen, fireEvent, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';
import { useSubmissionHistoryListQuery, useSubmissionInvoiceCommentsQuery } from '@/services/submissionHistory.service';

// ── Service mocks ─────────────────────────────────────────────────────────────

vi.mock('@/services/submissionHistory.service', () => ({
  useSubmissionHistoryListQuery: vi.fn(),
  useSubmissionInvoiceCommentsQuery: vi.fn(),
}));

const mockListQuery = vi.mocked(useSubmissionHistoryListQuery);
const mockCommentsQuery = vi.mocked(useSubmissionInvoiceCommentsQuery);

import { SubmissionHistoryPage } from './index';

const sampleRow = {
  cspSubmissionId: 1234,
  submissionDate: '2025-08-09',
  submittedBy: 'John Smith',
  clientNumber: '00001234',
  clientName: 'CANFOR CORPORATION',
  submissionStatus: 'Complete',
  invoiceCount: 12,
  commentedInvoiceCount: 2,
};

function setListData(content: unknown[]) {
  mockListQuery.mockReturnValue({
    data: { content, totalElements: content.length },
    isLoading: false,
    isError: false,
    error: null,
  } as unknown as ReturnType<typeof useSubmissionHistoryListQuery>);
}

function setComments(comments: unknown[]) {
  mockCommentsQuery.mockReturnValue({
    data: comments,
    isLoading: false,
    isError: false,
  } as unknown as ReturnType<typeof useSubmissionInvoiceCommentsQuery>);
}

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
  beforeEach(() => {
    vi.clearAllMocks();
    setComments([]);
  });

  it('renders the page title', () => {
    setListData([]);
    renderPage();
    expect(screen.getByRole('heading', { name: /submission history/i })).toBeInTheDocument();
  });

  it('renders the new column headers including Invoices and excluding Comments', () => {
    setListData([]);
    renderPage();
    const columnHeaders = screen.getAllByRole('columnheader');
    const headerText = columnHeaders.map((h) => h.textContent ?? '');
    for (const label of ['Submission Date', 'Submitted By', 'Client Name', 'Status', 'Invoices', 'Actions']) {
      expect(headerText.some((t) => t.includes(label))).toBe(true);
    }
    expect(headerText.some((t) => t.includes('Comments'))).toBe(false);
  });

  it('shows the empty state when there are no submissions', () => {
    setListData([]);
    renderPage();
    expect(screen.getByText(/no submissions found/i)).toBeInTheDocument();
  });

  it('renders the invoice count link and comment badge', () => {
    setListData([sampleRow]);
    renderPage();
    expect(screen.getByRole('link', { name: /12 invoices/i })).toBeInTheDocument();
    // commentedInvoiceCount badge
    expect(screen.getByTitle(/2 invoice\(s\) with comments/i)).toBeInTheDocument();
  });

  it('loads invoice comments into the expanded sub-table when a row is expanded', () => {
    setListData([sampleRow]);
    setComments([
      {
        invoiceNumber: 'INV-2025-08-0001',
        status: 'Approved',
        comment: 'All invoice line items verified and approved.',
      },
      { invoiceNumber: 'INV-2025-08-0002', status: 'Rejected', comment: 'Line item volume does not match.' },
    ]);
    renderPage();

    // Expand the submission row.
    fireEvent.click(screen.getAllByLabelText(/expand current row/i)[0]);

    expect(screen.getByText(/invoice comments \(2 of 2 have comments\)/i)).toBeInTheDocument();
    expect(screen.getByText('INV-2025-08-0001')).toBeInTheDocument();
    const rejectedCell = screen.getByText('Line item volume does not match.');
    expect(rejectedCell).toHaveClass('invoice-comments-panel__comment--rejected');
    // Status pills exist within the panel.
    const panel = rejectedCell.closest('.invoice-comments-panel') as HTMLElement;
    expect(within(panel).getByText('Approved')).toBeInTheDocument();
    expect(within(panel).getByText('Rejected')).toBeInTheDocument();
  });
});
