import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

export function validateR06(
  dateFrom: Date | null,
  dateTo: Date | null,
  hasInvoiceNumbers: boolean,
  submissionId: string,
): ValidationResult {
  const messages = new MessageCollector();

  if (!hasInvoiceNumbers) {
    if (!dateFrom) {
      messages.addError('report.r06.startdate.required.error');
    }
    if (!dateTo) {
      messages.addError('report.r06.enddate.required.error');
    }
  }

  if (submissionId.trim() && !/^\d+$/.test(submissionId.trim())) {
    messages.addError('report.submissionnumber.numeric.error');
  }

  if (dateFrom && dateTo && dateFrom > dateTo) {
    messages.addError('report.daterange.order.error');
  }

  return messages.result();
}
