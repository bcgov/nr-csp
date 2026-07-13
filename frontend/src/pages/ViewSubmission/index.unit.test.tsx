import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi } from 'vitest';

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
  submissionDate: '2025-11-10',
  submittedBy: 'Emily Davis',
  submissionStatus: 'Inbox',
  clientNumber: '00000987',
  clientName: 'INTERFOR CORPORATION',
  clientLocnCode: '00',
  email: 'mailto:emily.davis@gov.bc.ca',
  telephone: '2503878363',
  monthComplete: 'N',
  sellerSubmission: 'Y',
  adminComment: null,
  invoices: [
    {
      coastalLogSaleId: 100,
      invoiceNumber: 'INV-100',
      invoiceDate: '2024-04-16',
      type: 'SAL',
      status: 'Pending',
      sellerClient: '00126920/00',
      buyerClient: '00123946/00',
      maturity: 'O',
      fobLocation: 'TEST',
      totalAmount: 1,
      totalVolume: 1,
      totalPieces: 1,
      replacesInvoiceNumbers: null,
      adjustsInvoiceNumbers: null,
      sellerClientLocnCode: '00',
      buyerClientLocnCode: '00',
      otherPartyName: null,
      otherPartyCity: null,
      otherPartyProvState: null,
      primarySortCode: 'G',
      clientPrimarySortCode: 'G',
      boomNumbers: '1',
      timberMarks: '1',
      weighSlips: '1',
      submitterNotes: '1',
      staffComment: null,
    },
    {
      coastalLogSaleId: 101,
      invoiceNumber: 'INV-101',
      invoiceDate: '2024-04-16',
      type: 'SAL',
      status: 'Rejected',
      sellerClient: '00126920/00',
      buyerClient: '00123946/00',
      maturity: 'O',
      fobLocation: 'TEST',
      totalAmount: 1,
      totalVolume: 1,
      totalPieces: 1,
      replacesInvoiceNumbers: null,
      adjustsInvoiceNumbers: null,
      sellerClientLocnCode: '00',
      buyerClientLocnCode: '00',
      otherPartyName: null,
      otherPartyCity: null,
      otherPartyProvState: null,
      primarySortCode: 'G',
      clientPrimarySortCode: 'G',
      boomNumbers: null,
      timberMarks: null,
      weighSlips: null,
      submitterNotes: null,
      staffComment: 'Needs correction.',
    },
  ],
  lineItems: [
    {
      coastalLogSaleId: 100,
      invoiceNumber: 'INV-100',
      species: 'FI',
      grade: 'J',
      sortCode: 'G',
      clientSortCode: 'G',
      pieces: 1,
      volume: 1,
      price: 1,
    },
    {
      coastalLogSaleId: 100,
      invoiceNumber: 'INV-100',
      species: 'CE',
      grade: 'J',
      sortCode: 'G',
      clientSortCode: 'G',
      pieces: 1,
      volume: 1,
      price: 1,
    },
    {
      coastalLogSaleId: 101,
      invoiceNumber: 'INV-101',
      species: 'HE',
      grade: 'J',
      sortCode: 'G',
      clientSortCode: 'G',
      pieces: 1,
      volume: 1,
      price: 1,
    },
  ],
};

describe('ViewSubmissionPage', () => {
  it('renders a loading indicator while fetching', () => {
    useSubmissionDetailQuery.mockReturnValue({ data: undefined, isLoading: true, isError: false, error: null });
    renderPage();
    expect(screen.getByText(/loading submission/i)).toBeInTheDocument();
  });

  it('renders submission detail, the invoices summary and invoice rows', () => {
    useSubmissionDetailQuery.mockReturnValue({ data: sampleDetail, isLoading: false, isError: false, error: null });
    renderPage();
    expect(screen.getByRole('heading', { name: /view submission/i })).toBeInTheDocument();
    expect(screen.getByText('INTERFOR CORPORATION', { exact: false })).toBeInTheDocument();
    // Summary line counts: 2 invoices, 3 line items.
    expect(screen.getByText(/2 invoices · 3 line items/)).toBeInTheDocument();
    // Decision column renders the per-invoice status pill.
    expect(screen.getByText('Pending')).toBeInTheDocument();
    expect(screen.getByText('Rejected')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'INV-100' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'INV-101' })).toBeInTheDocument();
  });

  it('strips the mailto: prefix from the email address', () => {
    useSubmissionDetailQuery.mockReturnValue({ data: sampleDetail, isLoading: false, isError: false, error: null });
    renderPage();
    expect(screen.getByText('emily.davis@gov.bc.ca')).toBeInTheDocument();
    expect(screen.queryByText(/mailto:/)).not.toBeInTheDocument();
  });

  it('renders the per-invoice detail panel with its fields and line-item count', () => {
    useSubmissionDetailQuery.mockReturnValue({ data: sampleDetail, isLoading: false, isError: false, error: null });
    renderPage();
    // Carbon keeps expanded-row content in the DOM and toggles its visibility,
    // so the panel content is present regardless of the expand/collapse state.
    expect(screen.getByText('Invoice details for INV-100')).toBeInTheDocument();
    expect(screen.getByText('Line items for INV-100 (2)')).toBeInTheDocument();
    expect(screen.getByText('Line items for INV-101 (1)')).toBeInTheDocument();
    // Staff comment falls back to the placeholder when none is present (INV-100).
    expect(screen.getByText(/no comment provided/i)).toBeInTheDocument();
    // ...and shows the real comment when present (INV-101).
    expect(screen.getByText('Needs correction.')).toBeInTheDocument();
    // The "Expand all" control is available and clickable.
    fireEvent.click(screen.getByRole('link', { name: /expand all/i }));
    expect(screen.getByRole('link', { name: /collapse all/i })).toBeInTheDocument();
  });

  it('groups line items by coastal log sale id, not invoice number', () => {
    // Two invoices sharing the same invoice number: line items must still be
    // attributed to the correct invoice by its coastal_log_sale_id. Grouping by
    // invoice number would merge all three lines under one invoice.
    const duplicateNumbers = {
      ...sampleDetail,
      invoices: [
        { ...sampleDetail.invoices[0], coastalLogSaleId: 200, invoiceNumber: 'DUP' },
        { ...sampleDetail.invoices[1], coastalLogSaleId: 201, invoiceNumber: 'DUP' },
      ],
      lineItems: [
        { ...sampleDetail.lineItems[0], coastalLogSaleId: 200, invoiceNumber: 'DUP' },
        { ...sampleDetail.lineItems[1], coastalLogSaleId: 200, invoiceNumber: 'DUP' },
        { ...sampleDetail.lineItems[2], coastalLogSaleId: 201, invoiceNumber: 'DUP' },
      ],
    };
    useSubmissionDetailQuery.mockReturnValue({
      data: duplicateNumbers,
      isLoading: false,
      isError: false,
      error: null,
    });
    renderPage();
    // Invoice 200 owns 2 lines, invoice 201 owns 1 — despite identical numbers.
    expect(screen.getByText('Line items for DUP (2)')).toBeInTheDocument();
    expect(screen.getByText('Line items for DUP (1)')).toBeInTheDocument();
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
