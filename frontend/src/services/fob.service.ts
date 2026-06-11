import { useQuery } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { type LookupItemResponse } from '@/services/lookup.service';

const fetchFobCodes = (): Promise<LookupItemResponse[]> =>
  apiClient.get<LookupItemResponse[]>('/lookup/fob').then(({ data }) => data);

export const useFobCodesQuery = () =>
  useQuery({
    queryKey: ['lookup', 'fob'],
    queryFn: fetchFobCodes,
  });
