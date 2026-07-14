import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

import * as service from '@/services/flatPriceConversion.service';
import { useFlatPriceConversionDelete } from './useFlatPriceConversionDelete';

vi.mock('@/services/flatPriceConversion.service', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/services/flatPriceConversion.service')>();
  return {
    ...actual,
    useDeleteFlatPriceConversionMutation: vi.fn(),
  };
});

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

describe('useFlatPriceConversionDelete', () => {
  beforeEach(() => {
    vi.mocked(service.useDeleteFlatPriceConversionMutation).mockReturnValue(mockMutation() as any);
  });

  it('passes the id to the delete mutation on confirm', () => {
    const mutate = vi.fn();
    vi.mocked(service.useDeleteFlatPriceConversionMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const { result } = renderHook(() => useFlatPriceConversionDelete(true, vi.fn()), { wrapper });
    act(() => {
      result.current.handleConfirm(42);
    });
    expect(mutate).toHaveBeenCalledWith(42, expect.objectContaining({ onSuccess: expect.any(Function) }));
  });

  it('invokes the onSuccess callback with the deleted id', () => {
    const mutate = vi.fn((_id: number, opts?: { onSuccess?: () => void }) => opts?.onSuccess?.());
    vi.mocked(service.useDeleteFlatPriceConversionMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const onSuccess = vi.fn();
    const { result } = renderHook(() => useFlatPriceConversionDelete(true, onSuccess), { wrapper });
    act(() => {
      result.current.handleConfirm(7);
    });
    expect(onSuccess).toHaveBeenCalledWith(7);
  });

  it('resets the mutation when the modal opens', () => {
    const reset = vi.fn();
    vi.mocked(service.useDeleteFlatPriceConversionMutation).mockReturnValue(mockMutation({ reset }) as any);
    const { rerender } = renderHook(({ open }: { open: boolean }) => useFlatPriceConversionDelete(open, vi.fn()), {
      wrapper,
      initialProps: { open: false },
    });
    expect(reset).not.toHaveBeenCalled();
    rerender({ open: true });
    expect(reset).toHaveBeenCalled();
  });

  it('exposes the API error message when the mutation errors', () => {
    const error = { response: { data: { message: 'Row is referenced elsewhere.' } } };
    vi.mocked(service.useDeleteFlatPriceConversionMutation).mockReturnValue(
      mockMutation({ isError: true, error }) as any,
    );
    const { result } = renderHook(() => useFlatPriceConversionDelete(true, vi.fn()), { wrapper });
    expect(result.current.apiErrorMessage).toBe('Row is referenced elsewhere.');
  });

  it('exposes a null apiErrorMessage and pending state by default', () => {
    const { result } = renderHook(() => useFlatPriceConversionDelete(true, vi.fn()), { wrapper });
    expect(result.current.apiErrorMessage).toBeNull();
    expect(result.current.isPending).toBe(false);
  });
});
