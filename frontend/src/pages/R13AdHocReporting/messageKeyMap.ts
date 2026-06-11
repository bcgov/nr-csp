// Maps ValidationResult message keys to the R13 form field they belong to.
// Keys without a field mapping surface as form-level (banner) errors.
export const MESSAGE_KEY_TO_FIELD: Record<string, string> = {
  'report.r13.reportname.required.error': 'reportName',
  'report.startdate.required.error': 'startDate',
  'report.r13.enddate.or.timeframe.required.error': 'endDate',
  'report.daterange.order.error': 'startDate',
  'report.r13.showcolumns.minimum.error': 'showOptions',
};
