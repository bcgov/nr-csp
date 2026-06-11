import { usePermission } from './usePermission';

/**
 * Returns true if the current user has access to the given page.
 * The page string must match a top-level action constant in permissions.ts
 * (e.g. 'inbox', 'reports', 'tableMaintenance').
 */
export function usePageAccess(page: string): boolean {
  return usePermission(page);
}
