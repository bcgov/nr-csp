// Maps ValidationResult message keys to the R13 form field they belong to.
// Keys without a field mapping surface as form-level (banner) errors.
//
// 'report.r13.showcolumns.minimum.error' is deliberately absent: the show-on-report
// checkboxes are spread across the whole page, so the error belongs in the banner
// at the top rather than beside any one of them (this is also what legacy did).
export const MESSAGE_KEY_TO_FIELD: Record<string, string> = {
  'report.r13.reportname.required.error': 'reportName',
  'report.startdate.required.error': 'startDate',
  'report.r13.enddate.or.timeframe.required.error': 'endDate',
  'report.daterange.order.error': 'startDate',
  // The Approval ID number field only accepts digits, so this can only come from
  // the backend validator — an API caller that skipped the form.
  'report.submissionnumber.numeric.error': 'approvalIdNumber',
};
