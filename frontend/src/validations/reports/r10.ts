import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

type R10FormValues = {
  dateFrom: Date | null;
  dateTo: Date | null;
  timeFrame: string;
  sellerName: string;
  sellerNumber: string;
  buyerName: string;
  buyerNumber: string;
};

export function validateR10(values: R10FormValues): ValidationResult {
  const messages = new MessageCollector();

  if (!values.dateFrom) {
    messages.addError('report.startdate.required.error');
  }

  if (!values.dateTo && !values.timeFrame) {
    messages.addError('report.r10.enddate.or.timeframe.required.error');
  }

  if (values.timeFrame.trim() && !/^\d+$/.test(values.timeFrame.trim())) {
    messages.addError('report.timeframe.numeric.error');
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
