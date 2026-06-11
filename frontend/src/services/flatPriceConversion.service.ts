import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { parseContentDispositionFilename } from '@/utils/report';

export interface FlatPriceConversionResponse {
  id: number;
  modellingCode: string;
  maturity: string;
  species: string;
  grade: string;
  sortCode: string;
  flatPriceConversion: number;
  effectiveDate: string;
  expiryDate: string | null;
  revisionCount: number;
  entryUserid: string;
  entryTimestamp: string;
  updateUserid: string;
  updateTimestamp: string;
}

export interface FlatPriceConversionDetails {
  maturity: string;
  species: string;
  grade: string;
  sortCode: string;
  flatPriceConversion: number;
  effectiveDate: string;
  expiryDate: string | null;
}

export interface CreateFlatPriceConversionRequest {
  modellingCode: string;
  details: FlatPriceConversionDetails;
}

export interface UpdateFlatPriceConversionRequest {
  revisionCount: number;
  details: FlatPriceConversionDetails;
}

export interface CopyFlatPriceConversionRequest {
  sourceModellingCode: string;
  targetModellingCode: string;
}

export interface SearchFlatPriceConversionParams {
  modellingCode: string;
  maturity?: string | null;
  sortCode?: string | null;
  species?: string | null;
  grade?: string | null;
}

const QUERY_KEY = ['flat-price-conversions'] as const;

export const searchFlatPriceConversions = (
  params: SearchFlatPriceConversionParams,
): Promise<FlatPriceConversionResponse[]> => {
  const filteredParams = Object.fromEntries(Object.entries(params).filter(([, v]) => v != null && v !== ''));
  return apiClient
    .get<FlatPriceConversionResponse[]>('/flat-price-conversions', { params: filteredParams })
    .then(({ data }) => data);
};

export const createFlatPriceConversion = (
  req: CreateFlatPriceConversionRequest,
): Promise<FlatPriceConversionResponse> =>
  apiClient.post<FlatPriceConversionResponse>('/flat-price-conversions', req).then(({ data }) => data);

export const updateFlatPriceConversion = (
  id: number,
  req: UpdateFlatPriceConversionRequest,
): Promise<FlatPriceConversionResponse> =>
  apiClient.put<FlatPriceConversionResponse>(`/flat-price-conversions/${id}`, req).then(({ data }) => data);

export const deleteFlatPriceConversion = (id: number): Promise<void> =>
  apiClient.delete(`/flat-price-conversions/${id}`).then(() => undefined);

export const copyFlatPriceConversions = (req: CopyFlatPriceConversionRequest): Promise<void> =>
  apiClient.post('/flat-price-conversions/copy', req).then(() => undefined);

export const clearFlatPriceConversions = (modellingCode: string): Promise<void> =>
  apiClient.delete(`/flat-price-conversions/clear/${modellingCode}`).then(() => undefined);

export const useSearchFlatPriceConversionsQuery = (params: SearchFlatPriceConversionParams) =>
  useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: () => searchFlatPriceConversions(params),
    enabled: !!params.modellingCode,
  });

export const useCreateFlatPriceConversionMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createFlatPriceConversion,
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  });
};

export const useUpdateFlatPriceConversionMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, req }: { id: number; req: UpdateFlatPriceConversionRequest }) =>
      updateFlatPriceConversion(id, req),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  });
};

export const useDeleteFlatPriceConversionMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteFlatPriceConversion(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  });
};

export const useCopyFlatPriceConversionsMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: copyFlatPriceConversions,
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  });
};

export const useClearFlatPriceConversionsMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (modellingCode: string) => clearFlatPriceConversions(modellingCode),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  });
};

export interface ExportResult {
  blob: Blob;
  filename: string;
}

export const exportFlatPriceConversions = (
  format: 'pdf' | 'csv',
  params: SearchFlatPriceConversionParams,
): Promise<ExportResult> => {
  const filteredParams = Object.fromEntries(Object.entries(params).filter(([, v]) => v != null && v !== ''));
  return apiClient
    .get<Blob>(`/flat-price-conversions/export/${format}`, { params: filteredParams, responseType: 'blob' })
    .then((response) => {
      const disposition: string = response.headers['content-disposition'] ?? '';
      const filename = parseContentDispositionFilename(disposition) ?? `FlatPriceConversions.${format}`;
      return { blob: response.data, filename };
    });
};

export const useExportFlatPriceConversionsMutation = () =>
  useMutation({
    mutationFn: ({ format, params }: { format: 'pdf' | 'csv'; params: SearchFlatPriceConversionParams }) =>
      exportFlatPriceConversions(format, params),
  });

export const extractApiErrorMessage = (error: unknown): string =>
  (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'An unexpected error occurred.';
