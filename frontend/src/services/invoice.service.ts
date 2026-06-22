import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { parseContentDispositionFilename } from '@/utils/report';

// ----------------------------------------------------------------------------
// DTOs (mirror backend records in
// ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails)
// ----------------------------------------------------------------------------

export interface LineItemRequest {
  lineItemID?: number | null;
  secondSort?: string | null;
  clientSecondarySort?: string | null;
  species?: string | null;
  grade?: string | null;
  numOfPieces?: number | null;
  price?: number | null;
  volume?: number | null;
  convertedPrice?: number | null;
}

export interface LineItemResponse {
  lineItemID: number;
  invoiceID: number;
  secondSort: string;
  clientSecondarySort: string | null;
  species: string;
  /** Resolved from the log_sale_species_code lookup; null when unknown. */
  speciesDescription: string | null;
  grade: string;
  numOfPieces: number;
  price: number;
  volume: number;
  convertedPrice: number | null;
  amount: number;
}

export interface CreateInvoiceRequest {
  invNumber: string;
  invoiceDate: string; // ISO LocalDate, e.g. "2026-05-19"
  invType: string;
  maturity?: string | null;
  fobCode?: string | null;
  primarySortCode?: string | null;
  totalAmt?: number | null;
  totalPieces?: number | null;
  totalVol?: number | null;
  submitterClientNum: string;
  submitterLocation: string;
  submittedBy: 'Buyer' | 'Seller';
  clientNumber?: string | null;
  clientLocation?: string | null;
  otherClientNum?: string | null;
  otherClientLocation?: string | null;
  otherClientName?: string | null;
  otherClientCity?: string | null;
  otherClientProvState?: string | null;
  boomNumbers?: string[];
  timberMarks?: string[];
  weightSlips?: string[];
  replaceInvNum?: string | null;
  adjustInvNum?: string | null;
  reviewComments?: string | null;
  submitComments?: string | null;
  manual: boolean;
  lineItems?: LineItemRequest[];
}

// Update body is structurally identical — path id supplies the invoice id.
export type UpdateInvoiceRequest = CreateInvoiceRequest;

export interface ValidationMessageResponse {
  messageKey: string;
  args: unknown[];
  type: 'ERROR' | 'WARNING';
  /** Resolved human-readable text with args interpolated by the backend. */
  message: string;
}

export interface ValidationErrorResponse {
  code: string;
  message: string;
  errors: ValidationMessageResponse[];
}

export interface InvoiceResponse {
  invID: number;
  // Surrogate parent submission key (csp_submission_id) — internal join key, used
  // here only to derive the `manual` flag. NOT the value shown to the user.
  submissionId: number | null;
  // Business submission number (csp_submission.submission_id) shown as "Submission ID".
  // Null for manually-entered invoices.
  submissionNumber: number | null;
  invNumber: string;
  invoiceDate: string;
  invStatus: string;
  invType: string;
  maturity: string | null;
  fobCode: string | null;
  primarySortCode: string | null;
  totalAmt: number | null;
  totalPieces: number | null;
  totalVol: number | null;
  submitterClientNum: string;
  submitterLocation: string;
  submittedBy: string;
  clientNumber: string | null;
  clientLocation: string | null;
  otherClientNum: string | null;
  otherClientLocation: string | null;
  otherClientName: string | null;
  otherClientCity: string | null;
  otherClientProvState: string | null;
  boomNumbers: string[];
  timberMarks: string[];
  weightSlips: string[];
  replaceInvNum: string | null;
  adjustInvNum: string | null;
  reviewComments: string | null;
  submitComments: string | null;
  entryUserID: string;
  lineItems: LineItemResponse[];
  warnings: ValidationMessageResponse[];
  /**
   * Validation errors raised by re-validating the loaded record. The GET
   * runs the validator with ActionType.OTHER so passive-read errors come
   * back here without blocking anything; mutation endpoints still throw
   * 400 with a `ValidationErrorResponse` shape when their stricter
   * validation fails.
   */
  errors: ValidationMessageResponse[];
}

export type InvoiceStatusCode = 'APP' | 'REJ' | 'CAN' | 'UNA';

export interface ChangeStatusRequest {
  status: InvoiceStatusCode;
  reviewComments?: string | null;
}

// ----------------------------------------------------------------------------
// Endpoint fetchers
// ----------------------------------------------------------------------------

const ROOT = '/invoices';
const QUERY_KEY = ['invoices'] as const;

export const getInvoice = (id: number): Promise<InvoiceResponse> =>
  apiClient.get<InvoiceResponse>(`${ROOT}/${id}`).then(({ data }) => data);

export const createInvoice = (body: CreateInvoiceRequest): Promise<InvoiceResponse> =>
  apiClient.post<InvoiceResponse>(ROOT, body).then(({ data }) => data);

export const updateInvoice = (id: number, body: UpdateInvoiceRequest): Promise<InvoiceResponse> =>
  apiClient.put<InvoiceResponse>(`${ROOT}/${id}`, body).then(({ data }) => data);

export const deleteInvoice = (id: number): Promise<void> => apiClient.delete(`${ROOT}/${id}`).then(() => undefined);

export const submitInvoice = (id: number): Promise<InvoiceResponse> =>
  apiClient.post<InvoiceResponse>(`${ROOT}/${id}/submit`).then(({ data }) => data);

export const duplicateInvoice = (id: number): Promise<InvoiceResponse> =>
  apiClient.post<InvoiceResponse>(`${ROOT}/${id}/duplicate`).then(({ data }) => data);

export const changeInvoiceStatus = (id: number, body: ChangeStatusRequest): Promise<InvoiceResponse> =>
  apiClient.patch<InvoiceResponse>(`${ROOT}/${id}/status`, body).then(({ data }) => data);

// ----------------------------------------------------------------------------
// Line-item sub-resource endpoints (DFT-only on the backend)
// ----------------------------------------------------------------------------

export const addInvoiceLineItem = (invoiceId: number, body: LineItemRequest): Promise<InvoiceResponse> =>
  apiClient.post<InvoiceResponse>(`${ROOT}/${invoiceId}/line-items`, body).then(({ data }) => data);

export const updateInvoiceLineItem = (
  invoiceId: number,
  lineId: number,
  body: LineItemRequest,
): Promise<InvoiceResponse> =>
  apiClient.patch<InvoiceResponse>(`${ROOT}/${invoiceId}/line-items/${lineId}`, body).then(({ data }) => data);

export const deleteInvoiceLineItem = (invoiceId: number, lineId: number): Promise<InvoiceResponse> =>
  apiClient.delete<InvoiceResponse>(`${ROOT}/${invoiceId}/line-items/${lineId}`).then(({ data }) => data);

// ----------------------------------------------------------------------------
// React Query hooks
// ----------------------------------------------------------------------------

export const useInvoiceQuery = (id: number | undefined) =>
  useQuery({
    queryKey: [...QUERY_KEY, id],
    queryFn: () => getInvoice(id as number),
    enabled: id !== undefined && id !== null && !Number.isNaN(id),
  });

export const useCreateInvoiceMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createInvoice,
    onSuccess: (data) => {
      qc.setQueryData([...QUERY_KEY, data.invID], data);
    },
  });
};

export const useUpdateInvoiceMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateInvoiceRequest }) => updateInvoice(id, body),
    onSuccess: (data) => {
      qc.setQueryData([...QUERY_KEY, data.invID], data);
    },
  });
};

export const useDeleteInvoiceMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteInvoice,
    onSuccess: (_void, id) => {
      qc.removeQueries({ queryKey: [...QUERY_KEY, id] });
    },
  });
};

export const useSubmitInvoiceMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: submitInvoice,
    onSuccess: (data) => {
      qc.setQueryData([...QUERY_KEY, data.invID], data);
    },
  });
};

export const useDuplicateInvoiceMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: duplicateInvoice,
    onSuccess: (data) => {
      qc.setQueryData([...QUERY_KEY, data.invID], data);
    },
  });
};

export const useChangeInvoiceStatusMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: ChangeStatusRequest }) => changeInvoiceStatus(id, body),
    onSuccess: (data) => {
      qc.setQueryData([...QUERY_KEY, data.invID], data);
    },
  });
};

export const useAddInvoiceLineItemMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ invoiceId, body }: { invoiceId: number; body: LineItemRequest }) =>
      addInvoiceLineItem(invoiceId, body),
    onSuccess: (data) => {
      qc.setQueryData([...QUERY_KEY, data.invID], data);
    },
  });
};

export const useUpdateInvoiceLineItemMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ invoiceId, lineId, body }: { invoiceId: number; lineId: number; body: LineItemRequest }) =>
      updateInvoiceLineItem(invoiceId, lineId, body),
    onSuccess: (data) => {
      qc.setQueryData([...QUERY_KEY, data.invID], data);
    },
  });
};

export const useDeleteInvoiceLineItemMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ invoiceId, lineId }: { invoiceId: number; lineId: number }) =>
      deleteInvoiceLineItem(invoiceId, lineId),
    onSuccess: (data) => {
      qc.setQueryData([...QUERY_KEY, data.invID], data);
    },
  });
};

// ----------------------------------------------------------------------------
// Error helpers
// ----------------------------------------------------------------------------

interface AxiosLikeError {
  response?: { status?: number; data?: unknown };
}

export const isValidationErrorResponse = (data: unknown): data is ValidationErrorResponse =>
  typeof data === 'object' &&
  data !== null &&
  Array.isArray((data as { errors?: unknown }).errors) &&
  typeof (data as { message?: unknown }).message === 'string';

export const extractValidationErrors = (error: unknown): ValidationMessageResponse[] => {
  const data = (error as AxiosLikeError)?.response?.data;
  return isValidationErrorResponse(data) ? data.errors : [];
};

export const extractApiErrorMessage = (error: unknown): string => {
  const data = (error as AxiosLikeError)?.response?.data as { message?: string } | undefined;
  return data?.message ?? 'An unexpected error occurred.';
};

// ----------------------------------------------------------------------------
// Group-summary table export (CSV / PDF)
// The rows currently displayed are sent to the backend, which formats them —
// so the export contains exactly what's on screen.
// ----------------------------------------------------------------------------

export interface LineItemExportRow {
  secondarySort: string;
  species: string;
  clientSecondarySort: string;
  numberPieces: number;
  grade: string;
  volume: number;
  price: number;
  amount: number;
}

export interface GroupSummaryExportRow {
  groupNumber: number;
  secondarySort: string;
  description: string;
  species: string;
  totalPieces: number;
  totalVolume: number;
  totalAmount: number;
  priceConversion: string;
  lineItems: LineItemExportRow[];
}

export interface GroupSummaryExportRequest {
  invoiceNumber: string | null;
  rows: GroupSummaryExportRow[];
}

export interface ExportResult {
  blob: Blob;
  filename: string;
}

export const exportInvoiceGroupSummary = (
  format: 'csv' | 'pdf',
  request: GroupSummaryExportRequest,
): Promise<ExportResult> =>
  apiClient.post<Blob>(`/invoices/export/${format}`, request, { responseType: 'blob' }).then((response) => {
    const disposition: string = response.headers['content-disposition'] ?? '';
    const filename = parseContentDispositionFilename(disposition) ?? `InvoiceGroupSummary.${format}`;
    return { blob: response.data, filename };
  });

export const useExportInvoiceGroupSummaryMutation = () =>
  useMutation({
    mutationFn: ({ format, request }: { format: 'csv' | 'pdf'; request: GroupSummaryExportRequest }) =>
      exportInvoiceGroupSummary(format, request),
  });
