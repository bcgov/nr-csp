import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';

const useSubmissionDetailQuery = vi.fn();

vi.mock('@/services/submissionHistory.service', () => ({
  useSubmissionDetailQuery: (id: string | undefined) => useSubmissionDetailQuery(id),
}));

import { ViewSubmissionPage } from './index';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/submission-history/42']}>
        <PageTitleProvider>
          <ViewSubmissionPage />
        </PageTitleProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const sampleDetail = {
  cspSubmissionId: 42,
  submissionId: 'SUB-42',
  submissionDate: '2026-01-15',
  submissionStatus: 'Complete',
  submissionType: 'Electronic',
  numberInvoicesSubmitted: 2,
  clientNumber: '00012345',
  clientName: 'Acme Forestry',
  invoices: [
    { coastalLogSaleId: 100, invoiceNumber: 'INV-100', invoiceDate: '2026-01-10', invoiceStatus: 'Approved', type: 'Original' },
    { coastalLogSaleId: 101, invoiceNumber: 'INV-101', invoiceDate: '2026-01-11', invoiceStatus: 'Rejected', type: 'Original' },
  ],
};

describe('ViewSubmissionPage', () => {
  it('renders a loading indicator while fetching', () => {
    useSubmissionDetailQuery.mockReturnValue({ data: undefined, isLoading: true, isError: false, error: null });
    renderPage();
    expect(screen.getByText(/loading submission/i)).toBeInTheDocument();
  });

  it('renders submission detail and invoice rows', () => {
    useSubmissionDetailQuery.mockReturnValue({ data: sampleDetail, isLoading: false, isError: false, error: null });
    renderPage();
    expect(screen.getByRole('heading', { name: /view submission/i })).toBeInTheDocument();
    // "SUB-42" appears in both the breadcrumb and the detail value.
    expect(screen.getAllByText('SUB-42').length).toBeGreaterThan(0);
    expect(screen.getByText('Acme Forestry')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'INV-100' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'INV-101' })).toBeInTheDocument();
  });

  it('renders an error message when the request fails', () => {
    useSubmissionDetailQuery.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: { response: { status: 404 } },
    });
    renderPage();
    expect(screen.getByText(/submission not found/i)).toBeInTheDocument();
  });
});
