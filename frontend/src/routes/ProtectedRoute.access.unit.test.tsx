import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as useAuthModule from '@/context/auth/useAuth';

import { ROUTE_PAGES } from './access';
import { ProtectedRoute } from './ProtectedRoute';
import { ROUTES } from './routePaths';

import type { Role } from '@/context/auth/permissions';

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/components/core/LoadingScreen', () => ({
  LoadingScreen: () => <div data-testid="loading">Loading</div>,
}));

const mockUseAuth = useAuthModule.useAuth as ReturnType<typeof vi.fn>;

function renderRoute() {
  return render(
    <MemoryRouter>
      <ProtectedRoute>
        <div>Private Content</div>
      </ProtectedRoute>
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

  it('shows the loading screen and does not sign in while auth is loading', () => {
    const signIn = vi.fn();
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
      isSigningOut: false,
      signIn,
    });

    renderRoute();

    expect(screen.getByTestId('loading')).toBeInTheDocument();
    expect(screen.queryByText('Private Content')).not.toBeInTheDocument();
    expect(signIn).not.toHaveBeenCalled();
  });

  it('shows the loading screen and does not sign in while signing out', () => {
    const signIn = vi.fn();
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isLoading: false,
      isSigningOut: true,
      signIn,
    });

    renderRoute();

    expect(screen.getByTestId('loading')).toBeInTheDocument();
    expect(screen.queryByText('Private Content')).not.toBeInTheDocument();
    expect(signIn).not.toHaveBeenCalled();
  });

  it('renders children when authenticated with a CSP capability', () => {
    const signIn = vi.fn();
    mockUseAuth.mockReturnValue({
      user: { privileges: ['VIEW'] },
      isAuthenticated: true,
      isLoading: false,
      isSigningOut: false,
      signIn,
    });

    renderRoute();

    expect(screen.getByText('Private Content')).toBeInTheDocument();
    expect(screen.queryByTestId('loading')).not.toBeInTheDocument();
    expect(signIn).not.toHaveBeenCalled();
  });
  it('denies an authenticated account without a CSP grant', () => {
    mockUseAuth.mockReturnValue({
      user: { privileges: [] },
      isAuthenticated: true,
      isLoading: false,
      isSigningOut: false,
      signIn: vi.fn(),
      signOut: vi.fn(),
    });
    renderRoute();
    expect(screen.getByRole('heading', { name: 'No CSP access' })).toBeInTheDocument();
    expect(screen.queryByText('Private Content')).not.toBeInTheDocument();
  });
});

function LoginDestination() {
  return <div>{useLocation().state?.returnTo}</div>;
}

/** Renders `path` behind the real route guard, with stand-in pages. */
function renderAt(path: string) {
  const guarded = (route: keyof typeof ROUTE_PAGES) => (
    <ProtectedRoute page={ROUTE_PAGES[route]}>
      <div>Page {route}</div>
    </ProtectedRoute>
  );
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={ROUTES.LANDING} element={<LoginDestination />} />
        <Route
          path="*"
          element={
            <ProtectedRoute>
              <div>Not found</div>
            </ProtectedRoute>
          }
        />
        {(Object.keys(ROUTE_PAGES) as (keyof typeof ROUTE_PAGES)[]).map((route) => (
          <Route key={route} path={route} element={guarded(route)} />
        ))}
        <Route path={`${ROUTES.SUBMISSION_HISTORY}/:submissionId`} element={guarded(ROUTES.SUBMISSION_HISTORY)} />
      </Routes>
    </MemoryRouter>,
  );
}

const signedInAs = (privileges: Role[]) =>
  mockUseAuth.mockReturnValue({
    user: { privileges, forestClientNumbers: privileges.includes('SUBMITTER') ? ['00000001'] : [] },
    isAuthenticated: true,
    isLoading: false,
    isSigningOut: false,
    signIn: vi.fn(),
    signOut: vi.fn(),
  });

describe('ProtectedRoute — page access', () => {
  afterEach(() => {
    vi.clearAllMocks();
    vi.unstubAllGlobals();
  });

  const staffOnly = (Object.keys(ROUTE_PAGES) as string[]).filter(
    (route) => route !== ROUTES.UPLOAD_SUBMISSION && route !== ROUTES.SUBMISSION_HISTORY,
  );

  it.each(staffOnly)('sends a Business BCeID submitter from %s to Upload Submission', (route) => {
    signedInAs(['SUBMITTER']);
    renderAt(route);
    expect(screen.getByText(`Page ${ROUTES.UPLOAD_SUBMISSION}`)).toBeInTheDocument();
  });

  it.each([ROUTES.UPLOAD_SUBMISSION, ROUTES.SUBMISSION_HISTORY, `${ROUTES.SUBMISSION_HISTORY}/9001`])(
    'lets a submitter open %s',
    (path) => {
      signedInAs(['SUBMITTER']);
      renderAt(path);
      expect(screen.getByText(/^Page \//)).toBeInTheDocument();
      expect(screen.queryByText(`Page ${ROUTES.SEARCH}`)).not.toBeInTheDocument();
    },
  );

  it.each(['ADMIN', 'APPROVE', 'VIEW'] as Role[])('lets %s open every page', (role) => {
    for (const route of Object.keys(ROUTE_PAGES)) {
      signedInAs([role]);
      const { unmount } = renderAt(route);
      expect(screen.getByText(`Page ${route}`), route).toBeInTheDocument();
      unmount();
    }
  });

  it('shows No CSP access, not a page, to an account without a CSP role', () => {
    signedInAs([]);
    renderAt(ROUTES.UPLOAD_SUBMISSION);
    expect(screen.getByRole('heading', { name: 'No CSP access' })).toBeInTheDocument();
  });

  it.each([ROUTES.INBOX, '/home', '/submission-history/9001?tab=invoices#details'])(
    'redirects a signed-out visitor at %s to the welcome route with the requested destination',
    (path) => {
      const signIn = vi.fn();
      mockUseAuth.mockReturnValue({ isAuthenticated: false, isLoading: false, isSigningOut: false, signIn });
      window.history.replaceState(null, '', path);
      try {
        renderAt(path);
        expect(screen.getByText(path)).toBeInTheDocument();
        expect(signIn).not.toHaveBeenCalled();
      } finally {
        window.history.replaceState(null, '', '/');
      }
    },
  );
});
