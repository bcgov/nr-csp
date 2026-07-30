import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import { useFobCodesQuery } from '@/services/fob.service';

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

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useFobCodesQuery', () => {
  it('fetches the FOB lookup list', async () => {
    const items = [{ code: 'W', description: 'Water' }];
    vi.mocked(apiClient.get).mockResolvedValue({ data: items });

    const { result } = renderHook(() => useFobCodesQuery(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiClient.get).toHaveBeenCalledWith('/lookup/fob');
    expect(result.current.data).toEqual(items);
  });

  it('surfaces a failed lookup as a query error', async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error('boom'));

    const { result } = renderHook(() => useFobCodesQuery(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error?.message).toBe('boom');
  });
});
