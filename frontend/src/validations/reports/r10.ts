import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

export function validateR10(dateFrom: Date | null, dateTo: Date | null, timeFrame: string): ValidationResult {
  const messages = new MessageCollector();

  if (!dateFrom) {
    messages.addError('report.startdate.required.error');
  }

  if (!dateTo && !timeFrame) {
    messages.addError('report.r10.enddate.or.timeframe.required.error');
  }

  if (dateFrom && dateTo && dateFrom > dateTo) {
    messages.addError('report.daterange.order.error');
  }

  return messages.result();
}
