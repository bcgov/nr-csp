import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, it, expect, vi } from 'vitest';

import { LayoutSideNav } from './index';

vi.mock('@/context/layout/useLayout', () => ({
  useLayout: () => ({ isSideNavExpanded: true, closeMobileSideNav: vi.fn() }),
}));

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: () => ({ user: { username: 'u', email: '', roles: [], privileges: ['ADMIN'], forestClientNumbers: [] } }),
}));

// Controlled navigation tree so the test isn't coupled to the real menu (paths must
// still be real routes: the side nav shows only what the user may open).
vi.mock('@/routes/navigation', () => ({
  NAVIGATION_ITEMS: [
    { name: 'Dashboard', path: '/search' },
    { name: 'Reports', children: [{ name: 'R06 Printout', path: '/reports/r06-invoice-print-out' }] },
  ],
}));

const renderAt = (path: string) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <LayoutSideNav />
    </MemoryRouter>,
  );

describe('LayoutSideNav', () => {
  it('renders top-level links, group titles, and nested links', () => {
    renderAt('/search');
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Reports', expanded: false }));
    expect(screen.getByRole('button', { name: 'Reports', expanded: true })).toBeInTheDocument();
    expect(screen.getByText('R06 Printout')).toBeInTheDocument();
  });

  it('renders links as anchors pointing at their path', () => {
    renderAt('/search');
    fireEvent.click(screen.getByRole('button', { name: 'Reports' }));
    const link = screen.getByRole('link', { name: 'R06 Printout' });
    expect(link).toHaveAttribute('href', '/reports/r06-invoice-print-out');
  });

  it('marks the link matching the current route as active', () => {
    renderAt('/reports/r06-invoice-print-out');
    const active = screen.getByText('R06 Printout').closest('a');
    expect(screen.getByRole('button', { name: 'Reports', expanded: true })).toBeInTheDocument();
    expect(active).toHaveAttribute('aria-current', 'page');
  });
});
