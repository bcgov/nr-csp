import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useAuth } from '@/context/auth/useAuth';

import { LoginPage } from './index';

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

describe('LoginPage', () => {
  const signIn = vi.fn().mockResolvedValue(undefined);

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({ signIn } as unknown as ReturnType<typeof useAuth>);
  });

  it('shows both IDIR and BCeID login buttons', () => {
    render(<LoginPage />);

    expect(screen.getByRole('button', { name: /log in with idir/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log in with bceid/i })).toBeInTheDocument();
  });

  it('signs in with IDIR when the IDIR button is clicked', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.click(screen.getByRole('button', { name: /log in with idir/i }));

    expect(signIn).toHaveBeenCalledWith('IDIR');
  });

  it('signs in with BCEID when the BCeID button is clicked', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.click(screen.getByRole('button', { name: /log in with bceid/i }));

    expect(signIn).toHaveBeenCalledWith('BCEID');
  });
});
