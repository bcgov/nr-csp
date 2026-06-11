import { useMutation } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { parseContentDispositionFilename } from '@/utils/report';

export interface R06ReportRequest {
  reportFormat: 'PDF' | 'CSV';
  dateFrom?: string;
  dateTo?: string;
  sellerClientNumber?: string;
  sellerLocCode?: string;
  buyerClientNumber?: string;
  buyerLocCode?: string;
  /** Comma-separated maturity codes, e.g. `"O,S,M"` */
  maturityCodes?: string;
  submissionId?: number;
  /** Comma-separated invoice number ranges, e.g. `"100-200,300-400"` */
  invoiceNumbers?: string;
  logSaleEntryStatusCode?: string;
  cspInvoiceTypeCode?: string;
  userId?: string;
}

export interface R06ReportResult {
  blob: Blob;
  filename: string;
}

const generateR06Report = (request: R06ReportRequest): Promise<R06ReportResult> =>
  apiClient.post<Blob>('/R06', request, { responseType: 'blob' }).then((response) => {
    const disposition: string = response.headers['content-disposition'] ?? '';
    const ext = request.reportFormat === 'CSV' ? 'csv' : 'pdf';
    const filename = parseContentDispositionFilename(disposition) ?? `R06_${Date.now()}.${ext}`;
    return { blob: response.data, filename };
  });

export const useR06ReportMutation = () => useMutation({ mutationFn: generateR06Report });
