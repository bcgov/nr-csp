import {
  INBOX,
  INVOICE_DETAILS,
  PROD_FLAT_PRICE_CONV,
  PROD_SORT_CODE,
  R06,
  R07,
  R08,
  R10,
  R11,
  R12,
  R13,
  ROLE_PERMISSIONS,
  SEARCH,
  SUBMISSION_HISTORY,
  SUBMISSION_UPLOAD,
} from '@/context/auth/permissions';

import { ROUTES, type AppRoute } from './routePaths';

import type { AuthUser } from '@/context/auth/types';

type ProtectedPath = Exclude<AppRoute, typeof ROUTES.LANDING | typeof ROUTES.LOGOUT | typeof ROUTES.NOT_FOUND>;

/**
 * The page action (permissions.ts) each protected route needs; a route's
 * `/:param` children need the same one. Typed over every route, so a new route
 * cannot be added without saying who may open it. The API enforces the same
 * boundary; this keeps the UI from offering what the API would refuse.
 */
export const ROUTE_PAGES: Record<ProtectedPath, string> = {
  [ROUTES.SEARCH]: SEARCH,
  [ROUTES.SUBMISSION_HISTORY]: SUBMISSION_HISTORY,
  [ROUTES.UPLOAD_SUBMISSION]: SUBMISSION_UPLOAD,
  [ROUTES.INBOX]: INBOX,
  [ROUTES.INVOICE]: INVOICE_DETAILS,
  [ROUTES.SORT_CODE]: PROD_SORT_CODE,
  [ROUTES.FLAT_PRICE_CONVERSION]: PROD_FLAT_PRICE_CONV,
  [ROUTES.R06_INVOICE_PRINT_OUT]: R06,
  [ROUTES.R07_RECONCILIATION]: R07,
  [ROUTES.R08_INVOICE_AUDIT]: R08,
  [ROUTES.R10_LOG_SALES_SPECIES]: R10,
  [ROUTES.R11_AMV]: R11,
  [ROUTES.R12_CFPA_EXTRACT]: R12,
  [ROUTES.R13_AD_HOC]: R13,
};

/** Whether the user's privileges grant a page action; usePermission outside React. */
export const canAccess = (user: AuthUser | null | undefined, page: string): boolean =>
  !!user && user.privileges.some((role) => ROLE_PERMISSIONS[role]?.has(page));

/**
 * Where a signed-in user lands: Upload Submission for an industry submitter,
 * Search for everyone else (a user with no role sees No CSP access there).
 */
export const homeRouteFor = (user: AuthUser | null | undefined): AppRoute =>
  canAccess(user, SUBMISSION_UPLOAD) && !canAccess(user, SEARCH) ? ROUTES.UPLOAD_SUBMISSION : ROUTES.SEARCH;

/**
 * Whether Business BCeID users can open this path, so a signed-out visitor must
 * choose a provider on the welcome screen rather than be sent to IDIR.
 */
export const isIndustryPath = (pathname: string): boolean =>
  (Object.entries(ROUTE_PAGES) as [ProtectedPath, string][]).some(
    ([path, page]) => ROLE_PERMISSIONS.SUBMITTER.has(page) && (pathname === path || pathname.startsWith(`${path}/`)),
  );

type NavEntry = { path?: string; children?: NavEntry[] };

/**
 * The navigation entries this user may open (the same check ProtectedRoute
 * makes), and every group that still has one. A link to a path without a
 * ROUTE_PAGES entry is shown to nobody.
 */
export function visibleNavItems<T extends NavEntry>(items: readonly T[], user: AuthUser | null | undefined): T[] {
  return items.flatMap((item): T[] => {
    if (item.children) {
      const children = visibleNavItems(item.children, user);
      return children.length ? [{ ...item, children }] : [];
    }
    const page = item.path ? (ROUTE_PAGES as Record<string, string | undefined>)[item.path] : undefined;
    return page && canAccess(user, page) ? [item] : [];
  });
}
