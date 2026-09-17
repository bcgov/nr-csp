import { useQuery } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { type PageResponse } from '@/services/search.service';

/**
 * One row of the submission history list. `cspSubmissionId` is the internal id
 * the invoice-comments sub-resource is keyed on; `submissionId` is the business
 * submission number the detail page is keyed on, and is null for manual
 * submissions (which have no detail page).
 */
export interface SubmissionHistoryRowResponse {
  cspSubmissionId: number | null;
  submissionId: string | null;
  submissionDate: string;
  submittedBy: string | null;
  clientNumber: string | null;
  clientName: string | null;
  submissionStatus: string;
  invoiceCount: number | null;
  commentedInvoiceCount: number | null;
}

/** One row of a submission's expanded "Invoice comments" sub-table. */
export interface SubmissionInvoiceCommentResponse {
  invoiceNumber: string | null;
  status: string | null;
  comment: string | null;
}

/**
 * A row in the submission detail "Invoices" table. The first block backs the
 * table row; the rest backs the expandable per-invoice "Invoice details" panel.
 */
export interface SubmissionInvoiceResponse {
  coastalLogSaleId: number | null;
  invoiceNumber: string | null;
  invoiceDate: string | null;
  type: string | null;
  status: string | null;
  sellerClient: string | null;
  buyerClient: string | null;
  maturity: string | null;
  fobLocation: string | null;
  totalAmount: number | null;
  totalVolume: number | null;
  totalPieces: number | null;
  // Expandable "Invoice details" panel
  replacesInvoiceNumbers: string | null;
  adjustsInvoiceNumbers: string | null;
  sellerClientLocnCode: string | null;
  buyerClientLocnCode: string | null;
  otherPartyName: string | null;
  otherPartyCity: string | null;
  otherPartyProvState: string | null;
  primarySortCode: string | null;
  clientPrimarySortCode: string | null;
  boomNumbers: string | null;
  timberMarks: string | null;
  weighSlips: string | null;
  submitterNotes: string | null;
  staffComment: string | null;
}

/** A row in the submission detail "Invoice Line Items" table. */
export interface SubmissionLineItemResponse {
  coastalLogSaleId: number | null;
  invoiceNumber: string | null;
  species: string | null;
  grade: string | null;
  sortCode: string | null;
  clientSortCode: string | null;
  pieces: number | null;
  volume: number | null;
  price: number | null;
}

/** Full submission detail backing the View Submission page. */
export interface SubmissionDetailResponse {
  cspSubmissionId: number | null;
  submissionId: string | null;
  submissionDate: string;
  submittedBy: string | null;
  submissionStatus: string;
  clientNumber: string | null;
  clientName: string | null;
  clientLocnCode: string | null;
  email: string | null;
  telephone: string | null;
  monthComplete: string | null;
  sellerSubmission: string | null;
  adminComment: string | null;
  invoices: SubmissionInvoiceResponse[];
  lineItems: SubmissionLineItemResponse[];
}

export interface SubmissionHistoryListParams {
  page?: number;
  size?: number;
  // Single-column sort string in Spring's `field,direction` format, e.g. "submissionDate,desc".
  sort?: string;
}

export const listSubmissionHistory = (
  params: SubmissionHistoryListParams,
): Promise<PageResponse<SubmissionHistoryRowResponse>> => {
  const cleanParams = Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== ''));
  return apiClient
    .get<PageResponse<SubmissionHistoryRowResponse>>('/submission-history', { params: cleanParams })
    .then(({ data }) => data);
};

/** Keyed on the business submission number, not the internal csp submission id. */
export const getSubmissionDetail = (submissionId: string | number): Promise<SubmissionDetailResponse> =>
  apiClient.get<SubmissionDetailResponse>(`/submission-history/${submissionId}`).then(({ data }) => data);

/** Keyed on the internal csp submission id, so it also serves manual submissions. */
export const getSubmissionInvoiceComments = (
  cspSubmissionId: string | number,
): Promise<SubmissionInvoiceCommentResponse[]> =>
  apiClient
    .get<SubmissionInvoiceCommentResponse[]>(`/submission-history/${cspSubmissionId}/invoices`)
    .then(({ data }) => data);

/**
 * Root key shared by every submission-history query (list, detail, invoice
 * comments). Invoice mutations invalidate this prefix so reviewer comments and
 * statuses edited on the Invoice screen are not served from a stale cache here.
 */
export const SUBMISSION_HISTORY_QUERY_KEY = ['submission-history'] as const;

// Submission history reads invoice data (reviewer comments, invoice status)
// that is edited on the Invoice screen and by other users, so the global 3h
// aggressive cache is overridden to always refetch on mount and window focus.
const LIVE_DATA_OPTIONS = {
  staleTime: 0,
  refetchOnMount: true,
  refetchOnWindowFocus: true,
} as const;

export const useSubmissionHistoryListQuery = (params: SubmissionHistoryListParams) =>
  useQuery({
    queryKey: [...SUBMISSION_HISTORY_QUERY_KEY, params],
    queryFn: () => listSubmissionHistory(params),
    ...LIVE_DATA_OPTIONS,
  });

export const useSubmissionDetailQuery = (submissionId: string | undefined) =>
  useQuery({
    queryKey: [...SUBMISSION_HISTORY_QUERY_KEY, 'detail', submissionId],
    queryFn: () => getSubmissionDetail(submissionId as string),
    enabled: !!submissionId,
    ...LIVE_DATA_OPTIONS,
  });

// `enabled` gates the request to expanded rows so collapsed rows never fetch.
export const useSubmissionInvoiceCommentsQuery = (id: number | null, enabled: boolean) =>
  useQuery({
    queryKey: [...SUBMISSION_HISTORY_QUERY_KEY, 'invoice-comments', id],
    queryFn: () => getSubmissionInvoiceComments(id as number),
    enabled: enabled && id != null,
    ...LIVE_DATA_OPTIONS,
  });
