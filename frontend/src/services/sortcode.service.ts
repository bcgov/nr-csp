import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
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
  useQuery({ queryKey: [...QUERY_KEY, page, size, sort], queryFn: () => listSortCodes(page, size, sort) });

export const useCreateSortCodeMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createSortCode,
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  });
};

export const useUpdateSortCodeMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ code, req }: { code: string; req: UpdateSortCodeRequest }) => updateSortCode(code, req),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  });
};

export const useDeleteSortCodeMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteSortCode,
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
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
