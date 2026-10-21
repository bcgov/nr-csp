import { render } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

import { AuthContext } from './AuthContext';
import { useAuth } from './useAuth';

function HookConsumer() {
  const auth = useAuth();
  return <div data-testid="result">{String(auth.isAuthenticated)}</div>;
}

describe('useAuth', () => {
  it('throws when used outside AuthProvider', () => {
    // React logs the error a component throws; keep that expected noise out of the output.
    const silenceReact = vi.spyOn(console, 'error').mockImplementation(() => {});
    try {
      expect(() => render(<HookConsumer />)).toThrow('useAuth must be used within AuthProvider');
    } finally {
      silenceReact.mockRestore();
    }
  });

  it('returns context value when inside provider', () => {
    const value = {
      user: { username: 'u', email: 'u@x.com', roles: [], privileges: [], forestClientNumbers: [] },
      isAuthenticated: true,
      isLoading: false,
      isSigningOut: false,
      signIn: async () => {},
      signOut: async () => {},
    };
    const { getByTestId } = render(
      <AuthContext.Provider value={value}>
        <HookConsumer />
      </AuthContext.Provider>,
    );
    expect(getByTestId('result').textContent).toBe('true');
  });
});
