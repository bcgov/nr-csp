import { useMutation } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { parseContentDispositionFilename } from '@/utils/report';

export interface R10ReportRequest {
  reportFormat: 'PDF' | 'CSV';
  dateFrom?: string;
  dateTo?: string;
  timeFrame?: string;
  sellerClientNumber?: string;
  sellerLocnCode?: string;
  buyerClientNumber?: string;
  buyerLocnCode?: string;
  /** Comma-separated maturity codes, e.g. `"O,S,M"` */
  maturityCodes?: string;
  invoiceTypeCode?: string;
  userId?: string;
}

export interface R10ReportResult {
  blob: Blob;
  filename: string;
}

const generateR10Report = (request: R10ReportRequest): Promise<R10ReportResult> =>
  apiClient.post<Blob>('/R10', request, { responseType: 'blob' }).then((response) => {
    const disposition: string = response.headers['content-disposition'] ?? '';
    const ext = request.reportFormat === 'CSV' ? 'csv' : 'pdf';
    const filename = parseContentDispositionFilename(disposition) ?? `R10_${Date.now()}.${ext}`;
    return { blob: response.data, filename };
  });

export const useR10ReportMutation = () => useMutation({ mutationFn: generateR10Report });
