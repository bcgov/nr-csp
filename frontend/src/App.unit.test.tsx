import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import App from './App';

// Force the mock auth path so the full provider tree renders without Cognito,
// then report an anonymous visitor — MockAuthProvider is always signed in, and
// the welcome screen sends an authenticated visitor straight into the app.
vi.mock('@/env', () => ({ env: { mockUser: true } }));
vi.mock('@/context/auth/useAuth', () => ({
  useAuth: () => ({ isAuthenticated: false, isLoading: false, isSigningOut: false, signIn: vi.fn() }),
}));

describe('App', () => {
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
});
