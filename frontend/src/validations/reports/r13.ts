import type { R13ShowOptions } from '@/services/r13.service';
import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

export function countSelectedColumns(showOptions: R13ShowOptions): number {
  return Object.values(showOptions).filter(Boolean).length;
}

export function validateR13(
  reportName: string,
  startDate: Date | null,
  endDate: Date | null,
  timeFrame: string,
  showOptions: R13ShowOptions,
): ValidationResult {
  const messages = new MessageCollector();

  if (!reportName.trim()) {
    messages.addError('report.r13.reportname.required.error');
  }

  if (!startDate) {
    messages.addError('report.startdate.required.error');
  }

  // Either an explicit end date OR a time frame (which auto-calculates the end date) is required
  if (!endDate && !timeFrame) {
    messages.addError('report.r13.enddate.or.timeframe.required.error');
  }

  if (startDate && endDate && startDate > endDate) {
    messages.addError('report.daterange.order.error');
  }

  if (countSelectedColumns(showOptions) < 2) {
    messages.addError('report.r13.showcolumns.minimum.error');
  }

  return messages.result();
}
