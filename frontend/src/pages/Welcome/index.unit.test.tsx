import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as useAuthModule from '@/context/auth/useAuth';
import { NotificationProvider } from '@/context/notification/NotificationProvider';

import { WelcomePage } from './index';

vi.mock('@/context/auth/useAuth', () => ({ useAuth: vi.fn() }));
vi.mock('@/components/core/LoadingScreen', () => ({
  LoadingScreen: () => <div>Loading</div>,
}));

const mockUseAuth = useAuthModule.useAuth as ReturnType<typeof vi.fn>;
const signIn = vi.fn();

/** Renders at '/' with a stand-in for the app's authenticated home. */
const arrange = () =>
  render(
    <NotificationProvider>
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<WelcomePage />} />
          <Route path="/search" element={<div>Search page</div>} />
        </Routes>
      </MemoryRouter>
    </NotificationProvider>,
  );

describe('WelcomePage', () => {
  beforeEach(() => {
    vi.stubGlobal('location', { search: '' });
    mockUseAuth.mockReturnValue({ isAuthenticated: false, isLoading: false, signIn });
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.unstubAllGlobals();
  });

  it('offers both sign-in options to an anonymous visitor', () => {
    arrange();

    expect(screen.getByRole('heading', { name: 'Welcome to CSP' })).toBeInTheDocument();
    expect(screen.getByText('Coast Selling Price System')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log in with idir/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log in with business bceid/i })).toBeInTheDocument();
  });

  it('exposes the BC Gov logo but keeps the photo out of the accessibility tree', () => {
    const { container } = arrange();

    expect(screen.getAllByRole('img')).toHaveLength(1);
    expect(screen.getByAltText('Government of British Columbia')).toBeInTheDocument();
    expect(container.querySelector('.welcome-page__photo img')).toHaveAttribute('alt', '');
  });

  it('starts the IDIR sign-in when the primary button is pressed', async () => {
    const user = userEvent.setup();
    arrange();

    await user.click(screen.getByRole('button', { name: /log in with idir/i }));

    expect(signIn).toHaveBeenCalledTimes(1);
  });

  it('reports a sign-in that fails before it leaves the page', async () => {
    const user = userEvent.setup();
    signIn.mockRejectedValueOnce(new Error('UserAlreadyAuthenticatedException'));
    arrange();

    await user.click(screen.getByRole('button', { name: /log in with idir/i }));

    expect(await screen.findByText(/could not start the idir login/i)).toBeInTheDocument();
  });

  it('says Business BCeID is not wired up yet instead of starting a sign-in', async () => {
    const user = userEvent.setup();
    arrange();

    await user.click(screen.getByRole('button', { name: /log in with business bceid/i }));

    expect(await screen.findByText(/business bceid login is not available yet/i)).toBeInTheDocument();
    expect(signIn).not.toHaveBeenCalled();
  });

  it('sends an already-authenticated visitor into the app', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true, isLoading: false, signIn });

    arrange();

    expect(screen.getByText('Search page')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Welcome to CSP' })).not.toBeInTheDocument();
  });

  it('waits rather than showing the sign-in choice while the session is still loading', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false, isLoading: true, signIn });

    arrange();

    expect(screen.getByText('Loading')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Welcome to CSP' })).not.toBeInTheDocument();
  });

  // Cognito redirects back to '/', and Amplify reports isLoading: false before
  // the signedIn event lands — without the guard the welcome screen would flash
  // at someone who has just authenticated.
  it('does not flash the sign-in choice mid-OAuth-callback', () => {
    vi.stubGlobal('location', { search: '?code=abc&state=xyz' });

    arrange();

    expect(screen.getByText('Loading')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /log in with idir/i })).not.toBeInTheDocument();
  });

  // Only Amplify's success path clears the params, so without a bounded wait a
  // failed exchange would leave the loading screen up for good — and still up
  // after a reload.
  it('falls back to the sign-in choice when the callback never completes', async () => {
    vi.useFakeTimers();
    vi.stubGlobal('location', { search: '?code=abc&state=xyz', pathname: '/' });
    const replaceState = vi.fn();
    vi.stubGlobal('history', { replaceState });

    try {
      arrange();
      expect(screen.getByText('Loading')).toBeInTheDocument();

      await act(async () => {
        vi.advanceTimersByTime(15_000);
      });

      expect(screen.getByRole('button', { name: /log in with idir/i })).toBeInTheDocument();
      expect(replaceState).toHaveBeenCalledWith(null, '', '/');
    } finally {
      vi.useRealTimers();
    }
  });
});
