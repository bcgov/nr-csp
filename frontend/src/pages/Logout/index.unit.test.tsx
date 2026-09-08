import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { LogoutPage } from './index';

const navigate = vi.fn();

vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router');
  return { ...actual, useNavigate: () => navigate };
});
// The header is covered by its own tests; stub it so this one stays focused on
// the logout content.
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
  });

  it('shows the signed-out heading and message', () => {
    arrange();

    expect(screen.getByRole('heading', { name: /you’ve successfully logged out/i })).toBeInTheDocument();
    expect(screen.getByText(/you have been securely logged out of csp\./i)).toBeInTheDocument();
    expect(screen.getByText(/to continue working please sign back in\./i)).toBeInTheDocument();
  });

  it('keeps the illustration out of the accessibility tree', () => {
    const { container } = arrange();

    // Purely decorative, and it swaps per theme, so it is a CSS background
    // rather than an <img> — it must expose no role and no alt text.
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
