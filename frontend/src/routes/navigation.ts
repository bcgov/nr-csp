import { ChartColumn, Email, Receipt, Search, TableOfContents, Upload } from '@carbon/icons-react';

import { ROUTES } from './routePaths';

export const NAVIGATION_ITEMS = [
  {
    name: 'Search',
    path: ROUTES.SEARCH,
    icon: Search,
  },
  {
    name: 'Inbox',
    path: ROUTES.INBOX,
    icon: Email,
  },
  {
    name: 'Invoice',
    path: ROUTES.INVOICE,
    icon: Receipt,
  },
  {
    name: 'Table maintenance',
    icon: TableOfContents,
    children: [
      {
        name: 'Flat price conversion',
        path: ROUTES.FLAT_PRICE_CONVERSION,
      },
      {
        name: 'Sort code',
        path: ROUTES.SORT_CODE,
      },
    ],
  },
  {
    name: 'Reports',
    icon: ChartColumn,
    children: [
      {
        name: 'R06 - Invoice print out',
        path: ROUTES.R06_INVOICE_PRINT_OUT,
      },
      {
        name: 'R07 - Reconciliation',
        path: ROUTES.R07_RECONCILIATION,
      },
      {
        name: 'R08 - Invoice audit',
        path: ROUTES.R08_INVOICE_AUDIT,
      },
      {
        name: 'R10 - Species/sort/grade',
        path: ROUTES.R10_LOG_SALES_SPECIES,
      },
      {
        name: 'R11 - AMV',
        path: ROUTES.R11_AMV,
      },
      {
        name: 'R12 - CFPA extract',
        path: ROUTES.R12_CFPA_EXTRACT,
      },
      {
        name: 'R13 - Ad hoc',
        path: ROUTES.R13_AD_HOC,
      },
    ],
  },
  {
    name: 'Submissions',
    icon: Upload,
    children: [
      {
        name: 'Upload Submission',
        path: ROUTES.UPLOAD_SUBMISSION,
      },
      {
        name: 'Submission History',
        path: ROUTES.SUBMISSION_HISTORY,
      },
    ],
  },
];

export function useAppNavigation() {}
