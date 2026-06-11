import { QueryClient } from '@tanstack/react-query';

import { THREE_HOURS } from './TimeUnits';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: THREE_HOURS,
      gcTime: THREE_HOURS,
      refetchOnMount: false,
      refetchOnWindowFocus: false,
      retry: false,
    },
  },
});
