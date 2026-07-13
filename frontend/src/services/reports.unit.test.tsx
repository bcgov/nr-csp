import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import { useR06ReportMutation } from '@/services/r06.service';
import { useR07ReportMutation } from '@/services/r07.service';
import { useR08ReportMutation } from '@/services/r08.service';
import { useR10ReportMutation } from '@/services/r10.service';
import { useR11ReportMutation } from '@/services/r11.service';
import { useR12ReportMutation } from '@/services/r12.service';
import { useR13ReportMutation } from '@/services/r13.service';

vi.mock('@/config/api/request', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}));

// All seven report services share the same POST-a-request/receive-a-blob shape,
// so they are exercised through one parametrized suite.
interface ReportResult {
  blob: Blob;
  filename: string;
}

interface MutationLike {
  mutate: (request: never) => void;
  isSuccess: boolean;
  isError: boolean;
  data: ReportResult | undefined;
  error: Error | null;
}

interface ReportCase {
  name: string;
  endpoint: string;
  useHook: () => MutationLike;
  request: Record<string, unknown>;
}

const CASES: ReportCase[] = [
  { name: 'R06', endpoint: '/R06', useHook: useR06ReportMutation, request: { reportFormat: 'PDF' } },
  { name: 'R07', endpoint: '/R07', useHook: useR07ReportMutation, request: { reportFormat: 'PDF' } },
  { name: 'R08', endpoint: '/R08', useHook: useR08ReportMutation, request: { reportFormat: 'PDF' } },
  { name: 'R10', endpoint: '/R10', useHook: useR10ReportMutation, request: { reportFormat: 'PDF' } },
  { name: 'R11', endpoint: '/R11', useHook: useR11ReportMutation, request: { reportFormat: 'PDF' } },
  { name: 'R12', endpoint: '/R12', useHook: useR12ReportMutation, request: { reportFormat: 'PDF' } },
  {
    name: 'R13',
    endpoint: '/R13',
    useHook: useR13ReportMutation,
    request: { reportName: 'Ad hoc', reportFormat: 'PDF', showOptions: { showInvoiceNumber: true } },
  },
];

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe.each(CASES)('$name report mutation', ({ name, endpoint, useHook, request }) => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it(`posts to ${endpoint} as a blob and uses the content-disposition filename`, async () => {
    const blob = new Blob(['pdf-bytes']);
    vi.mocked(apiClient.post).mockResolvedValue({
      data: blob,
      headers: { 'content-disposition': `attachment; filename="${name}_result.pdf"` },
    });

    const { result } = renderHook(() => useHook(), { wrapper: createWrapper() });
    act(() => {
      result.current.mutate(request as never);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiClient.post).toHaveBeenCalledWith(endpoint, request, { responseType: 'blob' });
    expect(result.current.data).toEqual({ blob, filename: `${name}_result.pdf` });
  });

  it('falls back to a generated .csv filename when the header is absent and format is CSV', async () => {
    const blob = new Blob(['csv-bytes']);
    vi.mocked(apiClient.post).mockResolvedValue({ data: blob, headers: {} });

    const { result } = renderHook(() => useHook(), { wrapper: createWrapper() });
    act(() => {
      result.current.mutate({ ...request, reportFormat: 'CSV' } as never);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.filename).toMatch(new RegExp(`^${name}_\\d+\\.csv$`));
  });

  it('falls back to a generated .pdf filename when the header is absent and format is PDF', async () => {
    const blob = new Blob(['pdf-bytes']);
    vi.mocked(apiClient.post).mockResolvedValue({ data: blob, headers: {} });

    const { result } = renderHook(() => useHook(), { wrapper: createWrapper() });
    act(() => {
      result.current.mutate(request as never);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.filename).toMatch(new RegExp(`^${name}_\\d+\\.pdf$`));
  });

  it('surfaces a failed request as a mutation error', async () => {
    vi.mocked(apiClient.post).mockRejectedValue(new Error('server unavailable'));

    const { result } = renderHook(() => useHook(), { wrapper: createWrapper() });
    act(() => {
      result.current.mutate(request as never);
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error?.message).toBe('server unavailable');
  });
});
