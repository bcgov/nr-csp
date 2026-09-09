import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { setSignOutReason } from '@/context/auth/signOutReason';

import { LogoutPage } from './index';

const navigate = vi.fn();

vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router');
  return { ...actual, useNavigate: () => navigate };
});
// The header is covered by its own tests; stub it so this one stays focused on
// the signed-out content.
vi.mock('@/components/Layout/LayoutHeader/LayoutHeader', () => ({
  LayoutHeader: () => <header />,
}));

describe('LogoutPage', () => {
  const arrange = () =>
    render(
      <MemoryRouter>
        <LogoutPage />
      </MemoryRouter>,
    );

  beforeEach(() => {
    vi.clearAllMocks();
    window.sessionStorage.clear();
  });

  describe('after a deliberate sign-out', () => {
    it('shows the signed-out heading and message', () => {
      arrange();

      expect(screen.getByRole('heading', { name: /you’ve successfully logged out/i })).toBeInTheDocument();
      expect(screen.getByText(/you have been securely logged out of csp\./i)).toBeInTheDocument();
      expect(screen.getByText(/to continue working please sign back in\./i)).toBeInTheDocument();
    });

    it('shows the signed-out illustration', () => {
      const { container } = arrange();

      expect(container.querySelector('.logout-page--signed-out')).toBeInTheDocument();
      expect(container.querySelector('.logout-page--expired')).not.toBeInTheDocument();
    });
  });

  describe('after a session timeout', () => {
    beforeEach(() => {
      setSignOutReason('timeout');
    });

    it('explains that the session ended rather than that the user signed out', () => {
      arrange();

      expect(screen.getByRole('heading', { name: /you’ve been signed out/i })).toBeInTheDocument();
      expect(
        screen.getByText(/your session has ended and you’ve been securely logged out of csp\./i),
      ).toBeInTheDocument();
      expect(screen.getByText(/to continue working please sign back in\./i)).toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: /successfully logged out/i })).not.toBeInTheDocument();
    });

    it('shows the expired-session illustration', () => {
      const { container } = arrange();

      expect(container.querySelector('.logout-page--expired')).toBeInTheDocument();
      expect(container.querySelector('.logout-page--signed-out')).not.toBeInTheDocument();
    });

    it('does not repeat the timeout wording on a later visit', () => {
      arrange().unmount();

      arrange();

      expect(screen.getByRole('heading', { name: /you’ve successfully logged out/i })).toBeInTheDocument();
    });
  });

  it('keeps the illustration out of the accessibility tree', () => {
    const { container } = arrange();

    // Purely decorative, and it swaps per theme and per variant, so it is a CSS
    // background rather than an <img> — it must expose no role and no alt text.
    expect(container.querySelector('.logout-page__illustration')).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('sends the user home, which re-triggers sign-in', async () => {
    const user = userEvent.setup();
    arrange();

    await user.click(screen.getByRole('button', { name: /back to home/i }));

    expect(navigate).toHaveBeenCalledWith('/');
  });
});
