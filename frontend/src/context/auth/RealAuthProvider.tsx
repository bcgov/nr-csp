import { fetchAuthSession, signInWithRedirect, signOut } from 'aws-amplify/auth';
import { Hub } from 'aws-amplify/utils';
import { type ReactNode, useEffect, useState } from 'react';

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

  async function loadUser() {
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
  }

  useEffect(() => {
    loadUser();
    const unsubscribe = Hub.listen('auth', ({ payload }) => {
      if (payload.event === 'signedIn' || payload.event === 'signedOut') loadUser();
    });
    return unsubscribe;
  }, []);

  const value: AuthContextValue = {
    user,
    isAuthenticated: user !== null,
    isLoading,
    isSigningOut,
    signIn: () => {
      const idpName = window.amplifyConfig?.idpName ?? 'DEV-IDIR';
      return signInWithRedirect({ provider: { custom: idpName } });
    },
    signOut: async () => {
      setIsSigningOut(true);
      await signOut();
      setUser(null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
