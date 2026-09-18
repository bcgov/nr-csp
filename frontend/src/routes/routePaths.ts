export const ROUTES = {
  LANDING: '/',
  LOGOUT: '/logout',
  SEARCH: '/search',
  SUBMISSION_HISTORY: '/submission-history',
  UPLOAD_SUBMISSION: '/upload-submission',
  INBOX: '/inbox',
  INVOICE: '/invoice',
  SORT_CODE: '/sort-code',
  FLAT_PRICE_CONVERSION: '/table-maintenance/flat-price-conversion',
  R06_INVOICE_PRINT_OUT: '/reports/r06-invoice-print-out',
  R07_RECONCILIATION: '/reports/r07-reconciliation',
  R08_INVOICE_AUDIT: '/reports/r08-invoice-audit',
  R10_LOG_SALES_SPECIES: '/reports/r10-log-sales-species',
  R11_AMV: '/reports/r11-amv',
  R12_CFPA_EXTRACT: '/reports/r12-cfpa-extract',
  R13_AD_HOC: '/reports/r13-ad-hoc',
  NOT_FOUND: '*',
} as const;

export type AppRoute = (typeof ROUTES)[keyof typeof ROUTES];
