import { fireEvent, render, screen } from '@testing-library/react';
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
    vi.mocked(useLayout).mockReturnValue({
      isSideNavExpanded: true,
      toggleSideNav: vi.fn(),
      closeMobileSideNav: vi.fn(),
    } as unknown as ReturnType<typeof useLayout>);
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

    const submissions = screen.getByRole('button', { name: 'Submissions', expanded: false });
    fireEvent.click(submissions);
    expect(submissions).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('link', { name: /submission history/i })).toHaveAttribute(
      'href',
      ROUTES.SUBMISSION_HISTORY,
    );
  });

  it('marks the link matching the current location as the current page', () => {
    arrange(ROUTES.SEARCH);

    expect(screen.getByRole('link', { name: 'Search' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Inbox' })).not.toHaveAttribute('aria-current', 'page');
  });

  it('opens the Submissions group for a submission detail deep link', () => {
    arrange(`${ROUTES.SUBMISSION_HISTORY}/123`);
    expect(screen.getByRole('button', { name: 'Submissions', expanded: true })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Submission History' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('button', { name: 'Reports', expanded: false })).toBeInTheDocument();
  });

  it.each(['ADMIN', 'APPROVE', 'VIEW'] as Role[])('shows %s every page, as before', (role) => {
    arrange('/', [role]);

    for (const group of ['Table maintenance', 'Submissions']) {
      fireEvent.click(screen.getByRole('button', { name: group }));
    }
    for (const name of ['Search', 'Inbox', 'Invoice', 'Flat price conversion', 'Sort code', 'Upload Submission']) {
      expect(screen.getByRole('link', { name })).toBeInTheDocument();
    }
    expect(screen.getByText('Reports')).toBeInTheDocument();
    expect(screen.getByText('Table maintenance')).toBeInTheDocument();
  });

  it('shows a Business BCeID submitter only Upload Submission and Submission History', () => {
    arrange(ROUTES.UPLOAD_SUBMISSION, ['SUBMITTER']);
    expect(screen.getByRole('button', { name: 'Submissions', expanded: true })).toBeInTheDocument();

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
