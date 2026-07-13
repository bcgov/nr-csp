import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useAuth } from '@/context/auth/useAuth';
import { useLayout } from '@/context/layout/useLayout';

import { LayoutHeader } from './LayoutHeader';

vi.mock('@/context/auth/useAuth', () => ({ useAuth: vi.fn() }));
vi.mock('@/context/layout/useLayout', () => ({ useLayout: vi.fn() }));
// Children with their own providers/data needs are stubbed so this test stays
// focused on the header shell itself.
vi.mock('@/components/Layout/LayoutHeaderPanel', () => ({
  LayoutHeaderPanel: () => <div data-testid="header-panel" />,
}));
vi.mock('@/components/Layout/LayoutSideNav', () => ({
  LayoutSideNav: () => <nav aria-label="side navigation" />,
}));
vi.mock('@/components/Layout/ThemeToggle', () => ({
  ThemeToggle: () => <span data-testid="theme-toggle" />,
}));
vi.mock('./LayoutHeaderGlobalBar', () => ({
  default: () => <div data-testid="global-bar" />,
}));

describe('LayoutHeader', () => {
  const toggleSideNav = vi.fn();

  const arrange = ({ isAuthenticated = true, isSideNavExpanded = false } = {}) => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated } as unknown as ReturnType<typeof useAuth>);
    vi.mocked(useLayout).mockReturnValue({ isSideNavExpanded, toggleSideNav } as unknown as ReturnType<
      typeof useLayout
    >);
    return render(
      <MemoryRouter>
        <LayoutHeader />
      </MemoryRouter>,
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the app name as a link to the landing page', () => {
    arrange();

    const homeLink = screen.getByRole('link', { name: /coast selling application \(csp\)/i });
    expect(homeLink).toBeInTheDocument();
    expect(homeLink).toHaveAttribute('href', '/');
  });

  it('shows the menu button and side nav when authenticated', () => {
    arrange({ isAuthenticated: true });

    expect(screen.getByRole('button', { name: /open menu/i })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: /side navigation/i })).toBeInTheDocument();
  });

  it('hides the menu button and side nav when not authenticated', () => {
    arrange({ isAuthenticated: false });

    expect(screen.queryByRole('button', { name: /open menu/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: /side navigation/i })).not.toBeInTheDocument();
  });

  it('labels the menu button "Close menu" while the side nav is expanded', () => {
    arrange({ isSideNavExpanded: true });

    expect(screen.getByRole('button', { name: /close menu/i })).toBeInTheDocument();
  });

  it('toggles the side nav when the menu button is clicked', async () => {
    const user = userEvent.setup();
    arrange();

    await user.click(screen.getByRole('button', { name: /open menu/i }));

    expect(toggleSideNav).toHaveBeenCalledTimes(1);
  });
});
