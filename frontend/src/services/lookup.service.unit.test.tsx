import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import {
  useGradeLookupQuery,
  useGradesBySpeciesLookupQuery,
  useInvoiceStatusesQuery,
  useInvoiceTypesQuery,
  useMaturityCodesNoCantsQuery,
  useMaturityCodesQuery,
  useMaturityCodesWithCantsQuery,
  useSortCodesLookupQuery,
  useSpeciesGradeCombosQuery,
  useSpeciesLookupQuery,
  useSubmissionStatusesQuery,
} from '@/services/lookup.service';

vi.mock('@/config/api/request', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

const LOOKUP_ITEMS = [
  { code: 'A', description: 'Alpha' },
  { code: 'B', description: 'Beta' },
];

const MATURITY_ITEMS = [
  { code: 'O', description: 'Old Growth' },
  { code: 'C', description: 'Cants / Export' },
  { code: 'E', description: 'Export' },
];

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(apiClient.get).mockResolvedValue({ data: LOOKUP_ITEMS });
});

// The plain lookups only differ by endpoint, so exercise them as a table.
describe.each([
  { hook: useInvoiceStatusesQuery, path: '/lookup/status' },
  { hook: useInvoiceTypesQuery, path: '/lookup/type' },
  { hook: useMaturityCodesQuery, path: '/lookup/maturity' },
  { hook: useSortCodesLookupQuery, path: '/lookup/sort-code' },
  { hook: useSpeciesLookupQuery, path: '/lookup/species' },
  { hook: useGradeLookupQuery, path: '/lookup/grade' },
  { hook: useSubmissionStatusesQuery, path: '/lookup/submission-status' },
])('lookup query for $path', ({ hook, path }) => {
  it('fetches the lookup list and exposes the response data', async () => {
    const { result } = renderHook(() => hook(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiClient.get).toHaveBeenCalledWith(path);
    expect(result.current.data).toEqual(LOOKUP_ITEMS);
  });
});

describe('useMaturityCodesNoCantsQuery', () => {
  it('filters out both the Cants / Export and Export entries', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: MATURITY_ITEMS });

    const { result } = renderHook(() => useMaturityCodesNoCantsQuery(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([{ code: 'O', description: 'Old Growth' }]);
  });
});

describe('useMaturityCodesWithCantsQuery', () => {
  it('drops Export and renames Cants / Export to Cants', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: MATURITY_ITEMS });

    const { result } = renderHook(() => useMaturityCodesWithCantsQuery(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([
      { code: 'O', description: 'Old Growth' },
      { code: 'C', description: 'Cants' },
    ]);
  });
});

describe('useGradesBySpeciesLookupQuery', () => {
  it('does not fetch while no species is selected', async () => {
    const { result } = renderHook(() => useGradesBySpeciesLookupQuery(null), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('fetches grades for the selected species', async () => {
    const { result } = renderHook(() => useGradesBySpeciesLookupQuery('FI'), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiClient.get).toHaveBeenCalledWith('/lookup/grade-by-species/FI');
    expect(result.current.data).toEqual(LOOKUP_ITEMS);
  });
});

describe('useSpeciesGradeCombosQuery', () => {
  it('fetches the species/grade combination list', async () => {
    const combos = [{ species: 'FI', grade: 'A' }];
    vi.mocked(apiClient.get).mockResolvedValue({ data: combos });

    const { result } = renderHook(() => useSpeciesGradeCombosQuery(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiClient.get).toHaveBeenCalledWith('/lookup/species-grade-combinations');
    expect(result.current.data).toEqual(combos);
  });
});
