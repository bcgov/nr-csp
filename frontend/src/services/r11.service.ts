import { useMutation } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { parseContentDispositionFilename } from '@/utils/report';

export interface R11ReportRequest {
  reportFormat: 'PDF' | 'CSV';
  dateFrom?: string;
  dateTo?: string;
  timeFrame?: string;
  blended?: boolean;
  modelingCode?: string;
  /** Comma-separated maturity codes, e.g. `"O,S,M"` */
  maturityCodes?: string;
  /** Comma-separated maturity descriptions matching the order of `maturityCodes`; derived from `maturityCodes` by the backend if omitted */
  maturityDescriptions?: string;
  userId?: string;
}

export interface R11ReportResult {
  blob: Blob;
  filename: string;
}

const generateR11Report = (request: R11ReportRequest): Promise<R11ReportResult> =>
  apiClient.post<Blob>('/R11', request, { responseType: 'blob' }).then((response) => {
    const disposition: string = response.headers['content-disposition'] ?? '';
    const ext = request.reportFormat === 'CSV' ? 'csv' : 'pdf';
    const filename = parseContentDispositionFilename(disposition) ?? `R11_${Date.now()}.${ext}`;
    return { blob: response.data, filename };
  });

export const useR11ReportMutation = () => useMutation({ mutationFn: generateR11Report });
