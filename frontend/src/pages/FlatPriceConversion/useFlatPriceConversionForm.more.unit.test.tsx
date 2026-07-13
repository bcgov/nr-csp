import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

import * as service from '@/services/flatPriceConversion.service';
import { useFlatPriceConversionForm } from './useFlatPriceConversionForm';

import type { FlatPriceConversionResponse } from '@/services/flatPriceConversion.service';

vi.mock('@/services/flatPriceConversion.service', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/services/flatPriceConversion.service')>();
  return {
    ...actual,
    useCreateFlatPriceConversionMutation: vi.fn(),
    useUpdateFlatPriceConversionMutation: vi.fn(),
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

const existingRow: FlatPriceConversionResponse = {
  id: 42,
  modellingCode: 'M1',
  maturity: 'S',
  species: 'FD',
  grade: 'U',
  sortCode: 'A',
  flatPriceConversion: 100,
  effectiveDate: '2024-01-01',
  expiryDate: null,
  revisionCount: 3,
  entryUserid: 'user',
  entryTimestamp: '2024-01-01T00:00:00',
  updateUserid: 'user',
  updateTimestamp: '2024-01-01T00:00:00',
};

const fillValidValues = (result: { current: ReturnType<typeof useFlatPriceConversionForm> }) => {
  act(() => {
    result.current.set('species')({ target: { value: 'FD' } } as React.ChangeEvent<HTMLInputElement>);
    result.current.set('grade')({ target: { value: 'U' } } as React.ChangeEvent<HTMLInputElement>);
    result.current.setValue('sortCode', 'A');
    result.current.setValue('maturity', 'S');
    result.current.setValue('flatPriceConversion', '150');
    result.current.setValue('effectiveDate', '2024-01-01');
  });
};

describe('useFlatPriceConversionForm', () => {
  beforeEach(() => {
    vi.mocked(service.useCreateFlatPriceConversionMutation).mockReturnValue(mockMutation() as any);
    vi.mocked(service.useUpdateFlatPriceConversionMutation).mockReturnValue(mockMutation() as any);
  });

  it('initializes with empty values in add mode', () => {
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'add', 'M1', undefined, vi.fn()), {
      wrapper,
    });
    expect(result.current.values.species).toBe('');
    expect(result.current.values.flatPriceConversion).toBe('');
    expect(result.current.values.expiryDate).toBe('');
  });

  it('initializes from initialValues in edit mode', () => {
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'edit', 'M1', existingRow, vi.fn()), {
      wrapper,
    });
    expect(result.current.values.species).toBe('FD');
    expect(result.current.values.flatPriceConversion).toBe('100');
    expect(result.current.values.expiryDate).toBe('');
  });

  it('updates a value through the set() change-event helper', () => {
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'add', 'M1', undefined, vi.fn()), {
      wrapper,
    });
    act(() => {
      result.current.set('species')({ target: { value: 'HW' } } as React.ChangeEvent<HTMLInputElement>);
    });
    expect(result.current.values.species).toBe('HW');
  });

  it('sets validation errors and does not mutate when values are invalid', () => {
    const mutate = vi.fn();
    vi.mocked(service.useCreateFlatPriceConversionMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'add', 'M1', undefined, vi.fn()), {
      wrapper,
    });
    act(() => {
      result.current.handleSubmit();
    });
    expect(result.current.errors.species).toBe('Species is required.');
    expect(result.current.errors.flatPriceConversion).toBe('Flat price conversion is required.');
    expect(result.current.errors.effectiveDate).toBe('Effective date is required.');
    expect(mutate).not.toHaveBeenCalled();
  });

  it('calls the create mutation with numeric conversion and null expiry in add mode', () => {
    const mutate = vi.fn();
    vi.mocked(service.useCreateFlatPriceConversionMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'add', 'M1', undefined, vi.fn()), {
      wrapper,
    });
    fillValidValues(result);
    act(() => {
      result.current.handleSubmit();
    });
    expect(mutate).toHaveBeenCalledWith(
      {
        modellingCode: 'M1',
        details: {
          species: 'FD',
          grade: 'U',
          sortCode: 'A',
          maturity: 'S',
          flatPriceConversion: 150,
          effectiveDate: '2024-01-01',
          expiryDate: null,
        },
      },
      expect.anything(),
    );
    expect(result.current.errors).toEqual({});
  });

  it('passes a non-empty expiry date through to the create mutation', () => {
    const mutate = vi.fn();
    vi.mocked(service.useCreateFlatPriceConversionMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'add', 'M1', undefined, vi.fn()), {
      wrapper,
    });
    fillValidValues(result);
    act(() => {
      result.current.setValue('expiryDate', '2030-12-31');
    });
    act(() => {
      result.current.handleSubmit();
    });
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ details: expect.objectContaining({ expiryDate: '2030-12-31' }) }),
      expect.anything(),
    );
  });

  it('invokes the onSuccess callback with the created row', () => {
    const created = { ...existingRow, id: 99 };
    const mutate = vi.fn((_req, opts?: { onSuccess?: (row: FlatPriceConversionResponse) => void }) =>
      opts?.onSuccess?.(created),
    );
    vi.mocked(service.useCreateFlatPriceConversionMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const onSuccess = vi.fn();
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'add', 'M1', undefined, onSuccess), {
      wrapper,
    });
    fillValidValues(result);
    act(() => {
      result.current.handleSubmit();
    });
    expect(onSuccess).toHaveBeenCalledWith(created);
  });

  it('calls the update mutation with the row id and revision count in edit mode', () => {
    const mutate = vi.fn();
    vi.mocked(service.useUpdateFlatPriceConversionMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const onSuccess = vi.fn();
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'edit', 'M1', existingRow, onSuccess), {
      wrapper,
    });
    act(() => {
      result.current.setValue('flatPriceConversion', '200');
    });
    act(() => {
      result.current.handleSubmit();
    });
    expect(mutate).toHaveBeenCalledWith(
      {
        id: 42,
        req: {
          revisionCount: 3,
          details: expect.objectContaining({ flatPriceConversion: 200, expiryDate: null }),
        },
      },
      expect.anything(),
    );
  });

  it('invokes the onSuccess callback with the updated row in edit mode', () => {
    const updated = { ...existingRow, flatPriceConversion: 200 };
    const mutate = vi.fn((_req, opts?: { onSuccess?: (row: FlatPriceConversionResponse) => void }) =>
      opts?.onSuccess?.(updated),
    );
    vi.mocked(service.useUpdateFlatPriceConversionMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const onSuccess = vi.fn();
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'edit', 'M1', existingRow, onSuccess), {
      wrapper,
    });
    act(() => {
      result.current.handleSubmit();
    });
    expect(onSuccess).toHaveBeenCalledWith(updated);
  });

  it('exposes the API error message when the active mutation errors', () => {
    const error = { response: { data: { message: 'Overlapping date range.' } } };
    vi.mocked(service.useCreateFlatPriceConversionMutation).mockReturnValue(
      mockMutation({ isError: true, error }) as any,
    );
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'add', 'M1', undefined, vi.fn()), {
      wrapper,
    });
    expect(result.current.apiErrorMessage).toBe('Overlapping date range.');
  });

  it('exposes a null apiErrorMessage when the mutation has no error', () => {
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'add', 'M1', undefined, vi.fn()), {
      wrapper,
    });
    expect(result.current.apiErrorMessage).toBeNull();
  });

  it('reflects the pending state of the active mutation', () => {
    vi.mocked(service.useCreateFlatPriceConversionMutation).mockReturnValue(mockMutation({ isPending: true }) as any);
    const { result } = renderHook(() => useFlatPriceConversionForm(true, 'add', 'M1', undefined, vi.fn()), {
      wrapper,
    });
    expect(result.current.isPending).toBe(true);
  });

  it('resets values and mutations when the modal re-opens', () => {
    const createReset = vi.fn();
    const updateReset = vi.fn();
    vi.mocked(service.useCreateFlatPriceConversionMutation).mockReturnValue(
      mockMutation({ reset: createReset }) as any,
    );
    vi.mocked(service.useUpdateFlatPriceConversionMutation).mockReturnValue(
      mockMutation({ reset: updateReset }) as any,
    );
    const { result, rerender } = renderHook(
      ({ open }: { open: boolean }) => useFlatPriceConversionForm(open, 'add', 'M1', undefined, vi.fn()),
      { wrapper, initialProps: { open: false } },
    );
    act(() => {
      result.current.setValue('species', 'FD');
    });
    rerender({ open: true });
    expect(result.current.values.species).toBe('');
    expect(createReset).toHaveBeenCalled();
    expect(updateReset).toHaveBeenCalled();
  });
});
