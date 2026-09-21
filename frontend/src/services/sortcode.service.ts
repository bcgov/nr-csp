import { keepPreviousData, useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { SORT_CODE_LOOKUP_QUERY_KEY } from '@/services/lookup.service';
import { parseContentDispositionFilename } from '@/utils/report';

export interface SortCodeResponse {
  sortCode: string;
  description: string;
  effectiveDate: string;
  expiryDate: string;
  updateTimestamp: string;
}

export interface CreateSortCodeRequest {
  sortCode: string;
  description: string;
  effectiveDate: string;
  expiryDate: string;
}

export interface UpdateSortCodeRequest {
  description: string;
  effectiveDate: string;
  expiryDate: string;
}

// Mirror of Spring Data's serialized Page<T>.
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const QUERY_KEY = ['sort-codes'] as const;

export const listSortCodes = (page: number, size: number, sort?: string): Promise<PageResponse<SortCodeResponse>> =>
  apiClient
    .get<PageResponse<SortCodeResponse>>('/sort-codes', { params: { page, size, sort } })
    .then(({ data }) => data);

export const createSortCode = (req: CreateSortCodeRequest): Promise<SortCodeResponse> =>
  apiClient.post<SortCodeResponse>('/sort-codes', req).then(({ data }) => data);

export const updateSortCode = (code: string, req: UpdateSortCodeRequest): Promise<SortCodeResponse> =>
  apiClient.put<SortCodeResponse>(`/sort-codes/${code}`, req).then(({ data }) => data);

export const deleteSortCode = (code: string): Promise<void> =>
  apiClient.delete(`/sort-codes/${code}`).then(() => undefined);

export const useListSortCodesQuery = (page: number, size: number, sort?: string) =>
  useQuery({
    queryKey: [...QUERY_KEY, page, size, sort],
    queryFn: () => listSortCodes(page, size, sort),
    // Keep the previous page's rows and totals on screen while the next page's
    // request is in flight, so the pagination bar never flickers to
    // "0 – 0 of 0" on the first visit to a page (CSP-630).
    placeholderData: keepPreviousData,
  });

// Refresh every cache a sort-code change affects: the maintenance list (active,
// so invalidation refetches it) and the shared lookup that feeds the invoice and
// report dropdowns. The lookup is *removed* rather than invalidated: the global
// query config sets refetchOnMount:false with a long staleTime, so an inactive
// invalidated query would not refetch when those pages next mount — evicting it
// forces a fresh fetch (CSP-591).
const refreshSortCodeCaches = (qc: QueryClient) => {
  qc.invalidateQueries({ queryKey: QUERY_KEY });
  qc.removeQueries({ queryKey: SORT_CODE_LOOKUP_QUERY_KEY });
};

export const useCreateSortCodeMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createSortCode,
    onSuccess: () => refreshSortCodeCaches(qc),
  });
};

export const useUpdateSortCodeMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ code, req }: { code: string; req: UpdateSortCodeRequest }) => updateSortCode(code, req),
    onSuccess: () => refreshSortCodeCaches(qc),
  });
};

export const useDeleteSortCodeMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteSortCode,
    onSuccess: () => refreshSortCodeCaches(qc),
  });
};

export interface ExportResult {
  blob: Blob;
  filename: string;
}

export const exportSortCodes = (format: 'pdf' | 'csv'): Promise<ExportResult> =>
  apiClient.get<Blob>(`/sort-codes/export/${format}`, { responseType: 'blob' }).then((response) => {
    const disposition: string = response.headers['content-disposition'] ?? '';
    const filename = parseContentDispositionFilename(disposition) ?? `Sortcodes.${format}`;
    return { blob: response.data, filename };
  });

export const useExportSortCodesMutation = () =>
  useMutation({ mutationFn: (format: 'pdf' | 'csv') => exportSortCodes(format) });

export const extractApiErrorMessage = (error: unknown): string =>
  (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'An unexpected error occurred.';
