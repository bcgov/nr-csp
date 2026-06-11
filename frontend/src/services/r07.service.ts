import { useMutation } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { parseContentDispositionFilename } from '@/utils/report';

export interface R07ReportRequest {
  reportFormat: 'PDF' | 'CSV';
  year?: number;
  month?: number;
  dateFrom?: string;
  dateTo?: string;
  timeFrame?: string;
  sellerClientNumber?: string;
  sellerLocCode?: string;
  buyerClientNumber?: string;
  buyerLocCode?: string;
  showReplacesAdjusts?: boolean;
  /** Comma-separated maturity codes, e.g. `"O,S,M"` */
  maturityCodes?: string;
  submissionNumber?: string;
  submissionYearMonth?: string;
  invoiceType?: string;
  invoiceStatus?: string;
  submissionStatus?: string;
  userId?: string;
}

export interface R07ReportResult {
  blob: Blob;
  filename: string;
}

const generateR07Report = (request: R07ReportRequest): Promise<R07ReportResult> =>
  apiClient.post<Blob>('/R07', request, { responseType: 'blob' }).then((response) => {
    const disposition: string = response.headers['content-disposition'] ?? '';
    const ext = request.reportFormat === 'CSV' ? 'csv' : 'pdf';
    const filename = parseContentDispositionFilename(disposition) ?? `R07_${Date.now()}.${ext}`;
    return { blob: response.data, filename };
  });

export const useR07ReportMutation = () => useMutation({ mutationFn: generateR07Report });
