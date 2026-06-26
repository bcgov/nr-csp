import { useQuery } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { type PageResponse } from '@/services/search.service';

/** One row of the submission history list. */
export interface SubmissionHistoryRowResponse {
  cspSubmissionId: number | null;
  submissionDate: string;
  submittedBy: string | null;
  clientNumber: string | null;
  clientName: string | null;
  submissionStatus: string;
  comment: string | null;
}

/** A row in the submission detail "Invoice Details" table. */
export interface SubmissionInvoiceResponse {
  coastalLogSaleId: number | null;
  invoiceNumber: string | null;
  invoiceDate: string | null;
  type: string | null;
  sellerClient: string | null;
  buyerClient: string | null;
  maturity: string | null;
  fobLocation: string | null;
  totalAmount: number | null;
  totalVolume: number | null;
  totalPieces: number | null;
}

/** A row in the submission detail "Invoice Line Items" table. */
export interface SubmissionLineItemResponse {
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

export const getSubmissionDetail = (id: string | number): Promise<SubmissionDetailResponse> =>
  apiClient.get<SubmissionDetailResponse>(`/submission-history/${id}`).then(({ data }) => data);

export const useSubmissionHistoryListQuery = (params: SubmissionHistoryListParams) =>
  useQuery({
    queryKey: ['submission-history', params],
    queryFn: () => listSubmissionHistory(params),
  });

export const useSubmissionDetailQuery = (id: string | undefined) =>
  useQuery({
    queryKey: ['submission-history', 'detail', id],
    queryFn: () => getSubmissionDetail(id as string),
    enabled: !!id,
  });
