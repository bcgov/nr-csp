import { useQuery } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';

export interface HealthResponse {
  status: string;
  timestamp: string;
}

export const getHealth = (): Promise<HealthResponse> =>
  apiClient.get<HealthResponse>('/health').then(({ data }) => data);

export const useHealthQuery = () =>
  useQuery({
    queryKey: ['health'],
    queryFn: getHealth,
    enabled: false,
  });
