import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import App from './App';

// Force the mock auth path so the full provider tree renders without Cognito.
vi.mock('@/env', () => ({ env: { mockUser: true } }));

const mockUseAuth = vi.fn();
vi.mock('@/context/auth/useAuth', () => ({ useAuth: () => mockUseAuth() }));

// Real pages pull in heavy data-fetching/services; stand in with plain markers
// so this file asserts purely on ProtectedRoute's routing decisions for the
// real App route tree — each page's own behavior has its own dedicated tests.
vi.mock('@/pages/Search', () => ({ SearchPage: () => <div>Search Page</div> }));
vi.mock('@/pages/UploadSubmission', () => ({ UploadSubmissionPage: () => <div>Upload Submission Page</div> }));
vi.mock('@/pages/SubmissionHistory', () => ({ SubmissionHistoryPage: () => <div>Submission History Page</div> }));

describe('App', () => {
  beforeEach(() => {
    // Anonymous visitor by default — MockAuthProvider (unused, since useAuth is
    // mocked directly) is always signed in, and the welcome screen sends an
    // authenticated visitor straight into the app.
    mockUseAuth.mockReturnValue({ isAuthenticated: false, isLoading: false, isSigningOut: false, signIn: vi.fn() });
  });

  it('renders the provider tree and serves the public welcome route', async () => {
    window.history.pushState({}, '', '/');

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Welcome to CSP' })).toBeInTheDocument();
  });

  // The federated logout chain's return URL is fixed at <app>/logout, so the
  // route has to keep resolving even though nothing renders there any more —
  // without it the post-sign-out landing would fall through to NotFound.
  it('bounces the post-sign-out /logout landing to the welcome screen', async () => {
    window.history.pushState({}, '', '/logout');

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Welcome to CSP' })).toBeInTheDocument();
    // `replace`, so Back doesn't return to a URL with nothing on it.
    expect(window.location.pathname).toBe('/');
  });

  // ProtectedRoute's own unit tests exercise `bceidAllowed` against a synthetic
  // route; these confirm the prop is actually wired onto the right real routes
  // in App.tsx itself, which nothing else here checks.
  describe('BCeID route restriction (real App routes)', () => {
    const bceidUser = {
      user: { idpProvider: 'BCEIDBUSINESS' },
      isAuthenticated: true,
      isLoading: false,
      isSigningOut: false,
      signIn: vi.fn(),
    };

    it('renders Upload Submission for a BCeID user', async () => {
      mockUseAuth.mockReturnValue(bceidUser);
      window.history.pushState({}, '', '/upload-submission');

      render(<App />);

      expect(await screen.findByText('Upload Submission Page')).toBeInTheDocument();
    });

    it('renders Submission History for a BCeID user', async () => {
      mockUseAuth.mockReturnValue(bceidUser);
      window.history.pushState({}, '', '/submission-history');

      render(<App />);

      expect(await screen.findByText('Submission History Page')).toBeInTheDocument();
    });

    it('redirects a BCeID user away from Search to Upload Submission', async () => {
      mockUseAuth.mockReturnValue(bceidUser);
      window.history.pushState({}, '', '/search');

      render(<App />);

      expect(await screen.findByText('Upload Submission Page')).toBeInTheDocument();
      expect(screen.queryByText('Search Page')).not.toBeInTheDocument();
    });

    it('renders Search for an IDIR user (unaffected)', async () => {
      mockUseAuth.mockReturnValue({
        user: { idpProvider: 'IDIR' },
        isAuthenticated: true,
        isLoading: false,
        isSigningOut: false,
        signIn: vi.fn(),
      });
      window.history.pushState({}, '', '/search');

      render(<App />);

      expect(await screen.findByText('Search Page')).toBeInTheDocument();
    });

    // A bceidAllowed route can't assume an anonymous deep-linker is IDIR the
    // way ProtectedRoute's default does for every other route.
    it('sends an anonymous deep-link to a bceidAllowed route to the welcome screen instead of forcing IDIR sign-in', async () => {
      const signIn = vi.fn();
      mockUseAuth.mockReturnValue({ user: null, isAuthenticated: false, isLoading: false, isSigningOut: false, signIn });
      window.history.pushState({}, '', '/upload-submission');

      render(<App />);

      expect(await screen.findByRole('heading', { name: 'Welcome to CSP' })).toBeInTheDocument();
      expect(signIn).not.toHaveBeenCalled();
    });
  });
});
