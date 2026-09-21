import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import App from './App';

// Force the mock auth path so the full provider tree renders without Cognito,
// then report an anonymous visitor — MockAuthProvider is always signed in, and
// the welcome screen sends an authenticated visitor straight into the app.
vi.mock('@/env', () => ({ env: { mockUser: true } }));

const { mockUseAuth } = vi.hoisted(() => ({ mockUseAuth: vi.fn() }));
vi.mock('@/context/auth/useAuth', () => ({ useAuth: () => mockUseAuth() }));

const ANONYMOUS = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
  isSigningOut: false,
  signIn: vi.fn(),
  signOut: vi.fn(),
};

// Protected routes only render for a signed-in visitor, and the shell reads
// `user` for the profile panel and permission checks.
const SIGNED_IN = {
  ...ANONYMOUS,
  isAuthenticated: true,
  user: { username: 'jsmith', email: 'j.smith@gov.bc.ca', roles: ['CSP_ADMIN'], privileges: ['ADMIN'] },
};

const useSubmissionDetailQuery = vi.fn();
vi.mock('@/services/submissionHistory.service', () => ({
  useSubmissionDetailQuery: (submissionId: string | undefined) => useSubmissionDetailQuery(submissionId),
}));

describe('App', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(ANONYMOUS);
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

  // Guards the wiring between the route's param name and the page's useParams:
  // a mismatch leaves the id undefined and the detail page silently empty.
  it('passes the submission history url segment through to the detail lookup', async () => {
    mockUseAuth.mockReturnValue(SIGNED_IN);
    useSubmissionDetailQuery.mockReturnValue({ data: undefined, isLoading: true, isError: false, error: null });
    window.history.pushState({}, '', '/submission-history/9001');

    render(<App />);

    await waitFor(() => expect(useSubmissionDetailQuery).toHaveBeenCalledWith('9001'));
  });
});
