import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import { THREE_HOURS } from '@/config/react-query/TimeUnits';
import { useSortCodesLookupQuery } from '@/services/lookup.service';
import {
  createSortCode,
  deleteSortCode,
  exportSortCodes,
  extractApiErrorMessage,
  listSortCodes,
  updateSortCode,
  useCreateSortCodeMutation,
  useDeleteSortCodeMutation,
  useExportSortCodesMutation,
  useListSortCodesQuery,
  useUpdateSortCodeMutation,
} from '@/services/sortcode.service';

vi.mock('@/config/api/request', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
};

const SORT_CODE = {
  sortCode: 'A',
  description: 'Alpha',
  effectiveDate: '2026-01-01',
  expiryDate: '9999-12-31',
  updateTimestamp: '2026-01-01T00:00:00Z',
};

const PAGE = { content: [SORT_CODE], totalElements: 1, totalPages: 1, size: 10, number: 0 };

beforeEach(() => {
  vi.clearAllMocks();
});

describe('sort-code endpoint fetchers', () => {
  it('listSortCodes GETs /sort-codes with paging params', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: PAGE });

    await expect(listSortCodes(0, 10, 'sortCode,asc')).resolves.toEqual(PAGE);
    expect(apiClient.get).toHaveBeenCalledWith('/sort-codes', {
      params: { page: 0, size: 10, sort: 'sortCode,asc' },
    });
  });

  it('createSortCode POSTs the request body', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: SORT_CODE });
    const request = { sortCode: 'A', description: 'Alpha', effectiveDate: '2026-01-01', expiryDate: '9999-12-31' };

    await expect(createSortCode(request)).resolves.toEqual(SORT_CODE);
    expect(apiClient.post).toHaveBeenCalledWith('/sort-codes', request);
  });

  it('updateSortCode PUTs to /sort-codes/{code}', async () => {
    vi.mocked(apiClient.put).mockResolvedValue({ data: SORT_CODE });
    const request = { description: 'Alpha', effectiveDate: '2026-01-01', expiryDate: '9999-12-31' };

    await expect(updateSortCode('A', request)).resolves.toEqual(SORT_CODE);
    expect(apiClient.put).toHaveBeenCalledWith('/sort-codes/A', request);
  });

  it('deleteSortCode DELETEs /sort-codes/{code} and resolves to undefined', async () => {
    vi.mocked(apiClient.delete).mockResolvedValue({});

    await expect(deleteSortCode('A')).resolves.toBeUndefined();
    expect(apiClient.delete).toHaveBeenCalledWith('/sort-codes/A');
  });

  it('exportSortCodes downloads a blob and falls back to a default filename', async () => {
    const blob = new Blob(['csv']);
    vi.mocked(apiClient.get).mockResolvedValue({ data: blob, headers: {} });

    await expect(exportSortCodes('csv')).resolves.toEqual({ blob, filename: 'Sortcodes.csv' });
    expect(apiClient.get).toHaveBeenCalledWith('/sort-codes/export/csv', { responseType: 'blob' });
  });

  it('exportSortCodes uses the content-disposition filename when present', async () => {
    const blob = new Blob(['pdf']);
    vi.mocked(apiClient.get).mockResolvedValue({
      data: blob,
      headers: { 'content-disposition': 'attachment; filename="codes.pdf"' },
    });

    await expect(exportSortCodes('pdf')).resolves.toEqual({ blob, filename: 'codes.pdf' });
  });
});

describe('sort-code hooks', () => {
  it('useListSortCodesQuery fetches the page', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: PAGE });
    const { wrapper } = createWrapper();

    const { result } = renderHook(() => useListSortCodesQuery(0, 10), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(PAGE);
  });

  it('keeps the previous page on screen while the next page is fetching (CSP-630)', async () => {
    const PAGE_1 = { content: [SORT_CODE], totalElements: 42, totalPages: 3, size: 20, number: 0 };
    const PAGE_2 = { ...PAGE_1, content: [{ ...SORT_CODE, sortCode: 'B' }], number: 1 };

    // Page 1 resolves immediately; page 2's request is held pending so we can
    // observe the in-flight window a first visit to that page opens up.
    let releasePage2: (value: { data: typeof PAGE_2 }) => void = () => {};
    vi.mocked(apiClient.get)
      .mockResolvedValueOnce({ data: PAGE_1 })
      .mockReturnValueOnce(
        new Promise((resolve) => {
          releasePage2 = resolve;
        }),
      );

    const { wrapper } = createWrapper();
    const { result, rerender } = renderHook(({ page }) => useListSortCodesQuery(page, 20), {
      wrapper,
      initialProps: { page: 0 },
    });

    await waitFor(() => expect(result.current.data).toEqual(PAGE_1));

    // Navigate to page 2 (a page number never visited before): its request is
    // still in flight.
    rerender({ page: 1 });

    // Without keepPreviousData the hook would report `data: undefined` here, and
    // the consumer would collapse totalElements to 0 — the "0 – 0 of 0" flicker.
    // Instead the previous page's rows and totals must stay put until page 2 lands.
    await waitFor(() => expect(result.current.isPlaceholderData).toBe(true));
    expect(result.current.data).toEqual(PAGE_1);
    expect(result.current.data?.totalElements).toBe(42);

    // Once page 2 lands the hook swaps to the fresh data.
    act(() => releasePage2({ data: PAGE_2 }));
    await waitFor(() => expect(result.current.data).toEqual(PAGE_2));
    expect(result.current.isPlaceholderData).toBe(false);
  });

  it.each([
    {
      label: 'create',
      useHook: useCreateSortCodeMutation,
      arrange: () => vi.mocked(apiClient.post).mockResolvedValue({ data: SORT_CODE }),
      variables: { sortCode: 'A', description: 'Alpha', effectiveDate: '2026-01-01', expiryDate: '9999-12-31' },
    },
    {
      label: 'update',
      useHook: useUpdateSortCodeMutation,
      arrange: () => vi.mocked(apiClient.put).mockResolvedValue({ data: SORT_CODE }),
      variables: { code: 'A', req: { description: 'Alpha', effectiveDate: '2026-01-01', expiryDate: '9999-12-31' } },
    },
    {
      label: 'delete',
      useHook: useDeleteSortCodeMutation,
      arrange: () => vi.mocked(apiClient.delete).mockResolvedValue({}),
      variables: 'A',
    },
  ])(
    'use$label mutation invalidates the sort-code list and evicts the shared lookup on success',
    async ({ useHook, arrange, variables }) => {
      arrange();
      const { queryClient, wrapper } = createWrapper();
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
      const removeSpy = vi.spyOn(queryClient, 'removeQueries');

      const { result } = renderHook(() => useHook(), { wrapper });
      act(() => {
        result.current.mutate(variables as never);
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['sort-codes'] });
      expect(removeSpy).toHaveBeenCalledWith({ queryKey: ['lookup', 'sort-code'] });
    },
  );

  it('makes the shared sort-code lookup refetch on its next mount after a mutation (CSP-591)', async () => {
    // Mirror the app's real query config: with the default staleTime:0 /
    // refetchOnMount:true the lookup would refetch on every mount anyway and
    // mask the bug. The fix must hold under these settings.
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: THREE_HOURS,
          gcTime: THREE_HOURS,
          refetchOnMount: false,
          refetchOnWindowFocus: false,
        },
        mutations: { retry: false },
      },
    });
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    // 1. An invoice-style consumer loads the lookup once, then leaves the page
    //    (the query goes inactive but stays cached).
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: [{ code: 'A', description: 'Alpha' }] });
    const first = renderHook(() => useSortCodesLookupQuery(), { wrapper });
    await waitFor(() => expect(first.result.current.data).toHaveLength(1));
    first.unmount();

    // 2. A code is added on the maintenance page.
    vi.mocked(apiClient.post).mockResolvedValue({ data: { ...SORT_CODE, sortCode: 'B', description: 'Bravo' } });
    const mutation = renderHook(() => useCreateSortCodeMutation(), { wrapper });
    act(() => {
      mutation.result.current.mutate({
        sortCode: 'B',
        description: 'Bravo',
        effectiveDate: '2026-01-01',
        expiryDate: '9999-12-31',
      });
    });
    await waitFor(() => expect(mutation.result.current.isSuccess).toBe(true));

    // 3. Re-opening the consumer must fetch fresh codes, not serve the stale cache.
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: [
        { code: 'A', description: 'Alpha' },
        { code: 'B', description: 'Bravo' },
      ],
    });
    const second = renderHook(() => useSortCodesLookupQuery(), { wrapper });
    await waitFor(() => expect(second.result.current.data).toHaveLength(2));
  });

  it('useExportSortCodesMutation resolves with the export result', async () => {
    const blob = new Blob(['pdf']);
    vi.mocked(apiClient.get).mockResolvedValue({ data: blob, headers: {} });
    const { wrapper } = createWrapper();

    const { result } = renderHook(() => useExportSortCodesMutation(), { wrapper });
    act(() => {
      result.current.mutate('pdf');
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ blob, filename: 'Sortcodes.pdf' });
  });
});

describe('extractApiErrorMessage', () => {
  it('returns the server message when present, else a generic fallback', () => {
    expect(extractApiErrorMessage({ response: { data: { message: 'Duplicate code' } } })).toBe('Duplicate code');
    expect(extractApiErrorMessage(new Error('x'))).toBe('An unexpected error occurred.');
  });
});
