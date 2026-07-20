import {
  Search,
  TableOfContents,
  Currency,
  Types,
  Receipt,
  TextLinkAnalysis,
  Email,
  RecentlyViewed,
  Upload,
} from '@carbon/icons-react';

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
        icon: Currency,
      },
      {
        name: 'Sort code',
        path: ROUTES.SORT_CODE,
        icon: Types,
      },
    ],
  },
  {
    name: 'Reports',
    icon: TextLinkAnalysis,
    children: [
      {
        name: 'R06 - Invoice print out',
        path: ROUTES.R06_INVOICE_PRINT_OUT,
        icon: TextLinkAnalysis,
      },
      {
        name: 'R07 - Reconciliation',
        path: ROUTES.R07_RECONCILIATION,
        icon: TextLinkAnalysis,
      },
      {
        name: 'R08 - Invoice audit',
        path: ROUTES.R08_INVOICE_AUDIT,
        icon: TextLinkAnalysis,
      },
      {
        name: 'R10 - Species/sort/grade',
        path: ROUTES.R10_LOG_SALES_SPECIES,
        icon: TextLinkAnalysis,
      },
      {
        name: 'R11 - AMV',
        path: ROUTES.R11_AMV,
        icon: TextLinkAnalysis,
      },
      {
        name: 'R12 - CFPA extract',
        path: ROUTES.R12_CFPA_EXTRACT,
        icon: TextLinkAnalysis,
      },
      {
        name: 'R13 - Ad hoc',
        path: ROUTES.R13_AD_HOC,
        icon: TextLinkAnalysis,
      },
    ],
  },
  {
    name: 'Submissions',
    icon: RecentlyViewed,
    children: [
      {
        name: 'Upload Submission',
        path: ROUTES.UPLOAD_SUBMISSION,
        icon: Upload,
      },
      {
        name: 'Submission History',
        path: ROUTES.SUBMISSION_HISTORY,
        icon: RecentlyViewed,
      },
    ],
  },
];

export function useAppNavigation() {}
