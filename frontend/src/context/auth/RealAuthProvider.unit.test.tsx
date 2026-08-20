import { act, render, screen, waitFor } from '@testing-library/react';
import { fetchAuthSession, signInWithRedirect, signOut } from 'aws-amplify/auth';
import { Hub } from 'aws-amplify/utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { IDIR_REALM_LOGOUT_GRACE_MS } from '@/utils/logoutChain';

import { AuthContext } from './AuthContext';
import { RealAuthProvider } from './RealAuthProvider';
import { emitSessionExpired } from './sessionExpiredSignal';

import type { AuthContextValue } from './types';

vi.mock('aws-amplify/auth', () => ({
  fetchAuthSession: vi.fn(),
  signInWithRedirect: vi.fn(),
  signOut: vi.fn(),
}));

vi.mock('aws-amplify/utils', () => ({
  Hub: { listen: vi.fn() },
}));

const mockFetchAuthSession = vi.mocked(fetchAuthSession);
const mockSignInWithRedirect = vi.mocked(signInWithRedirect);
const mockSignOut = vi.mocked(signOut);
const mockHubListen = vi.mocked(Hub.listen);

type Session = Awaited<ReturnType<typeof fetchAuthSession>>;
type HubAuthListener = (data: { payload: { event: string } }) => void;

function sessionWith(payload: Record<string, unknown>): Session {
  return { tokens: { idToken: { payload } } } as unknown as Session;
}

function emptySession(): Session {
  return {} as unknown as Session;
}

/** Returns the 'auth' listener the provider registered with Hub.listen. */
function getHubListener(): HubAuthListener {
  expect(mockHubListen).toHaveBeenCalledWith('auth', expect.any(Function));
  const call = mockHubListen.mock.calls[0];
  return call[1] as unknown as HubAuthListener;
}

function renderProvider() {
  let ctx: AuthContextValue | null = null;
  const { unmount } = render(
    <RealAuthProvider>
      <AuthContext.Consumer>
        {(value) => {
          ctx = value;
          return <div data-testid="child">child</div>;
        }}
      </AuthContext.Consumer>
    </RealAuthProvider>,
  );
  return { getCtx: () => ctx, unmount };
}

async function renderAndSettle() {
  const utils = renderProvider();
  await waitFor(() => expect(utils.getCtx()?.isLoading).toBe(false));
  return utils;
}

describe('RealAuthProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHubListen.mockReturnValue(() => {});
    mockSignInWithRedirect.mockResolvedValue(undefined);
    mockSignOut.mockResolvedValue(undefined as never);
  });

  afterEach(() => {
    window.amplifyConfig = undefined;
    window.sessionStorage.clear();
  });

  it('renders children and starts in the loading state', () => {
    mockFetchAuthSession.mockReturnValue(new Promise(() => {}));
    const { getCtx } = renderProvider();

    expect(screen.getByTestId('child')).toBeInTheDocument();
    expect(getCtx()?.isLoading).toBe(true);
    expect(getCtx()?.user).toBeNull();
    expect(getCtx()?.isAuthenticated).toBe(false);
    expect(getCtx()?.isSigningOut).toBe(false);
  });

  it('extracts the user from an authenticated session with FAM-prefixed groups', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({
        'cognito:username': 'jdoe@idir',
        'cognito:groups': ['CSP_ADMIN', 'NRS_CSP_VIEW'],
        name: 'Jane Doe',
        email: 'jane.doe@gov.bc.ca',
      }),
    );

    const { getCtx } = await renderAndSettle();
    const user = getCtx()?.user;

    expect(getCtx()?.isAuthenticated).toBe(true);
    expect(user?.username).toBe('jdoe@idir');
    expect(user?.displayName).toBe('Jane Doe');
    expect(user?.email).toBe('jane.doe@gov.bc.ca');
    expect(user?.roles).toEqual(['CSP_ADMIN', 'NRS_CSP_VIEW']);
    expect(user?.privileges).toEqual(['ADMIN', 'VIEW']);
    expect(user?.idpProvider).toBe('IDIR');
  });

  it('defaults idpProvider to IDIR when the custom:idp_name claim is absent', async () => {
    mockFetchAuthSession.mockResolvedValue(sessionWith({ 'cognito:username': 'u', email: 'u@x' }));

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.idpProvider).toBe('IDIR');
  });

  it('sets idpProvider to IDIR when the custom:idp_name claim is idir', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({ 'cognito:username': 'u', email: 'u@x', 'custom:idp_name': 'idir' }),
    );

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.idpProvider).toBe('IDIR');
  });

  it('sets idpProvider to BCEID when the custom:idp_name claim starts with bceid', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({ 'cognito:username': 'u', email: 'u@x', 'custom:idp_name': 'bceidbusiness' }),
    );

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.idpProvider).toBe('BCEID');
  });

  it('matches the custom:idp_name claim case-insensitively', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({ 'cognito:username': 'u', email: 'u@x', 'custom:idp_name': 'BCEIDBUSINESS' }),
    );

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.idpProvider).toBe('BCEID');
  });

  it('matches plain group names case-insensitively', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({
        'cognito:username': 'u',
        'cognito:groups': ['approve'],
        email: 'u@example.com',
      }),
    );

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.privileges).toEqual(['APPROVE']);
  });

  it('ignores group names that only contain a role as a substring', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({
        'cognito:username': 'u',
        'cognito:groups': ['VIEWER', 'CSP_ADMINISTRATOR', 'SOMETHING_ELSE'],
        email: 'u@example.com',
      }),
    );

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.privileges).toEqual([]);
    expect(getCtx()?.user?.roles).toEqual(['VIEWER', 'CSP_ADMINISTRATOR', 'SOMETHING_ELSE']);
  });

  it('defaults to no roles when the token has no cognito:groups claim', async () => {
    mockFetchAuthSession.mockResolvedValue(sessionWith({ 'cognito:username': 'u', email: 'u@example.com' }));

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.roles).toEqual([]);
    expect(getCtx()?.user?.privileges).toEqual([]);
  });

  it('builds displayName from given_name and family_name when name is blank', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({
        'cognito:username': 'u',
        name: '   ',
        given_name: 'Jane',
        family_name: 'Doe',
        email: 'u@example.com',
      }),
    );

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.displayName).toBe('Jane Doe');
  });

  it('uses only the available name part when the other is missing', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({ 'cognito:username': 'u', given_name: 'Jane', email: 'u@example.com' }),
    );

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.displayName).toBe('Jane');
  });

  it('leaves displayName undefined when no name claims are present', async () => {
    mockFetchAuthSession.mockResolvedValue(sessionWith({ 'cognito:username': 'u', email: 'u@example.com' }));

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.displayName).toBeUndefined();
  });

  it('falls back to the sub claim for username, and to empty strings otherwise', async () => {
    mockFetchAuthSession.mockResolvedValue(sessionWith({ sub: 'sub-123' }));

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.username).toBe('sub-123');
    expect(getCtx()?.user?.email).toBe('');
  });

  it('falls back to an empty username when neither cognito:username nor sub exists', async () => {
    mockFetchAuthSession.mockResolvedValue(sessionWith({ email: 'u@example.com' }));

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user?.username).toBe('');
  });

  it('treats a session without an id-token payload as unauthenticated', async () => {
    mockFetchAuthSession.mockResolvedValue(emptySession());

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user).toBeNull();
    expect(getCtx()?.isAuthenticated).toBe(false);
  });

  it('treats a fetchAuthSession failure as unauthenticated', async () => {
    mockFetchAuthSession.mockRejectedValue(new Error('no session'));

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.user).toBeNull();
    expect(getCtx()?.isAuthenticated).toBe(false);
    expect(getCtx()?.isLoading).toBe(false);
  });

  it('keeps the previously authenticated user when a later fetchAuthSession call fails unexpectedly', async () => {
    mockFetchAuthSession.mockResolvedValueOnce(
      sessionWith({ 'cognito:username': 'u', 'cognito:groups': ['CSP_ADMIN'], email: 'u@x' }),
    );
    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.isAuthenticated).toBe(true);

    mockFetchAuthSession.mockRejectedValueOnce(new Error('network blip'));
    await act(async () => {
      getHubListener()({ payload: { event: 'signedIn' } });
    });

    expect(getCtx()?.isAuthenticated).toBe(true);
    expect(getCtx()?.user?.username).toBe('u');
  });

  it('signs the user out when the session-expired signal fires', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({ 'cognito:username': 'u', 'cognito:groups': ['CSP_ADMIN'], email: 'u@x' }),
    );
    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.isAuthenticated).toBe(true);

    await act(async () => {
      emitSessionExpired();
    });

    expect(mockSignOut).toHaveBeenCalledTimes(1);
    expect(getCtx()?.isAuthenticated).toBe(false);
  });

  it('only signs out once for multiple rapid session-expired signals', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({ 'cognito:username': 'u', 'cognito:groups': ['CSP_ADMIN'], email: 'u@x' }),
    );
    await renderAndSettle();

    await act(async () => {
      emitSessionExpired();
      emitSessionExpired();
    });

    expect(mockSignOut).toHaveBeenCalledTimes(1);
  });

  it('reloads the user when a Hub signedIn event fires', async () => {
    mockFetchAuthSession.mockResolvedValueOnce(emptySession());
    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.isAuthenticated).toBe(false);

    mockFetchAuthSession.mockResolvedValueOnce(
      sessionWith({ 'cognito:username': 'u', 'cognito:groups': ['CSP_VIEW'], email: 'u@x' }),
    );
    await act(async () => {
      getHubListener()({ payload: { event: 'signedIn' } });
    });

    expect(getCtx()?.isAuthenticated).toBe(true);
    expect(getCtx()?.user?.privileges).toEqual(['VIEW']);
  });

  it('reloads the user when a Hub signedOut event fires', async () => {
    mockFetchAuthSession.mockResolvedValueOnce(sessionWith({ 'cognito:username': 'u', email: 'u@x' }));
    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.isAuthenticated).toBe(true);

    mockFetchAuthSession.mockResolvedValueOnce(emptySession());
    await act(async () => {
      getHubListener()({ payload: { event: 'signedOut' } });
    });

    expect(getCtx()?.isAuthenticated).toBe(false);
  });

  it('ignores unrelated Hub auth events', async () => {
    mockFetchAuthSession.mockResolvedValue(emptySession());
    await renderAndSettle();
    expect(mockFetchAuthSession).toHaveBeenCalledTimes(1);

    await act(async () => {
      getHubListener()({ payload: { event: 'tokenRefresh' } });
    });

    expect(mockFetchAuthSession).toHaveBeenCalledTimes(1);
  });

  it('unsubscribes the Hub listener on unmount', async () => {
    const unsubscribe = vi.fn();
    mockHubListen.mockReturnValue(unsubscribe);
    mockFetchAuthSession.mockResolvedValue(emptySession());

    const { unmount } = await renderAndSettle();
    unmount();

    expect(unsubscribe).toHaveBeenCalledTimes(1);
  });

  it('signIn(IDIR) redirects with the idpName from window.amplifyConfig', async () => {
    window.amplifyConfig = {
      appEnv: 'test',
      idpName: 'TEST-IDIR',
      idpNameBceid: 'TEST-BCEID',
      region: 'ca-central-1',
      userPoolId: 'pool',
      userPoolClientId: 'client',
      cognitoDomain: 'auth.example.com',
      redirectSignIn: 'https://example.com/',
      redirectSignOut: 'https://example.com/logout',
    };
    mockFetchAuthSession.mockResolvedValue(emptySession());

    const { getCtx } = await renderAndSettle();
    await getCtx()?.signIn('IDIR');

    expect(mockSignInWithRedirect).toHaveBeenCalledWith({ provider: { custom: 'TEST-IDIR' } });
  });

  it('signIn(BCEID) redirects with the idpNameBceid from window.amplifyConfig', async () => {
    window.amplifyConfig = {
      appEnv: 'test',
      idpName: 'TEST-IDIR',
      idpNameBceid: 'TEST-BCEID',
      region: 'ca-central-1',
      userPoolId: 'pool',
      userPoolClientId: 'client',
      cognitoDomain: 'auth.example.com',
      redirectSignIn: 'https://example.com/',
      redirectSignOut: 'https://example.com/logout',
    };
    mockFetchAuthSession.mockResolvedValue(emptySession());

    const { getCtx } = await renderAndSettle();
    await getCtx()?.signIn('BCEID');

    expect(mockSignInWithRedirect).toHaveBeenCalledWith({ provider: { custom: 'TEST-BCEID' } });
  });

  it('signIn(IDIR) falls back to DEV-IDIR when window.amplifyConfig is not set', async () => {
    window.amplifyConfig = undefined;
    mockFetchAuthSession.mockResolvedValue(emptySession());

    const { getCtx } = await renderAndSettle();
    await getCtx()?.signIn('IDIR');

    expect(mockSignInWithRedirect).toHaveBeenCalledWith({ provider: { custom: 'DEV-IDIR' } });
  });

  it('signIn(BCEID) falls back to DEV-BCEID when window.amplifyConfig is not set', async () => {
    window.amplifyConfig = undefined;
    mockFetchAuthSession.mockResolvedValue(emptySession());

    const { getCtx } = await renderAndSettle();
    await getCtx()?.signIn('BCEID');

    expect(mockSignInWithRedirect).toHaveBeenCalledWith({ provider: { custom: 'DEV-BCEID' } });
  });

  it('signOut flips isSigningOut, calls Amplify signOut, and clears the user', async () => {
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({ 'cognito:username': 'u', 'cognito:groups': ['CSP_ADMIN'], email: 'u@x' }),
    );

    const { getCtx } = await renderAndSettle();
    expect(getCtx()?.isAuthenticated).toBe(true);

    await act(async () => {
      await getCtx()?.signOut();
    });

    expect(mockSignOut).toHaveBeenCalledTimes(1);
    expect(getCtx()?.isSigningOut).toBe(true);
    expect(getCtx()?.user).toBeNull();
    expect(getCtx()?.isAuthenticated).toBe(false);
  });

  it('clears persisted table state on signOut', async () => {
    window.sessionStorage.setItem('csp.table.search.v1.page', '3');
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({ 'cognito:username': 'u', 'cognito:groups': ['CSP_ADMIN'], email: 'u@x' }),
    );

    const { getCtx } = await renderAndSettle();

    await act(async () => {
      await getCtx()?.signOut();
    });

    expect(window.sessionStorage.getItem('csp.table.search.v1.page')).toBeNull();
  });

  describe('federated logout chain', () => {
    const chainConfig = {
      appEnv: 'test',
      idpName: 'TEST-IDIR',
      region: 'ca-central-1',
      userPoolId: 'ca-central-1_test',
      userPoolClientId: 'cognito-client-id',
      cognitoDomain: 'pool.auth.ca-central-1.amazoncognito.com',
      redirectSignIn: 'https://app.example.com',
      redirectSignOut: 'https://app.example.com/logout',
      logoutSiteminderUrl: 'https://logontest7.gov.bc.ca/clp-cgi/logoff.cgi',
      logoutKeycloakUrl: 'https://test.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/logout',
      logoutKeycloakClientId: 'fsa-cognito-idir-dev-4088',
    };

    const originalLocation = window.location;
    let assignMock: ReturnType<typeof vi.fn>;

    beforeEach(() => {
      assignMock = vi.fn();
      Object.defineProperty(window, 'location', {
        value: { origin: originalLocation.origin, href: originalLocation.href, assign: assignMock },
        writable: true,
        configurable: true,
      });
    });

    afterEach(() => {
      Object.defineProperty(window, 'location', {
        value: originalLocation,
        writable: true,
        configurable: true,
      });
      window.localStorage.clear();
      vi.restoreAllMocks();
    });

    it('navigates the SiteMinder chain and bypasses Amplify signOut when configured', async () => {
      // Popup blocked → sign-out proceeds immediately, without the grace wait.
      vi.spyOn(window, 'open').mockReturnValue(null);
      window.amplifyConfig = chainConfig;
      window.localStorage.setItem('CognitoIdentityServiceProvider.cognito-client-id.user.idToken', 'token');
      window.sessionStorage.setItem('csp.table.search.v1.page', '3');
      mockFetchAuthSession.mockResolvedValue(
        sessionWith({ 'cognito:username': 'u', 'cognito:groups': ['CSP_ADMIN'], email: 'u@x' }),
      );

      const { getCtx } = await renderAndSettle();

      await act(async () => {
        await getCtx()?.signOut();
      });

      expect(assignMock).toHaveBeenCalledWith(
        expect.stringMatching(/^https:\/\/logontest7\.gov\.bc\.ca\/clp-cgi\/logoff\.cgi\?retnow=1&returl=/),
      );
      expect(mockSignOut).not.toHaveBeenCalled();
      // Amplify tokens and persisted UI state are cleared before navigation.
      expect(window.localStorage.getItem('CognitoIdentityServiceProvider.cognito-client-id.user.idToken')).toBeNull();
      expect(window.sessionStorage.getItem('csp.table.search.v1.page')).toBeNull();
    });

    it('logs out the idir broker realm via a popup before navigating the chain', async () => {
      const popup = { close: vi.fn() } as unknown as Window;
      const openSpy = vi.spyOn(window, 'open').mockReturnValue(popup);
      window.amplifyConfig = chainConfig;
      mockFetchAuthSession.mockResolvedValue(
        sessionWith({ 'cognito:username': 'u', 'cognito:groups': ['CSP_ADMIN'], email: 'u@x' }),
      );

      const { getCtx } = await renderAndSettle();

      vi.useFakeTimers();
      try {
        await act(async () => {
          const pending = getCtx()?.signOut();
          await vi.advanceTimersByTimeAsync(0);

          // Chain navigation is held while the popup completes the idir logout.
          expect(openSpy).toHaveBeenCalledWith(
            'https://test.loginproxy.gov.bc.ca/auth/realms/idir/protocol/openid-connect/logout',
            'csp-idir-realm-logout',
            expect.stringContaining('popup'),
          );
          expect(assignMock).not.toHaveBeenCalled();
          expect(popup.close).not.toHaveBeenCalled();

          await vi.advanceTimersByTimeAsync(IDIR_REALM_LOGOUT_GRACE_MS);
          await pending;
        });
      } finally {
        vi.useRealTimers();
      }

      expect(popup.close).toHaveBeenCalledTimes(1);
      expect(assignMock).toHaveBeenCalledWith(
        expect.stringMatching(/^https:\/\/logontest7\.gov\.bc\.ca\/clp-cgi\/logoff\.cgi/),
      );
    });

    it('skips the idir-realm popup for a BCeID user and navigates the chain directly', async () => {
      const openSpy = vi.spyOn(window, 'open');
      window.amplifyConfig = chainConfig;
      mockFetchAuthSession.mockResolvedValue(
        sessionWith({
          'cognito:username': 'u',
          'cognito:groups': ['CSP_ADMIN'],
          email: 'u@x',
          'custom:idp_name': 'bceidbusiness',
        }),
      );

      const { getCtx } = await renderAndSettle();
      expect(getCtx()?.user?.idpProvider).toBe('BCEID');

      await act(async () => {
        await getCtx()?.signOut();
      });

      expect(openSpy).not.toHaveBeenCalled();
      expect(assignMock).toHaveBeenCalledWith(
        expect.stringMatching(/^https:\/\/logontest7\.gov\.bc\.ca\/clp-cgi\/logoff\.cgi/),
      );
    });

    it('falls back to Amplify signOut when chain config is incomplete', async () => {
      window.amplifyConfig = { ...chainConfig, logoutKeycloakUrl: undefined };
      mockFetchAuthSession.mockResolvedValue(
        sessionWith({ 'cognito:username': 'u', 'cognito:groups': ['CSP_ADMIN'], email: 'u@x' }),
      );

      const { getCtx } = await renderAndSettle();

      await act(async () => {
        await getCtx()?.signOut();
      });

      expect(assignMock).not.toHaveBeenCalled();
      expect(mockSignOut).toHaveBeenCalledTimes(1);
      expect(getCtx()?.user).toBeNull();
    });
  });
});
