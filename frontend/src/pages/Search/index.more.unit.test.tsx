import { render, screen, fireEvent, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';

// ── Service and router mocks ──────────────────────────────────────────────────

const mockNavigate = vi.hoisted(() => vi.fn());

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('@/services/search.service', () => ({
  useSearchQuery: vi.fn(),
  getClientsByName: vi.fn().mockResolvedValue([]),
}));

vi.mock('@/services/lookup.service', () => ({
  useInvoiceStatusesQuery: vi.fn(),
  useInvoiceTypesQuery: vi.fn(),
  useMaturityCodesQuery: vi.fn(),
}));

import { useSearchQuery, getClientsByName } from '@/services/search.service';
import { useInvoiceStatusesQuery, useInvoiceTypesQuery, useMaturityCodesQuery } from '@/services/lookup.service';
import { SearchPage } from './index';

const mockUseSearchQuery = vi.mocked(useSearchQuery);
const mockGetClientsByName = vi.mocked(getClientsByName);

// ── Fixtures ──────────────────────────────────────────────────────────────────

const mockStatusItems = [{ code: 'APP', description: 'Approved' }];
const mockTypeItems = [{ code: 'SAL', description: 'Sales' }];
const mockMaturityItems = [
  { code: 'O', description: 'Old Growth' },
  { code: 'C', description: 'Cants / Export' },
  { code: 'E', description: 'Export' },
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
  maturity: 'Cants / Export',
  submissionType: 'Electronic',
};

const emptyPage = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };

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

const lastQueryParams = () =>
  mockUseSearchQuery.mock.calls[mockUseSearchQuery.mock.calls.length - 1][0] as Record<string, unknown>;

const setDate = (label: RegExp, value: string) => {
  const input = screen.getByLabelText(label);
  fireEvent.input(input, { target: { value } });
  fireEvent.blur(input);
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('SearchPage interactions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.sessionStorage.clear();
    mockGetClientsByName.mockResolvedValue([]);
    mockUseSearchQuery.mockReturnValue({ data: emptyPage, isLoading: false, isError: false } as never);
    vi.mocked(useInvoiceStatusesQuery).mockReturnValue({ data: mockStatusItems, isLoading: false } as never);
    vi.mocked(useInvoiceTypesQuery).mockReturnValue({ data: mockTypeItems, isLoading: false } as never);
    vi.mocked(useMaturityCodesQuery).mockReturnValue({ data: mockMaturityItems, isLoading: false } as never);
  });

  it('navigates to the invoice page when an invoice number link is clicked', () => {
    mockUseSearchQuery.mockReturnValue({
      data: { content: [mockSearchResult], totalElements: 1, totalPages: 1, size: 20, number: 0 },
      isLoading: false,
      isError: false,
    } as never);
    renderSearchPage();
    fireEvent.click(screen.getByRole('link', { name: 'WFP521046' }));
    expect(mockNavigate).toHaveBeenCalledWith('/invoice/200456', { state: { fromSearch: true } });
  });

  it('renders a Cants / Export maturity cell as Cants', () => {
    mockUseSearchQuery.mockReturnValue({
      data: { content: [mockSearchResult], totalElements: 1, totalPages: 1, size: 20, number: 0 },
      isLoading: false,
      isError: false,
    } as never);
    renderSearchPage();
    const row = screen.getByRole('link', { name: 'WFP521046' }).closest('tr') as HTMLElement;
    expect(within(row).getByText('Cants')).toBeInTheDocument();
  });

  it('applies date, text and dropdown filters to the query when Search is clicked', () => {
    renderSearchPage();
    setDate(/^invoice date/i, '2024-01-31');
    setDate(/start date \(invoice\)/i, '2024-01-01');
    setDate(/end date \(invoice\)/i, '2024-02-28');
    fireEvent.change(screen.getByLabelText(/invoice number/i), { target: { value: ' WFP* ' } });
    fireEvent.click(screen.getByRole('combobox', { name: /status/i }));
    fireEvent.click(screen.getByText('Approved'));
    fireEvent.click(screen.getByRole('combobox', { name: /^type$/i }));
    fireEvent.click(screen.getByText('Sales'));
    fireEvent.click(screen.getByRole('combobox', { name: /seller submission/i }));
    fireEvent.click(screen.getByText('Yes'));
    fireEvent.click(screen.getByRole('combobox', { name: /maturity/i }));
    fireEvent.click(screen.getByRole('option', { name: 'Cants' }));

    fireEvent.click(screen.getByRole('button', { name: 'Search' }));

    expect(lastQueryParams()).toMatchObject({
      invDate: '2024-01-31',
      startDate: '2024-01-01',
      endDate: '2024-02-28',
      invNumber: 'WFP*',
      invStatus: 'APP',
      invType: 'SAL',
      sellerSubmitter: true,
      maturity: 'C',
      page: 0,
      size: 20,
    });
  });

  it('maps a No seller submission selection to false', () => {
    renderSearchPage();
    fireEvent.click(screen.getByRole('combobox', { name: /seller submission/i }));
    fireEvent.click(screen.getByText('No'));
    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(lastQueryParams()).toMatchObject({ sellerSubmitter: false });
  });

  it('pads the submitter client number to 8 digits on blur and strips non-digits', () => {
    renderSearchPage();
    const input = screen.getByLabelText(/submitter client number/i);
    fireEvent.change(input, { target: { value: '12ab34' } });
    expect(input).toHaveValue('1234');
    fireEvent.focusOut(input, { target: { value: '1234' } });
    expect(input).toHaveValue('00001234');

    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(lastQueryParams()).toMatchObject({ submitterClientNum: '00001234' });
  });

  it('applies a seller/buyer selection from the autocomplete to the query', async () => {
    mockGetClientsByName.mockResolvedValue([
      { clientNumber: '00005555', clientName: 'ACME LOGGING LTD', clientLocnCode: '01', clientLocnName: 'Duncan' },
    ] as never);
    renderSearchPage();
    const nameInput = screen.getByRole('combobox', { name: /seller or buyer name/i });
    fireEvent.click(nameInput);
    fireEvent.change(nameInput, { target: { value: 'acme' } });

    const option = await screen.findByRole('option', { name: /acme logging ltd – duncan/i }, { timeout: 3000 });
    fireEvent.click(option);
    expect(mockGetClientsByName).toHaveBeenCalledWith('acme');

    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(lastQueryParams()).toMatchObject({ sellerBuyerClientNum: '00005555', sellerBuyerLocNum: '01' });
  });

  it('clears every filter when Clear filters is clicked', () => {
    renderSearchPage();
    setDate(/^invoice date/i, '2024-01-31');
    fireEvent.change(screen.getByLabelText(/invoice number/i), { target: { value: 'WFP*' } });
    fireEvent.change(screen.getByLabelText(/submitter client number/i), { target: { value: '99' } });
    fireEvent.click(screen.getByRole('combobox', { name: /status/i }));
    fireEvent.click(screen.getByText('Approved'));

    fireEvent.click(screen.getByRole('button', { name: /clear filters/i }));

    expect(screen.getByLabelText(/invoice number/i)).toHaveValue('');
    expect(screen.getByLabelText(/submitter client number/i)).toHaveValue('');
    expect(screen.getByLabelText(/^invoice date/i)).toHaveValue('');

    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    const params = lastQueryParams();
    expect(params.invDate).toBeUndefined();
    expect(params.invNumber).toBeUndefined();
    expect(params.invStatus).toBeUndefined();
    expect(params.submitterClientNum).toBeUndefined();
  });

  it('applies the keyword filter and resets to page 1 on Enter', () => {
    mockUseSearchQuery.mockReturnValue({
      data: { content: [mockSearchResult], totalElements: 1, totalPages: 1, size: 20, number: 0 },
      isLoading: false,
      isError: false,
    } as never);
    renderSearchPage();
    const keywordInput = screen.getByRole('searchbox', { name: /search by keyword/i });
    fireEvent.change(keywordInput, { target: { value: 'hemlock' } });
    fireEvent.keyDown(keywordInput, { key: 'Enter' });
    expect(lastQueryParams()).toMatchObject({ keyword: 'hemlock', page: 0 });
  });

  it('cycles the sort param asc -> desc -> none when a header is clicked', () => {
    mockUseSearchQuery.mockReturnValue({
      data: { content: [mockSearchResult], totalElements: 1, totalPages: 1, size: 20, number: 0 },
      isLoading: false,
      isError: false,
    } as never);
    renderSearchPage();
    const header = screen.getByRole('columnheader', { name: /invoice date/i });
    const sortButton = within(header).getByRole('button');

    fireEvent.click(sortButton);
    expect(lastQueryParams()).toMatchObject({ sort: 'invoiceDate,asc', page: 0 });
    fireEvent.click(sortButton);
    expect(lastQueryParams()).toMatchObject({ sort: 'invoiceDate,desc', page: 0 });
    fireEvent.click(sortButton);
    expect(lastQueryParams().sort).toBeUndefined();
  });

  it('updates page and page size through the pagination bar', () => {
    mockUseSearchQuery.mockReturnValue({
      data: { content: [mockSearchResult], totalElements: 90, totalPages: 5, size: 20, number: 0 },
      isLoading: false,
      isError: false,
    } as never);
    renderSearchPage();

    fireEvent.click(screen.getByRole('button', { name: /next page/i }));
    expect(lastQueryParams()).toMatchObject({ page: 1, size: 20 });

    fireEvent.change(screen.getByLabelText('Invoices per page:'), { target: { value: '40' } });
    expect(lastQueryParams()).toMatchObject({ size: 40 });
  });
});
