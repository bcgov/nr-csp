import type { ReactNode } from 'react';

import { AuthContext } from './AuthContext';
import { ROLES } from './permissions';
import type { Role } from './permissions';
import type { AuthContextValue } from './types';

export const MOCK_ROLE_KEY = 'csp.mockRole';

export function getStoredRole(): Role {
  const stored = localStorage.getItem(MOCK_ROLE_KEY);
  return (ROLES as readonly string[]).includes(stored ?? '') ? (stored as Role) : 'ADMIN';
}

export function MockAuthProvider({ children }: { children: ReactNode }) {
  const role = getStoredRole();

  const value: AuthContextValue = {
    user: {
      username: 'mock-user',
      displayName: 'Mock User',
      email: 'mock@example.com',
      roles: [`CSP_${role}`],
      privileges: [role],
    },
    isAuthenticated: true,
    isLoading: false,
    isSigningOut: false,
    signIn: async () => {},
    signOut: async () => {},
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
