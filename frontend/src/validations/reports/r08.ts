import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

type R08FormValues = {
  dateFrom: Date | null;
  dateTo: Date | null;
  submissionNumber: string;
  submissionYearMonth: Date | null;
  timeFrame: string;
};

export function validateR08(values: R08FormValues): ValidationResult {
  const messages = new MessageCollector();

  const hasFilter =
    values.dateFrom !== null || values.submissionNumber.trim() !== '' || values.submissionYearMonth !== null;
  if (!hasFilter) {
    messages.addError('report.r08.filter.required.error');
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
