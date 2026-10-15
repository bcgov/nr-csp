import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ErrorResponse } from 'oidc-client-ts';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { IdleTimeoutWatcher } from './IdleTimeoutWatcher';
import { RealAuthProvider } from './RealAuthProvider';
import { emitSessionExpired } from './sessionExpiredSignal';
import { takeSignOutReason } from './signOutReason';
import { useAuth } from './useAuth';

import type * as OidcService from '@/service/oidc-service';
import type { User } from 'oidc-client-ts';

const mocks = vi.hoisted(() => ({
  restore: vi.fn(),
  renew: vi.fn(),
  callback: vi.fn(),
  signIn: vi.fn(),
  signOut: vi.fn(),
  addLoaded: vi.fn(),
  removeLoaded: vi.fn(),
  loadingHistory: [] as boolean[],
}));
vi.mock('@/env', () => ({
  env: {
    mockUser: false,
    idleTimeoutMs: 30 * 60_000,
    VITE_OIDC_ISSUER_URI: 'https://sso.test/realms/standard',
    VITE_OIDC_CLIENT_ID: 'csp-browser',
  },
}));
vi.mock('@/service/oidc-service', async (importOriginal) => ({
  AUTH_CALLBACK_PATH: '/authCallback',
  isOidcConfigured: true,
  isSessionEnded: (await importOriginal<typeof OidcService>()).isSessionEnded,
  getOidcUser: mocks.renew,
  restoreOidcUser: mocks.restore,
  completeOidcLogin: mocks.callback,
  startOidcLogin: mocks.signIn,
  endOidcSession: mocks.signOut,
  getOidcReturnPath: () => '/search',
  getUserManager: () => ({ events: { addUserLoaded: mocks.addLoaded, removeUserLoaded: mocks.removeLoaded } }),
}));
function access(claims: Record<string, unknown> = {}) {
  return (
    'header.' +
    btoa(
      JSON.stringify({
        iss: 'https://sso.test/realms/standard',
        azp: 'csp-browser',
        typ: 'Bearer',
        identity_provider: 'azureidir',
        idir_username: 'JSMITH',
        client_roles: ['CSP_VIEW'],
        ...claims,
      }),
    ) +
    '.signature'
  );
}
function Probe() {
  const auth = useAuth();
  mocks.loadingHistory.push(auth.isLoading);
  return (
    <>
      <span data-testid="authenticated">{String(auth.isAuthenticated)}</span>
      <span data-testid="signing-out">{String(auth.isSigningOut)}</span>
      <span data-testid="privileges">{auth.user?.privileges.join(',')}</span>
      <span data-testid="audit">{auth.user?.idirUsername}</span>
      <span data-testid="error">{auth.loginError}</span>
      <button onClick={() => void auth.signIn('business-bceid')}>Business BCeID</button>
      <button onClick={() => void auth.signOut()}>Sign out</button>
    </>
  );
}
function renderProvider() {
  return render(
    <RealAuthProvider>
      <Probe />
    </RealAuthProvider>,
  );
}
async function renderSignedIn() {
  const view = renderProvider();
  await waitFor(() => expect(screen.getByTestId('privileges')).toHaveTextContent('VIEW'));
  return view;
}
beforeEach(() => {
  vi.clearAllMocks();
  mocks.loadingHistory.length = 0;
  window.history.replaceState(null, '', '/');
  mocks.restore.mockResolvedValue({ access_token: access() });
  mocks.renew.mockResolvedValue({ access_token: access() });
  mocks.signOut.mockResolvedValue(undefined);
});
describe('CSS AuthProvider', () => {
  it('uses access-token roles and the actual directory audit username', async () => {
    render(
      <RealAuthProvider>
        <Probe />
      </RealAuthProvider>,
    );
    await waitFor(() => expect(screen.getByTestId('privileges')).toHaveTextContent('VIEW'));
    expect(screen.getByTestId('audit')).toHaveTextContent('JSMITH');
  });
  it('does not grant staff privileges to a Business BCeID account', async () => {
    mocks.restore.mockResolvedValue({
      access_token: access({
        identity_provider: 'bceidbusiness',
        bceid_username: 'BUSINESS',
        client_roles: ['CSP_ADMIN'],
      }),
    });
    render(
      <RealAuthProvider>
        <Probe />
      </RealAuthProvider>,
    );
    await waitFor(() => expect(screen.getByTestId('audit')).toHaveTextContent('BUSINESS'));
    expect(screen.getByTestId('privileges')).toBeEmptyDOMElement();
  });
  it('starts the selected provider and performs federated sign-out', async () => {
    render(
      <RealAuthProvider>
        <Probe />
      </RealAuthProvider>,
    );
    await waitFor(() => expect(screen.getByTestId('privileges')).toHaveTextContent('VIEW'));
    await userEvent.click(screen.getByRole('button', { name: 'Business BCeID' }));
    expect(mocks.signIn).toHaveBeenCalledWith('business-bceid');
    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(mocks.signOut).toHaveBeenCalledOnce();
  });
  it('completes a callback before rendering protected content', async () => {
    window.history.replaceState(null, '', '/authCallback?code=test&state=test');
    mocks.callback.mockResolvedValue({ access_token: access() });
    render(
      <RealAuthProvider>
        <Probe />
      </RealAuthProvider>,
    );
    await waitFor(() => expect(screen.getByTestId('audit')).toHaveTextContent('JSMITH'));
    expect(window.location.pathname).toBe('/search');
  });
  it('clears a failed callback and offers another sign-in', async () => {
    window.history.replaceState(null, '', '/authCallback?error=access_denied&state=test');
    mocks.callback.mockRejectedValue(new Error('expired state'));
    render(
      <RealAuthProvider>
        <Probe />
      </RealAuthProvider>,
    );
    await waitFor(() => expect(screen.getByTestId('error')).toHaveTextContent('Sign-in could not be completed'));
    expect(window.location.search).toBe('');
  });
  it('signs out once, as a timeout, however often the session-expired signal fires', async () => {
    await renderSignedIn();
    act(() => {
      emitSessionExpired();
      emitSessionExpired();
    });
    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('false'));
    emitSessionExpired();
    expect(mocks.signOut).toHaveBeenCalledOnce();
    expect(takeSignOutReason()).toBe('timeout');
  });
  it('clears persisted table state before handing the browser to SSO sign-out', async () => {
    window.sessionStorage.setItem('csp.table.search.v1.page', '2');
    window.sessionStorage.setItem('unrelated', 'keep');
    let tableStateAtSignOut: string | null = 'not signed out';
    mocks.signOut.mockImplementation(async () => {
      tableStateAtSignOut = window.sessionStorage.getItem('csp.table.search.v1.page');
    });
    await renderSignedIn();
    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(tableStateAtSignOut).toBeNull();
    expect(window.sessionStorage.getItem('unrelated')).toBe('keep');
    expect(takeSignOutReason()).toBe('user');
    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('false'));
  });
  it('ends the local session when SSO sign-out cannot start', async () => {
    mocks.signOut.mockRejectedValue(new Error('discovery unavailable'));
    await renderSignedIn();
    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('false'));
    expect(screen.getByTestId('signing-out')).toHaveTextContent('false');
  });
  it('applies a renewed access token without returning to the loading state', async () => {
    await renderSignedIn();
    const renewed = mocks.addLoaded.mock.calls[0][0] as (user: User) => void;
    const settledRenders = mocks.loadingHistory.length;
    act(() => renewed({ access_token: access({ client_roles: ['CSP_ADMIN'] }) } as User));
    expect(screen.getByTestId('privileges')).toHaveTextContent('ADMIN');
    expect(mocks.loadingHistory.slice(settledRenders)).not.toContain(true);
    expect(mocks.restore).toHaveBeenCalledOnce();
  });
  it('stops listening for renewals and session expiry on unmount', async () => {
    const { unmount } = await renderSignedIn();
    const renewed = mocks.addLoaded.mock.calls[0][0] as (user: User) => void;
    unmount();
    expect(mocks.removeLoaded).toHaveBeenCalledWith(renewed);
    emitSessionExpired();
    expect(mocks.signOut).not.toHaveBeenCalled();
  });
  describe('activity renewal', () => {
    async function renderWatched() {
      render(
        <RealAuthProvider>
          <IdleTimeoutWatcher />
          <Probe />
        </RealAuthProvider>,
      );
      await waitFor(() => expect(screen.getByTestId('privileges')).toHaveTextContent('VIEW'));
    }
    it('keeps the access token fresh while the user is active', async () => {
      await renderWatched();
      act(() => {
        window.dispatchEvent(new Event('keydown'));
      });
      expect(mocks.renew).toHaveBeenCalledOnce();
      expect(mocks.renew).toHaveBeenLastCalledWith();
    });
    it('keeps the session after a temporary activity renewal failure', async () => {
      mocks.renew.mockRejectedValueOnce(new TypeError('Failed to fetch'));
      await renderWatched();
      await act(async () => {
        window.dispatchEvent(new Event('keydown'));
        await new Promise((resolve) => setTimeout(resolve, 0));
      });
      expect(mocks.renew).toHaveBeenCalledOnce();
      expect(mocks.signOut).not.toHaveBeenCalled();
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });
    it('ends the session as a timeout when activity renewal finds it rejected by SSO', async () => {
      mocks.renew.mockRejectedValueOnce(new ErrorResponse({ error: 'invalid_grant' }));
      await renderWatched();
      act(() => {
        window.dispatchEvent(new Event('keydown'));
      });
      await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('false'));
      expect(mocks.signOut).toHaveBeenCalledOnce();
      expect(takeSignOutReason()).toBe('timeout');
    });
  });
});
