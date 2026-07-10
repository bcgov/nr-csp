import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import {
  clearFlatPriceConversions,
  copyFlatPriceConversions,
  createFlatPriceConversion,
  deleteFlatPriceConversion,
  exportFlatPriceConversions,
  extractApiErrorMessage,
  searchFlatPriceConversions,
  updateFlatPriceConversion,
  useClearFlatPriceConversionsMutation,
  useCopyFlatPriceConversionsMutation,
  useCreateFlatPriceConversionMutation,
  useDeleteFlatPriceConversionMutation,
  useExportFlatPriceConversionsMutation,
  useSearchFlatPriceConversionsQuery,
  useUpdateFlatPriceConversionMutation,
  type CreateFlatPriceConversionRequest,
  type FlatPriceConversionDetails,
} from '@/services/flatPriceConversion.service';

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

const DETAILS: FlatPriceConversionDetails = {
  maturity: 'O',
  species: 'FI',
  grade: 'A',
  sortCode: '01',
  flatPriceConversion: 1.5,
  effectiveDate: '2026-01-01',
  expiryDate: null,
};

const ROW = { id: 1, modellingCode: 'P', ...DETAILS };

beforeEach(() => {
  vi.clearAllMocks();
});

describe('flat-price-conversion endpoint fetchers', () => {
  it('searchFlatPriceConversions strips null and empty params', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [ROW] });

    const data = await searchFlatPriceConversions({ modellingCode: 'P', maturity: null, species: '', grade: 'A' });

    expect(apiClient.get).toHaveBeenCalledWith('/flat-price-conversions', {
      params: { modellingCode: 'P', grade: 'A' },
    });
    expect(data).toEqual([ROW]);
  });

  it('createFlatPriceConversion POSTs the request body', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: ROW });
    const request: CreateFlatPriceConversionRequest = { modellingCode: 'P', details: DETAILS };

    await expect(createFlatPriceConversion(request)).resolves.toEqual(ROW);
    expect(apiClient.post).toHaveBeenCalledWith('/flat-price-conversions', request);
  });

  it('updateFlatPriceConversion PUTs to /flat-price-conversions/{id}', async () => {
    vi.mocked(apiClient.put).mockResolvedValue({ data: ROW });
    const request = { revisionCount: 3, details: DETAILS };

    await expect(updateFlatPriceConversion(1, request)).resolves.toEqual(ROW);
    expect(apiClient.put).toHaveBeenCalledWith('/flat-price-conversions/1', request);
  });

  it('deleteFlatPriceConversion DELETEs by id and resolves to undefined', async () => {
    vi.mocked(apiClient.delete).mockResolvedValue({});

    await expect(deleteFlatPriceConversion(1)).resolves.toBeUndefined();
    expect(apiClient.delete).toHaveBeenCalledWith('/flat-price-conversions/1');
  });

  it('copyFlatPriceConversions POSTs the source/target pair', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({});
    const request = { sourceModellingCode: 'P', targetModellingCode: 'M1' };

    await expect(copyFlatPriceConversions(request)).resolves.toBeUndefined();
    expect(apiClient.post).toHaveBeenCalledWith('/flat-price-conversions/copy', request);
  });

  it('clearFlatPriceConversions DELETEs the modelling code', async () => {
    vi.mocked(apiClient.delete).mockResolvedValue({});

    await expect(clearFlatPriceConversions('M1')).resolves.toBeUndefined();
    expect(apiClient.delete).toHaveBeenCalledWith('/flat-price-conversions/clear/M1');
  });

  it('exportFlatPriceConversions filters params and falls back to a default filename', async () => {
    const blob = new Blob(['csv']);
    vi.mocked(apiClient.get).mockResolvedValue({ data: blob, headers: {} });

    const result = await exportFlatPriceConversions('csv', { modellingCode: 'P', maturity: null });

    expect(apiClient.get).toHaveBeenCalledWith('/flat-price-conversions/export/csv', {
      params: { modellingCode: 'P' },
      responseType: 'blob',
    });
    expect(result).toEqual({ blob, filename: 'FlatPriceConversions.csv' });
  });

  it('exportFlatPriceConversions uses the content-disposition filename when present', async () => {
    const blob = new Blob(['pdf']);
    vi.mocked(apiClient.get).mockResolvedValue({
      data: blob,
      headers: { 'content-disposition': 'attachment; filename="fpc.pdf"' },
    });

    await expect(exportFlatPriceConversions('pdf', { modellingCode: 'P' })).resolves.toEqual({
      blob,
      filename: 'fpc.pdf',
    });
  });
});

describe('useSearchFlatPriceConversionsQuery', () => {
  it('does not fetch without a modelling code', async () => {
    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useSearchFlatPriceConversionsQuery({ modellingCode: '' }), { wrapper });

    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('fetches rows for the selected modelling code', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [ROW] });
    const { wrapper } = createWrapper();

    const { result } = renderHook(() => useSearchFlatPriceConversionsQuery({ modellingCode: 'P' }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([ROW]);
  });
});

describe('flat-price-conversion mutation hooks', () => {
  it.each([
    {
      label: 'create',
      useHook: useCreateFlatPriceConversionMutation,
      arrange: () => vi.mocked(apiClient.post).mockResolvedValue({ data: ROW }),
      variables: { modellingCode: 'P', details: DETAILS },
    },
    {
      label: 'update',
      useHook: useUpdateFlatPriceConversionMutation,
      arrange: () => vi.mocked(apiClient.put).mockResolvedValue({ data: ROW }),
      variables: { id: 1, req: { revisionCount: 3, details: DETAILS } },
    },
    {
      label: 'delete',
      useHook: useDeleteFlatPriceConversionMutation,
      arrange: () => vi.mocked(apiClient.delete).mockResolvedValue({}),
      variables: 1,
    },
    {
      label: 'copy',
      useHook: useCopyFlatPriceConversionsMutation,
      arrange: () => vi.mocked(apiClient.post).mockResolvedValue({}),
      variables: { sourceModellingCode: 'P', targetModellingCode: 'M1' },
    },
    {
      label: 'clear',
      useHook: useClearFlatPriceConversionsMutation,
      arrange: () => vi.mocked(apiClient.delete).mockResolvedValue({}),
      variables: 'M1',
    },
  ])('use$label mutation invalidates the search results on success', async ({ useHook, arrange, variables }) => {
    arrange();
    const { queryClient, wrapper } = createWrapper();
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useHook(), { wrapper });
    act(() => {
      result.current.mutate(variables as never);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['flat-price-conversions'] });
  });

  it('useExportFlatPriceConversionsMutation resolves with the export result', async () => {
    const blob = new Blob(['csv']);
    vi.mocked(apiClient.get).mockResolvedValue({ data: blob, headers: {} });
    const { wrapper } = createWrapper();

    const { result } = renderHook(() => useExportFlatPriceConversionsMutation(), { wrapper });
    act(() => {
      result.current.mutate({ format: 'csv', params: { modellingCode: 'P' } });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ blob, filename: 'FlatPriceConversions.csv' });
  });
});

describe('extractApiErrorMessage', () => {
  it('returns the server message when present, else a generic fallback', () => {
    expect(extractApiErrorMessage({ response: { data: { message: 'Conflict' } } })).toBe('Conflict');
    expect(extractApiErrorMessage({})).toBe('An unexpected error occurred.');
  });
});
