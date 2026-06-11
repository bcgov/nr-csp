import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

import * as service from '@/services/sortcode.service';
import { useSortCodeForm } from './useSortCodeForm';

vi.mock('@/services/sortcode.service', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/services/sortcode.service')>();
  return {
    ...actual,
    useCreateSortCodeMutation: vi.fn(),
    useUpdateSortCodeMutation: vi.fn(),
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

describe('useSortCodeForm', () => {
  beforeEach(() => {
    vi.mocked(service.useCreateSortCodeMutation).mockReturnValue(mockMutation() as any);
    vi.mocked(service.useUpdateSortCodeMutation).mockReturnValue(mockMutation() as any);
  });

  it('initializes with empty values in add mode', () => {
    const { result } = renderHook(() => useSortCodeForm(true, 'add', undefined, vi.fn()), { wrapper });
    expect(result.current.values.sortCode).toBe('');
    expect(result.current.values.description).toBe('');
    expect(result.current.values.effectiveDate).toBe('');
    expect(result.current.values.expiryDate).toBe('');
  });

  it('initializes with initialValues in edit mode', () => {
    const initialValues = {
      sortCode: 'A',
      description: 'Lumber',
      effectiveDate: '1990-01-01',
      expiryDate: '9999-12-31',
      updateTimestamp: '2024-01-01',
    };
    const { result } = renderHook(() => useSortCodeForm(true, 'edit', initialValues, vi.fn()), { wrapper });
    expect(result.current.values.sortCode).toBe('A');
    expect(result.current.values.description).toBe('Lumber');
    expect(result.current.values.effectiveDate).toBe('1990-01-01');
  });

  it('sets validation errors for missing required fields on submit', () => {
    const { result } = renderHook(() => useSortCodeForm(true, 'add', undefined, vi.fn()), { wrapper });
    act(() => {
      result.current.handleSubmit();
    });
    expect(result.current.errors.sortCode).toBe('Sort code is required.');
    expect(result.current.errors.description).toBe('Description is required.');
    expect(result.current.errors.effectiveDate).toBe('Effective date is required.');
    expect(result.current.errors.expiryDate).toBe('Expiry date is required.');
  });

  it('sets validation error when sort code exceeds 1 character', () => {
    const { result } = renderHook(() => useSortCodeForm(true, 'add', undefined, vi.fn()), { wrapper });
    act(() => {
      result.current.set('sortCode')({ target: { value: 'AB' } } as any);
    });
    act(() => {
      result.current.handleSubmit();
    });
    expect(result.current.errors.sortCode).toBe('Sort code must be at most 1 character.');
  });

  it('sets validation error when expiryDate is before effectiveDate', () => {
    const { result } = renderHook(() => useSortCodeForm(true, 'add', undefined, vi.fn()), { wrapper });
    act(() => {
      result.current.set('sortCode')({ target: { value: 'A' } } as any);
      result.current.set('description')({ target: { value: 'Lumber' } } as any);
      result.current.setValue('effectiveDate', '2030-01-01');
      result.current.setValue('expiryDate', '2020-01-01');
    });
    act(() => {
      result.current.handleSubmit();
    });
    expect(result.current.errors.expiryDate).toBe('Expiry date must not be before effective date.');
  });

  it('calls createMutation with uppercased sort code', () => {
    const mutate = vi.fn();
    vi.mocked(service.useCreateSortCodeMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const { result } = renderHook(() => useSortCodeForm(true, 'add', undefined, vi.fn()), { wrapper });
    act(() => {
      result.current.set('sortCode')({ target: { value: 'a' } } as any);
      result.current.set('description')({ target: { value: 'Lumber' } } as any);
      result.current.setValue('effectiveDate', '1990-01-01');
      result.current.setValue('expiryDate', '9999-12-31');
    });
    act(() => {
      result.current.handleSubmit();
    });
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ sortCode: 'A', description: 'Lumber' }),
      expect.anything(),
    );
  });

  it('calls updateMutation with the existing sort code and updated fields', () => {
    const mutate = vi.fn();
    vi.mocked(service.useUpdateSortCodeMutation).mockReturnValue(mockMutation({ mutate }) as any);
    const initialValues = {
      sortCode: 'A',
      description: 'Old',
      effectiveDate: '1990-01-01',
      expiryDate: '9999-12-31',
      updateTimestamp: '',
    };
    const { result } = renderHook(() => useSortCodeForm(true, 'edit', initialValues, vi.fn()), { wrapper });
    act(() => {
      result.current.set('description')({ target: { value: 'New' } } as any);
    });
    act(() => {
      result.current.handleSubmit();
    });
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ code: 'A', req: expect.objectContaining({ description: 'New' }) }),
      expect.anything(),
    );
  });

  it('returns apiErrorMessage when the mutation has an error', () => {
    const error = { response: { data: { message: "Sort code 'A' already exists." } } };
    vi.mocked(service.useCreateSortCodeMutation).mockReturnValue(mockMutation({ isError: true, error }) as any);
    const { result } = renderHook(() => useSortCodeForm(true, 'add', undefined, vi.fn()), { wrapper });
    expect(result.current.apiErrorMessage).toBe("Sort code 'A' already exists.");
  });
});
