import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, it, expect, vi } from 'vitest';

import { useAuth } from '@/context/auth/useAuth';

import { LayoutSideNav } from './index';

vi.mock('@/context/layout/useLayout', () => ({
  useLayout: () => ({ isSideNavExpanded: true }),
}));

vi.mock('@/context/auth/useAuth', () => ({ useAuth: vi.fn() }));

// Controlled navigation tree so the test isn't coupled to the real routes.
vi.mock('@/routes/navigation', () => ({
  NAVIGATION_ITEMS: [
    { name: 'Dashboard', path: '/' },
    { name: 'Reports', children: [{ name: 'R06 Printout', path: '/r06' }] },
    { name: 'Submissions', children: [{ name: 'Upload Submission', path: '/upload-submission', bceidAllowed: true }] },
  ],
}));

const renderAt = (path: string, idpProvider: 'IDIR' | 'BCEID' = 'IDIR') => {
  vi.mocked(useAuth).mockReturnValue({ user: { idpProvider } } as unknown as ReturnType<typeof useAuth>);
  return render(
    <MemoryRouter initialEntries={[path]}>
      <LayoutSideNav />
    </MemoryRouter>,
  );
};

describe('LayoutSideNav', () => {
  it('renders top-level links, group titles, and nested links', () => {
    renderAt('/');
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Reports')).toBeInTheDocument();
    expect(screen.getByText('R06 Printout')).toBeInTheDocument();
  });

  it('renders links as anchors pointing at their path', () => {
    renderAt('/');
    const link = screen.getByText('R06 Printout').closest('a');
    expect(link).toHaveAttribute('href', '/r06');
  });

  it('marks the link matching the current route as active', () => {
    renderAt('/r06');
    const active = screen.getByText('R06 Printout').closest('a');
    expect(active).toHaveClass('cds--side-nav__link--current');
  });

  it('hides items without bceidAllowed for a BCeID user', () => {
    renderAt('/', 'BCEID');
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument();
    expect(screen.queryByText('Reports')).not.toBeInTheDocument();
    expect(screen.getByText('Upload Submission')).toBeInTheDocument();
  });
});
