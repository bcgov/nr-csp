import { render } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import { AuthContext } from './AuthContext';
import { useAuth } from './useAuth';

function HookConsumer() {
  const auth = useAuth();
  return <div data-testid="result">{String(auth.isAuthenticated)}</div>;
}

describe('useAuth', () => {
  it('throws when used outside AuthProvider', () => {
    const consoleError = console.error;
    console.error = () => {};
    expect(() => render(<HookConsumer />)).toThrow('useAuth must be used within AuthProvider');
    console.error = consoleError;
  });

  it('returns context value when inside provider', () => {
    const value = {
      user: { username: 'u', email: 'u@x.com', roles: [] },
      isAuthenticated: true,
      isLoading: false,
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
