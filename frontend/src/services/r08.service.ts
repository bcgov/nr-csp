import { useMutation } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { parseContentDispositionFilename } from '@/utils/report';

export interface R08ReportRequest {
  reportFormat: 'PDF' | 'CSV';
  year?: number;
  month?: number;
  dateFrom?: string;
  dateTo?: string;
  sellerClientNumber?: string;
  sellerLocCode?: string;
  buyerClientNumber?: string;
  buyerLocCode?: string;
  /** Comma-separated maturity codes, e.g. `"O,S,M"` */
  maturityCodes?: string;
  invoiceType?: string;
  invoiceStatus?: string;
  submissionStatus?: string;
  submissionNumber?: string;
  submissionYearMonth?: string;
  userId?: string;
}

export interface R08ReportResult {
  blob: Blob;
  filename: string;
}

const generateR08Report = (request: R08ReportRequest): Promise<R08ReportResult> =>
  apiClient.post<Blob>('/R08', request, { responseType: 'blob' }).then((response) => {
    const disposition: string = response.headers['content-disposition'] ?? '';
    const ext = request.reportFormat === 'CSV' ? 'csv' : 'pdf';
    const filename = parseContentDispositionFilename(disposition) ?? `R08_${Date.now()}.${ext}`;
    return { blob: response.data, filename };
  });

export const useR08ReportMutation = () => useMutation({ mutationFn: generateR08Report });
