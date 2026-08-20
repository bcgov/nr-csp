import { clearPersistedTableState } from '@/hooks/usePersistentState';

import { AuthContext } from './AuthContext';
import { ROLES } from './permissions';

import type { Role } from './permissions';
import type { AuthContextValue, IdpProvider } from './types';
import type { ReactNode } from 'react';

export const MOCK_ROLE_KEY = 'csp.mockRole';
export const MOCK_IDP_KEY = 'csp.mockIdp';

const IDP_PROVIDERS: readonly IdpProvider[] = ['IDIR', 'BCEID'];

// Dev-only mock provider; these helpers live alongside the component by design.
// eslint-disable-next-line react-refresh/only-export-components
export function getStoredRole(): Role {
  const stored = localStorage.getItem(MOCK_ROLE_KEY);
  return (ROLES as readonly string[]).includes(stored ?? '') ? (stored as Role) : 'ADMIN';
}

// eslint-disable-next-line react-refresh/only-export-components
export function getStoredIdp(): IdpProvider {
  const stored = localStorage.getItem(MOCK_IDP_KEY);
  return (IDP_PROVIDERS as readonly string[]).includes(stored ?? '') ? (stored as IdpProvider) : 'IDIR';
}

export function MockAuthProvider({ children }: { children: ReactNode }) {
  const role = getStoredRole();
  const idpProvider = getStoredIdp();

  const value: AuthContextValue = {
    user: {
      username: 'mock-user',
      idirUsername: 'mock-user',
      displayName: 'Mock User',
      email: 'mock@example.com',
      roles: [`CSP_${role}`],
      privileges: [role],
      idpProvider,
    },
    isAuthenticated: true,
    isLoading: false,
    isSigningOut: false,
    signIn: async () => {},
    signOut: async () => {
      clearPersistedTableState();
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
