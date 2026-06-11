import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

export function validateR11(dateFrom: Date | null, dateTo: Date | null, modelingCode: string): ValidationResult {
  const messages = new MessageCollector();

  if (!dateFrom) {
    messages.addError('report.startdate.required.error');
  }

  if (!modelingCode) {
    messages.addError('report.r11.reporttype.required.error');
  }

  if (dateFrom && dateTo && dateFrom > dateTo) {
    messages.addError('report.daterange.order.error');
  }

  return messages.result();
}
