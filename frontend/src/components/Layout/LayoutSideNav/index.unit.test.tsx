import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useAuth } from '@/context/auth/useAuth';
import { useLayout } from '@/context/layout/useLayout';
import { ROUTES } from '@/routes/routePaths';

import { LayoutSideNav } from './index';

import type { Role } from '@/context/auth/permissions';

vi.mock('@/context/auth/useAuth', () => ({ useAuth: vi.fn() }));
vi.mock('@/context/layout/useLayout', () => ({ useLayout: vi.fn() }));

describe('LayoutSideNav', () => {
  const arrange = (initialPath = '/', privileges: Role[] = ['ADMIN']) => {
    vi.mocked(useAuth).mockReturnValue({
      user: { username: 'u', email: '', roles: [], privileges, forestClientNumbers: [] },
    } as unknown as ReturnType<typeof useAuth>);
    vi.mocked(useLayout).mockReturnValue({ isSideNavExpanded: true, toggleSideNav: vi.fn() } as unknown as ReturnType<
      typeof useLayout
    >);
    return render(
      <MemoryRouter initialEntries={[initialPath]}>
        <LayoutSideNav />
      </MemoryRouter>,
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the top-level navigation links with their routes', () => {
    arrange();

    // Exact names: report links like "Invoice Print Out" also contain these words.
    expect(screen.getByRole('link', { name: 'Search' })).toHaveAttribute('href', ROUTES.SEARCH);
    expect(screen.getByRole('link', { name: 'Inbox' })).toHaveAttribute('href', ROUTES.INBOX);
    expect(screen.getByRole('link', { name: 'Invoice' })).toHaveAttribute('href', ROUTES.INVOICE);
  });

  it('renders grouped links under their group title', () => {
    arrange();

    expect(screen.getByText('Submissions')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /submission history/i })).toHaveAttribute(
      'href',
      ROUTES.SUBMISSION_HISTORY,
    );
  });

  it('marks the link matching the current location as the current page', () => {
    arrange(ROUTES.SEARCH);

    // Carbon's SideNavLink signals the active item via its --current modifier class.
    expect(screen.getByRole('link', { name: 'Search' })).toHaveClass('cds--side-nav__link--current');
    expect(screen.getByRole('link', { name: 'Inbox' })).not.toHaveClass('cds--side-nav__link--current');
  });

  it.each(['ADMIN', 'APPROVE', 'VIEW'] as Role[])('shows %s every page, as before', (role) => {
    arrange('/', [role]);

    for (const name of ['Search', 'Inbox', 'Invoice', 'Flat price conversion', 'Sort code', 'Upload Submission']) {
      expect(screen.getByRole('link', { name })).toBeInTheDocument();
    }
    expect(screen.getByText('Reports')).toBeInTheDocument();
    expect(screen.getByText('Table maintenance')).toBeInTheDocument();
  });

  it('shows a Business BCeID submitter only Upload Submission and Submission History', () => {
    arrange('/', ['SUBMITTER']);

    expect(screen.getAllByRole('link').map((link) => link.textContent)).toEqual([
      'Upload Submission',
      'Submission History',
    ]);
    // Groups left with nothing the user may open are dropped.
    expect(screen.queryByText('Reports')).not.toBeInTheDocument();
    expect(screen.queryByText('Table maintenance')).not.toBeInTheDocument();
    expect(screen.getByText('Submissions')).toBeInTheDocument();
  });

  it('shows nothing to a user without a CSP role', () => {
    arrange('/', []);

    expect(screen.queryAllByRole('link')).toHaveLength(0);
  });
});
