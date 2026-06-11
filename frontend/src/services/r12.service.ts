import { useMutation } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { parseContentDispositionFilename } from '@/utils/report';

export interface R12ReportRequest {
  reportFormat: 'PDF' | 'CSV';
  year?: number;
  month?: number;
  dateFrom?: string;
  dateTo?: string;
  timeFrame?: string;
  logSaleTypeCode?: string;
  userId?: string;
}

export interface R12ReportResult {
  blob: Blob;
  filename: string;
}

const generateR12Report = (request: R12ReportRequest): Promise<R12ReportResult> =>
  apiClient.post<Blob>('/R12', request, { responseType: 'blob' }).then((response) => {
    const disposition: string = response.headers['content-disposition'] ?? '';
    const ext = request.reportFormat === 'CSV' ? 'csv' : 'pdf';
    const filename = parseContentDispositionFilename(disposition) ?? `R12_${Date.now()}.${ext}`;
    return { blob: response.data, filename };
  });

export const useR12ReportMutation = () => useMutation({ mutationFn: generateR12Report });
