import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, within } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import { MockAuthProvider } from '@/context/auth/MockAuthProvider';
import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';
import { ViewSubmissionPage } from '@/pages/ViewSubmission';
import { useSubmissionHistoryListQuery, useSubmissionInvoiceCommentsQuery } from '@/services/submissionHistory.service';

import { SubmissionHistoryPage } from './index';

// ── Service mocks ─────────────────────────────────────────────────────────────

vi.mock('@/services/submissionHistory.service', () => ({
  useSubmissionDetailQuery: () => ({ isLoading: true }),
  useSubmissionHistoryListQuery: vi.fn(),
  useSubmissionInvoiceCommentsQuery: vi.fn(),
}));

const mockListQuery = vi.mocked(useSubmissionHistoryListQuery);
const mockCommentsQuery = vi.mocked(useSubmissionInvoiceCommentsQuery);

const sampleRow = {
  cspSubmissionId: 1234,
  submissionId: '9001',
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
  const router = createMemoryRouter(
    [
      { path: '/submission-history', element: <SubmissionHistoryPage /> },
      { path: '/submission-history/:submissionId', element: <ViewSubmissionPage /> },
    ],
    { initialEntries: ['/submission-history'] },
  );
  render(
    <QueryClientProvider client={queryClient}>
      <MockAuthProvider>
        <PageTitleProvider>
          <RouterProvider router={router} />
        </PageTitleProvider>
      </MockAuthProvider>
    </QueryClientProvider>,
  );
  return router;
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('SubmissionHistoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.sessionStorage.clear();
    setComments([]);
  });

  afterEach(() => {
    window.sessionStorage.clear();
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
    // The link targets the submission number, not the internal csp submission id.
    expect(screen.getByRole('link', { name: /12 invoices/i })).toHaveAttribute('href', '/submission-history/9001');
    // commentedInvoiceCount badge
    expect(screen.getByTitle(/2 invoice\(s\) with comments/i)).toBeInTheDocument();
  });

  it.each(['12 invoices', 'View submission'])('leaves a modified click on %s to the browser', (name) => {
    setListData([sampleRow]);
    const router = renderPage();
    const link = screen.getByRole('link', { name });
    expect(link).toHaveAttribute('href', '/submission-history/9001');

    expect(fireEvent.click(link, { ctrlKey: true })).toBe(true);
    expect(router.state.location.pathname).toBe('/submission-history');
  });

  it('does not link a manual submission, which has no submission number', () => {
    setListData([{ ...sampleRow, submissionId: null }]);
    renderPage();
    expect(screen.queryByRole('link', { name: /12 invoices/i })).not.toBeInTheDocument();
    expect(screen.getByText(/12 invoices/i)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /view submission/i })).not.toBeInTheDocument();
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

  it.each(['12 invoices', 'View submission'])('restores the History table after opening %s', async (name) => {
    window.sessionStorage.setItem('csp.table.submissionHistory.v1.page', '2');
    window.sessionStorage.setItem('csp.table.submissionHistory.v1.pageSize', '20');
    window.sessionStorage.setItem('csp.table.submissionHistory.v1.expanded', '["101"]');
    window.sessionStorage.setItem('csp.table.submissionHistory.v1.sort', JSON.stringify('submissionDate,desc'));

    mockListQuery.mockReturnValue({
      data: {
        content: [
          { ...sampleRow, cspSubmissionId: 101 },
          { ...sampleRow, cspSubmissionId: 102, submissionId: '9002' },
        ],
        totalElements: 60,
      },
      isLoading: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useSubmissionHistoryListQuery>);
    setComments([{ invoiceNumber: 'INV-2025-08-0001', status: 'Approved', comment: 'All good.' }]);

    const router = renderPage();
    fireEvent.click(screen.getAllByRole('link', { name })[0]);
    const backLink = await screen.findByRole('link', { name: 'Back to Submission History' });
    expect(router.state.location).toMatchObject({
      pathname: '/submission-history/9001',
      state: { from: '/submission-history' },
    });
    expect(backLink).toHaveAttribute('href', '/submission-history');
    fireEvent.click(backLink);

    const pageSelect = await screen.findByLabelText(/page of \d+ pages/i);
    expect((pageSelect as HTMLSelectElement).value).toBe('2');
    expect(mockListQuery).toHaveBeenLastCalledWith({ page: 1, size: 20, sort: 'submissionDate,desc' });
    expect(screen.getByRole('columnheader', { name: /submission date/i })).toHaveAttribute('aria-sort', 'descending');

    expect(screen.getByTestId('invoice-comments-panel-101')).toBeInTheDocument();

    // Row 101 was seeded in the restored `expanded` Set; row 102 was not. Assert the
    // actual aria-expanded state of each row's expand toggle (tied to the row via
    // aria-controls="...-expanded-row-<id>") so the test genuinely exercises the
    // restored `expandedRowIds` Set rather than the always-rendered expanded-content row.
    const toggle101 = document.querySelector('button[aria-controls$="-101"]');
    const toggle102 = document.querySelector('button[aria-controls$="-102"]');
    expect(toggle101).toHaveAttribute('aria-expanded', 'true');
    expect(toggle102).toHaveAttribute('aria-expanded', 'false');
  });
});
