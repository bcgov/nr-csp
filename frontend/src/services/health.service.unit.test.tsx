import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import { getHealth, useHealthQuery } from '@/services/health.service';

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

const HEALTH = { status: 'UP', timestamp: '2026-07-09T00:00:00Z' };

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(apiClient.get).mockResolvedValue({ data: HEALTH });
});

describe('getHealth', () => {
  it('fetches /health and returns the payload', async () => {
    await expect(getHealth()).resolves.toEqual(HEALTH);
    expect(apiClient.get).toHaveBeenCalledWith('/health');
  });
});

describe('useHealthQuery', () => {
  it('is disabled by default and does not fetch on mount', async () => {
    const { result } = renderHook(() => useHealthQuery(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('fetches when explicitly refetched', async () => {
    const { result } = renderHook(() => useHealthQuery(), { wrapper: createWrapper() });
    // Read `data` up front: useQuery tracks accessed properties, and only
    // tracked properties trigger re-renders when they change.
    expect(result.current.data).toBeUndefined();

    await act(async () => {
      await result.current.refetch();
    });

    expect(apiClient.get).toHaveBeenCalledWith('/health');
    await waitFor(() => expect(result.current.data).toEqual(HEALTH));
  });
});
