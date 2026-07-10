import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import {
  addInvoiceLineItem,
  changeInvoiceStatus,
  createInvoice,
  deleteInvoice,
  deleteInvoiceLineItem,
  duplicateInvoice,
  exportInvoiceGroupSummary,
  extractApiErrorMessage,
  extractValidationErrors,
  getInvoice,
  isValidationErrorResponse,
  submitInvoice,
  updateInvoice,
  updateInvoiceLineItem,
  useAddInvoiceLineItemMutation,
  useChangeInvoiceStatusMutation,
  useCreateInvoiceMutation,
  useDeleteInvoiceLineItemMutation,
  useDeleteInvoiceMutation,
  useDuplicateInvoiceMutation,
  useExportInvoiceGroupSummaryMutation,
  useInvoiceQuery,
  useSubmitInvoiceMutation,
  useUpdateInvoiceLineItemMutation,
  useUpdateInvoiceMutation,
  type CreateInvoiceRequest,
  type InvoiceResponse,
} from '@/services/invoice.service';

vi.mock('@/config/api/request', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
};

const INVOICE = { invID: 7, invNumber: 'INV-7' } as InvoiceResponse;

const CREATE_BODY = {
  invNumber: 'INV-7',
  invoiceDate: '2026-05-19',
  invType: 'SAL',
  submitterClientNum: '00012345',
  submitterLocation: '00',
  submittedBy: 'Seller',
  manual: true,
} as CreateInvoiceRequest;

beforeEach(() => {
  vi.clearAllMocks();
});

// ── Endpoint fetchers ─────────────────────────────────────────────────────────

describe('invoice endpoint fetchers', () => {
  it('getInvoice GETs /invoices/{id}', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: INVOICE });
    await expect(getInvoice(7)).resolves.toEqual(INVOICE);
    expect(apiClient.get).toHaveBeenCalledWith('/invoices/7');
  });

  it('createInvoice POSTs the body to /invoices', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: INVOICE });
    await expect(createInvoice(CREATE_BODY)).resolves.toEqual(INVOICE);
    expect(apiClient.post).toHaveBeenCalledWith('/invoices', CREATE_BODY);
  });

  it('updateInvoice PUTs the body to /invoices/{id}', async () => {
    vi.mocked(apiClient.put).mockResolvedValue({ data: INVOICE });
    await expect(updateInvoice(7, CREATE_BODY)).resolves.toEqual(INVOICE);
    expect(apiClient.put).toHaveBeenCalledWith('/invoices/7', CREATE_BODY);
  });

  it('deleteInvoice DELETEs /invoices/{id} and resolves to undefined', async () => {
    vi.mocked(apiClient.delete).mockResolvedValue({});
    await expect(deleteInvoice(7)).resolves.toBeUndefined();
    expect(apiClient.delete).toHaveBeenCalledWith('/invoices/7');
  });

  it('submitInvoice POSTs to /invoices/{id}/submit', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: INVOICE });
    await expect(submitInvoice(7)).resolves.toEqual(INVOICE);
    expect(apiClient.post).toHaveBeenCalledWith('/invoices/7/submit');
  });

  it('duplicateInvoice POSTs to /invoices/{id}/duplicate', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: INVOICE });
    await expect(duplicateInvoice(7)).resolves.toEqual(INVOICE);
    expect(apiClient.post).toHaveBeenCalledWith('/invoices/7/duplicate');
  });

  it('changeInvoiceStatus PATCHes /invoices/{id}/status', async () => {
    vi.mocked(apiClient.patch).mockResolvedValue({ data: INVOICE });
    await expect(changeInvoiceStatus(7, { status: 'APP' })).resolves.toEqual(INVOICE);
    expect(apiClient.patch).toHaveBeenCalledWith('/invoices/7/status', { status: 'APP' });
  });

  it('line-item add/update/delete hit the sub-resource endpoints', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: INVOICE });
    vi.mocked(apiClient.patch).mockResolvedValue({ data: INVOICE });
    vi.mocked(apiClient.delete).mockResolvedValue({ data: INVOICE });

    await expect(addInvoiceLineItem(7, { species: 'FI' })).resolves.toEqual(INVOICE);
    expect(apiClient.post).toHaveBeenCalledWith('/invoices/7/line-items', { species: 'FI' });

    await expect(updateInvoiceLineItem(7, 3, { species: 'FI' })).resolves.toEqual(INVOICE);
    expect(apiClient.patch).toHaveBeenCalledWith('/invoices/7/line-items/3', { species: 'FI' });

    await expect(deleteInvoiceLineItem(7, 3)).resolves.toEqual(INVOICE);
    expect(apiClient.delete).toHaveBeenCalledWith('/invoices/7/line-items/3');
  });
});

// ── React Query hooks ─────────────────────────────────────────────────────────

describe('useInvoiceQuery', () => {
  it('does not fetch for an undefined or NaN id', async () => {
    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useInvoiceQuery(undefined), { wrapper });
    const { result: nanResult } = renderHook(() => useInvoiceQuery(Number.NaN), { wrapper });

    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    await waitFor(() => expect(nanResult.current.fetchStatus).toBe('idle'));
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('fetches the invoice for a valid id', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: INVOICE });
    const { wrapper } = createWrapper();

    const { result } = renderHook(() => useInvoiceQuery(7), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(INVOICE);
  });
});

describe('invoice mutation hooks', () => {
  it('useCreateInvoiceMutation stores the created invoice in the query cache', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: INVOICE });
    const { queryClient, wrapper } = createWrapper();

    const { result } = renderHook(() => useCreateInvoiceMutation(), { wrapper });
    act(() => {
      result.current.mutate(CREATE_BODY);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryData(['invoices', 7])).toEqual(INVOICE);
  });

  it('useSubmitInvoiceMutation refreshes the cached invoice', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: INVOICE });
    const { queryClient, wrapper } = createWrapper();

    const { result } = renderHook(() => useSubmitInvoiceMutation(), { wrapper });
    act(() => {
      result.current.mutate(7);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryData(['invoices', 7])).toEqual(INVOICE);
  });

  it('useChangeInvoiceStatusMutation refreshes the cached invoice', async () => {
    vi.mocked(apiClient.patch).mockResolvedValue({ data: INVOICE });
    const { queryClient, wrapper } = createWrapper();

    const { result } = renderHook(() => useChangeInvoiceStatusMutation(), { wrapper });
    act(() => {
      result.current.mutate({ id: 7, body: { status: 'REJ', reviewComments: 'nope' } });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryData(['invoices', 7])).toEqual(INVOICE);
  });

  it.each([
    {
      label: 'Update',
      useHook: useUpdateInvoiceMutation,
      arrange: () => vi.mocked(apiClient.put).mockResolvedValue({ data: INVOICE }),
      variables: { id: 7, body: CREATE_BODY },
    },
    {
      label: 'Duplicate',
      useHook: useDuplicateInvoiceMutation,
      arrange: () => vi.mocked(apiClient.post).mockResolvedValue({ data: INVOICE }),
      variables: 7,
    },
    {
      label: 'AddLineItem',
      useHook: useAddInvoiceLineItemMutation,
      arrange: () => vi.mocked(apiClient.post).mockResolvedValue({ data: INVOICE }),
      variables: { invoiceId: 7, body: { species: 'FI' } },
    },
    {
      label: 'UpdateLineItem',
      useHook: useUpdateInvoiceLineItemMutation,
      arrange: () => vi.mocked(apiClient.patch).mockResolvedValue({ data: INVOICE }),
      variables: { invoiceId: 7, lineId: 3, body: { species: 'FI' } },
    },
    {
      label: 'DeleteLineItem',
      useHook: useDeleteInvoiceLineItemMutation,
      arrange: () => vi.mocked(apiClient.delete).mockResolvedValue({ data: INVOICE }),
      variables: { invoiceId: 7, lineId: 3 },
    },
  ])('use$label mutation refreshes the cached invoice from the response', async ({ useHook, arrange, variables }) => {
    arrange();
    const { queryClient, wrapper } = createWrapper();

    const { result } = renderHook(() => useHook(), { wrapper });
    act(() => {
      result.current.mutate(variables as never);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryData(['invoices', 7])).toEqual(INVOICE);
  });

  it('useExportInvoiceGroupSummaryMutation resolves with the export result', async () => {
    const blob = new Blob(['csv']);
    vi.mocked(apiClient.post).mockResolvedValue({ data: blob, headers: {} });
    const { wrapper } = createWrapper();

    const { result } = renderHook(() => useExportInvoiceGroupSummaryMutation(), { wrapper });
    act(() => {
      result.current.mutate({ format: 'csv', request: { invoiceNumber: 'INV-7', rows: [] } });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ blob, filename: 'InvoiceGroupSummary.csv' });
  });

  it('useDeleteInvoiceMutation removes the invoice from the query cache', async () => {
    vi.mocked(apiClient.delete).mockResolvedValue({});
    const { queryClient, wrapper } = createWrapper();
    queryClient.setQueryData(['invoices', 7], INVOICE);

    const { result } = renderHook(() => useDeleteInvoiceMutation(), { wrapper });
    act(() => {
      result.current.mutate(7);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryData(['invoices', 7])).toBeUndefined();
  });
});

// ── Error helpers ─────────────────────────────────────────────────────────────

describe('error helpers', () => {
  const validationBody = {
    code: 'VALIDATION',
    message: 'Validation failed',
    errors: [{ messageKey: 'k', args: [], type: 'ERROR', message: 'bad' }],
  };

  it('isValidationErrorResponse recognises the backend validation shape', () => {
    expect(isValidationErrorResponse(validationBody)).toBe(true);
    expect(isValidationErrorResponse(null)).toBe(false);
    expect(isValidationErrorResponse({ message: 'x' })).toBe(false);
    expect(isValidationErrorResponse({ errors: [] })).toBe(false);
  });

  it('extractValidationErrors returns the errors array from an axios-like error', () => {
    expect(extractValidationErrors({ response: { data: validationBody } })).toEqual(validationBody.errors);
    expect(extractValidationErrors({ response: { data: { message: 'plain' } } })).toEqual([]);
    expect(extractValidationErrors(undefined)).toEqual([]);
  });

  it('extractApiErrorMessage falls back to a generic message', () => {
    expect(extractApiErrorMessage({ response: { data: { message: 'Not found' } } })).toBe('Not found');
    expect(extractApiErrorMessage(new Error('x'))).toBe('An unexpected error occurred.');
  });
});

// ── Group-summary export ──────────────────────────────────────────────────────

describe('exportInvoiceGroupSummary', () => {
  it('POSTs the rows as a blob request and uses the content-disposition filename', async () => {
    const blob = new Blob(['csv']);
    vi.mocked(apiClient.post).mockResolvedValue({
      data: blob,
      headers: { 'content-disposition': 'attachment; filename="summary.csv"' },
    });

    const request = { invoiceNumber: 'INV-7', rows: [] };
    const result = await exportInvoiceGroupSummary('csv', request);

    expect(apiClient.post).toHaveBeenCalledWith('/invoices/export/csv', request, { responseType: 'blob' });
    expect(result).toEqual({ blob, filename: 'summary.csv' });
  });

  it('falls back to a default filename when the header is missing', async () => {
    const blob = new Blob(['pdf']);
    vi.mocked(apiClient.post).mockResolvedValue({ data: blob, headers: {} });

    const result = await exportInvoiceGroupSummary('pdf', { invoiceNumber: null, rows: [] });

    expect(result.filename).toBe('InvoiceGroupSummary.pdf');
  });
});
