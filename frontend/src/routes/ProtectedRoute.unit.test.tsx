import { act, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
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

  it('redirects to the welcome page when the callback wait expires', async () => {
    vi.useFakeTimers();
    vi.stubGlobal('location', { search: '?code=abc&state=xyz', pathname: '/search' });
    vi.stubGlobal('history', { replaceState: vi.fn() });

    const signIn = vi.fn();
    mockUseAuth.mockReturnValue({ isAuthenticated: false, isLoading: false, signIn });

    try {
      render(
        <MemoryRouter initialEntries={['/search']}>
          <Routes>
            <Route path="/" element={<div>Welcome</div>} />
            <Route
              path="/search"
              element={
                <ProtectedRoute>
                  <div>Private Content</div>
                </ProtectedRoute>
              }
            />
          </Routes>
        </MemoryRouter>,
      );
      expect(screen.getByText('Loading')).toBeInTheDocument();

      await act(async () => {
        vi.advanceTimersByTime(OAUTH_CALLBACK_TIMEOUT_MS);
      });

      expect(screen.getByText('Welcome')).toBeInTheDocument();
      expect(signIn).not.toHaveBeenCalled();
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
