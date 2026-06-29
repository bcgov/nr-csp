import { useQuery } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { type PageResponse } from '@/services/search.service';

export interface InboxRowResponse {
  coastalLogSaleId: number | null;
  submissionId: string | null;
  submissionDate: string;
  submissionStatus: string;
  submissionType: string;
  invTotal: number;
  invApproved: number;
  invRejected: number;
  invProcessing: number;
  invCancelled: number;
}

export interface InboxSearchParams {
  submissionDateFrom?: string;
  submissionDateTo?: string;
  submittedBy?: string;
  submissionType?: string;
  submissionStatus?: string;
  invoiceNum?: string;
  submitterClientNum?: string;
  submitterLocNum?: string;
  keyword?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export const searchInbox = (params: InboxSearchParams): Promise<PageResponse<InboxRowResponse>> => {
  const cleanParams = Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== ''));
  return apiClient.get<PageResponse<InboxRowResponse>>('/inbox', { params: cleanParams }).then(({ data }) => data);
};

// The inbox shows live submission/invoice status across users; override the
// global 3h cache to always refetch on mount and window focus.
export const useInboxSearchQuery = (params: InboxSearchParams, enabled: boolean) =>
  useQuery({
    queryKey: ['inbox', params],
    queryFn: () => searchInbox(params),
    enabled,
    staleTime: 0,
    refetchOnMount: true,
    refetchOnWindowFocus: true,
  });
