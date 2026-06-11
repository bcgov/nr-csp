import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import LayoutHeaderGlobalBar from './LayoutHeaderGlobalBar';

// Mutable, hoisted mock state so each test can flip env.mockUser /
// authentication without re-importing the module. A static import keeps the
// heavy Carbon module loaded once and lets React Testing Library auto-clean
// up between tests (the previous vi.doMock + vi.resetModules + dynamic-import
// approach reloaded Carbon per block, timed out on the first load, and leaked
// a rendered combobox into the next test). The component reads `env.mockUser`
// and `isAuthenticated` at render time, so mutating these before each render
// is enough.
const h = vi.hoisted(() => ({
  env: { mockUser: false },
  auth: { isAuthenticated: true },
}));

vi.mock('@/env', () => ({ env: h.env }));

vi.mock('@/context/layout/useLayout', () => ({
  useLayout: () => ({ toggleHeaderPanel: vi.fn(), isHeaderPanelOpen: false }),
}));

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: () => ({
    isAuthenticated: h.auth.isAuthenticated,
    user: { username: 'mock-user', email: 'mock@test.com', roles: [], privileges: ['ADMIN'] },
    isLoading: false,
    signIn: vi.fn(),
    signOut: vi.fn(),
  }),
}));

// Render the role switcher as a plain combobox without localStorage side-effects.
vi.mock('@/components/Layout/MockRoleSelector', () => ({
  MockRoleSelector: () => (
    <select aria-label="Mock user role">
      <option value="ADMIN">ADMIN</option>
    </select>
  ),
}));

describe('LayoutHeaderGlobalBar', () => {
  beforeEach(() => {
    h.env.mockUser = false;
    h.auth.isAuthenticated = true;
  });

  it('renders the user-settings button and no combobox when mock auth is off', () => {
    h.env.mockUser = false;
    render(<LayoutHeaderGlobalBar />);
    expect(screen.getByRole('button', { name: /user settings/i })).toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  it('renders the role combobox and no user-settings button when mock auth is on', () => {
    h.env.mockUser = true;
    render(<LayoutHeaderGlobalBar />);
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /user settings/i })).not.toBeInTheDocument();
  });

  it('renders nothing when not authenticated', () => {
    h.auth.isAuthenticated = false;
    const { container } = render(<LayoutHeaderGlobalBar />);
    expect(container.firstChild).toBeNull();
  });
});
