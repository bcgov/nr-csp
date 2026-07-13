import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import {
  getSubmissionDetail,
  getSubmissionInvoiceComments,
  listSubmissionHistory,
  useSubmissionDetailQuery,
  useSubmissionHistoryListQuery,
  useSubmissionInvoiceCommentsQuery,
} from '@/services/submissionHistory.service';

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

const PAGE = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };

beforeEach(() => {
  vi.clearAllMocks();
});

describe('listSubmissionHistory', () => {
  it('strips undefined and empty params before calling the API', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: PAGE });

    const data = await listSubmissionHistory({ page: 0, size: 20, sort: undefined });

    expect(apiClient.get).toHaveBeenCalledWith('/submission-history', { params: { page: 0, size: 20 } });
    expect(data).toEqual(PAGE);
  });
});

describe('getSubmissionDetail', () => {
  it('fetches the detail for the given id', async () => {
    const detail = { cspSubmissionId: 42, submissionStatus: 'SUB' };
    vi.mocked(apiClient.get).mockResolvedValue({ data: detail });

    const data = await getSubmissionDetail(42);

    expect(apiClient.get).toHaveBeenCalledWith('/submission-history/42');
    expect(data).toEqual(detail);
  });
});

describe('getSubmissionInvoiceComments', () => {
  it('fetches the invoice comments for the given id', async () => {
    const comments = [{ invoiceNumber: 'INV-1', status: 'APP', comment: 'ok' }];
    vi.mocked(apiClient.get).mockResolvedValue({ data: comments });

    const data = await getSubmissionInvoiceComments(42);

    expect(apiClient.get).toHaveBeenCalledWith('/submission-history/42/invoices');
    expect(data).toEqual(comments);
  });
});

describe('useSubmissionHistoryListQuery', () => {
  it('fetches and returns the history page', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: PAGE });

    const { result } = renderHook(() => useSubmissionHistoryListQuery({ page: 0, size: 20 }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(PAGE);
  });
});

describe('useSubmissionDetailQuery', () => {
  it('does not fetch while the id is undefined', async () => {
    const { result } = renderHook(() => useSubmissionDetailQuery(undefined), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('fetches the detail when an id is provided', async () => {
    const detail = { cspSubmissionId: 7 };
    vi.mocked(apiClient.get).mockResolvedValue({ data: detail });

    const { result } = renderHook(() => useSubmissionDetailQuery('7'), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiClient.get).toHaveBeenCalledWith('/submission-history/7');
    expect(result.current.data).toEqual(detail);
  });
});

describe('useSubmissionInvoiceCommentsQuery', () => {
  it('does not fetch for collapsed rows even with an id', async () => {
    const { result } = renderHook(() => useSubmissionInvoiceCommentsQuery(7, false), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('does not fetch when enabled but the id is null', async () => {
    const { result } = renderHook(() => useSubmissionInvoiceCommentsQuery(null, true), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('fetches comments for an expanded row', async () => {
    const comments = [{ invoiceNumber: 'INV-1', status: 'APP', comment: 'ok' }];
    vi.mocked(apiClient.get).mockResolvedValue({ data: comments });

    const { result } = renderHook(() => useSubmissionInvoiceCommentsQuery(7, true), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiClient.get).toHaveBeenCalledWith('/submission-history/7/invoices');
    expect(result.current.data).toEqual(comments);
  });
});
