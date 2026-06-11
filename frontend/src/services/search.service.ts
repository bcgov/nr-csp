import { useQuery } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';

export interface SearchResultResponse {
  coastalLogSaleId: number;
  cspSubmissionId: number;
  invoiceStatus: string;
  invoiceNumber: string;
  invoiceDate: string;
  type: string;
  clientNumber: string;
  clientName: string;
  maturity: string;
  submissionType: string;
}

export interface ClientLocationResponse {
  clientNumber: string;
  clientName: string;
  clientLocnCode: string;
  clientLocnName: string;
  city: string;
  province: string;
}

// Mirror of Spring Data's serialized Page<T>. Only the fields the UI needs are typed.
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface SearchParams {
  invDate?: string;
  startDate?: string;
  endDate?: string;
  submitterClientNum?: string;
  sellerBuyerClientNum?: string;
  sellerBuyerLocNum?: string;
  sellerSubmitter?: boolean;
  invNumber?: string;
  invStatus?: string;
  invType?: string;
  maturity?: string;
  keyword?: string;
  page?: number;
  size?: number;
  // Single-column sort string in Spring's `field,direction` format, e.g. "invoiceDate,desc".
  sort?: string;
}

export const searchInvoices = (params: SearchParams): Promise<PageResponse<SearchResultResponse>> => {
  const cleanParams = Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== ''));
  return apiClient.get<PageResponse<SearchResultResponse>>('/search', { params: cleanParams }).then(({ data }) => data);
};

export const getClientsByName = (name: string): Promise<ClientLocationResponse[]> =>
  apiClient.get<ClientLocationResponse[]>('/clients', { params: { name } }).then(({ data }) => data);

export const getClientsByNumber = (number: string): Promise<ClientLocationResponse[]> =>
  apiClient.get<ClientLocationResponse[]>('/clients', { params: { number } }).then(({ data }) => data);

export const useSearchQuery = (params: SearchParams, enabled: boolean) =>
  useQuery({
    queryKey: ['search', params],
    queryFn: () => searchInvoices(params),
    enabled,
  });
