import { env } from '@/env';
import { useIdleTimeout } from '@/hooks/useIdleTimeout';

import { useAuth } from './useAuth';

/**
 * Signs the user out after a period of inactivity, independent of the
 * underlying Cognito token TTL. Disabled in mock-user dev mode, since
 * MockAuthProvider's signOut() doesn't actually deauthenticate.
 */
export function IdleTimeoutWatcher() {
  const { isAuthenticated, signOut } = useAuth();

  useIdleTimeout({
    enabled: isAuthenticated && !env.mockUser,
    timeoutMs: env.idleTimeoutMs,
    onTimeout: () => void signOut(),
  });

  return null;
}
