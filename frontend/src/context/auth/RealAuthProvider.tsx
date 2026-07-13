import { fetchAuthSession, signInWithRedirect, signOut } from 'aws-amplify/auth';
import { Hub } from 'aws-amplify/utils';
import { type ReactNode, useCallback, useEffect, useMemo, useState } from 'react';

import { AuthContext } from './AuthContext';
import { ROLES } from './permissions';
import type { Role } from './permissions';
import type { AuthContextValue, AuthUser } from './types';

/**
 * Match a Cognito group name against a role constant.
 * Handles plain names ("VIEWER") and FAM-prefixed names ("CSP_VIEWER", "NRS_CSP_VIEWER").
 */
function groupMatchesRole(group: string, role: string): boolean {
  const upper = group.toUpperCase();
  return upper === role || upper.endsWith(`_${role}`);
}

/**
 * Derive app-level privileges from raw Cognito group strings.
 * Returns a privilege for each known role constant whose group name appears in {@link groups}.
 */
function extractPrivileges(groups: string[]): Role[] {
  const privileges: Role[] = [];
  for (const role of ROLES) {
    if (groups.some((g) => groupMatchesRole(g, role))) {
      privileges.push(role);
    }
  }
  return privileges;
}

export function RealAuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSigningOut, setIsSigningOut] = useState(false);

  const loadUser = useCallback(async () => {
    try {
      const session = await fetchAuthSession();
      const payload = session.tokens?.idToken?.payload;
      if (payload) {
        const groups = (payload['cognito:groups'] as string[] | undefined) ?? [];
        const displayName =
          String(payload['name'] ?? '').trim() ||
          [payload['given_name'], payload['family_name']].filter(Boolean).join(' ') ||
          undefined;

        setUser({
          username: String(payload['cognito:username'] ?? payload.sub ?? ''),
          displayName: displayName || undefined,
          email: String(payload['email'] ?? ''),
          roles: groups,
          privileges: extractPrivileges(groups),
        });
      } else {
        setUser(null);
      }
    } catch {
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUser();
    const unsubscribe = Hub.listen('auth', ({ payload }) => {
      // tokenRefresh_failure means the session is dead (refresh token expired/revoked).
      // Without handling it the user stays isAuthenticated with a token that 401s every
      // request; treat it as a sign-out so the UI reflects reality.
      if (payload.event === 'signedIn' || payload.event === 'signedOut') {
        loadUser();
      } else if (payload.event === 'tokenRefresh_failure') {
        setUser(null);
      }
    });
    return unsubscribe;
  }, [loadUser]);

  const signIn = useCallback(() => {
    const idpName = window.amplifyConfig?.idpName ?? 'DEV-IDIR';
    return signInWithRedirect({ provider: { custom: idpName } });
  }, []);

  const doSignOut = useCallback(async () => {
    setIsSigningOut(true);
    try {
      await signOut();
    } finally {
      // Always clear local state and release the signing-out flag, even if the
      // Cognito sign-out call rejects — otherwise the app is stuck on LoadingScreen.
      setUser(null);
      setIsSigningOut(false);
    }
  }, []);

  const value: AuthContextValue = useMemo(
    () => ({
      user,
      isAuthenticated: user !== null,
      isLoading,
      isSigningOut,
      signIn,
      signOut: doSignOut,
    }),
    [user, isLoading, isSigningOut, signIn, doSignOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
