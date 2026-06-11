import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

import { AuthContext } from './AuthContext';
import { AuthProvider } from './AuthProvider';

vi.mock('@/env', () => ({ env: { mockUser: true } }));

function renderWithConsumer() {
  let ctx: ReturnType<typeof vi.fn> | null = null;
  render(
    <AuthProvider>
      <AuthContext.Consumer>
        {(value) => {
          ctx = value as never;
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
    const ctx = getCtx() as any;
    expect(ctx.isAuthenticated).toBe(true);
    expect(ctx.user).toBeDefined();
    expect(ctx.user.username).toBe('mock-user');
  });

  it('exposes isLoading as false in mock mode', () => {
    const getCtx = renderWithConsumer();
    expect((getCtx() as any).isLoading).toBe(false);
  });

  it('signOut resolves without error', async () => {
    const getCtx = renderWithConsumer();
    await act(async () => {
      await (getCtx() as any).signOut();
    });
  });

  it('signIn resolves without error', async () => {
    const getCtx = renderWithConsumer();
    await act(async () => {
      await (getCtx() as any).signIn();
    });
  });
});
