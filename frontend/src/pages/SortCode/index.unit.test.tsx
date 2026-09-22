import { render, screen, fireEvent, within, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import * as service from '@/services/sortcode.service';
import { NotificationContext } from '@/context/notification/NotificationContext';
import { SortCodePage } from './index';

vi.mock('@/context/auth/usePermission', () => ({ usePermission: () => true }));

vi.mock('@/services/sortcode.service', () => ({
  useListSortCodesQuery: vi.fn(),
  useCreateSortCodeMutation: vi.fn(),
  useUpdateSortCodeMutation: vi.fn(),
  useDeleteSortCodeMutation: vi.fn(),
  useExportSortCodesMutation: vi.fn(),
  extractApiErrorMessage: vi.fn(),
}));

const mockMutation = () => ({ mutate: vi.fn(), isPending: false, isError: false, error: null, reset: vi.fn() });

const SAMPLE_ROWS = [
  {
    sortCode: 'A',
    description: 'Lumber - Cedar',
    effectiveDate: '1990-01-01',
    expiryDate: '9999-12-31',
    updateTimestamp: '2024-01-01',
  },
  {
    sortCode: 'B',
    description: 'Boomsticks',
    effectiveDate: '1990-01-01',
    expiryDate: '9999-12-31',
    updateTimestamp: '2024-01-01',
  },
];

const renderPage = () => {
  const qc = new QueryClient();
  return render(
    <QueryClientProvider client={qc}>
      <NotificationContext.Provider
        value={{ notifications: [], addNotification: vi.fn(), removeNotification: vi.fn() }}
      >
        <SortCodePage />
      </NotificationContext.Provider>
    </QueryClientProvider>,
  );
};

describe('SortCodePage', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.mocked(service.useListSortCodesQuery).mockReturnValue({
      data: { content: SAMPLE_ROWS, totalElements: SAMPLE_ROWS.length },
      isLoading: false,
      isError: false,
      error: null,
    } as any);
    vi.mocked(service.useCreateSortCodeMutation).mockReturnValue(mockMutation() as any);
    vi.mocked(service.useUpdateSortCodeMutation).mockReturnValue(mockMutation() as any);
    vi.mocked(service.useDeleteSortCodeMutation).mockReturnValue(mockMutation() as any);
    vi.mocked(service.useExportSortCodesMutation).mockReturnValue(mockMutation() as any);
  });

  afterEach(() => {
    window.sessionStorage.clear();
  });

  it('renders the page title', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /^sort code$/i })).toBeInTheDocument();
  });

  it('renders all table column headers', () => {
    renderPage();
    expect(within(screen.getByRole('table')).getByText('Sort code')).toBeInTheDocument();
    expect(screen.getByText('Description')).toBeInTheDocument();
    expect(screen.getByText('Effective date')).toBeInTheDocument();
    expect(screen.getByText('Expiry date')).toBeInTheDocument();
    expect(screen.getByText('Actions')).toBeInTheDocument();
  });

  it('renders toolbar actions', () => {
    renderPage();
    const exportButton = screen.getByRole('button', { name: /export table/i });
    expect(exportButton).toBeInTheDocument();
    fireEvent.click(exportButton);
    expect(screen.getByRole('menuitem', { name: /csv/i })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /pdf/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add new row/i })).toBeInTheDocument();
  });

  it('renders rows from the query', () => {
    renderPage();
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText('Lumber - Cedar')).toBeInTheDocument();
    expect(screen.getByText('B')).toBeInTheDocument();
  });

  it('shows a DataTableSkeleton when loading', () => {
    vi.mocked(service.useListSortCodesQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      error: null,
    } as any);
    renderPage();
    // DataTableSkeleton renders a table with aria-label or role=table skeleton
    expect(screen.queryByText('A')).not.toBeInTheDocument();
  });

  it('skeletons the rows but keeps the pagination counts while a new page loads (CSP-630)', () => {
    // keepPreviousData: the previous page's rows are still in `data`, but during
    // the fetch (isPlaceholderData) the body should skeleton while the bar keeps
    // rendering the retained totals rather than blanking to zero.
    vi.mocked(service.useListSortCodesQuery).mockReturnValue({
      data: { content: SAMPLE_ROWS, totalElements: 42 },
      isLoading: false,
      isPlaceholderData: true,
      isError: false,
      error: null,
    } as any);

    const { container } = renderPage();

    // Stale rows are hidden behind the skeleton.
    expect(screen.queryByText('Lumber - Cedar')).not.toBeInTheDocument();
    // Pagination bar stays with the retained totals.
    const pagination = container.querySelector('.cds--pagination');
    expect(pagination).not.toBeNull();
    expect(pagination).toHaveTextContent('42');
  });

  it('shows an error notification when the query fails', () => {
    vi.mocked(service.useListSortCodesQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: { message: 'Network error' },
    } as any);
    renderPage();
    expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
  });

  it('shows empty state when data is empty', () => {
    vi.mocked(service.useListSortCodesQuery).mockReturnValue({
      data: { content: [], totalElements: 0 },
      isLoading: false,
      isError: false,
      error: null,
    } as any);
    renderPage();
    expect(screen.getByText(/no sort codes found/i)).toBeInTheDocument();
  });

  it('opens the add modal when Add new row is clicked', () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /add new row/i }));
    expect(screen.getByRole('heading', { name: /add new sort code/i })).toBeInTheDocument();
  });

  it('opens the edit modal when Edit is clicked', () => {
    renderPage();
    fireEvent.click(screen.getAllByRole('button', { name: /edit/i })[0]);
    expect(screen.getByRole('heading', { name: /edit sort code/i })).toBeInTheDocument();
  });

  it('opens the delete modal when Delete is clicked', () => {
    renderPage();
    fireEvent.click(screen.getAllByRole('button', { name: /delete/i })[0]);
    expect(screen.getByRole('heading', { name: /delete sort code/i })).toBeInTheDocument();
  });

  it('keeps the pagination control while the query is in flight', () => {
    // `data` is undefined during the initial load, so the old `totalElements > 0` gate
    // dropped the bar under the skeleton and shifted the layout once the rows arrived.
    vi.mocked(service.useListSortCodesQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      error: null,
    } as any);

    const { container } = renderPage();
    expect(container.querySelector('.cds--pagination')).not.toBeNull();
  });

  it('keeps the pagination control when the table is empty', () => {
    vi.mocked(service.useListSortCodesQuery).mockReturnValue({
      data: { content: [], totalElements: 0 },
      isLoading: false,
      isError: false,
      error: null,
    } as any);

    renderPage();

    expect(screen.getByText(/no sort codes found/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/results per page:/i)).toBeInTheDocument();
  });

  it('defaults to 100 results per page, matching the other tables', () => {
    renderPage();
    const perPageSelect = screen.getByLabelText(/results per page:/i) as HTMLSelectElement;
    expect(perPageSelect.value).toBe('100');
  });

  it('restores page and page size from sessionStorage on mount', async () => {
    window.sessionStorage.setItem('csp.table.sortCode.v1.page', '3');
    window.sessionStorage.setItem('csp.table.sortCode.v1.pageSize', '40');
    vi.mocked(service.useListSortCodesQuery).mockReturnValue({
      data: { content: SAMPLE_ROWS, totalElements: 200 },
      isLoading: false,
      isError: false,
      error: null,
    } as any);

    renderPage();

    const pageSelect = await screen.findByLabelText(/page of \d+ pages/i);
    expect((pageSelect as HTMLSelectElement).value).toBe('3');
  });

  it('clamps an out-of-range restored page back to the last valid page even when the current page has no rows', async () => {
    // The persisted page (5) is out of range for a 30-item, 20-per-page result set (last page = 2).
    // pageSize is persisted explicitly: at the 100-row default a 30-item set is a single page, which
    // would clamp to 1 and make this a weaker assertion than the last-valid-page snap it is testing.
    // 20 is one of ResultsTable's default `pageSizes` options, so the Carbon Pagination control
    // itself isn't confused by an unlisted page size. The server returns an empty `content` for that
    // out-of-range page while `totalElements` still reflects the real total — this is the scenario
    // where the pagination control must still render so the ResultsTable clamp effect can fire and
    // snap the page back.
    window.sessionStorage.setItem('csp.table.sortCode.v1.page', '5');
    window.sessionStorage.setItem('csp.table.sortCode.v1.pageSize', '20');
    vi.mocked(service.useListSortCodesQuery).mockReturnValue({
      data: { content: [], totalElements: 30 },
      isLoading: false,
      isError: false,
      error: null,
    } as any);

    renderPage();

    // With the old `totalElements > 0` gate, onPaginationChange would be undefined and no
    // pagination control (and thus no clamp) would ever render — this findBy would time out.
    await screen.findByLabelText(/page of \d+ pages/i);

    // The clamp effect fires asynchronously after the initial (out-of-range) render, so wait
    // for the page select to reflect the corrected page rather than asserting immediately.
    await waitFor(() => {
      const pageSelect = screen.getByLabelText(/page of \d+ pages/i) as HTMLSelectElement;
      expect(pageSelect.value).toBe('2');
    });

    // The query hook should have been re-invoked with the clamped page (page 2 -> index 1).
    expect(service.useListSortCodesQuery).toHaveBeenCalledWith(1, 20, undefined);
  });
});
