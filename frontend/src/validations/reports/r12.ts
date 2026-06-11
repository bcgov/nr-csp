import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

export function validateR12(
  year: string,
  dateFrom: Date | null,
  dateTo: Date | null,
  timeFrame: string,
): ValidationResult {
  const messages = new MessageCollector();

  if (!year) {
    if (!dateFrom) {
      messages.addError('report.r12.startdate.required.error');
    }
    if (!dateTo && !timeFrame) {
      messages.addError('report.r12.enddate.or.timeframe.required.error');
    }
  }

  if (dateFrom && dateTo && dateFrom > dateTo) {
    messages.addError('report.daterange.order.error');
  }

  return messages.result();
}
