import { clearPersistedTableState } from '@/hooks/usePersistentState';

import { AuthContext } from './AuthContext';
import { mockUser } from './mockUser';
import { setSignOutReason } from './signOutReason';

import type { AuthContextValue } from './types';
import type { ReactNode } from 'react';

export function MockAuthProvider({ children }: { children: ReactNode }) {
  const value: AuthContextValue = {
    user: mockUser(),
    isAuthenticated: true,
    isLoading: false,
    isSigningOut: false,
    signIn: async () => {},
    // Not `async`: the mock sign-out is entirely synchronous, so there is nothing to
    // await. Resolve the contract's Promise explicitly instead of wearing an `async`
    // keyword that promises asynchrony the body never has.
    signOut: (reason = 'user') => {
      setSignOutReason(reason);
      clearPersistedTableState();
      return Promise.resolve();
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
