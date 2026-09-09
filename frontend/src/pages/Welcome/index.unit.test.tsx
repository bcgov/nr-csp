import { render, screen } from '@testing-library/react';
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
    expect(container.querySelector('.welcome-page__photo')).toHaveAttribute('alt', '');
  });

  it('starts the IDIR sign-in when the primary button is pressed', async () => {
    const user = userEvent.setup();
    arrange();

    await user.click(screen.getByRole('button', { name: /log in with idir/i }));

    expect(signIn).toHaveBeenCalledTimes(1);
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
});
