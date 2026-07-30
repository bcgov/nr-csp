import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

import { AuthContext, type AuthContextType } from './AuthContext';
import { AuthProvider } from './AuthProvider';

vi.mock('@/env', () => ({ env: { mockUser: true } }));

function renderWithConsumer() {
  let ctx: AuthContextType | null = null;
  render(
    <AuthProvider>
      <AuthContext.Consumer>
        {(value) => {
          ctx = value;
          return <div data-testid="child">child</div>;
        }}
      </AuthContext.Consumer>
    </AuthProvider>,
  );
  return () => ctx;
}

describe('AuthProvider (mock mode)', () => {
  it('renders children', () => {
    renderWithConsumer();
    expect(screen.getByTestId('child')).toBeInTheDocument();
  });

  it('provides auth context to consumers', () => {
    const getCtx = renderWithConsumer();
    expect(getCtx()).not.toBeNull();
  });

  it('exposes isAuthenticated and user from MockAuthProvider', () => {
    const getCtx = renderWithConsumer();
    const ctx = getCtx();
    expect(ctx?.isAuthenticated).toBe(true);
    expect(ctx?.user).toBeDefined();
    expect(ctx?.user?.username).toBe('mock-user');
  });

  it('exposes isLoading as false in mock mode', () => {
    const getCtx = renderWithConsumer();
    expect(getCtx()?.isLoading).toBe(false);
  });

  it('signOut resolves without error', async () => {
    const getCtx = renderWithConsumer();
    await expect(getCtx()?.signOut()).resolves.toBeUndefined();
  });

  it('signIn resolves without error', async () => {
    const getCtx = renderWithConsumer();
    await expect(getCtx()?.signIn()).resolves.toBeUndefined();
  });
});
