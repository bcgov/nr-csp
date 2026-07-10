import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useAuth } from '@/context/auth/useAuth';

import { LogoutPage } from './index';

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

describe('LogoutPage', () => {
  const signIn = vi.fn().mockResolvedValue(undefined);

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({ signIn } as unknown as ReturnType<typeof useAuth>);
  });

  it('shows the signed-out heading and message', () => {
    render(<LogoutPage />);

    expect(screen.getByRole('heading', { name: /you have been signed out/i })).toBeInTheDocument();
    expect(screen.getByText(/thank you for using the coast selling application/i)).toBeInTheDocument();
  });

  it('offers a sign-in button that starts a new sign-in', async () => {
    const user = userEvent.setup();
    render(<LogoutPage />);

    await user.click(screen.getByRole('button', { name: /sign in again/i }));

    expect(signIn).toHaveBeenCalledTimes(1);
  });
});
