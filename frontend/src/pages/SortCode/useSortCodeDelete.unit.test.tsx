import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

import * as service from '@/services/sortcode.service';
import { useSortCodeDelete } from './useSortCodeDelete';

vi.mock('@/services/sortcode.service', () => ({
  useDeleteSortCodeMutation: vi.fn(),
  extractApiErrorMessage: vi.fn((err) => (err as any)?.response?.data?.message ?? 'An unexpected error occurred.'),
}));

const mockMutation = (overrides = {}) => ({
  mutate: vi.fn(),
  isPending: false,
  isError: false,
  error: null,
  reset: vi.fn(),
  ...overrides,
});

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const qc = new QueryClient();
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
};

describe('useSortCodeDelete', () => {
  beforeEach(() => {
    vi.mocked(service.useDeleteSortCodeMutation).mockReturnValue(mockMutation() as any);
  });

  it('calls deleteMutation with the given sort code', () => {
    const mutate = vi.fn();
    vi.mocked(service.useDeleteSortCodeMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const { result } = renderHook(() => useSortCodeDelete(true, vi.fn()), { wrapper });
    act(() => {
      result.current.handleConfirm('A');
    });
    expect(mutate).toHaveBeenCalledWith('A', expect.anything());
  });

  it('returns apiErrorMessage when the mutation has an error', () => {
    const error = { response: { data: { message: "Sort code 'A' not found." } } };
    vi.mocked(service.useDeleteSortCodeMutation).mockReturnValue(mockMutation({ isError: true, error }) as any);
    const { result } = renderHook(() => useSortCodeDelete(true, vi.fn()), { wrapper });
    expect(result.current.apiErrorMessage).toBe("Sort code 'A' not found.");
  });

  it('isPending reflects the mutation pending state', () => {
    vi.mocked(service.useDeleteSortCodeMutation).mockReturnValue(mockMutation({ isPending: true }) as any);
    const { result } = renderHook(() => useSortCodeDelete(true, vi.fn()), { wrapper });
    expect(result.current.isPending).toBe(true);
  });

  it('resets the mutation when open changes to true', () => {
    const reset = vi.fn();
    vi.mocked(service.useDeleteSortCodeMutation).mockReturnValue(mockMutation({ reset }) as any);
    const { rerender } = renderHook(({ open }: { open: boolean }) => useSortCodeDelete(open, vi.fn()), {
      wrapper,
      initialProps: { open: false },
    });
    expect(reset).not.toHaveBeenCalled();
    rerender({ open: true });
    expect(reset).toHaveBeenCalledTimes(1);
  });

  it('calls onSuccess with the sort code after a successful delete', () => {
    const onSuccess = vi.fn();
    let capturedOptions: any;
    const mutate = vi.fn((_code: string, options: any) => {
      capturedOptions = options;
    });
    vi.mocked(service.useDeleteSortCodeMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const { result } = renderHook(() => useSortCodeDelete(true, onSuccess), { wrapper });
    act(() => {
      result.current.handleConfirm('A');
    });
    act(() => {
      capturedOptions.onSuccess();
    });
    expect(onSuccess).toHaveBeenCalledWith('A');
  });
});
