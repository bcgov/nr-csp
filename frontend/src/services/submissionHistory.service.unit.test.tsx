import { QueryClient, QueryClientProvider, focusManager } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import { THREE_HOURS } from '@/config/react-query/TimeUnits';
import {
  SUBMISSION_HISTORY_QUERY_KEY,
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

// Mirrors the app's aggressive global cache (config/react-query/config.ts): a
// wrapper that would serve cached data forever unless a hook opts out.
const createCachingWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: THREE_HOURS,
        gcTime: THREE_HOURS,
        refetchOnMount: false,
        refetchOnWindowFocus: false,
        retry: false,
      },
      mutations: { retry: false },
    },
  });
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
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

  it('keeps the previous page on screen while the next page is fetching (CSP-630)', async () => {
    const PAGE_1 = { content: [], totalElements: 42, totalPages: 3, size: 20, number: 0 };
    const PAGE_2 = { ...PAGE_1, number: 1 };

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

    const { result, rerender } = renderHook(({ page }) => useSubmissionHistoryListQuery({ page, size: 20 }), {
      wrapper: createWrapper(),
      initialProps: { page: 0 },
    });

    await waitFor(() => expect(result.current.data).toEqual(PAGE_1));

    // Navigate to a page number never visited before: its request is in flight.
    rerender({ page: 1 });

    // Without keepPreviousData the hook would report `data: undefined` here, and
    // the consumer would collapse totalElements to 0 — the "0 – 0 of 0" flicker.
    await waitFor(() => expect(result.current.isPlaceholderData).toBe(true));
    expect(result.current.data).toEqual(PAGE_1);
    expect(result.current.data?.totalElements).toBe(42);

    act(() => releasePage2({ data: PAGE_2 }));
    await waitFor(() => expect(result.current.data).toEqual(PAGE_2));
    expect(result.current.isPlaceholderData).toBe(false);
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

// ── Freshness (CSP: reviewer comment added on the Invoice screen) ─────────────

describe('submission-history queries under the app-wide aggressive cache', () => {
  it('exposes the root key every submission-history query is nested under', () => {
    expect(SUBMISSION_HISTORY_QUERY_KEY).toEqual(['submission-history']);
  });

  it.each([
    {
      label: 'invoice comments',
      seedKey: [...SUBMISSION_HISTORY_QUERY_KEY, 'invoice-comments', 7],
      stale: [{ invoiceNumber: 'INV-1', status: 'APP', comment: null }],
      fresh: [{ invoiceNumber: 'INV-1', status: 'APP', comment: 'Reviewer comment' }],
      useHook: () => useSubmissionInvoiceCommentsQuery(7, true),
    },
    {
      label: 'submission detail',
      seedKey: [...SUBMISSION_HISTORY_QUERY_KEY, 'detail', '7'],
      stale: { cspSubmissionId: 7, invoices: [{ staffComment: null }] },
      fresh: { cspSubmissionId: 7, invoices: [{ staffComment: 'Reviewer comment' }] },
      useHook: () => useSubmissionDetailQuery('7'),
    },
    {
      label: 'history list',
      seedKey: [...SUBMISSION_HISTORY_QUERY_KEY, { page: 0, size: 20 }],
      stale: { ...PAGE, content: [{ cspSubmissionId: 7, commentedInvoiceCount: 0 }] },
      fresh: { ...PAGE, content: [{ cspSubmissionId: 7, commentedInvoiceCount: 1 }] },
      useHook: () => useSubmissionHistoryListQuery({ page: 0, size: 20 }),
    },
  ])('refetches the $label on mount instead of serving the cached copy', async ({ seedKey, stale, fresh, useHook }) => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: fresh });
    const { queryClient, wrapper } = createCachingWrapper();
    queryClient.setQueryData(seedKey, stale);

    const { result } = renderHook(() => useHook(), { wrapper });

    await waitFor(() => expect(result.current.data).toEqual(fresh));
    expect(apiClient.get).toHaveBeenCalled();
  });

  it('refetches invoice comments when the window regains focus', async () => {
    const stale = [{ invoiceNumber: 'INV-1', status: 'APP', comment: null }];
    const fresh = [{ invoiceNumber: 'INV-1', status: 'APP', comment: 'Comment added by another approver' }];
    vi.mocked(apiClient.get).mockResolvedValue({ data: stale });
    const { wrapper } = createCachingWrapper();

    const { result } = renderHook(() => useSubmissionInvoiceCommentsQuery(7, true), { wrapper });
    await waitFor(() => expect(result.current.data).toEqual(stale));

    // A comment saved in another tab/session while this one sat idle.
    vi.mocked(apiClient.get).mockResolvedValue({ data: fresh });
    act(() => {
      focusManager.setFocused(false);
      focusManager.setFocused(true);
    });

    await waitFor(() => expect(result.current.data).toEqual(fresh));
    focusManager.setFocused(undefined);
  });
});
