import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import { searchInbox, useInboxSearchQuery } from '@/services/inbox.service';

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

describe('searchInbox', () => {
  it('strips undefined and empty-string params before calling the API', async () => {
    const data = await searchInbox({
      submissionStatus: 'SUB',
      submittedBy: '',
      invoiceNum: undefined,
      page: 0,
      size: 10,
    });

    expect(apiClient.get).toHaveBeenCalledWith('/inbox', {
      params: { submissionStatus: 'SUB', page: 0, size: 10 },
    });
    expect(data).toEqual(PAGE);
  });
});

describe('useInboxSearchQuery', () => {
  it('does not fetch while disabled', async () => {
    const { result } = renderHook(() => useInboxSearchQuery({ page: 0 }, false), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('fetches and returns the inbox page when enabled', async () => {
    const { result } = renderHook(() => useInboxSearchQuery({ page: 0, size: 10 }, true), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(PAGE);
  });
});
