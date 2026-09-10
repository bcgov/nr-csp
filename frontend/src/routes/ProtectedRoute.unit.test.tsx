import { act, render } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { vi, describe, afterEach, it, expect } from 'vitest';

import * as useAuthModule from '@/context/auth/useAuth';
import { OAUTH_CALLBACK_TIMEOUT_MS } from '@/context/auth/useOauthCallbackPending';

import { ProtectedRoute } from './ProtectedRoute';

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/components/core/LoadingScreen', () => ({
  LoadingScreen: () => <div>Loading</div>,
}));

const mockUseAuth = useAuthModule.useAuth as ReturnType<typeof vi.fn>;

describe('ProtectedRoute — OAuth callback guard', () => {
  afterEach(() => {
    vi.clearAllMocks();
    vi.unstubAllGlobals();
  });

  // The params are only cleared by Amplify's success path, so before the wait
  // was bounded this sat on the loading screen for good: the effect returned
  // early every time and none of its deps could ever change.
  it('falls through to a fresh sign-in when the callback never completes', async () => {
    vi.useFakeTimers();
    vi.stubGlobal('location', { search: '?code=abc&state=xyz', pathname: '/search' });
    vi.stubGlobal('history', { replaceState: vi.fn() });

    const signIn = vi.fn();
    mockUseAuth.mockReturnValue({ isAuthenticated: false, isLoading: false, signIn });

    try {
      render(
        <MemoryRouter>
          <ProtectedRoute>
            <div>Private Content</div>
          </ProtectedRoute>
        </MemoryRouter>,
      );
      expect(signIn).not.toHaveBeenCalled();

      await act(async () => {
        vi.advanceTimersByTime(OAUTH_CALLBACK_TIMEOUT_MS);
      });

      expect(signIn).toHaveBeenCalledTimes(1);
    } finally {
      vi.useRealTimers();
    }
  });

  it('does not call signIn when code and state params are present', () => {
    vi.stubGlobal('location', { search: '?code=abc&state=xyz' });

    const signIn = vi.fn();
    mockUseAuth.mockReturnValue({ isAuthenticated: false, isLoading: false, signIn });

    render(
      <MemoryRouter>
        <ProtectedRoute>
          <div>Private Content</div>
        </ProtectedRoute>
      </MemoryRouter>,
    );

    expect(signIn).not.toHaveBeenCalled();
  });
});
