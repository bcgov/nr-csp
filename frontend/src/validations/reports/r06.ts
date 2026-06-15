import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

type R06FormValues = {
  dateFrom: Date | null;
  dateTo: Date | null;
  hasInvoiceNumbers: boolean;
  submissionId: string;
  sellerName: string;
  sellerNumber: string;
  buyerName: string;
  buyerNumber: string;
};

export function validateR06(values: R06FormValues): ValidationResult {
  const messages = new MessageCollector();

  if (!values.hasInvoiceNumbers) {
    if (!values.dateFrom) {
      messages.addError('report.r06.startdate.required.error');
    }
    if (!values.dateTo) {
      messages.addError('report.r06.enddate.required.error');
    }
  }

  if (values.submissionId.trim() && !/^\d+$/.test(values.submissionId.trim())) {
    messages.addError('report.submissionnumber.numeric.error');
  }

  if (values.sellerName.trim() && !values.sellerNumber.trim()) {
    messages.addError('report.client.noselection.error', ['seller']);
  }

  if (values.buyerName.trim() && !values.buyerNumber.trim()) {
    messages.addError('report.client.noselection.error', ['buyer']);
  }

  if (values.dateFrom && values.dateTo && values.dateFrom > values.dateTo) {
    messages.addError('report.daterange.order.error');
  }

  return messages.result();
}
