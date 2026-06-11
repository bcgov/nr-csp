import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';

import { useAuth } from '@/context/auth/useAuth';

import { HeaderPanelProfile } from './index';

const mockSignOut = vi.fn();
const mockUser = { username: 'jdoe', displayName: 'Jane Doe', email: 'jane@example.com', roles: [] };

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

describe('HeaderPanelProfile', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({ signOut: mockSignOut, user: mockUser });
  });

  it('renders the display name', () => {
    render(<HeaderPanelProfile />);
    expect(screen.getByText('Jane Doe')).toBeInTheDocument();
  });

  it('renders the email', () => {
    render(<HeaderPanelProfile />);
    expect(screen.getByText('jane@example.com')).toBeInTheDocument();
  });

  it('calls signOut when Sign out is clicked', () => {
    render(<HeaderPanelProfile />);
    fireEvent.click(screen.getByText('Sign out'));
    expect(mockSignOut).toHaveBeenCalled();
  });

  it('falls back to username when displayName is absent', () => {
    vi.mocked(useAuth).mockReturnValue({
      signOut: mockSignOut,
      user: { username: 'fallback-user', email: 'x@x.com', roles: [] },
    });
    render(<HeaderPanelProfile />);
    expect(screen.getByText('fallback-user')).toBeInTheDocument();
  });
});
