// General, cross-page validation messages. Grouped so they can be reused by any
// report or form rather than being duplicated per page.

export const DATE_MESSAGES: Record<string, string> = {
  'report.daterange.order.error': 'Start date must not be after end date.',
  'report.startdate.required.error': 'Start date is required.',
  'report.timeframe.numeric.error': 'Time frame must be numeric.',
};

export const NUMBER_MESSAGES: Record<string, string> = {
  'report.submissionnumber.numeric.error': 'Submission number must be numeric.',
  'report.submissionnumber.notfound.error': 'No submission found for submission number: {0}',
};

export const CLIENT_MESSAGES: Record<string, string> = {
  'report.client.number.notfound.error': 'No client found for {0} number: {1}',
  'report.client.name.notfound.error': 'No client found for {0} name: {1}',
  'report.client.name.nomatch.error': 'The Client Name ({0}) cannot be found for {1}.',
  'report.client.noselection.error': 'Select a valid {0} client from the suggestion list.',
};

// Page-specific messages that only apply to a single report.
export const REPORT_MESSAGES: Record<string, string> = {
  'report.r06.startdate.required.error': 'Start date is required when no invoice numbers are provided.',
  'report.r06.enddate.required.error': 'End date is required when no invoice numbers are provided.',
  'report.r07.filter.required.error':
    'At least one of the following is required: reporting month, date range, seller/buyer client, or submission number.',
  'report.r08.filter.required.error':
    'At least one of the following is required: date range, submission number, or submission year-month.',
  'report.r10.enddate.or.timeframe.required.error': 'End date or time frame is required.',
  'report.r11.reporttype.required.error': 'Report type is required.',
  'report.r12.startdate.required.error': 'Start date is required when no report year is provided.',
  'report.r12.enddate.or.timeframe.required.error':
    'End date or time frame is required when no report year is provided.',
  'report.r13.reportname.required.error': 'Report name is required.',
  'report.r13.enddate.or.timeframe.required.error': 'End date or time frame is required.',
  'report.r13.showcolumns.minimum.error': 'At least 2 columns must be selected to show on report.',
};

// Invoice header — client-side structural checks that mirror the backend
// request-DTO constraints (required / pattern), surfaced inline as the user types.
export const INVOICE_MESSAGES: Record<string, string> = {
  'invoice.client.invnumber.required.error': 'Invoice number is required.',
  'invoice.client.invnumber.pattern.error': 'Invoice number may only contain uppercase letters, digits and hyphens.',
  'invoice.client.invdate.required.error': 'Invoice date is required.',
  'invoice.client.invtype.required.error': 'Invoice type is required.',
  'invoice.client.invtype.pattern.error': 'Invoice type must be uppercase letters only.',
  'invoice.client.submittedby.required.error': 'Submitted by is required.',
  'invoice.client.submittedby.pattern.error': 'Submitted by must be Buyer or Seller.',
  'invoice.client.submitterlocation.required.error': 'Location is required.',
  'invoice.client.submitterlocation.pattern.error': 'Location must be exactly 2 digits.',
  'invoice.client.otherlocation.pattern.error': 'Location must be exactly 2 digits.',
};

// Invoice line item (Add New Line Item) — client-side structural checks on the
// free-text numeric inputs.
export const INVOICE_LINE_ITEM_MESSAGES: Record<string, string> = {
  'invoice.client.pieces.integer.error': 'Number of pieces must be a whole number.',
  'invoice.client.pieces.positive.error': 'Number of pieces must be greater than zero.',
  'invoice.client.volume.numeric.error': 'Volume must be a valid number.',
  'invoice.client.volume.negative.error': 'Volume cannot be negative.',
  'invoice.client.price.numeric.error': 'Price must be a valid number.',
  'invoice.client.price.negative.error': 'Price cannot be negative.',
};

// CSP submission metadata (upload form) — client-side structural checks on the
// editable header fields, surfaced inline the same way the report filters are.
export const SUBMISSION_MESSAGES: Record<string, string> = {
  'submission.client.clientnumber.required.error': 'Submission client number is required.',
  'submission.client.clientnumber.pattern.error': 'Submission client number must be exactly 8 digits.',
  'submission.client.locationcode.required.error': 'Submission client location code is required.',
  'submission.client.locationcode.pattern.error': 'Submission client location code must be exactly 2 digits.',
  'submission.client.monthcomplete.required.error': 'Month complete is required.',
  'submission.client.monthcomplete.pattern.error': 'Month complete must be Y or N.',
  'submission.client.sellersubmission.required.error': 'Seller submission is required.',
  'submission.client.sellersubmission.pattern.error': 'Seller submission must be Y or N.',
};

export const ALL_MESSAGES: Record<string, string> = {
  ...DATE_MESSAGES,
  ...NUMBER_MESSAGES,
  ...CLIENT_MESSAGES,
  ...REPORT_MESSAGES,
  ...INVOICE_MESSAGES,
  ...INVOICE_LINE_ITEM_MESSAGES,
  ...SUBMISSION_MESSAGES,
};
