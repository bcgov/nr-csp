import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

type R07FormValues = {
  reportingYearMonth: Date | null;
  dateFrom: Date | null;
  dateTo: Date | null;
  sellerNumber: string;
  buyerNumber: string;
  submissionNumber: string;
  timeFrame: string;
};

export function validateR07(values: R07FormValues): ValidationResult {
  const messages = new MessageCollector();

  const hasFilter =
    values.reportingYearMonth !== null ||
    values.dateFrom !== null ||
    values.sellerNumber.trim() !== '' ||
    values.buyerNumber.trim() !== '' ||
    values.submissionNumber.trim() !== '';
  if (!hasFilter) {
    messages.addError('report.r07.filter.required.error');
  }

  if (values.submissionNumber.trim() && !/^\d+$/.test(values.submissionNumber.trim())) {
    messages.addError('report.submissionnumber.numeric.error');
  }

  if (values.timeFrame.trim() && !/^\d+$/.test(values.timeFrame.trim())) {
    messages.addError('report.timeframe.numeric.error');
  }

  if (values.dateFrom && values.dateTo && values.dateFrom > values.dateTo) {
    messages.addError('report.daterange.order.error');
  }

  return messages.result();
}
