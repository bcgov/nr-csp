import { render, screen, fireEvent, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import * as service from '@/services/flatPriceConversion.service';
import * as lookup from '@/services/lookup.service';
import { NotificationContext } from '@/context/notification/NotificationContext';
import { FlatPriceConversionPage } from './index';

vi.mock('@/context/auth/usePermission', () => ({
  usePermission: () => true,
}));

vi.mock('@/services/flatPriceConversion.service', () => ({
  useSearchFlatPriceConversionsQuery: vi.fn(),
  useCreateFlatPriceConversionMutation: vi.fn(),
  useUpdateFlatPriceConversionMutation: vi.fn(),
  useDeleteFlatPriceConversionMutation: vi.fn(),
  useExportFlatPriceConversionsMutation: vi.fn(),
  extractApiErrorMessage: (e: unknown) => (e as any)?.message ?? 'Error',
}));

vi.mock('@/services/lookup.service', () => ({
  useMaturityCodesQuery: vi.fn(),
  useSortCodesLookupQuery: vi.fn(),
  useSpeciesLookupQuery: vi.fn(),
  useGradeLookupQuery: vi.fn(),
  useGradesBySpeciesLookupQuery: vi.fn(),
}));

const mockMutation = () => ({ mutate: vi.fn(), isPending: false, isError: false, error: null, reset: vi.fn() });

const SAMPLE_ROWS: service.FlatPriceConversionResponse[] = [
  {
    id: 1,
    modellingCode: 'P',
    maturity: 'O',
    species: 'FD',
    grade: 'D',
    sortCode: 'C',
    flatPriceConversion: 100,
    effectiveDate: '1990-01-01',
    expiryDate: null,
    revisionCount: 0,
    entryUserid: 'system',
    entryTimestamp: '2024-01-01',
    updateUserid: 'system',
    updateTimestamp: '2024-01-01',
  },
];

const renderPage = () => {
  const qc = new QueryClient();
  render(
    <QueryClientProvider client={qc}>
      <NotificationContext.Provider
        value={{ notifications: [], addNotification: vi.fn(), removeNotification: vi.fn() }}
      >
        <FlatPriceConversionPage />
      </NotificationContext.Provider>
    </QueryClientProvider>,
  );
};

describe('FlatPriceConversionPage', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.mocked(service.useSearchFlatPriceConversionsQuery).mockReturnValue({
      data: SAMPLE_ROWS,
      isLoading: false,
      isError: false,
      error: null,
    } as any);
    vi.mocked(service.useCreateFlatPriceConversionMutation).mockReturnValue(mockMutation() as any);
    vi.mocked(service.useUpdateFlatPriceConversionMutation).mockReturnValue(mockMutation() as any);
    vi.mocked(service.useDeleteFlatPriceConversionMutation).mockReturnValue(mockMutation() as any);
    vi.mocked(service.useExportFlatPriceConversionsMutation).mockReturnValue(mockMutation() as any);
    vi.mocked(lookup.useMaturityCodesQuery).mockReturnValue({ data: [], isLoading: false } as any);
    vi.mocked(lookup.useSortCodesLookupQuery).mockReturnValue({ data: [], isLoading: false } as any);
    vi.mocked(lookup.useSpeciesLookupQuery).mockReturnValue({ data: [], isLoading: false } as any);
    vi.mocked(lookup.useGradeLookupQuery).mockReturnValue({ data: [], isLoading: false } as any);
    vi.mocked(lookup.useGradesBySpeciesLookupQuery).mockReturnValue({ data: [], isLoading: false } as any);
  });

  afterEach(() => {
    window.sessionStorage.clear();
  });

  it('renders the page title', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /flat price conversion/i })).toBeInTheDocument();
  });

  it('renders Add new row button', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /add new row/i })).toBeInTheDocument();
  });

  it('renders filter dropdowns', () => {
    renderPage();
    expect(screen.getByRole('combobox', { name: /maturity/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /species/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /grade/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /^sort$/i })).toBeInTheDocument();
  });

  it('renders all table column headers', () => {
    renderPage();
    const table = screen.getByRole('table');
    expect(within(table).getByText('Maturity')).toBeInTheDocument();
    expect(within(table).getByText('Species')).toBeInTheDocument();
    expect(within(table).getByText('Sort code')).toBeInTheDocument();
    expect(within(table).getByText('Grade')).toBeInTheDocument();
    expect(within(table).getByText('Flat price conversion')).toBeInTheDocument();
    expect(within(table).getByText('Effective date')).toBeInTheDocument();
    expect(within(table).getByText('Expiry date')).toBeInTheDocument();
    expect(within(table).getByText('Actions')).toBeInTheDocument();
  });

  it('renders data rows from the query', () => {
    renderPage();
    expect(screen.getByText('FD')).toBeInTheDocument();
    expect(screen.getByText('O')).toBeInTheDocument();
    expect(screen.getByText('C')).toBeInTheDocument();
  });

  it('shows no data rows when isLoading is true', () => {
    vi.mocked(service.useSearchFlatPriceConversionsQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      error: null,
    } as any);
    renderPage();
    expect(screen.queryByText('FD')).not.toBeInTheDocument();
  });

  it('shows empty state immediately when no results exist', () => {
    vi.mocked(service.useSearchFlatPriceConversionsQuery).mockReturnValue({
      data: [],
      isLoading: false,
      isError: false,
      error: null,
    } as any);
    renderPage();
    expect(screen.getByText(/no results found/i)).toBeInTheDocument();
  });

  it('shows error notification when query fails', () => {
    vi.mocked(service.useSearchFlatPriceConversionsQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: { message: 'Network error' },
    } as any);
    renderPage();
    expect(screen.getByText(/failed to load flat price conversions/i)).toBeInTheDocument();
  });

  it('opens add modal when Add new row is clicked', () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /add new row/i }));
    expect(screen.getByRole('heading', { name: /add new row/i })).toBeInTheDocument();
  });

  it('opens edit modal when Edit is clicked', () => {
    renderPage();
    fireEvent.click(screen.getAllByRole('button', { name: /edit/i })[0]);
    expect(screen.getByRole('heading', { name: /edit row/i })).toBeInTheDocument();
  });

  it('opens delete modal when Delete is clicked', () => {
    renderPage();
    fireEvent.click(screen.getAllByRole('button', { name: /delete/i })[0]);
    expect(screen.getByRole('heading', { name: /delete row/i })).toBeInTheDocument();
  });

  it('renders Export table menu button', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /export table/i })).toBeInTheDocument();
  });

  it('calls export mutation with csv when Export as CSV is clicked', async () => {
    const mutateMock = vi.fn();
    vi.mocked(service.useExportFlatPriceConversionsMutation).mockReturnValue({
      mutate: mutateMock,
      isPending: false,
      isError: false,
      error: null,
      reset: vi.fn(),
    } as any);
    renderPage();
    // Open the MenuButton first, then click the menu item
    fireEvent.click(screen.getByRole('button', { name: /export table/i }));
    fireEvent.click(await screen.findByRole('menuitem', { name: /export as csv/i }));
    expect(mutateMock).toHaveBeenCalledWith(expect.objectContaining({ format: 'csv' }), expect.any(Object));
  });

  it('calls export mutation with pdf when Export as PDF is clicked', async () => {
    const mutateMock = vi.fn();
    vi.mocked(service.useExportFlatPriceConversionsMutation).mockReturnValue({
      mutate: mutateMock,
      isPending: false,
      isError: false,
      error: null,
      reset: vi.fn(),
    } as any);
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /export table/i }));
    fireEvent.click(await screen.findByRole('menuitem', { name: /export as pdf/i }));
    expect(mutateMock).toHaveBeenCalledWith(expect.objectContaining({ format: 'pdf' }), expect.any(Object));
  });

  it('restores filters, search params, and pagination from sessionStorage on mount', async () => {
    window.sessionStorage.setItem('csp.table.flatPriceConversion.v1.filterSpecies', JSON.stringify('FD'));
    window.sessionStorage.setItem('csp.table.flatPriceConversion.v1.page', '2');
    window.sessionStorage.setItem(
      'csp.table.flatPriceConversion.v1.searchParams',
      JSON.stringify({ modellingCode: 'P', species: 'FD' }),
    );

    vi.mocked(lookup.useSpeciesLookupQuery).mockReturnValue({
      data: [{ code: 'FD', description: 'Douglas Fir' }],
      isLoading: false,
    } as any);

    const manyRows: service.FlatPriceConversionResponse[] = Array.from({ length: 25 }, (_, i) => ({
      ...SAMPLE_ROWS[0],
      id: i + 1,
    }));
    vi.mocked(service.useSearchFlatPriceConversionsQuery).mockReturnValue({
      data: manyRows,
      isLoading: false,
      isError: false,
      error: null,
    } as any);

    renderPage();

    const pageSelect = await screen.findByLabelText(/page of \d+ pages/i);
    expect((pageSelect as HTMLSelectElement).value).toBe('2');

    expect(await screen.findByDisplayValue(/Douglas Fir/i)).toBeInTheDocument();
  });
});
