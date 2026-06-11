import { useAuth } from './useAuth';
import { ROLE_PERMISSIONS } from './permissions';

/**
 * Returns true if the current user's role grants the given action string.
 * Returns false when no user is authenticated.
 *
 * Expects user.privileges to contain role names matching ROLE_PERMISSIONS keys:
 * 'ADMIN', 'APPROVE', or 'VIEW' (set by RealAuthProvider/MockAuthProvider).
 */
export function usePermission(action: string): boolean {
  const { user } = useAuth();
  if (!user) return false;
  return user.privileges.some((role) => ROLE_PERMISSIONS[role]?.has(action));
}
