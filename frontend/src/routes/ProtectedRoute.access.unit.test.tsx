import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as useAuthModule from '@/context/auth/useAuth';

import { ProtectedRoute } from './ProtectedRoute';
import { ROUTES } from './routePaths';

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/components/core/LoadingScreen', () => ({
  LoadingScreen: () => <div data-testid="loading">Loading</div>,
}));

const mockUseAuth = useAuthModule.useAuth as ReturnType<typeof vi.fn>;

function renderRoute({ bceidAllowed }: { bceidAllowed?: boolean } = {}) {
  return render(
    <MemoryRouter initialEntries={['/private']}>
      <Routes>
        <Route path={ROUTES.LOGIN} element={<div>Login Page</div>} />
        <Route path={ROUTES.UPLOAD_SUBMISSION} element={<div>Upload Submission Page</div>} />
        <Route
          path="/private"
          element={<ProtectedRoute bceidAllowed={bceidAllowed}>{<div>Private Content</div>}</ProtectedRoute>}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute — auth states', () => {
  beforeEach(() => {
    // No OAuth callback params, so the redirect guard does not short-circuit.
    vi.stubGlobal('location', { search: '' });
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.unstubAllGlobals();
  });

  it('shows the loading screen while auth is loading', () => {
    mockUseAuth.mockReturnValue({ user: null, isAuthenticated: false, isLoading: true, isSigningOut: false });

    renderRoute();

    expect(screen.getByTestId('loading')).toBeInTheDocument();
    expect(screen.queryByText('Private Content')).not.toBeInTheDocument();
  });

  it('shows the loading screen while signing out', () => {
    mockUseAuth.mockReturnValue({ user: null, isAuthenticated: false, isLoading: false, isSigningOut: true });

    renderRoute();

    expect(screen.getByTestId('loading')).toBeInTheDocument();
    expect(screen.queryByText('Private Content')).not.toBeInTheDocument();
  });

  it('redirects to /login when unauthenticated', () => {
    mockUseAuth.mockReturnValue({ user: null, isAuthenticated: false, isLoading: false, isSigningOut: false });

    renderRoute();

    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Private Content')).not.toBeInTheDocument();
  });

  it('renders children when authenticated as IDIR', () => {
    mockUseAuth.mockReturnValue({
      user: { idpProvider: 'IDIR' },
      isAuthenticated: true,
      isLoading: false,
      isSigningOut: false,
    });

    renderRoute();

    expect(screen.getByText('Private Content')).toBeInTheDocument();
    expect(screen.queryByTestId('loading')).not.toBeInTheDocument();
  });

  it('redirects a BCeID user to Upload Submission when the route is not bceidAllowed', () => {
    mockUseAuth.mockReturnValue({
      user: { idpProvider: 'BCEID' },
      isAuthenticated: true,
      isLoading: false,
      isSigningOut: false,
    });

    renderRoute();

    expect(screen.getByText('Upload Submission Page')).toBeInTheDocument();
    expect(screen.queryByText('Private Content')).not.toBeInTheDocument();
  });

  it('renders children for a BCeID user when the route is bceidAllowed', () => {
    mockUseAuth.mockReturnValue({
      user: { idpProvider: 'BCEID' },
      isAuthenticated: true,
      isLoading: false,
      isSigningOut: false,
    });

    renderRoute({ bceidAllowed: true });

    expect(screen.getByText('Private Content')).toBeInTheDocument();
  });
});
