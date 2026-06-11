import { useMutation } from '@tanstack/react-query';

import { apiClient } from '@/config/api/request';
import { parseContentDispositionFilename } from '@/utils/report';

export interface R13ShowOptions {
  showSubmissionStatus?: boolean;
  showSubmissionNumber?: boolean;
  showSubmissionMonthYear?: boolean;
  showSubmissionType?: boolean;
  showApprovedBy?: boolean;
  showApprovalMonthYear?: boolean;
  showReviewer?: boolean;
  showInvoiceNumber?: boolean;
  showClientInvoiceDate?: boolean;
  showInvoiceStatus?: boolean;
  showInvoiceType?: boolean;
  showInvoiceReplacesAdjusts?: boolean;
  showInvoiceBoomNumber?: boolean;
  showInvoiceTimberMark?: boolean;
  showInvoiceWeighSlip?: boolean;
  showSellerName?: boolean;
  showSellerNumber?: boolean;
  showBuyerName?: boolean;
  showBuyerNumber?: boolean;
  showMaturity?: boolean;
  showSpecies?: boolean;
  showGrade?: boolean;
  showFobPoint?: boolean;
  showPieces?: boolean;
  showVolume?: boolean;
  showAmount?: boolean;
  showPrice?: boolean;
  showFlatPrice?: boolean;
  showSpreadPrice?: boolean;
  showSortCodePrimary?: boolean;
  showSortCodeSecondary?: boolean;
  showComments?: boolean;
  showEntryUserid?: boolean;
}

export interface R13ReportRequest {
  reportName: string;
  reportFormat: 'PDF' | 'CSV';
  invoiceDateFrom?: string;
  invoiceDateTo?: string;
  timeFrame?: string;
  submissionStatus?: string[];
  submissionTypes?: string[];
  submissionNumber?: string;
  entryUserId?: string;
  approvedBy?: string[];
  approvalMonthYear?: string;
  invoiceStatus?: string[];
  invoiceTypes?: string[];
  invoiceNumberFrom?: string;
  invoiceNumberTo?: string;
  invoiceReplacesAdjustsFrom?: string;
  invoiceReplacesAdjustsTo?: string;
  invoiceBoomNumberFrom?: string;
  invoiceBoomNumberTo?: string;
  invoiceTimberMarkFrom?: string;
  invoiceTimberMarkTo?: string;
  invoiceWeighSlipFrom?: string;
  invoiceWeighSlipTo?: string;
  sellerName?: string;
  sellerNumbers?: string[];
  buyerName?: string;
  buyerNumbers?: string[];
  maturityCodes?: string[];
  species?: string[];
  sortCodes?: string[];
  grades?: string[];
  showOptions: R13ShowOptions;
}

export interface R13ReportResult {
  blob: Blob;
  filename: string;
}

const generateR13Report = (request: R13ReportRequest): Promise<R13ReportResult> =>
  apiClient.post<Blob>('/R13', request, { responseType: 'blob' }).then((response) => {
    const disposition: string = response.headers['content-disposition'] ?? '';
    const ext = request.reportFormat === 'CSV' ? 'csv' : 'pdf';
    const filename = parseContentDispositionFilename(disposition) ?? `R13_${Date.now()}.${ext}`;
    return { blob: response.data, filename };
  });

export const useR13ReportMutation = () => useMutation({ mutationFn: generateR13Report });
