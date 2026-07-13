import { act, render, screen, waitFor } from '@testing-library/react';
import { fetchAuthSession, signInWithRedirect, signOut } from 'aws-amplify/auth';
import { Hub } from 'aws-amplify/utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthContext } from './AuthContext';
import { RealAuthProvider } from './RealAuthProvider';
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
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({ 'cognito:username': 'u', email: 'u@example.com' }),
    );

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
    mockFetchAuthSession.mockResolvedValue(
      sessionWith({ 'cognito:username': 'u', email: 'u@example.com' }),
    );

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
    mockFetchAuthSession.mockResolvedValueOnce(
      sessionWith({ 'cognito:username': 'u', email: 'u@x' }),
    );
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

  it('signIn redirects with the idp name from window.amplifyConfig', async () => {
    window.amplifyConfig = {
      appEnv: 'test',
      idpName: 'TEST-IDIR',
      region: 'ca-central-1',
      userPoolId: 'pool',
      userPoolClientId: 'client',
      cognitoDomain: 'auth.example.com',
      redirectSignIn: 'https://example.com/',
      redirectSignOut: 'https://example.com/logout',
    };
    mockFetchAuthSession.mockResolvedValue(emptySession());

    const { getCtx } = await renderAndSettle();
    await getCtx()?.signIn();

    expect(mockSignInWithRedirect).toHaveBeenCalledWith({ provider: { custom: 'TEST-IDIR' } });
  });

  it('signIn falls back to DEV-IDIR when window.amplifyConfig is not set', async () => {
    window.amplifyConfig = undefined;
    mockFetchAuthSession.mockResolvedValue(emptySession());

    const { getCtx } = await renderAndSettle();
    await getCtx()?.signIn();

    expect(mockSignInWithRedirect).toHaveBeenCalledWith({ provider: { custom: 'DEV-IDIR' } });
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
});
