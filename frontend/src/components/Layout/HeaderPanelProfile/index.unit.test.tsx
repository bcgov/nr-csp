import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { AuthUser } from '@/context/auth/types';
import { useAuth } from '@/context/auth/useAuth';

import { HeaderPanelProfile } from './index';

vi.mock('@/context/auth/useAuth', () => ({ useAuth: vi.fn() }));

describe('HeaderPanelProfile', () => {
  const signOut = vi.fn();

  const baseUser: AuthUser = {
    username: 'jdoe',
    displayName: 'Jane Doe',
    email: 'jane.doe@gov.bc.ca',
    roles: ['CSP_SUBMITTER'],
    privileges: ['ADMIN'],
  };

  const arrange = (user: AuthUser | null = baseUser) => {
    vi.mocked(useAuth).mockReturnValue({ user, signOut } as unknown as ReturnType<typeof useAuth>);
    return render(<HeaderPanelProfile />);
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows the display name and email of the signed-in user', () => {
    arrange();
    expect(screen.getByText('Jane Doe')).toBeInTheDocument();
    expect(screen.getByText('jane.doe@gov.bc.ca')).toBeInTheDocument();
  });

  it('falls back to the username when there is no display name', () => {
    arrange({ ...baseUser, displayName: undefined });
    expect(screen.getByText('jdoe')).toBeInTheDocument();
    expect(screen.queryByText('Jane Doe')).not.toBeInTheDocument();
  });

  it('renders without crashing when there is no user', () => {
    const { container } = arrange(null);
    expect(container.querySelector('.header-panel-profile-name')).toBeEmptyDOMElement();
    expect(container.querySelector('.header-panel-profile-email')).toBeEmptyDOMElement();
  });

  it('signs the user out when the sign out button is clicked', () => {
    arrange();
    fireEvent.click(screen.getByRole('button', { name: /sign out/i }));
    expect(signOut).toHaveBeenCalledTimes(1);
  });
});
