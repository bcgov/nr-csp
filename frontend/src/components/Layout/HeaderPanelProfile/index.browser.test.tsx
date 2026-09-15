import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';

import { useAuth } from '@/context/auth/useAuth';

import { HeaderPanelProfile } from './index';

import type { AuthContextValue, AuthUser } from '@/context/auth/types';

const mockSignOut = vi.fn();
const mockUser: AuthUser = {
  username: 'jdoe',
  idirUsername: 'JDOE',
  displayName: 'Jane Doe',
  email: 'jane@example.com',
  roles: ['CSP_ADMIN'],
  privileges: ['ADMIN'],
};

const makeAuthValue = (user: AuthUser): AuthContextValue => ({
  user,
  isAuthenticated: true,
  isLoading: false,
  isSigningOut: false,
  signIn: vi.fn(),
  signOut: mockSignOut,
});

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

describe('HeaderPanelProfile', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue(makeAuthValue(mockUser));
  });

  it('renders the display name', () => {
    render(<HeaderPanelProfile />);
    expect(screen.getByText('Jane Doe')).toBeInTheDocument();
  });

  it('renders the email', () => {
    render(<HeaderPanelProfile />);
    expect(screen.getByText('jane@example.com')).toBeInTheDocument();
  });

  it('renders the role and IDIR', () => {
    render(<HeaderPanelProfile />);
    expect(screen.getByText('Role: Admin')).toBeInTheDocument();
    expect(screen.getByText('IDIR: JDOE')).toBeInTheDocument();
  });

  it('calls signOut when Log out is clicked', () => {
    render(<HeaderPanelProfile />);
    fireEvent.click(screen.getByText('Log out'));
    expect(mockSignOut).toHaveBeenCalled();
  });

  it('falls back to the email local part, never the raw username, when displayName is absent', () => {
    vi.mocked(useAuth).mockReturnValue(
      makeAuthValue({ username: 'dev-idir_abc@idir', email: 'jsmith@gov.bc.ca', roles: [], privileges: [] }),
    );
    render(<HeaderPanelProfile />);
    expect(screen.getByText('jsmith')).toBeInTheDocument();
    expect(screen.queryByText('dev-idir_abc@idir')).not.toBeInTheDocument();
  });
});
