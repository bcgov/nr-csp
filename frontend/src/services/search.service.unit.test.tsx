import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import { getClientsByName, getClientsByNumber, searchInvoices, useSearchQuery } from '@/services/search.service';

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

const PAGE = { content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 };

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(apiClient.get).mockResolvedValue({ data: PAGE });
});

describe('searchInvoices', () => {
  it('strips undefined and empty-string params before calling the API', async () => {
    const data = await searchInvoices({ invNumber: 'INV-1', invStatus: '', keyword: undefined, page: 2 });

    expect(apiClient.get).toHaveBeenCalledWith('/search', { params: { invNumber: 'INV-1', page: 2 } });
    expect(data).toEqual(PAGE);
  });
});

describe('client lookups', () => {
  const clients = [{ clientNumber: '00012345', clientName: 'ACME FOREST LTD' }];

  it('getClientsByName queries /clients by name', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: clients });

    await expect(getClientsByName('ACME')).resolves.toEqual(clients);
    expect(apiClient.get).toHaveBeenCalledWith('/clients', { params: { name: 'ACME' } });
  });

  it('getClientsByNumber queries /clients by number', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: clients });

    await expect(getClientsByNumber('00012345')).resolves.toEqual(clients);
    expect(apiClient.get).toHaveBeenCalledWith('/clients', { params: { number: '00012345' } });
  });
});

describe('useSearchQuery', () => {
  it('does not fetch while disabled', async () => {
    const { result } = renderHook(() => useSearchQuery({ page: 0 }, false), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('fetches and returns the search page when enabled', async () => {
    const { result } = renderHook(() => useSearchQuery({ page: 0 }, true), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(PAGE);
  });
});
