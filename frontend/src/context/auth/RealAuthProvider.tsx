import { fetchAuthSession, signInWithRedirect, signOut } from 'aws-amplify/auth';
import { Hub } from 'aws-amplify/utils';
import { type ReactNode, useEffect, useState } from 'react';

import { clearPersistedTableState } from '@/hooks/usePersistentState';

import { AuthContext } from './AuthContext';
import { ROLES } from './permissions';
import { onSessionExpired } from './sessionExpiredSignal';

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

        // The backend reads this same claim as the principal (JwtService#extractUsername),
        // so it's what lands in audit fields such as entryUserID.
        const idpUsernameClaim = payload['custom:idp_username'];
        const idirUsername = typeof idpUsernameClaim === 'string' ? idpUsernameClaim.trim() : '';

        setUser({
          username: String(payload['cognito:username'] ?? payload.sub ?? ''),
          idirUsername: idirUsername || undefined,
          displayName: displayName || undefined,
          email: String(payload['email'] ?? ''),
          roles: groups,
          privileges: extractPrivileges(groups),
        });
      } else {
        setUser(null);
      }
    } catch {
      // A thrown error here doesn't reliably mean the session is dead — it's usually a
      // transient fetchAuthSession() failure (network blip, momentary Cognito
      // unavailability), and treating every failure as a logout bounced users on those
      // blips. A genuinely dead session (refresh token expired/invalid) is instead caught
      // by the 401 response interceptor in request.ts, an unambiguous signal from the
      // backend itself — see sessionExpiredSignal. So leave `user` as it was.
    } finally {
      setIsLoading(false);
    }
  }

  async function performSignOut() {
    setIsSigningOut(true);
    await signOut();
    clearPersistedTableState();
    setUser(null);
  }

  useEffect(() => {
    loadUser();
    let handledSessionExpired = false;
    const unsubscribeHub = Hub.listen('auth', ({ payload }) => {
      if (payload.event === 'signedIn' || payload.event === 'signedOut') loadUser();
    });
    const unsubscribeSessionExpired = onSessionExpired(() => {
      if (handledSessionExpired) return;
      handledSessionExpired = true;
      void performSignOut();
    });
    return () => {
      unsubscribeHub();
      unsubscribeSessionExpired();
    };
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
    signOut: performSignOut,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
