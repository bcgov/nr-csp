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
    idirUsername: 'JDOE',
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

  it('shows the name, role, IDIR, email and avatar initials of the signed-in user', () => {
    arrange();
    expect(screen.getByText('Jane Doe')).toBeInTheDocument();
    expect(screen.getByText('Role: Admin')).toBeInTheDocument();
    expect(screen.getByText('IDIR: JDOE')).toBeInTheDocument();
    expect(screen.getByText('jane.doe@gov.bc.ca')).toBeInTheDocument();
    expect(screen.getByTestId('avatar-initials')).toHaveTextContent('JD');
  });

  it('shows the highest-precedence role when the user holds several', () => {
    arrange({ ...baseUser, privileges: ['VIEW', 'ADMIN'] });
    expect(screen.getByText('Role: Admin')).toBeInTheDocument();
  });

  it('maps each role constant to its label', () => {
    arrange({ ...baseUser, privileges: ['APPROVE'] });
    expect(screen.getByText('Role: Approver')).toBeInTheDocument();
  });

  it('omits the Role line when the user has no privileges', () => {
    arrange({ ...baseUser, privileges: [] });
    expect(screen.queryByText(/^Role:/)).not.toBeInTheDocument();
  });

  it('omits the IDIR line when there is no IDIR username', () => {
    arrange({ ...baseUser, idirUsername: undefined });
    expect(screen.queryByText(/^IDIR:/)).not.toBeInTheDocument();
  });

  it('falls back to the username when there is no display name', () => {
    arrange({ ...baseUser, displayName: undefined });
    expect(screen.getByText('jdoe')).toBeInTheDocument();
    expect(screen.queryByText('Jane Doe')).not.toBeInTheDocument();
  });

  it('renders without crashing when there is no user', () => {
    const { container } = arrange(null);
    expect(container.querySelector('.header-panel-profile-name')).toBeEmptyDOMElement();
    expect(screen.queryByText(/^Role:/)).not.toBeInTheDocument();
    expect(screen.queryByText(/^IDIR:/)).not.toBeInTheDocument();
  });

  it('logs the user out when the Log out row is clicked', () => {
    arrange();
    fireEvent.click(screen.getByRole('button', { name: /log out/i }));
    expect(signOut).toHaveBeenCalledTimes(1);
  });
});
